package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.db.PropertyDao
import com.example.data.firebase.FirebaseService
import com.example.data.firebase.UserProfile
import com.example.data.model.Property
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PropertyRepository(
    private val propertyDao: PropertyDao,
    context: Context
) {

    val firebaseService = FirebaseService(context)

    // Room DB Flow for offline access
    val allProperties: Flow<List<Property>> = propertyDao.getAllProperties().distinctUntilChanged()
    val favoriteProperties: Flow<List<Property>> = propertyDao.getFavoriteProperties()

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    private val externalScope = CoroutineScope(Dispatchers.IO)

    init {
        // Start listening to Firebase Auth state
        externalScope.launch {
            firebaseService.observeAuthState().collectLatest { user ->
                if (user != null) {
                    val profile = firebaseService.getUserProfile(user.uid)
                    _userProfile.value = profile ?: UserProfile(
                        uid = user.uid,
                        email = user.email ?: "",
                        name = user.displayName ?: if (user.isAnonymous) "Guest User" else "User",
                        createdAt = System.currentTimeMillis()
                    )
                } else {
                    _userProfile.value = null
                }
            }
        }

        // Start Realtime Firestore Listener to sync Firestore properties -> Room DB
        externalScope.launch {
            try {
                firebaseService.observeAllProperties()
                    .distinctUntilChanged()
                    .collectLatest { firestoreList ->
                        if (firestoreList.isNotEmpty()) {
                            val allLocal = propertyDao.getAllPropertiesList()
                            val existingDocMap = allLocal.filter { it.docId.isNotBlank() }.associateBy { it.docId }
                            val existingTitlePhoneMap = allLocal.associateBy { "${it.title}_${it.agentPhone}" }

                            val toBatchInsert = mutableListOf<Property>()

                            for (prop in firestoreList) {
                                var existing: Property? = if (prop.docId.isNotBlank()) {
                                    existingDocMap[prop.docId]
                                } else null

                                if (existing == null) {
                                    existing = existingTitlePhoneMap["${prop.title}_${prop.agentPhone}"]
                                }

                                if (existing != null) {
                                    val updatedProp = prop.copy(
                                        id = existing.id,
                                        isFavorite = existing.isFavorite,
                                        docId = if (prop.docId.isNotBlank()) prop.docId else existing.docId
                                    )
                                    if (!isPropertyIdentical(existing, updatedProp)) {
                                        toBatchInsert.add(updatedProp)
                                    }
                                } else {
                                    toBatchInsert.add(prop)
                                }
                            }

                            if (toBatchInsert.isNotEmpty()) {
                                propertyDao.insertProperties(toBatchInsert)
                            }
                        }
                    }
            } catch (e: Exception) {
                Log.e("PropertyRepository", "Firestore realtime listener error: ${e.message}")
            }
        }
    }

    private fun isPropertyIdentical(p1: Property, p2: Property): Boolean {
        return p1.id == p2.id &&
               p1.title == p2.title &&
               p1.listingType == p2.listingType &&
               p1.propertyType == p2.propertyType &&
               p1.priceLakhs == p2.priceLakhs &&
               p1.pricePeriod == p2.pricePeriod &&
               p1.city == p2.city &&
               p1.township == p2.township &&
               p1.address == p2.address &&
               p1.areaSqft == p2.areaSqft &&
               p1.bedrooms == p2.bedrooms &&
               p1.bathrooms == p2.bathrooms &&
               p1.floorLevel == p2.floorLevel &&
               p1.furnishing == p2.furnishing &&
               p1.deedType == p2.deedType &&
               p1.description == p2.description &&
               p1.agentName == p2.agentName &&
               p1.agentPhone == p2.agentPhone &&
               p1.imageResName == p2.imageResName &&
               p1.isFavorite == p2.isFavorite &&
               p1.userId == p2.userId &&
               p1.docId == p2.docId
    }

    fun getPropertyById(id: Long): Flow<Property?> = propertyDao.getPropertyById(id)

    suspend fun toggleFavorite(id: Long, currentStatus: Boolean) {
        propertyDao.updateFavorite(id, !currentStatus)
    }

    suspend fun insertPropertyWithFirebase(
        property: Property,
        selectedImagePaths: List<String>
    ): Result<Long> = withContext(Dispatchers.IO) {
        try {
            // Upload images to Storage & save doc to Firestore
            val firestoreResult = firebaseService.addPropertyToFirestore(property, selectedImagePaths)
            val docId = firestoreResult.getOrDefault("")

            // Insert locally in Room DB
            val finalProp = property.copy(
                docId = docId,
                userId = firebaseService.currentUserId,
                imageResName = if (selectedImagePaths.isNotEmpty()) selectedImagePaths.joinToString(",") else property.imageResName
            )
            val localId = propertyDao.insertProperty(finalProp)

            Result.success(localId)
        } catch (e: Exception) {
            Log.e("PropertyRepository", "insertPropertyWithFirebase error: ${e.message}")
            // Fallback: save locally
            val localId = propertyDao.insertProperty(property)
            Result.success(localId)
        }
    }

    suspend fun updatePropertyWithFirebase(
        property: Property,
        selectedImagePaths: List<String>
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (property.docId.isNotBlank()) {
                firebaseService.updatePropertyInFirestore(property, selectedImagePaths)
            }
            propertyDao.insertProperty(property)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("PropertyRepository", "updatePropertyWithFirebase error: ${e.message}")
            propertyDao.insertProperty(property)
            Result.success(Unit)
        }
    }

    suspend fun deletePropertyWithFirebase(property: Property): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (property.docId.isNotBlank()) {
                firebaseService.deletePropertyFromFirestore(property)
            }
            if (property.id > 0) {
                propertyDao.deleteProperty(property.id)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("PropertyRepository", "deletePropertyWithFirebase error: ${e.message}")
            if (property.id > 0) {
                propertyDao.deleteProperty(property.id)
            }
            Result.success(Unit)
        }
    }

    suspend fun checkAndSeedInitialData() {
        if (propertyDao.getPropertyCount() == 0) {
            val sampleProperties = listOf(
                Property(
                    title = "ကမာရွတ်မြို့နယ် ပြင်ဆင်ပြီး ကွန်ဒိုသစ် အမြန်ရောင်းမည်",
                    listingType = "BUY",
                    propertyType = "Condo",
                    priceLakhs = 3800.0,
                    pricePeriod = "TOTAL",
                    city = "Yangon",
                    township = "ကမာရွတ် (Kamayut)",
                    address = "လှိုင်မြစ်လမ်းမအနီး၊ ကမာရွတ်မြို့နယ်၊ ရန်ကုန်။",
                    areaSqft = 1450,
                    bedrooms = 3,
                    bathrooms = 2,
                    floorLevel = "8th Floor",
                    furnishing = "Fully Furnished",
                    deedType = "Grant Land (ဂရန်အမည်ပေါက်)",
                    description = "လှိုင်မြစ်မြင်ကွင်းရ ကွန်ဒိုအခန်းကျယ်။ Master Bedroom ၁ ခန်း၊ Single Bedroom ၂ ခန်း ပါဝင်သည်။ Lift, 24hr Security, Backup Generator, Swimming Pool ပါဝင်ပြီး အသင့်နေထိုင်နိုင်ပါသည်။",
                    imageResName = "img_hero_banner",
                    agentName = "ဦးမင်းသူ (Grace Estate)",
                    agentPhone = "09420011223",
                    agentType = "Verified Agent",
                    isFavorite = true
                ),
                Property(
                    title = "ဗဟန်းမြို့နယ် ဆိတ်ငြိမ်ရပ်ကွက် လုံးချင်း 2RC အိမ်ကျယ် ရောင်းမည်",
                    listingType = "BUY",
                    propertyType = "House",
                    priceLakhs = 18500.0,
                    pricePeriod = "TOTAL",
                    city = "Yangon",
                    township = "ဗဟန်း (Bahan)",
                    address = "ဆရာစံလမ်းသွယ်၊ ဗဟန်းမြို့နယ်၊ ရန်ကုန်။",
                    areaSqft = 3600,
                    bedrooms = 4,
                    bathrooms = 4,
                    floorLevel = "2-Story Villa",
                    furnishing = "Semi-Furnished",
                    deedType = "Ancestral Land (ဘိုးဘွားပိုင်မြေ)",
                    description = "ရွှေတိဂုံဘုရားအနီး ဆိတ်ငြိမ်ရပ်ကွက်ရှိ လုံးချင်းနှစ်ထပ်တိုက်။ ကား ၄ စီး ရပ်နားရန် နေရာကျယ် ပါဝင်ပြီး သစ်ပင်ရိပ် ဝန်းကျင်ကောင်းမွန်ပါသည်။",
                    imageResName = "img_property_villa",
                    agentName = "ဒေါ်နန်းမိုး (Yangon Luxury Realty)",
                    agentPhone = "09790099887",
                    agentType = "Exclusive Agent",
                    isFavorite = false
                ),
                Property(
                    title = "ရန်ကင်းမြို့နယ် ဆိုင်ခန်း သို့မဟုတ် ရုံးခန်းဌားရန်",
                    listingType = "RENT",
                    propertyType = "Commercial",
                    priceLakhs = 35.0,
                    pricePeriod = "PER_MONTH",
                    city = "Yangon",
                    township = "ရန်ကင်း (Yankin)",
                    address = "ကံဘဲ့လမ်းမကြီးပေါ်၊ ရန်ကင်းမြို့နယ်၊ ရန်ကုန်။",
                    areaSqft = 1200,
                    bedrooms = 1,
                    bathrooms = 2,
                    floorLevel = "Ground Floor",
                    furnishing = "Unfurnished",
                    deedType = "Commercial Lease",
                    description = "လူစည်ကားသော လမ်းမကြီးပေါ်တွင် တည်ရှိပြီး Showroom, Clinic, သို့မဟုတ် Office ခန်းမဖွင့်လှစ်ရန် လွန်စွာ သင့်တော်ပါသည်။ ကားပါကင် နေရာကျယ်ဝန်းသည်။",
                    imageResName = "img_hero_banner",
                    agentName = "ကိုအောင်ကျော် (City Property Agency)",
                    agentPhone = "09250123456",
                    agentType = "Verified Agent",
                    isFavorite = false
                )
            )
            propertyDao.insertProperties(sampleProperties)
        }
    }
}
