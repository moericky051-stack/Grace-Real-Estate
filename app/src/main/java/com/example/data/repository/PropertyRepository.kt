package com.example.data.repository

import android.util.Log
import com.example.data.db.PropertyDao
import com.example.data.model.Property
import com.example.data.remote.CloudNetworkClient
import com.example.data.remote.CloudPropertyDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class PropertyRepository(private val propertyDao: PropertyDao) {

    val allProperties: Flow<List<Property>> = propertyDao.getAllProperties()
    val favoriteProperties: Flow<List<Property>> = propertyDao.getFavoriteProperties()

    fun getPropertyById(id: Long): Flow<Property?> = propertyDao.getPropertyById(id)

    suspend fun toggleFavorite(id: Long, currentStatus: Boolean) {
        propertyDao.updateFavorite(id, !currentStatus)
    }

    suspend fun insertProperty(property: Property): Long {
        val insertedId = propertyDao.insertProperty(property)
        val insertedProp = property.copy(id = insertedId)

        // Push to Cloud so all users see this property
        withContext(Dispatchers.IO) {
            try {
                val dto = CloudPropertyDto.fromProperty(insertedProp)
                val response = CloudNetworkClient.api.postCloudProperty(dto)
                if (response.isSuccessful) {
                    Log.d("PropertyRepository", "Successfully synced new property to cloud: ${response.body()?.name}")
                } else {
                    Log.e("PropertyRepository", "Failed to sync to cloud: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("PropertyRepository", "Cloud sync error: ${e.message}")
            }
        }
        return insertedId
    }

    suspend fun deleteProperty(id: Long) {
        propertyDao.deleteProperty(id)
    }

    suspend fun syncWithCloud(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val response = CloudNetworkClient.api.getAllCloudProperties()
            if (response.isSuccessful) {
                val cloudMap = response.body()
                var newCount = 0
                if (!cloudMap.isNullOrEmpty()) {
                    for ((key, dto) in cloudMap) {
                        val count = propertyDao.countByTitleAndPhone(dto.title, dto.agentPhone)
                        if (count == 0) {
                            val newProp = dto.toProperty()
                            propertyDao.insertProperty(newProp)
                            newCount++
                        }
                    }
                }
                Result.success(newCount)
            } else {
                Result.failure(Exception("HTTP error ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("PropertyRepository", "syncWithCloud error: ${e.message}")
            Result.failure(e)
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
                ),
                Property(
                    title = "ပြင်ဦးလွင် တောင်လှေကား မြေကွက်ကျယ် အမြန်ရောင်းမည်",
                    listingType = "BUY",
                    propertyType = "Land",
                    priceLakhs = 4200.0,
                    pricePeriod = "TOTAL",
                    city = "Pyin Oo Lwin",
                    township = "ရပ်ကွက် (၆) ပြင်ဦးလွင်",
                    address = "အမျိုးသားကန်တော်ကြီးဥယျာဉ်အနီး၊ ပြင်ဦးလွင်။",
                    areaSqft = 7200,
                    bedrooms = 0,
                    bathrooms = 0,
                    floorLevel = "Land Plot",
                    furnishing = "Unfurnished",
                    deedType = "Grant Land (ဂရန်မြေ ၆၀ နှစ်)",
                    description = "ရာသီဥတု သာယာအေးမြသော ပြင်ဦးလွင် ကန်တော်ကြီးအနီး မြေကွက်ကျယ်။ အပန်းဖြေစခန်း သို့မဟုတ် လုံးချင်းအိမ် ဆောက်လုပ်ရန် သင့်တော်သည်။ စာရွက်စာတမ်း စုံလင်ပါသည်။",
                    imageResName = "img_property_villa",
                    agentName = "ဦးကျော်စွာ (မန္တလေး ရွှေမြေ)",
                    agentPhone = "09400223344",
                    agentType = "Owner Direct",
                    isFavorite = true
                ),
                Property(
                    title = "မန္တလေး ချမ်းအေးသာစံ မြို့နယ် တိုက်ခန်းပြင်ဆင်ပြီး ငှားမည်",
                    listingType = "RENT",
                    propertyType = "Apartment",
                    priceLakhs = 18.0,
                    pricePeriod = "PER_MONTH",
                    city = "Mandalay",
                    township = "ချမ်းအေးသာစံ (Chanayethazan)",
                    address = "၇၈ လမ်းနှင့် ၃၀ လမ်းထောင့်၊ မန္တလေး။",
                    areaSqft = 950,
                    bedrooms = 2,
                    bathrooms = 1,
                    floorLevel = "2nd Floor",
                    furnishing = "Fully Furnished",
                    deedType = "Apartment Deed",
                    description = "မန္တလေးဈေးချိုနှင့် လမ်းလျှောက်အကွာအဝေးရှိ ပြင်ဆင်ပြီး တိုက်ခန်း။ အဲကွန်း ၃ လုံး၊ ရေပူအေး၊ မီးဖိုချောင် ကက်ဘိနက် ပါဝင်ပြီးဖြစ်ပါသည်။",
                    imageResName = "img_hero_banner",
                    agentName = "မသီတာ (Mandalay Homes)",
                    agentPhone = "09970112233",
                    agentType = "Verified Agent",
                    isFavorite = false
                ),
                Property(
                    title = "တောင်ကြီး စန်တာရိုဆာ လမ်းမကြီးပေါ် စီးပွားရေးမြေ ရောင်းမည်",
                    listingType = "BUY",
                    propertyType = "Land",
                    priceLakhs = 9500.0,
                    pricePeriod = "TOTAL",
                    city = "Taunggyi",
                    township = "ရေအေးကွင်း၊ တောင်ကြီးမြို့။",
                    address = "စန်တာရိုဆာ လမ်းမကြီးပေါ်၊ တောင်ကြီး။",
                    areaSqft = 4800,
                    bedrooms = 0,
                    bathrooms = 0,
                    floorLevel = "Land Plot",
                    furnishing = "Unfurnished",
                    deedType = "Grant Land (ဂရန်အမည်ပေါက်)",
                    description = "တောင်ကြီးမြို့လယ် စီးပွားရေးဇုန်အနီး လမ်းမကြီးတိုက်ရိုက်မျက်နှာစာရှိ မြေကွက်။ ဟိုတယ်၊ ကွန်ဒို သို့မဟုတ် မောလ် ဆောက်လုပ်ရန် သင့်တော်ပါသည်။",
                    imageResName = "img_property_villa",
                    agentName = "စောဟန်လင်း (Shan Hills Realty)",
                    agentPhone = "09880123999",
                    agentType = "Exclusive Agent",
                    isFavorite = false
                )
            )
            propertyDao.insertProperties(sampleProperties)

            // Seed to cloud if empty
            withContext(Dispatchers.IO) {
                try {
                    val res = CloudNetworkClient.api.getAllCloudProperties()
                    if (res.isSuccessful && res.body().isNullOrEmpty()) {
                        sampleProperties.forEach { p ->
                            CloudNetworkClient.api.postCloudProperty(CloudPropertyDto.fromProperty(p))
                        }
                    }
                } catch (e: Exception) {
                    Log.e("PropertyRepository", "Initial cloud seed error: ${e.message}")
                }
            }
        }

        // Always sync with cloud on app startup
        syncWithCloud()
    }
}
