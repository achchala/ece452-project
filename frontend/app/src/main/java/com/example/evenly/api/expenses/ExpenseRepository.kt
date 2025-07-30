package com.example.evenly.api.expenses

import com.example.evenly.api.expenses.models.*

class ExpenseRepository(private val expenseApiService: ExpenseApiService) {
    
    suspend fun createExpense(
        title: String,
        totalAmount: Int,
        firebaseId: String,
        groupId: String? = null,
        dueDate: String? = null,
        category: String? = null,
        splits: List<ExpenseSplit> = emptyList()
    ): Result<CreateExpenseResponse> {
        return try {
            val request = CreateExpenseRequest(title, totalAmount, firebaseId, groupId, dueDate, category, splits)
            val response = expenseApiService.createExpense(request)

            if (groupId != null) {
                val notificationRequest =
                    ExpenseNotificationRequest(
                        groupId = groupId,
                        expenseTitle = title
                    )

                expenseApiService.addedToExpenseNotification(notificationRequest)
            }

            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to create expense: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getUserExpenses(firebaseId: String): Result<GetUserExpensesResponse> {
        return try {
            val request = GetUserExpensesRequest(firebaseId)
            val response = expenseApiService.getUserExpenses(request)
            
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to get user expenses: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getGroupExpenses(groupId: String): Result<GetGroupExpensesResponse> {
        return try {
            val request = GetGroupExpensesRequest(groupId)
            val response = expenseApiService.getGroupExpenses(request)
            
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to get group expenses: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getUserGroupExpenses(firebaseId: String, groupId: String): Result<GetUserGroupExpensesResponse> {
        return try {
            val request = GetUserGroupExpensesRequest(firebaseId, groupId)
            val response = expenseApiService.getUserGroupExpenses(request)
            
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to get user group expenses: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getDashboardData(firebaseId: String): Result<GetDashboardDataResponse> {
        return try {
            val request = GetDashboardDataRequest(firebaseId)
            val response = expenseApiService.getDashboardData(request)
            
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to get dashboard data: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun requestPaymentConfirmation(splitId: String, userId: String): Result<PaymentRequestResponse> {
        return try {
            val request = PaymentRequestRequest(splitId, userId)
            val response = expenseApiService.requestPaymentConfirmation(request)
            
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to request payment confirmation: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun confirmPayment(splitId: String, lenderId: String): Result<PaymentConfirmResponse> {
        return try {
            val request = PaymentConfirmRequest(splitId, lenderId)
            val response = expenseApiService.confirmPayment(request)
            
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to confirm payment: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun rejectPayment(splitId: String, lenderId: String): Result<PaymentRejectResponse> {
        return try {
            val request = PaymentRejectRequest(splitId, lenderId)
            val response = expenseApiService.rejectPayment(request)
            
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to reject payment: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPendingPaymentRequests(lenderId: String): Result<PendingPaymentRequestsResponse> {
        return try {
            val response = expenseApiService.getPendingPaymentRequests(lenderId)
            
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to get pending payment requests: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
} 