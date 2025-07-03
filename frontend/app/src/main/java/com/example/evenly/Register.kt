package com.example.evenly

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.evenly.api.ApiRepository
import com.example.evenly.api.auth.AuthRepository
import com.example.evenly.databinding.ActivityRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class Register : AppCompatActivity() {
    private lateinit var binding:ActivityRegisterBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        firebaseAuth = FirebaseAuth.getInstance()
        authRepository = ApiRepository.auth

        binding.registered.setOnClickListener{
            val intent = Intent(this, Login::class.java)
            startActivity(intent)
        }
        
        binding.btnRegister.setOnClickListener{
            val email = binding.email.text.toString()
            val password = binding.password.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) { registerUser(email, password)
            } else {
                Toast.makeText(this, "Empty fields are not allowed.", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun registerUser(email: String, password: String, retryCount: Int = 0) {
        // Show loading indicator
        binding.btnRegister.isEnabled = false
        binding.btnRegister.text = "Creating Account..."
        
        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                binding.btnRegister.isEnabled = true
                binding.btnRegister.text = "Register"
                
                if (task.isSuccessful) {
                    Log.d("TAG", "register check")
                    lifecycleScope.launch {
                        try {
                            val uid = task.result!!.user!!.uid
                            val result = async { authRepository.register(email, uid) }
                            result.await()
                            val intent = Intent(this@Register, NameCollection::class.java)
                            startActivity(intent)
                        } catch (e: Exception) {
                            Log.e("TAG", "Error registering user: ${e.message}")
                            Toast.makeText(this@Register, "Registration failed: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    val exception = task.exception
                    println(exception)
                    val errorMessage = when {
                        exception?.message?.contains("network", ignoreCase = true) == true -> 
                            "Network error. Please check your internet connection."
                        exception?.message?.contains("recaptcha", ignoreCase = true) == true -> 
                            "reCAPTCHA verification failed. Please try again."
                        exception?.message?.contains("timeout", ignoreCase = true) == true -> 
                            "Request timed out. Please try again."
                        exception?.message?.contains("unreachable", ignoreCase = true) == true -> 
                            "Server unreachable. Please try again."
                        else -> exception?.message ?: "Registration failed"
                    }
                    
                    // Retry logic for network errors
                    if ((exception?.message?.contains("network", ignoreCase = true) == true ||
                         exception?.message?.contains("timeout", ignoreCase = true) == true ||
                         exception?.message?.contains("unreachable", ignoreCase = true) == true) &&
                        retryCount < 2) {
                        
                        Toast.makeText(this, "Retrying... (${retryCount + 1}/3)", Toast.LENGTH_SHORT).show()
                        lifecycleScope.launch {
                            delay(2000) // Wait 2 seconds before retry
                            registerUser(email, password, retryCount + 1)
                        }
                    } else {
                        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                        Log.e("TAG", "Registration failed: $exception")
                    }
                }
            }
    }
}