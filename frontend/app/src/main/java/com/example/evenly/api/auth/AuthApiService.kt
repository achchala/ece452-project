package com.example.evenly.api.auth

import com.example.evenly.api.auth.models.RegisterRequest
import com.example.evenly.api.auth.models.RegisterResponse

import retrofit2.Response
import retrofit2.http.*

/**
 * API service interface for authentication operations.
 * Handles login, registration, logout, token refresh, and password recovery.
 */
interface AuthApiService {
    @POST("/api/auth/register/")
    suspend fun register(@Body registerRequest: RegisterRequest): Response<RegisterResponse>
} 