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
import com.example.evenly.databinding.ActivityNameCollectionBinding
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class NameCollection : AppCompatActivity() {
    private lateinit var binding: ActivityNameCollectionBinding
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNameCollectionBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        firebaseAuth = FirebaseAuth.getInstance()

        binding.btnContinue.setOnClickListener {
            val name = binding.nameInput.text.toString().trim()

            if (name.isNotEmpty()) {
                // Basic name validation
                if (name.length < 2) {
                    Toast.makeText(this, "Name must be at least 2 characters long.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                
                if (name.length > 50) {
                    Toast.makeText(this, "Name must be less than 50 characters long.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                
                // Check for valid name characters (letters, spaces, hyphens, apostrophes only)
                if (!name.matches(Regex("^[a-zA-Z\\s\\-']+$"))) {
                    Toast.makeText(this, "Name can only contain letters, spaces, hyphens, and apostrophes.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                lifecycleScope.launch {
                    try {
                        val currentUser = firebaseAuth.currentUser
                        if (currentUser != null) {
                            val result = ApiRepository.auth.updateName(currentUser.uid, name)
                            result.fold(
                                onSuccess = { response ->
                                    Log.d("NameCollection", "Name updated successfully: ${response.message}")
                                    val intent = Intent(this@NameCollection, MainActivity::class.java)
                                    intent.putExtra("user_name", name)
                                    startActivity(intent)
                                },
                                onFailure = { exception ->
                                    Log.e("NameCollection", "Failed to update name", exception)
                                    Toast.makeText(this@NameCollection, "Failed to save name: ${exception.message}", Toast.LENGTH_LONG).show()
                                }
                            )
                        } else {
                            Toast.makeText(this@NameCollection, "User not authenticated", Toast.LENGTH_LONG).show()
                        }
                    } catch (e: Exception) {
                        Log.e("NameCollection", "Exception during name update", e)
                        Toast.makeText(this@NameCollection, "An error occurred: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                Toast.makeText(this, "Please enter your name.", Toast.LENGTH_SHORT).show()
            }
        }
    }
} 