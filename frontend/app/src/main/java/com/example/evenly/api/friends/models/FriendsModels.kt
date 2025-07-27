package com.example.evenly.api.friends.models

import com.google.gson.annotations.SerializedName

data class FriendNotificationRequest(
        @SerializedName("from_user_email") val fromUser: String,
        @SerializedName("to_user_email") val toUser: String
)
