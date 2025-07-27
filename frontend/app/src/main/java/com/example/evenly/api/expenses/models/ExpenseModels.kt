package com.example.evenly.api.expenses.models

import com.google.gson.annotations.SerializedName

// Request Models
data class CreateExpenseRequest(
    @SerializedName("title") val title: String,
    @SerializedName("totalAmount") val totalAmount: Int,
    @SerializedName("firebaseId") val firebaseId: String,
    @SerializedName("groupId") val groupId: String? = null,
    @SerializedName("splits") val splits: List<ExpenseSplit> = emptyList()
)

data class GetUserExpensesRequest(
    @SerializedName("firebaseId") val firebaseId: String
)

data class GetGroupExpensesRequest(
    @SerializedName("groupId") val groupId: String
)

data class GetUserGroupExpensesRequest(
    @SerializedName("firebaseId") val firebaseId: String,
    @SerializedName("groupId") val groupId: String
)

data class GetDashboardDataRequest(
    @SerializedName("firebaseId") val firebaseId: String
)

// Response Models
data class CreateExpenseResponse(
    @SerializedName("message") val message: String,
    @SerializedName("expense") val expense: Expense
)

data class GetUserExpensesResponse(
    @SerializedName("lent_expenses") val lentExpenses: List<Expense>,
    @SerializedName("owed_splits") val owedSplits: List<Split>
)

data class GetGroupExpensesResponse(
    @SerializedName("expenses") val expenses: List<Expense>
)

data class GetUserGroupExpensesResponse(
    @SerializedName("created") val created: List<Expense>,
    @SerializedName("owed") val owed: List<Expense>
)

data class GetDashboardDataResponse(
    @SerializedName("lent") val lent: LentData,
    @SerializedName("owed") val owed: OwedData
)

// Data Models
data class Expense(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("total_amount") val totalAmount: Int,
    @SerializedName("created_by") val createdBy: String,
    @SerializedName("group_id") val groupId: String? = null,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("splits") val splits: List<Split> = emptyList()
)

data class Split(
    @SerializedName("id") val id: String,
    @SerializedName("expenseid") val expenseId: String,
    @SerializedName("userid") val userId: String,
    @SerializedName("amount_owed") val amountOwed: Int,
    @SerializedName("created_at") val createdAt: String
)

data class ExpenseSplit(
    @SerializedName("userEmail") val userEmail: String,
    @SerializedName("amountOwed") val amountOwed: Int
)

data class LentData(
    @SerializedName("total_amount") val totalAmount: Int,
    @SerializedName("expenses") val expenses: List<Expense>
)

data class OwedData(
    @SerializedName("total_amount") val totalAmount: Int,
    @SerializedName("splits") val splits: List<Split>
)

data class ExpenseNotificationRequest(
    @SerializedName("groupId") val groupId: String,
    @SerializedName("expenseTitle") val expenseTitle: String
)