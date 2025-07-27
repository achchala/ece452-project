package com.example.evenly.api.friends

import com.example.evenly.api.friends.models.FriendNotificationRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface FriendsApiService {
    @GET("/api/friend/get-all-requests/")
    suspend fun getIncomingRequests(@Query("username") username: String): GetRequestsResponse
    
    @GET("/api/friend/get-outgoing-requests/")
    suspend fun getOutgoingRequests(@Query("username") username: String): GetRequestsResponse
    
    @GET("/api/friend/get-friends/")
    suspend fun getFriends(@Query("username") username: String): GetFriendsResponse
    
    @POST("/api/friend/add-friend/")
    suspend fun sendFriendRequest(@Body request: SendFriendRequestRequest): SendFriendRequestResponse
    
    @POST("/api/friend/accept-friend/")
    suspend fun acceptFriendRequest(@Body request: AcceptFriendRequestRequest): AcceptFriendRequestResponse
    
    @POST("/api/friend/reject-friend/")
    suspend fun rejectFriendRequest(@Body request: RejectFriendRequestRequest): RejectFriendRequestResponse

    @POST("/api/friend/friend-request-notification/")
    suspend fun friendRequestNotification(
        @Body request: FriendNotificationRequest
    ): Response<Unit>
}

data class FriendRequest(
    val id: String,
    val from_user: String,
    val to_user: String,
    val request_completed: Boolean,
    val created_at: String
)

data class GetRequestsResponse(
    val requests: List<FriendRequest>
)

data class GetFriendsResponse(
    val friends: List<FriendRequest>
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