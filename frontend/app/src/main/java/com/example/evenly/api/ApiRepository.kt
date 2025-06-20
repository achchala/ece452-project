package com.example.evenly.api

import com.example.evenly.api.auth.AuthRepository
import com.example.evenly.api.dashboard.DashboardRepository

/**
 * Main API repository that coordinates all specialized API repositories.
 * Provides a single point of access to all API functionality across different domains.
 * Usage: ApiRepository.auth.login() or ApiRepository.user.getUserProfile()
 */
object ApiRepository {
    // Auth repository - static instance
    val auth: AuthRepository = AuthRepository
    
    // Dashboard repository - static instance
    val dashboard: DashboardRepository = DashboardRepository
    
    // Add more repositories here as needed
    // Example:
    // val payment: PaymentRepository = PaymentRepository()
    // val expense: ExpenseRepository = ExpenseRepository()
} 