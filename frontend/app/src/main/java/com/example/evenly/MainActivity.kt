package com.example.evenly

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.example.evenly.api.ApiRepository
import com.example.evenly.ui.theme.HelloWorldTheme
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Get the user name and ID from intent if passed
        val userName = intent.getStringExtra("user_name")
        val userId = intent.getIntExtra("user_id", -1) // Use -1 to indicate not provided
        
        setContent {
            HelloWorldTheme {
                DashboardScreen(
                    userId = userId,
                    userName = userName,
                    onLogout = {
                        lifecycleScope.launch {
//                            ApiRepository.auth.logout()
                            val intent = Intent(this@MainActivity, Login::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()
                        }
                    }
                )
            }
        }
    }
}