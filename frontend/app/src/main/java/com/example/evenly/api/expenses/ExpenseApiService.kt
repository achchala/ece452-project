package com.example.evenly.api.expenses

import com.example.evenly.api.expenses.models.CreateExpenseRequest
import com.example.evenly.api.expenses.models.CreateExpenseResponse
import com.example.evenly.api.expenses.models.GetUserExpensesRequest
import com.example.evenly.api.expenses.models.GetUserExpensesResponse
import com.example.evenly.api.expenses.models.GetGroupExpensesRequest
import com.example.evenly.api.expenses.models.GetGroupExpensesResponse
import com.example.evenly.api.expenses.models.GetUserGroupExpensesRequest
import com.example.evenly.api.expenses.models.GetUserGroupExpensesResponse
import com.example.evenly.api.expenses.models.GetDashboardDataRequest
import com.example.evenly.api.expenses.models.GetDashboardDataResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ExpenseApiService {
    @POST("/api/expenses/create/")
    suspend fun createExpense(@Body request: CreateExpenseRequest): Response<CreateExpenseResponse>

    @POST("/api/expenses/user-expenses/")
    suspend fun getUserExpenses(@Body request: GetUserExpensesRequest): Response<GetUserExpensesResponse>

    @POST("/api/expenses/group-expenses/")
    suspend fun getGroupExpenses(@Body request: GetGroupExpensesRequest): Response<GetGroupExpensesResponse>

    @POST("/api/expenses/user-group-expenses/")
    suspend fun getUserGroupExpenses(@Body request: GetUserGroupExpensesRequest): Response<GetUserGroupExpensesResponse>

    @POST("/api/expenses/dashboard/")
    suspend fun getDashboardData(@Body request: GetDashboardDataRequest): Response<GetDashboardDataResponse>
} 