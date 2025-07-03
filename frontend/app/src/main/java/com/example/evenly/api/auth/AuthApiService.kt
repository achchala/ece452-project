package com.example.evenly.api.auth

import com.example.evenly.api.auth.models.RegisterRequest
import com.example.evenly.api.auth.models.RegisterResponse
import com.example.evenly.api.auth.models.UpdateNameRequest
import com.example.evenly.api.auth.models.UpdateNameResponse
import com.example.evenly.api.auth.models.GetUserRequest
import com.example.evenly.api.auth.models.GetUserResponse

import retrofit2.Response
import retrofit2.http.*

/**
 * API service interface for authentication operations.
 * Handles login, registration, logout, token refresh, and password recovery.
 */
interface AuthApiService {
    @POST("api/auth/register/")
    suspend fun register(@Body registerRequest: RegisterRequest): Response<RegisterResponse>
    
    @POST("api/auth/update-name/")
    suspend fun updateName(@Body updateNameRequest: UpdateNameRequest): Response<UpdateNameResponse>
    
    @POST("api/auth/get-user/")
    suspend fun getUser(@Body getUserRequest: GetUserRequest): Response<GetUserResponse>
} 