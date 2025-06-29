package com.example.catprepapp.network

import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Body // New import
import retrofit2.http.POST   // New import

data class ChatMessage(val user: String, val bot: String)
data class ChatHistoryResponse(val history: List<ChatMessage>)

data class NotificationItem(
    val timestamp: String,
    val title: String,
    val body: String
)

data class NotificationsResponse(
    val notifications: List<NotificationItem>
)

// Add these two new data classes
data class ChatRequestBody(
    val action: String = "askCatbot",
    val secret: String,
    val query: String
)

data class ChatResponseBody(
    val reply: String
)

// Add these two new data classes
data class LogHistoryItem(
    val id: Int,
    val date: String,
    val topic: String,
    val questions: Int,
    val confidence: Int
)

data class LogHistoryResponse(
    val history: List<LogHistoryItem>
)

// Add these two new data classes
data class WeakestTopic(
    val topic: String,
    val avgConfidence: Int
)

data class DashboardResponse(
    val totalQuestions: Int,
    val avgConfidence: Int,
    val streakDays: Int,
    val weakestTopics: List<WeakestTopic>
)

// Represents the structured list of topics from our API
data class TopicsResponse(
    val qa: List<String>,
    val dilr: List<String>,
    val varc: List<String>
)

// This is the full URL of your deployed Google Apps Script
// IMPORTANT: Replace "YOUR_WEB_APP_URL" with your actual URL
private const val BASE_URL = "https://script.google.com/macros/s/AKfycbzvELQSRGndcvOn6o4KcIcFdYCTTWnMlJF9YxdAgEi5sc8wIly_eUdGGM9IpsxWa546/"

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

    // Inside the ApiService interface
    @GET("exec")
    suspend fun getLogHistory(
        @Query("action") action: String = "getLogHistory",
        @Query("secret") secret: String = SECRET_KEY
    ): Response<LogHistoryResponse>

    @GET("exec")
    suspend fun deleteLog(
        @Query("action") action: String = "deleteLog",
        @Query("secret") secret: String = SECRET_KEY,
        @Query("rowId") rowId: Int
    ): Response<Unit> // We don't care about the response body, just that it succeeds
    
    // Inside the ApiService interface
    @GET("exec")
    suspend fun getDashboard(
        @Query("action") action: String = "getDashboard",
        @Query("secret") secret: String = SECRET_KEY
    ): Response<DashboardResponse>

    @GET("exec")
    suspend fun getTopics(
        @Query("action") action: String = "getTopics",
        @Query("secret") secret: String = SECRET_KEY
    ): Response<TopicsResponse>

    // Inside the ApiService interface
    @POST("exec")
    suspend fun askCatbot(@Body requestBody: ChatRequestBody): Response<ChatResponseBody>

    @GET("exec")
    suspend fun getChatHistory(
        @Query("action") action: String = "getChatHistory",
        @Query("secret") secret: String = SECRET_KEY
    ): Response<ChatHistoryResponse>
    @GET("exec")
    suspend fun getNotifications(
        @Query("action") action: String = "getNotifications",
        @Query("secret") secret: String = SECRET_KEY
    ): Response<NotificationsResponse>
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

