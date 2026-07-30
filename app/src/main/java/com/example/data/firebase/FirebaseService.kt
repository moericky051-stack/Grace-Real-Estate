package com.example.data.firebase

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.data.model.Property
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

class FirebaseService(private val context: Context) {

    private val auth: FirebaseAuth?
        get() = try {
            ensureFirebaseApp(context)
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            Log.e("FirebaseService", "FirebaseAuth getInstance failed: ${e.message}")
            null
        }

    private val firestore: FirebaseFirestore?
        get() = try {
            ensureFirebaseApp(context)
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.e("FirebaseService", "FirebaseFirestore getInstance failed: ${e.message}")
            null
        }

    private val storage: FirebaseStorage?
        get() = try {
            ensureFirebaseApp(context)
            FirebaseStorage.getInstance()
        } catch (e: Exception) {
            Log.e("FirebaseService", "FirebaseStorage getInstance failed: ${e.message}")
            null
        }

    companion object {
        @Volatile
        private var initialized = false

        fun ensureFirebaseApp(context: Context) {
            if (!initialized) {
                synchronized(this) {
                    if (!initialized) {
                        try {
                            val appContext = context.applicationContext ?: context
                            if (FirebaseApp.getApps(appContext).isEmpty()) {
                                FirebaseApp.initializeApp(appContext)
                            }
                            initialized = true
                        } catch (e: Exception) {
                            Log.e("FirebaseService", "FirebaseApp initialization exception: ${e.message}")
                        }
                    }
                }
            }
        }
    }

    // ==========================================
    // AUTHENTICATION
    // ==========================================

    val currentUser: FirebaseUser?
        get() = try { auth?.currentUser } catch (e: Exception) { null }

    val currentUserId: String
        get() = currentUser?.uid ?: ""

    fun observeAuthState(): Flow<FirebaseUser?> = callbackFlow {
        val safeAuth = auth
        if (safeAuth == null) {
            trySend(null)
            awaitClose { }
            return@callbackFlow
        }
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser)
        }
        try {
            safeAuth.addAuthStateListener(listener)
        } catch (e: Exception) {
            Log.e("FirebaseService", "addAuthStateListener error: ${e.message}")
            trySend(null)
        }
        awaitClose {
            try { safeAuth.removeAuthStateListener(listener) } catch (_: Exception) {}
        }
    }

    suspend fun ensureAuthenticated(): FirebaseUser? = withContext(Dispatchers.IO) {
        try {
            val safeAuth = auth ?: return@withContext null
            val user = safeAuth.currentUser
            if (user != null) return@withContext user
            
            // Sign in anonymously if not signed in, so every user has a valid uid for Storage & Firestore
            val result = safeAuth.signInAnonymously().await()
            result.user
        } catch (e: Exception) {
            Log.e("FirebaseService", "Anonymous sign in failed: ${e.message}")
            null
        }
    }

    suspend fun signUpWithEmail(
        email: String,
        pass: String,
        name: String,
        phone: String,
        agency: String
    ): Result<FirebaseUser> = withContext(Dispatchers.IO) {
        try {
            val safeAuth = auth ?: throw Exception("Firebase Auth မရရှိနိုင်ပါ")
            val authResult = safeAuth.createUserWithEmailAndPassword(email, pass).await()
            val user = authResult.user ?: throw Exception("User creation failed")
            
            val profile = UserProfile(
                uid = user.uid,
                name = name,
                email = email,
                phone = phone,
                agencyName = agency,
                createdAt = System.currentTimeMillis()
            )
            saveUserProfile(profile)
            Result.success(user)
        } catch (e: Exception) {
            Log.e("FirebaseService", "SignUp error: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun signInWithEmail(email: String, pass: String): Result<FirebaseUser> = withContext(Dispatchers.IO) {
        try {
            val safeAuth = auth ?: throw Exception("Firebase Auth မရရှိနိုင်ပါ")
            val authResult = safeAuth.signInWithEmailAndPassword(email, pass).await()
            val user = authResult.user ?: throw Exception("Sign in failed")
            Result.success(user)
        } catch (e: Exception) {
            Log.e("FirebaseService", "SignIn error: ${e.message}")
            Result.failure(e)
        }
    }

    fun signOut() {
        try {
            auth?.signOut()
        } catch (e: Exception) {
            Log.e("FirebaseService", "SignOut error: ${e.message}")
        }
    }

    // ==========================================
    // USER PROFILE
    // ==========================================

    suspend fun getUserProfile(userId: String): UserProfile? = withContext(Dispatchers.IO) {
        if (userId.isBlank()) return@withContext null
        try {
            val safeFirestore = firestore ?: return@withContext null
            val snapshot = safeFirestore.collection("users").document(userId).get().await()
            if (snapshot.exists()) {
                snapshot.toObject(UserProfile::class.java)
            } else null
        } catch (e: Exception) {
            Log.e("FirebaseService", "getUserProfile error: ${e.message}")
            null
        }
    }

    suspend fun saveUserProfile(profile: UserProfile): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val safeFirestore = firestore ?: throw Exception("Firestore မရရှိနိုင်ပါ")
            safeFirestore.collection("users").document(profile.uid)
                .set(profile, SetOptions.merge())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseService", "saveUserProfile error: ${e.message}")
            Result.failure(e)
        }
    }

    // ==========================================
    // FIREBASE STORAGE (PROPERTY IMAGES)
    // ==========================================

    suspend fun uploadPropertyImages(userId: String, imagePaths: List<String>): String = withContext(Dispatchers.IO) {
        val validUserId = userId.ifBlank { currentUserId.ifBlank { "anonymous_${UUID.randomUUID()}" } }
        val uploadedUrls = mutableListOf<String>()
        val safeStorage = storage

        for (path in imagePaths) {
            val trimmed = path.trim()
            if (trimmed.isBlank()) continue

            // If it's already a web/remote URL, keep it
            if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                uploadedUrls.add(trimmed)
                continue
            }

            if (safeStorage == null) {
                uploadedUrls.add(trimmed)
                continue
            }

            try {
                val imageUri = when {
                    trimmed.startsWith("content://") || trimmed.startsWith("file://") -> Uri.parse(trimmed)
                    else -> Uri.fromFile(java.io.File(trimmed))
                }

                val imageFileName = "${UUID.randomUUID()}.jpg"
                val storageRef = safeStorage.reference.child("properties/$validUserId/$imageFileName")
                
                storageRef.putFile(imageUri).await()
                val downloadUrl = storageRef.downloadUrl.await().toString()
                uploadedUrls.add(downloadUrl)
            } catch (e: Exception) {
                Log.e("FirebaseService", "Error uploading image $trimmed: ${e.message}")
                if (!trimmed.startsWith("content://") && !trimmed.startsWith("file://")) {
                    uploadedUrls.add(trimmed)
                }
            }
        }

        if (uploadedUrls.isEmpty()) "img_hero_banner" else uploadedUrls.joinToString(",")
    }

    suspend fun deleteImageFromStorage(imageUrl: String) = withContext(Dispatchers.IO) {
        if (!imageUrl.startsWith("https://firebasestorage.googleapis.com")) return@withContext
        try {
            val safeStorage = storage ?: return@withContext
            val storageRef = safeStorage.getReferenceFromUrl(imageUrl)
            storageRef.delete().await()
        } catch (e: Exception) {
            Log.e("FirebaseService", "deleteImageFromStorage error: ${e.message}")
        }
    }

    // ==========================================
    // FIRESTORE REALTIME PROPERTY LISTINGS
    // ==========================================

    fun observeAllProperties(): Flow<List<Property>> = callbackFlow {
        val safeFirestore = firestore
        if (safeFirestore == null) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        val registration = try {
            val collectionRef = safeFirestore.collection("properties")
                .orderBy("createdAt", Query.Direction.DESCENDING)

            collectionRef.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FirebaseService", "observeAllProperties error: ${error.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val properties = snapshot.documents.mapNotNull { doc ->
                        try {
                            val dto = doc.toObject(FirestorePropertyDto::class.java)?.copy(docId = doc.id)
                            dto?.toProperty()
                        } catch (e: Exception) {
                            Log.e("FirebaseService", "Error parsing property doc: ${e.message}")
                            null
                        }
                    }
                    trySend(properties)
                }
            }
        } catch (e: Exception) {
            Log.e("FirebaseService", "observeAllProperties exception: ${e.message}")
            trySend(emptyList())
            null
        }

        awaitClose { registration?.remove() }
    }

    fun observeUserProperties(userId: String): Flow<List<Property>> = callbackFlow {
        val safeFirestore = firestore
        if (safeFirestore == null || userId.isBlank()) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        val registration = try {
            val collectionRef = safeFirestore.collection("properties")
                .whereEqualTo("userId", userId)

            collectionRef.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FirebaseService", "observeUserProperties error: ${error.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val properties = snapshot.documents.mapNotNull { doc ->
                        try {
                            val dto = doc.toObject(FirestorePropertyDto::class.java)?.copy(docId = doc.id)
                            dto?.toProperty()
                        } catch (e: Exception) {
                            Log.e("FirebaseService", "Error parsing user property doc: ${e.message}")
                            null
                        }
                    }.sortedByDescending { it.createdAt }
                    trySend(properties)
                }
            }
        } catch (e: Exception) {
            Log.e("FirebaseService", "observeUserProperties exception: ${e.message}")
            trySend(emptyList())
            null
        }

        awaitClose { registration?.remove() }
    }

    suspend fun addPropertyToFirestore(property: Property, selectedImagePaths: List<String>): Result<String> = withContext(Dispatchers.IO) {
        try {
            val safeFirestore = firestore ?: throw Exception("Firestore မရရှိနိုင်ပါ")
            val authUser = ensureAuthenticated()
            val userId = authUser?.uid ?: property.userId.ifBlank { currentUserId }

            val imageUrlsString = uploadPropertyImages(userId, selectedImagePaths)

            val docRef = safeFirestore.collection("properties").document()
            val docId = docRef.id

            val dto = FirestorePropertyDto.fromProperty(
                property.copy(
                    docId = docId,
                    userId = userId,
                    imageResName = imageUrlsString
                ),
                idOverride = docId
            )

            docRef.set(dto).await()
            Result.success(docId)
        } catch (e: Exception) {
            Log.e("FirebaseService", "addPropertyToFirestore error: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun updatePropertyInFirestore(property: Property, selectedImagePaths: List<String>): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val safeFirestore = firestore ?: throw Exception("Firestore မရရှိနိုင်ပါ")
            if (property.docId.isBlank()) {
                throw IllegalArgumentException("Missing Firestore docId for update")
            }

            val userId = property.userId.ifBlank { currentUserId }

            val updatedImageString = uploadPropertyImages(userId, selectedImagePaths)

            val dto = FirestorePropertyDto.fromProperty(
                property.copy(imageResName = updatedImageString)
            )

            safeFirestore.collection("properties").document(property.docId)
                .set(dto, SetOptions.merge())
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseService", "updatePropertyInFirestore error: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun deletePropertyFromFirestore(property: Property): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val safeFirestore = firestore ?: throw Exception("Firestore မရရှိနိုင်ပါ")
            if (property.docId.isNotBlank()) {
                safeFirestore.collection("properties").document(property.docId).delete().await()

                if (property.imageResName.isNotBlank()) {
                    val urls = property.imageResName.split(",")
                    for (url in urls) {
                        deleteImageFromStorage(url.trim())
                    }
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseService", "deletePropertyFromFirestore error: ${e.message}")
            Result.failure(e)
        }
    }
}
