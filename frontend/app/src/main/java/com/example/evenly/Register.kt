package com.example.evenly

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.example.evenly.api.ApiRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class Register : ComponentActivity() {
    private lateinit var firebaseAuth: FirebaseAuth

    companion object {
        private const val TAG = "RegisterActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        firebaseAuth = FirebaseAuth.getInstance()

        setContent {
            RegisterScreen(
                onRegister = { name, email, password ->
                    Log.d(TAG, "Register attempt with name: $name, email: $email, password length: ${password.length}")
                    if (name.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty()) {
                        registerUser(name, email, password)
                    } else {
                        Toast.makeText(this, "All fields are required.", Toast.LENGTH_SHORT).show()
                    }
                },
                onBackPressed = {
                    onBackPressedDispatcher.onBackPressed()
                },
                onSignIn = {
                    val intent = Intent(this, Login::class.java)
                    startActivity(intent)
                }
            )
        }
    }
    
    private fun registerUser(name: String, email: String, password: String) {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Firebase registration successful")
                    lifecycleScope.launch {
                        try {
                            val uid = task.result!!.user!!.uid
                            val result = ApiRepository.auth.register(email, uid)
                            result.fold(
                                onSuccess = { response ->
                                    Log.d(TAG, "User registered successfully in backend")
                                    // Update name immediately using the name from the form
                                    val nameResult = ApiRepository.auth.updateName(uid, name)
                                    nameResult.fold(
                                        onSuccess = { nameResponse ->
                                            Log.d(TAG, "Name updated successfully: ${nameResponse.message}")
                                            // Get the user info to get the user ID
                                            val userResult = ApiRepository.auth.getUser(uid)
                                            userResult.fold(
                                                onSuccess = { userResponse ->
                                                    val intent = Intent(this@Register, MainActivity::class.java)
                                                    intent.putExtra("user_name", name)
                                                    intent.putExtra("user_id", userResponse.user.id)
                                                    startActivity(intent)
                                                    finish()
                                                },
                                                onFailure = { exception ->
                                                    Log.e(TAG, "Failed to get user info", exception)
                                                    Toast.makeText(this@Register, "Failed to get user info: ${exception.message}", Toast.LENGTH_LONG).show()
                                                }
                                            )
                                        },
                                        onFailure = { exception ->
                                            Log.e(TAG, "Failed to update name", exception)
                                            Toast.makeText(this@Register, "Failed to save name: ${exception.message}", Toast.LENGTH_LONG).show()
                                        }
                                    )
                                },
                                onFailure = { exception ->
                                    Log.e(TAG, "Failed to register user in backend", exception)
                                    Toast.makeText(this@Register, "Registration failed: ${exception.message}", Toast.LENGTH_LONG).show()
                                }
                            )
                        } catch (e: Exception) {
                            Log.e(TAG, "Exception during registration", e)
                            Toast.makeText(this@Register, "Registration failed: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    Log.e(TAG, "Firebase registration failed: ${task.exception}")
                    val errorMessage = task.exception?.message ?: "Registration failed"
                    Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                }
            }
    }
}