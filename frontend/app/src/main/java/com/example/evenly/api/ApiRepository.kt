package com.example.evenly.api

import com.example.evenly.api.auth.AuthRepository

/**
 * Main API repository that coordinates all specialized API repositories.
 * Provides a single point of access to all API functionality across different domains.
 * Usage: ApiRepository.auth.login() or ApiRepository.user.getUserProfile()
 */
object ApiRepository {
    // Auth repository - static instance
    val auth: AuthRepository = AuthRepository
    
    // Add more repositories here as needed
    // Example:
    // val payment: PaymentRepository = PaymentRepository()
    // val expense: ExpenseRepository = ExpenseRepository()
} 