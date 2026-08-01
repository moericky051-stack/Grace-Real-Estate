package com.example.data.remote

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

// 1. DTO Structure
@JsonClass(generateAdapter = true)
data class CloudPropertyDto(
    val cloudId: String? = null,
    val title: String = "",
    val listingType: String = "BUY"
)

// 2. API Interface
interface CloudPropertyApi {
    
    @GET("properties")
    suspend fun getProperties(): Response<List<CloudPropertyDto>>

    @GET("properties/{id}")
    suspend fun getPropertyDetail(
        @Path("id") propertyId: String
    ): Response<CloudPropertyDto>

    @POST("properties")
    suspend fun createProperty(
        @Body property: CloudPropertyDto
    ): Response<CloudPropertyDto>
}

// 3. Retrofit Builder / Provider
object CloudPropertyApiProvider {

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // Network Timeout (၁၅ စက္ကန့်) ထည့်သွင်းထားသော OkHttpClient
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS) // Server ချိတ်ဆက်ချိန် ၁၅ စက္ကန့်
        .readTimeout(15, TimeUnit.SECONDS)    // Data ဖတ်ချိန် ၁၅ စက္ကန့်
        .writeTimeout(15, TimeUnit.SECONDS)   // Data ပို့ချိန် ၁၅ စက္ကန့်
        .addInterceptor(loggingInterceptor)
        .build()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://your-api-base-url.com/") // သင့်ရဲ့ Server Base URL ထည့်ပါ
        .client(okHttpClient) // Timeout ပြင်ထားသည့် OkHttpClient ကို ထည့်သွင်းပေးခြင်း
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val api: CloudPropertyApi by lazy {
        retrofit.create(CloudPropertyApi::class.java)
    }
}
