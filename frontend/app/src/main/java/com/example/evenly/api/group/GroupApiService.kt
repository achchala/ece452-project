package com.example.evenly.api.group

import com.example.evenly.api.group.models.AddMemberRequest
import com.example.evenly.api.group.models.CreateGroupRequest
import com.example.evenly.api.group.models.CreateGroupResponse
import com.example.evenly.api.group.models.GetUserGroupsRequest
import com.example.evenly.api.group.models.Group
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * API service interface for group operations. Handles creating groups and fetching user's groups.
 */
interface GroupApiService {
    @POST("/api/groups/create/")
    suspend fun createGroup(@Body request: CreateGroupRequest): Response<CreateGroupResponse>

    @POST("/api/groups/user-groups/")
    suspend fun getUserGroups(@Body request: GetUserGroupsRequest): Response<List<Group>>

    @GET("/api/groups/{groupId}/detail/")
    suspend fun getGroupById(@retrofit2.http.Path("groupId") groupId: Int): Response<Group>

    @POST("/api/groups/{groupId}/add-member/")
    suspend fun addMemberToGroup(
            @retrofit2.http.Path("groupId") groupId: Int,
            @Body request: AddMemberRequest
    ): Response<Unit>
}
