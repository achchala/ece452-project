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

    suspend fun updateExpense(
        expenseId: String,
        title: String,
        totalAmount: Int,
        category: String?,
        dueDate: String?,
        firebaseId: String
    ): Result<UpdateExpenseResponse> {
        return try {
            val request = UpdateExpenseRequest(title, totalAmount, firebaseId, dueDate, category)
            val response = expenseApiService.updateExpense(expenseId, request)
            
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to update expense: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteExpense(
        expenseId: String,
        firebaseId: String
    ): Result<DeleteExpenseResponse> {
        return try {
            val request = DeleteExpenseRequest(firebaseId)
            val response = expenseApiService.deleteExpense(expenseId, request)
            
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to delete expense: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
} 