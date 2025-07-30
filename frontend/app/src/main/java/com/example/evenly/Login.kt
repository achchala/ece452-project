package com.example.evenly

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.lifecycle.lifecycleScope
import com.example.evenly.api.ApiRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class Login : ComponentActivity() {

    private lateinit var firebaseAuth: FirebaseAuth

    companion object {
        private const val TAG = "LoginActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        firebaseAuth = FirebaseAuth.getInstance()

        setContent {
            LoginScreen(
                onLogin = { email, password ->
                    Log.d("Login", "Login attempt with email: $email, password length: ${password.length}")
                    if (email.isNotEmpty() && password.isNotEmpty()) {
                        firebaseAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener {
                            Log.d("Login", "Firebase auth result: ${it.isSuccessful}")
                            if (it.isSuccessful) {
                                lifecycleScope.launch {
                                    val currentUser = it.result!!.user!!
                                    try {
                                        val result = ApiRepository.auth.getUser(currentUser.uid)
                                        result.fold(
                                            onSuccess = { response ->
                                                if (response.user.name.isNullOrBlank()) {
                                                    // User doesn't have a name set, go to NameCollection
                                                    val intent = Intent(this@Login, NameCollection::class.java)
                                                    startActivity(intent)
                                                } else {
                                                    // User has a name, go to MainActivity
                                                    val intent = Intent(this@Login, MainActivity::class.java)
                                                    intent.putExtra("user_name", response.user.name)
                                                    intent.putExtra("user_id", response.user.id)
                                                    startActivity(intent)
                                                }
                                            },
                                            onFailure = { exception ->
                                                Log.e("Login", "Failed to get user info", exception)
                                                // If we can't get user info, assume they need to set their name
                                                val intent = Intent(this@Login, NameCollection::class.java)
                                                startActivity(intent)
                                            }
                                        )
                                    } catch (e: Exception) {
                                        Log.e("Login", "Exception during user info fetch", e)
                                        // If there's an exception, assume they need to set their name
                                        val intent = Intent(this@Login, NameCollection::class.java)
                                        startActivity(intent)
                                    }
                                }
                            } else {
                                Log.e("Login", "Firebase auth failed: ${it.exception}")
                                Toast.makeText(this@Login, it.exception.toString(), Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        Log.d("Login", "Empty email or password")
                        Toast.makeText(this@Login, "Please enter email and password", Toast.LENGTH_SHORT).show()
                    }
                },
                onBackPressed = {
                    onBackPressedDispatcher.onBackPressed()
                },
                onSignUp = {
                    val intent = Intent(this@Login, Register::class.java)
                    startActivity(intent)
                }
            )
        }
    }

}
