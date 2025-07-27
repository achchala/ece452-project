package com.example.evenly.api

import com.example.evenly.api.auth.AuthApiService
import com.example.evenly.api.dashboard.DashboardApiService
import com.example.evenly.api.expenses.ExpenseApiService
import com.example.evenly.api.friends.FriendsApiService
import com.example.evenly.api.group.GroupApiService
import com.example.evenly.api.notifications.NotificationsApiService
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Central HTTP client configuration for all API services.
 * Manages Retrofit instance, OkHttp client, and provides access to all API service interfaces.
 */
object RetrofitClient {
    private const val BASE_URL = "http://10.0.2.2:8000/" // Django development server
    
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    // Auth API Service
    val authApiService: AuthApiService = retrofit.create(AuthApiService::class.java)
    
    // Dashboard API Service
    val dashboardApiService: DashboardApiService = retrofit.create(DashboardApiService::class.java)

    val friendsApiService: FriendsApiService = retrofit.create(FriendsApiService::class.java)
    // Group API Service
    val groupApiService: GroupApiService = retrofit.create(GroupApiService::class.java)
    // Expense API Service
    val expenseApiService: ExpenseApiService = retrofit.create(ExpenseApiService::class.java)

    val notificationsApiService: NotificationsApiService = retrofit.create(NotificationsApiService::class.java)
    // Add more API services here as needed
    // Example:
    // val paymentApiService: PaymentApiService = retrofit.create(PaymentApiService::class.java)
}