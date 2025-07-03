package com.example.evenly.api.group

import com.example.evenly.api.RetrofitClient
import com.example.evenly.api.group.models.CreateGroupRequest
import com.example.evenly.api.group.models.CreateGroupResponse
import com.example.evenly.api.group.models.Group
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository class for group operations.
 * Handles API calls to the backend for group-related functionality.
 */
class GroupRepository {
    private val groupApiService = RetrofitClient.groupApiService
    
    /**
     * Creates a new group
     * @param name The name of the group
     * @param description Optional description of the group
     * @param creatorId The ID of the user creating the group
     * @return Result containing either the created group or an exception
     */
    suspend fun createGroup(
        name: String,
        description: String?,
        creatorId: Int
    ): Result<CreateGroupResponse> = withContext(Dispatchers.IO) {
        try {
            val request = CreateGroupRequest(
                name = name,
                description = description,
                creatorId = creatorId
            )
            
            val response = groupApiService.createGroup(request)
            
            if (response.isSuccessful) {
                response.body()?.let {
                    Result.success(it)
                } ?: Result.failure(Exception("Empty response body"))
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                Result.failure(Exception("Failed to create group: $errorBody"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Gets all groups for a user
     * @param userId The ID of the user
     * @return Result containing either the list of groups or an exception
     */
    suspend fun getUserGroups(userId: Int): Result<List<Group>> = withContext(Dispatchers.IO) {
        try {
            val response = groupApiService.getUserGroups(userId)
            
            if (response.isSuccessful) {
                response.body()?.let {
                    Result.success(it)
                } ?: Result.failure(Exception("Empty response body"))
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                Result.failure(Exception("Failed to get user groups: $errorBody"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
} 