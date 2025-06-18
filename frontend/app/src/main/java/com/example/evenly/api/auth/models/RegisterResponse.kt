package com.example.evenly.api.auth.models

import java.time.LocalDate

data class RegisterResponse(
    val message: String,
    val user: User
)

data class User (
    val email: String,
    val firebaseId: String,
    val dateJoined: LocalDate
)