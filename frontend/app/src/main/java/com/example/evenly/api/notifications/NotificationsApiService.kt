package com.example.evenly.api.notifications

import com.example.evenly.api.notifications.models.GetAllNotificationRequest
import com.example.evenly.api.notifications.models.Notification
import com.example.evenly.api.notifications.models.UpdateNotificationProcessedRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * API service interface for notification operations.
 */
interface NotificationsApiService {
    @POST("/api/notifications/get-all-notifications/")
    suspend fun getAllNotifications(
        @Body request: GetAllNotificationRequest
    ): Response<List<Notification>>

    @POST("/api/notifications/update-notification-processed/")
    suspend fun updateNotificationProcessed(
        @Body request: UpdateNotificationProcessedRequest
    ): Response<Unit>
}