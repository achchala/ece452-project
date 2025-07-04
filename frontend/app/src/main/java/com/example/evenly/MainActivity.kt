package com.example.evenly

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import com.example.evenly.ui.theme.HelloWorldTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize persistent group storage
        GroupStorage.initialize(this)

        // Get the user name and ID from intent if passed
        val userName = intent.getStringExtra("user_name")
        val userId = intent.getIntExtra("user_id", -1) // Use -1 to indicate not provided

        setContent {
            HelloWorldTheme {
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
    }
}
