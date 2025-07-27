package com.example.evenly.api.notifications

import com.example.evenly.api.RetrofitClient
import com.example.evenly.api.notifications.models.GetAllNotificationRequest
import com.example.evenly.api.notifications.models.Notification
import com.example.evenly.api.notifications.models.UpdateNotificationProcessedRequest
import retrofit2.Response

class NotificationsRepository {
    private val notificationsApiService = RetrofitClient.notificationsApiService

    suspend fun getAllNotifications(userId: String): Response<List<Notification>> {
        val request = GetAllNotificationRequest(
            firebaseId = userId
        )
        return notificationsApiService.getAllNotifications(request)
    }

    suspend fun updateNotificationsProcessed(notificationId: String): Response<Unit> {
        val request = UpdateNotificationProcessedRequest(
            notificationId = notificationId
        )

        return notificationsApiService.updateNotificationProcessed(request)
    }
}