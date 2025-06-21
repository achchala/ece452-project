package com.example.evenly.api.dashboard

import com.example.evenly.api.dashboard.models.DashboardResponse
import com.example.evenly.api.dashboard.models.LentExpensesResponse
import com.example.evenly.api.dashboard.models.OwedSplitsResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * API service interface for dashboard operations.
 * Handles fetching user's lent and owed expense data.
 */
interface DashboardApiService {
    @GET("/api/dashboard/user-expenses/")
    suspend fun getUserExpenses(@Query("user_id") userId: Int): Response<DashboardResponse>
    
    @GET("/api/dashboard/lent/")
    suspend fun getLentExpenses(@Query("user_id") userId: Int): Response<LentExpensesResponse>
    
    @GET("/api/dashboard/owed/")
    suspend fun getOwedSplits(@Query("user_id") userId: Int): Response<OwedSplitsResponse>
} 