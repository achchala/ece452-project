package com.example.evenly.api.dashboard.models

import com.google.gson.annotations.SerializedName

/**
 * Response model for the complete dashboard data
 */
data class DashboardResponse(
    val lent: LentData,
    val owed: OwedData
)

/**
 * Data model for lent expenses section
 */
data class LentData(
    @SerializedName("total_amount")
    val totalAmount: Long,
    val expenses: List<Expense>
)

/**
 * Data model for owed splits section
 */
data class OwedData(
    @SerializedName("total_amount")
    val totalAmount: Long,
    val splits: List<Split>
)

/**
 * Response model for lent expenses only
 */
data class LentExpensesResponse(
    @SerializedName("lent_expenses")
    val lentExpenses: List<Expense>
)

/**
 * Response model for owed splits only
 */
data class OwedSplitsResponse(
    @SerializedName("owed_splits")
    val owedSplits: List<Split>
)

/**
 * Expense data model
 */
data class Expense(
    val id: String,
    val title: String,
    @SerializedName("total_amount")
    val totalAmount: Long,
    @SerializedName("created_by")
    val createdBy: String,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("due_date")
    val dueDate: String? = null,
    @SerializedName("category")
    val category: String? = null,
    val splits: List<DebtorSplit> = emptyList()
)

/**
 * A model representing a split for a lent expense, showing who owes money.
 */
data class DebtorSplit(
    @SerializedName("amount_owed")
    val amountOwed: Long,
    val debtor: Debtor?
)

/**
 * A simple model for the user who owes money (the debtor).
 */
data class Debtor(
    val name: String?
)

/**
 * Split data model
 */
data class Split(
    val id: String,
    @SerializedName("expenseid")
    val expenseId: String,
    @SerializedName("userid")
    val userId: String,
    @SerializedName("amount_owed")
    val amountOwed: Long,
    val expense: OwedExpenseDetails? = null
)

/**
 * A model for the details of an expense that is owed.
 */
data class OwedExpenseDetails(
    val title: String,
    @SerializedName("due_date")
    val dueDate: String? = null,
    val lender: Lender? = null
)

/**
 * A simple model for the person who lent the money.
 */
data class Lender(
    val name: String? = null
) 