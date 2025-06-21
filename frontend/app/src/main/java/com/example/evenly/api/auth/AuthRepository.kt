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
    
    suspend fun updateName(firebaseId: String, name: String): Result<UpdateNameResponse> {
        return try {
            val response = authApiService.updateName(UpdateNameRequest(firebaseId, name))
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Name update failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getUser(firebaseId: String): Result<GetUserResponse> {
        return try {
            val response = authApiService.getUser(GetUserRequest(firebaseId))
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Get user failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
} 