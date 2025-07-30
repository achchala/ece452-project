package com.example.evenly.api.expenses

import com.example.evenly.api.expenses.models.CreateExpenseRequest
import com.example.evenly.api.expenses.models.CreateExpenseResponse
import com.example.evenly.api.expenses.models.ExpenseNotificationRequest
import com.example.evenly.api.expenses.models.GetUserExpensesRequest
import com.example.evenly.api.expenses.models.GetUserExpensesResponse
import com.example.evenly.api.expenses.models.GetGroupExpensesRequest
import com.example.evenly.api.expenses.models.GetGroupExpensesResponse
import com.example.evenly.api.expenses.models.GetUserGroupExpensesRequest
import com.example.evenly.api.expenses.models.GetUserGroupExpensesResponse
import com.example.evenly.api.expenses.models.GetDashboardDataRequest
import com.example.evenly.api.expenses.models.GetDashboardDataResponse
import com.example.evenly.api.expenses.models.UpdateExpenseRequest
import com.example.evenly.api.expenses.models.UpdateExpenseResponse
import com.example.evenly.api.expenses.models.DeleteExpenseRequest
import com.example.evenly.api.expenses.models.DeleteExpenseResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.DELETE
import retrofit2.http.Path

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

    @POST("/api/expenses/expense-notification/")
    suspend fun addedToExpenseNotification(
        @Body request: ExpenseNotificationRequest
    ): Response<Unit>

    @PUT("/api/expenses/{expenseId}/update/")
    suspend fun updateExpense(
        @Path("expenseId") expenseId: String,
        @Body request: UpdateExpenseRequest
    ): Response<UpdateExpenseResponse>

    @DELETE("/api/expenses/{expenseId}/delete/")
    suspend fun deleteExpense(
        @Path("expenseId") expenseId: String,
        @Body request: DeleteExpenseRequest
    ): Response<DeleteExpenseResponse>
} 