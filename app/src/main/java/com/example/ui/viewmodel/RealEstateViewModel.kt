package com.example.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.firebase.UserProfile
import com.example.data.model.Property
import com.example.data.repository.PropertyRepository
import com.example.ui.theme.AppThemeOption
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import kotlinx.coroutines.flow.firstOrNull

class RealEstateViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PropertyRepository

    val searchQuery = MutableStateFlow("")
    val selectedListingTypeTab = MutableStateFlow("ALL") // "ALL", "BUY", "RENT", "MY_LISTINGS"
    val selectedCity = MutableStateFlow("ALL") // "ALL", "Yangon", "Mandalay", "Naypyidaw", "Pyin Oo Lwin", "Taunggyi"
    val selectedPropertyType = MutableStateFlow("ALL") // "ALL", "Condo", "Apartment", "House", "Land", "Commercial"
    val maxPriceLakhs = MutableStateFlow(20000f) // Max price limit
    val minBedrooms = MutableStateFlow(0) // 0 = Any
    val isFilterSheetOpen = MutableStateFlow(false)
    val isSyncing = MutableStateFlow(false)
    val isLoading = MutableStateFlow(false)
    val loadingMessage = MutableStateFlow("လုပ်ဆောင်နေပါသည်...")
    val selectedTheme = MutableStateFlow(AppThemeOption.NAVY_GOLD)

    val userProfile: StateFlow<UserProfile?>

    fun setTheme(theme: AppThemeOption) {
        selectedTheme.value = theme
    }

    init {
        val db = AppDatabase.getInstance(application)
        repository = PropertyRepository(db.propertyDao(), application)
        userProfile = repository.userProfile

        viewModelScope.launch {
            repository.checkAndSeedInitialData()
        }
    }

    fun currentUserId(): String = repository.firebaseService.currentUserId

    fun isUserSignedIn(): Boolean {
        val user = repository.firebaseService.currentUser
        return user != null && !user.isAnonymous
    }

    // Auth actions
    fun signUp(
        email: String,
        pass: String,
        name: String,
        phone: String,
        agency: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            isLoading.value = true
            loadingMessage.value = "အကောင့် သစ်ပြုလုပ်နေပါသည်..."
            val result = repository.firebaseService.signUpWithEmail(email, pass, name, phone, agency)
            isLoading.value = false
            if (result.isSuccess) {
                onSuccess()
            } else {
                onError(result.exceptionOrNull()?.localizedMessage ?: "အကောင့် ပြုလုပ်ခြင်း မအောင်မြင်ပါ")
            }
        }
    }

    fun signIn(
        email: String,
        pass: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            isLoading.value = true
            loadingMessage.value = "အကောင့် ဝင်ရောက်နေပါသည်..."
            val result = repository.firebaseService.signInWithEmail(email, pass)
            isLoading.value = false
            if (result.isSuccess) {
                onSuccess()
            } else {
                onError(result.exceptionOrNull()?.localizedMessage ?: "အကောင့် ဝင်ရောက်ခြင်း မအောင်မြင်ပါ")
            }
        }
    }

    fun signOut() {
        repository.firebaseService.signOut()
    }

    fun updateProfileInfo(
        name: String,
        phone: String,
        agencyName: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val uid = repository.firebaseService.currentUserId
            if (uid.isBlank()) {
                onError("အကောင့်ဝင်ရောက်ရန် လိုအပ်ပါသည်")
                return@launch
            }
            isLoading.value = true
            loadingMessage.value = "ကိုယ်ရေးအချက်အလက် ပြင်ဆင်နေပါသည်..."
            val updated = UserProfile(
                uid = uid,
                name = name,
                email = repository.firebaseService.currentUser?.email ?: "",
                phone = phone,
                agencyName = agencyName
            )
            val res = repository.firebaseService.saveUserProfile(updated)
            isLoading.value = false
            if (res.isSuccess) {
                onSuccess()
            } else {
                onError(res.exceptionOrNull()?.localizedMessage ?: "ပြင်ဆင်ခြင်း မအောင်မြင်ပါ")
            }
        }
    }

    fun refreshCloudData(onComplete: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            isSyncing.value = true
            // Realtime listener is automatically active
            isSyncing.value = false
            onComplete?.invoke(true)
        }
    }

    val favoriteProperties: StateFlow<List<Property>> = repository.favoriteProperties
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private data class FilterParams(
        val query: String,
        val tab: String,
        val city: String,
        val propType: String,
        val maxPrice: Float,
        val currentUid: String
    )

    private val filterState = combine(
        searchQuery,
        selectedListingTypeTab,
        selectedCity,
        selectedPropertyType,
        maxPriceLakhs
    ) { query, tab, city, propType, maxPrice ->
        FilterParams(query, tab, city, propType, maxPrice, repository.firebaseService.currentUserId)
    }

    val filteredProperties: StateFlow<List<Property>> = combine(
        repository.allProperties,
        filterState,
        minBedrooms
    ) { properties, filter, beds ->
        properties.filter { p ->
            val matchesQuery = filter.query.isBlank() ||
                    p.title.contains(filter.query, ignoreCase = true) ||
                    p.township.contains(filter.query, ignoreCase = true) ||
                    p.city.contains(filter.query, ignoreCase = true) ||
                    p.address.contains(filter.query, ignoreCase = true)

            val matchesTab = when (filter.tab) {
                "BUY" -> p.listingType == "BUY"
                "RENT" -> p.listingType == "RENT"
                "MY_LISTINGS" -> (p.userId.isNotBlank() && p.userId == filter.currentUid) || p.agentType == "Direct Post" || p.agentType == "User Post"
                else -> true
            }

            val matchesCity = filter.city == "ALL" || p.city.equals(filter.city, ignoreCase = true)
            val matchesPropType = filter.propType == "ALL" || p.propertyType.equals(filter.propType, ignoreCase = true)
            val matchesPrice = p.priceLakhs <= filter.maxPrice
            val matchesBeds = beds == 0 || p.bedrooms >= beds

            matchesQuery && matchesTab && matchesCity && matchesPropType && matchesPrice && matchesBeds
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val myListings: StateFlow<List<Property>> = repository.allProperties
        .combine(repository.userProfile) { properties, profile ->
            val uid = profile?.uid ?: repository.firebaseService.currentUserId
            properties.filter { p ->
                (uid.isNotBlank() && p.userId == uid) || p.agentType == "Direct Post" || p.agentType == "User Post"
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun getPropertyById(id: Long): StateFlow<Property?> {
        return repository.getPropertyById(id)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = null
            )
    }

    fun toggleFavorite(property: Property) {
        viewModelScope.launch {
            repository.toggleFavorite(property.id, property.isFavorite)
        }
    }

    fun postNewProperty(
        title: String,
        listingType: String,
        propertyType: String,
        priceLakhs: Double,
        pricePeriod: String,
        city: String,
        township: String,
        address: String,
        areaSqft: Int,
        bedrooms: Int,
        bathrooms: Int,
        floorLevel: String,
        furnishing: String,
        deedType: String,
        description: String,
        agentName: String,
        agentPhone: String,
        imageResName: String? = null,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            if (!isUserSignedIn()) {
                isLoading.value = false
                Log.w("RealEstateViewModel", "Guest users are not allowed to post properties.")
                return@launch
            }
            isLoading.value = true
            loadingMessage.value = "Firebase Storage သို့ ဓာတ်ပုံများ တင်နေပါသည်..."

            val defaultImg = if (propertyType == "House" || propertyType == "Land") "img_property_villa" else "img_hero_banner"
            val rawImageString = if (!imageResName.isNullOrBlank()) imageResName else defaultImg
            val imageList = if (rawImageString.contains(",")) {
                rawImageString.split(",").map { it.trim() }.filter { it.isNotBlank() }
            } else {
                listOf(rawImageString)
            }

            val currentUid = repository.firebaseService.currentUserId.ifBlank { "anon_${System.currentTimeMillis()}" }

            val newProperty = Property(
                userId = currentUid,
                title = title.ifBlank { "အိမ်ခြံမြေ ရောင်းရန်/ငှားရန်" },
                listingType = listingType,
                propertyType = propertyType,
                priceLakhs = priceLakhs,
                pricePeriod = pricePeriod,
                city = city,
                township = township,
                address = address,
                areaSqft = areaSqft,
                bedrooms = bedrooms,
                bathrooms = bathrooms,
                floorLevel = floorLevel,
                furnishing = furnishing,
                deedType = deedType,
                description = description,
                imageResName = rawImageString,
                agentName = agentName.ifBlank { "အိမ်ပိုင်ရှင်" },
                agentPhone = agentPhone.ifBlank { "0912345678" },
                agentType = "User Post",
                isFavorite = false,
                status = "ACTIVE"
            )

            loadingMessage.value = "Firestore သို့ ကြော်ငြာ သိမ်းဆည်းနေပါသည်..."
            repository.insertPropertyWithFirebase(newProperty, imageList)

            isLoading.value = false
            onSuccess()
        }
    }

    fun updateProperty(
        property: Property,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            isLoading.value = true
            loadingMessage.value = "Firestore နှင့် Storage တွင် ပြင်ဆင်နေပါသည်..."

            val imageList = if (property.imageResName.contains(",")) {
                property.imageResName.split(",").map { it.trim() }.filter { it.isNotBlank() }
            } else {
                listOf(property.imageResName)
            }

            repository.updatePropertyWithFirebase(property, imageList)

            isLoading.value = false
            onSuccess()
        }
    }

    fun deleteProperty(
        propertyId: Long,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            isLoading.value = true
            loadingMessage.value = "Firebase မှ ဖျက်သိမ်းနေပါသည်..."

            try {
                val property = repository.getPropertyById(propertyId).firstOrNull()
                if (property != null) {
                    repository.deletePropertyWithFirebase(property)
                }
            } catch (e: Exception) {
                Log.e("RealEstateViewModel", "Error deleting property: ${e.message}")
            } finally {
                isLoading.value = false
                onSuccess()
            }
        }
    }

    fun resetFilters() {
        searchQuery.value = ""
        selectedListingTypeTab.value = "ALL"
        selectedCity.value = "ALL"
        selectedPropertyType.value = "ALL"
        maxPriceLakhs.value = 20000f
        minBedrooms.value = 0
    }
}
