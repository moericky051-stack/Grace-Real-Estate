package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.Property
import com.example.data.repository.PropertyRepository
import com.example.ui.theme.AppThemeOption
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RealEstateViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PropertyRepository

    val searchQuery = MutableStateFlow("")
    val selectedListingTypeTab = MutableStateFlow("ALL") // "ALL", "BUY", "RENT"
    val selectedCity = MutableStateFlow("ALL") // "ALL", "Yangon", "Mandalay", "Naypyidaw", "Pyin Oo Lwin", "Taunggyi"
    val selectedPropertyType = MutableStateFlow("ALL") // "ALL", "Condo", "Apartment", "House", "Land", "Commercial"
    val maxPriceLakhs = MutableStateFlow(20000f) // Max price limit
    val minBedrooms = MutableStateFlow(0) // 0 = Any
    val isFilterSheetOpen = MutableStateFlow(false)
    val isSyncing = MutableStateFlow(false)
    val selectedTheme = MutableStateFlow(AppThemeOption.NAVY_GOLD)

    fun setTheme(theme: AppThemeOption) {
        selectedTheme.value = theme
    }

    init {
        val db = AppDatabase.getInstance(application)
        repository = PropertyRepository(db.propertyDao())
        
        viewModelScope.launch {
            repository.checkAndSeedInitialData()
        }
    }

    fun refreshCloudData(onComplete: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            isSyncing.value = true
            val result = repository.syncWithCloud()
            isSyncing.value = false
            onComplete?.invoke(result.isSuccess)
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
        val maxPrice: Float
    )

    private val filterState = combine(
        searchQuery,
        selectedListingTypeTab,
        selectedCity,
        selectedPropertyType,
        maxPriceLakhs
    ) { query, tab, city, propType, maxPrice ->
        FilterParams(query, tab, city, propType, maxPrice)
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

    fun getPropertyById(id: Long): StateFlow<Property?> {
        val flow = MutableStateFlow<Property?>(null)
        viewModelScope.launch {
            repository.getPropertyById(id).collect {
                flow.value = it
            }
        }
        return flow
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
            val defaultImg = if (propertyType == "House" || propertyType == "Land") "img_property_villa" else "img_hero_banner"
            val finalImgName = if (!imageResName.isNullOrBlank()) imageResName else defaultImg
            val newProperty = Property(
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
                imageResName = finalImgName,
                agentName = agentName.ifBlank { "အိမ်ပိုင်ရှင်" },
                agentPhone = agentPhone.ifBlank { "0912345678" },
                agentType = "Direct Post",
                isFavorite = false
            )
            repository.insertProperty(newProperty)
            onSuccess()
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
