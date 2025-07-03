package com.example.evenly.api.friends

import retrofit2.http.*

interface FriendsApiService {
    @GET("/api/friend/get-all-requests/")
    suspend fun getIncomingRequests(@Query("username") username: String): GetRequestsResponse
    
    @POST("/api/friend/add-friend/")
    suspend fun sendFriendRequest(@Body request: SendFriendRequestRequest): SendFriendRequestResponse
    
    @POST("/api/friend/accept-friend/")
    suspend fun acceptFriendRequest(@Body request: AcceptFriendRequestRequest): AcceptFriendRequestResponse
    
    @POST("/api/friend/reject-friend/")
    suspend fun rejectFriendRequest(@Body request: RejectFriendRequestRequest): RejectFriendRequestResponse
}

data class FriendRequest(
    val id: Int,
    val from_user: String,
    val to_user: String,
    val request_completed: Boolean,
    val created_at: String
)

data class GetRequestsResponse(
    val requests: List<FriendRequest>
)

data class SendFriendRequestRequest(
    val from_user: String,
    val to_user: String
)

data class SendFriendRequestResponse(
    val status: String,
    val data: FriendRequest?
)

data class AcceptFriendRequestRequest(
    val from_user: String,
    val to_user: String
)

data class AcceptFriendRequestResponse(
    val status: String
)

data class RejectFriendRequestRequest(
    val from_user: String,
    val to_user: String
)

data class RejectFriendRequestResponse(
    val status: String
) 