package com.example.evenly.api.auth.models

data class User(
    val id: String,
    val email: String,
    val firebase_id: String,
    val name: String? = null
) 