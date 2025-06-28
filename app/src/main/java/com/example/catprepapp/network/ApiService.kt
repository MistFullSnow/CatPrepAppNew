package com.example.catprepapp.network

import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Body // New import
import retrofit2.http.POST   // New import

// This is the full URL of your deployed Google Apps Script
// IMPORTANT: Replace "YOUR_WEB_APP_URL" with your actual URL
private const val BASE_URL = "https://script.google.com/macros/s/AKfycbzLkdW1_cC9FOER5-RXKsrR6JLF-e7gDvTteXyCgsMijMvthAsc95bugt_2PHKTTR4/"

// IMPORTANT: Replace "YOUR_SECRET_KEY" with the key you chose (e.g., "CATPREP123")
private const val SECRET_KEY = "CATPREP123"

// This builds the Retrofit object
private val retrofit = Retrofit.Builder()
    .addConverterFactory(GsonConverterFactory.create())
    .baseUrl(BASE_URL)
    .build()

// This interface defines the API endpoints
interface ApiService {
    @GET("exec") // All requests to Apps Script go to the /exec endpoint
    suspend fun getSchedule(
        @Query("action") action: String = "getSchedule",
        @Query("secret") secret: String = SECRET_KEY
    ): Response<ScheduleResponse>
    
    // Inside the ApiService interface
    @POST("exec")
    suspend fun submitLog(@Body requestBody: LogRequestBody): Response<Unit> // Response can be empty
}

// This creates a public object that the rest of our app can use to call the API
object ApiClient {
    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }
}
// Add this new data class
data class LogRequestBody(
    val action: String = "submitLog",
    val secret: String,
    val topic: String,
    val questions: Int,
    val confidence: Int
)
