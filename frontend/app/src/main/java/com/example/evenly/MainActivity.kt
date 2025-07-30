package com.example.evenly

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.evenly.api.ApiRepository
import com.example.evenly.ui.theme.HelloWorldTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Group storage is no longer needed as we use backend API
        // Clear any existing local groups to ensure only backend groups are used
        GroupStorage.initialize(this)
        GroupStorage.clearAllLocalGroups()

        // Get the user name and ID from intent if passed
        val userName = intent.getStringExtra("user_name")
        val userId = intent.getStringExtra("user_id") ?: ""

        setContent {
            HelloWorldTheme(dynamicColor = false) {
                MainScreen(
                        userId = userId,
                        userName = userName,
                        onLogout = {
                            Firebase.auth.signOut()
                            val intent = Intent(this, Login::class.java)
                            startActivity(intent)
                            finish()
                        }
                )
            }
        }

        val channel = NotificationChannel(
            "evenly_notification_id",
            "Evenly Notification Channel",
            NotificationManager.IMPORTANCE_DEFAULT
        )

        channel.description = "Channel for app notifications"

        val notificationManager = getSystemService(
            NotificationManager::class.java
        )
        notificationManager.createNotificationChannel(channel)

        // polls for new notifications every x seconds
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                var notificationId = 0
                while (userId != "") {

                    // get new notifications
                    val response = ApiRepository.notifications.getAllNotifications(userId)

                    // display notifications
                    response.body()?.forEach{ notification ->
                        val builder = NotificationCompat.Builder(this@MainActivity, "evenly_notification_id")
                            .setContentTitle("Evenly Notification")
                            .setContentText(notification.notificationMessage)
                            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                            .setSmallIcon(R.drawable.small_icon_notification)

                        with(NotificationManagerCompat.from(this@MainActivity)) {
                            if (ActivityCompat.checkSelfPermission(
                                    this@MainActivity,
                                    Manifest.permission.POST_NOTIFICATIONS
                                ) != PackageManager.PERMISSION_GRANTED
                            ) {
                                return@with
                            }

                            notify(notificationId, builder.build())
                            notificationId++

                            // mark current notification as processed so it is not shown again
                            ApiRepository.notifications.updateNotificationsProcessed(notification.notificationId)
                        }
                    }



                    delay(10_000L)  // repeat every 10 seconds
                }
            }
        }
    }

}
