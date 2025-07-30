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
import com.example.evenly.api.expenses.models.PaymentRequestRequest
import com.example.evenly.api.expenses.models.PaymentRequestResponse
import com.example.evenly.api.expenses.models.PaymentConfirmRequest
import com.example.evenly.api.expenses.models.PaymentConfirmResponse
import com.example.evenly.api.expenses.models.PaymentRejectRequest
import com.example.evenly.api.expenses.models.PaymentRejectResponse
import com.example.evenly.api.expenses.models.PendingPaymentRequestsResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

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

    @POST("/api/expenses/request-payment/")
    suspend fun requestPaymentConfirmation(@Body request: PaymentRequestRequest): Response<PaymentRequestResponse>

    @POST("/api/expenses/confirm-payment/")
    suspend fun confirmPayment(@Body request: PaymentConfirmRequest): Response<PaymentConfirmResponse>

    @POST("/api/expenses/reject-payment/")
    suspend fun rejectPayment(@Body request: PaymentRejectRequest): Response<PaymentRejectResponse>

    @GET("/api/expenses/pending-payments/")
    suspend fun getPendingPaymentRequests(@Query("lender_id") lenderId: String): Response<PendingPaymentRequestsResponse>
} 