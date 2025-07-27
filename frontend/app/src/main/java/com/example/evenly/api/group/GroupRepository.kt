package com.example.evenly.api.group

import com.example.evenly.api.RetrofitClient
import com.example.evenly.api.group.models.AddMemberRequest
import com.example.evenly.api.group.models.CreateGroupRequest
import com.example.evenly.api.group.models.CreateGroupResponse
import com.example.evenly.api.group.models.GetUserGroupsRequest
import com.example.evenly.api.group.models.Group
import com.example.evenly.api.group.models.GroupNotificationRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository class for group operations. Handles API calls to the backend for group-related
 * functionality.
 */
class GroupRepository {
    private val groupApiService = RetrofitClient.groupApiService

    /**
     * Creates a new group
     * @param name The name of the group
     * @param description Optional description of the group
     * @param firebaseId The Firebase ID of the user creating the group
     * @return Result containing either the created group or an exception
     */
    suspend fun createGroup(
            name: String,
            description: String?,
            firebaseId: String
    ): Result<CreateGroupResponse> =
            withContext(Dispatchers.IO) {
                try {
                    val request =
                            CreateGroupRequest(
                                    name = name,
                                    description = description,
                                    firebaseId = firebaseId
                            )

                    val response = groupApiService.createGroup(request)

                    if (response.isSuccessful) {
                        response.body()?.let { Result.success(it) }
                                ?: Result.failure(Exception("Empty response body"))
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
     * @param firebaseId The Firebase ID of the user
     * @return Result containing either the list of groups or an exception
     */
    suspend fun getUserGroups(firebaseId: String): Result<List<Group>> =
            withContext(Dispatchers.IO) {
                try {
                    val request = GetUserGroupsRequest(firebaseId = firebaseId)
                    val response = groupApiService.getUserGroups(request)

                    if (response.isSuccessful) {
                        response.body()?.let { Result.success(it) }
                                ?: Result.failure(Exception("Empty response body"))
                    } else {
                        val errorBody = response.errorBody()?.string() ?: "Unknown error"
                        Result.failure(Exception("Failed to get user groups: $errorBody"))
                    }
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }

    /**
     * Gets a specific group by ID
     * @param groupId The ID of the group to fetch
     * @return Result containing either the group or an exception
     */
    suspend fun getGroupById(groupId: String): Result<Group> =
            withContext(Dispatchers.IO) {
                try {
                    val response = groupApiService.getGroupById(groupId)

                    if (response.isSuccessful) {
                        response.body()?.let { Result.success(it) }
                                ?: Result.failure(Exception("Empty response body"))
                    } else {
                        val errorBody = response.errorBody()?.string() ?: "Unknown error"
                        Result.failure(Exception("Failed to get group: $errorBody"))
                    }
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }

    /**
     * Adds a member to a group
     * @param groupId The ID of the group
     * @param memberEmail The email of the member to add
     * @return Result containing either success or an exception
     */
    suspend fun addMemberToGroup(groupId: String, memberEmail: String): Result<Unit> =
            withContext(Dispatchers.IO) {
                try {
                    val firebaseUser =
                        com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                            ?: return@withContext Result.failure(Exception("User not authenticated"))

                    val request =
                            AddMemberRequest(
                                    firebaseId = firebaseUser.uid,
                                    memberEmail = memberEmail
                            )

                    val response = groupApiService.addMemberToGroup(groupId, request)

                    val notificationRequest =
                        GroupNotificationRequest(
                            email = memberEmail,
                            groupId = groupId
                        )

                    groupApiService.addedToGroupNotification(notificationRequest)

                    if (response.isSuccessful) {
                        Result.success(Unit)
                    } else {
                        val errorBody = response.errorBody()?.string() ?: "Unknown error"
                        Result.failure(Exception("Failed to add member: $errorBody"))
                    }
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }
}
