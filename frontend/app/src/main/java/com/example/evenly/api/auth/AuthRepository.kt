package com.example.evenly.api.auth

import com.example.evenly.api.RetrofitClient
import com.example.evenly.api.auth.models.*

/**
 * Repository for authentication operations.
 * Provides clean API for login, registration, logout, and token management with error handling.
 */
object AuthRepository {
    private val authApiService = RetrofitClient.authApiService
    
    suspend fun register(email: String, firebaseId: String): Result<RegisterResponse> {
        return try {
            val response = authApiService.register(RegisterRequest(email, firebaseId))
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Registration failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
} 