package com.example.evenly.api.dashboard

import com.example.evenly.api.RetrofitClient
import com.example.evenly.api.dashboard.models.DashboardResponse
import com.example.evenly.api.dashboard.models.LentExpensesResponse
import com.example.evenly.api.dashboard.models.OwedSplitsResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository for dashboard operations.
 * Handles API calls and provides a clean interface for dashboard data.
 */
object DashboardRepository {
    
    /**
     * Get complete dashboard data for a user
     */
    suspend fun getUserExpenses(userId: Int): Result<DashboardResponse> = withContext(Dispatchers.IO) {
        try {
            val response = RetrofitClient.dashboardApiService.getUserExpenses(userId)
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to fetch dashboard data: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get only lent expenses for a user
     */
    suspend fun getLentExpenses(userId: Int): Result<LentExpensesResponse> = withContext(Dispatchers.IO) {
        try {
            val response = RetrofitClient.dashboardApiService.getLentExpenses(userId)
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to fetch lent expenses: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get only owed splits for a user
     */
    suspend fun getOwedSplits(userId: Int): Result<OwedSplitsResponse> = withContext(Dispatchers.IO) {
        try {
            val response = RetrofitClient.dashboardApiService.getOwedSplits(userId)
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to fetch owed splits: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
} 