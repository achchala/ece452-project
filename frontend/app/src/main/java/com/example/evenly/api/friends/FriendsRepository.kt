package com.example.evenly.api.friends

import com.example.evenly.api.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object FriendsRepository {
    private val friendsApiService = RetrofitClient.friendsApiService
    
    suspend fun getIncomingRequests(username: String): Result<List<FriendRequest>> = withContext(Dispatchers.IO) {
        try {
            val response = friendsApiService.getIncomingRequests(username)
            Result.success(response.requests)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun sendFriendRequest(fromUser: String, toUser: String): Result<SendFriendRequestResponse> = withContext(Dispatchers.IO) {
        try {
            val request = SendFriendRequestRequest(fromUser, toUser)
            val response = friendsApiService.sendFriendRequest(request)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun acceptFriendRequest(fromUser: String, toUser: String): Result<AcceptFriendRequestResponse> = withContext(Dispatchers.IO) {
        try {
            val request = AcceptFriendRequestRequest(fromUser, toUser)
            val response = friendsApiService.acceptFriendRequest(request)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun rejectFriendRequest(fromUser: String, toUser: String): Result<RejectFriendRequestResponse> = withContext(Dispatchers.IO) {
        try {
            val request = RejectFriendRequestRequest(fromUser, toUser)
            val response = friendsApiService.rejectFriendRequest(request)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
} 