package com.example.evenly.api.group

import com.example.evenly.api.group.models.CreateGroupRequest
import com.example.evenly.api.group.models.CreateGroupResponse
import com.example.evenly.api.group.models.Group
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * API service interface for group operations.
 * Handles creating groups and fetching user's groups.
 */
interface GroupApiService {
    @POST("/api/groups/create/")
    suspend fun createGroup(@Body request: CreateGroupRequest): Response<CreateGroupResponse>
    
    @GET("/api/groups/user-groups/")
    suspend fun getUserGroups(@Query("user_id") userId: Int): Response<List<Group>>
} 