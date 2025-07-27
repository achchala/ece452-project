package com.example.evenly.api.notifications.models

import com.google.gson.annotations.SerializedName

data class Notification(
    @SerializedName("user_id") val userId: String,
    @SerializedName("notification_id") val notificationId: String,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("processed") val processed: Boolean,
    @SerializedName("notification_message") val notificationMessage: String,
)

data class GetAllNotificationRequest(
    @SerializedName("firebaseId") val firebaseId: String
)

data class UpdateNotificationProcessedRequest(
    @SerializedName("notificationId") val notificationId: String
)