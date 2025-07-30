package com.example.evenly

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

class WelcomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WelcomeScreen(
                onAnimationComplete = {
                    val intent = Intent(this@WelcomeActivity, Login::class.java)
                    startActivity(intent)
                    finish()
                }
            )
        }
    }
} 