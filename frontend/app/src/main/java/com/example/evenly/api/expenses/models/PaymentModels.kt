package com.example.evenly.api.expenses.models

import com.google.gson.annotations.SerializedName

/**
 * Request model for requesting payment confirmation
 */
data class PaymentRequestRequest(
    @SerializedName("split_id") val splitId: String,
    @SerializedName("user_id") val userId: String
)

/**
 * Response model for payment request
 */
data class PaymentRequestResponse(
    val message: String,
    val split: Split
)

/**
 * Request model for confirming payment
 */
data class PaymentConfirmRequest(
    @SerializedName("split_id") val splitId: String,
    @SerializedName("lender_id") val lenderId: String
)

/**
 * Response model for payment confirmation
 */
data class PaymentConfirmResponse(
    val message: String,
    val split: Split
)

/**
 * Request model for rejecting payment
 */
data class PaymentRejectRequest(
    @SerializedName("split_id") val splitId: String,
    @SerializedName("lender_id") val lenderId: String
)

/**
 * Response model for payment rejection
 */
data class PaymentRejectResponse(
    val message: String,
    val split: Split
)

/**
 * Model for pending payment request
 */
data class PendingPaymentRequest(
    val id: String,
    @SerializedName("amount_owed") val amountOwed: Long,
    @SerializedName("paid_request") val paidRequest: String,
    val debtor: PendingDebtor,
    val expense: PendingExpenseDetails
)

/**
 * Model for debtor in pending payment request
 */
data class PendingDebtor(
    val name: String,
    val id: String
)

/**
 * Model for expense details in pending payment request
 */
data class PendingExpenseDetails(
    val title: String,
    val id: String
)

/**
 * Response model for pending payment requests
 */
data class PendingPaymentRequestsResponse(
    @SerializedName("pending_requests") val pendingRequests: List<PendingPaymentRequest>
) 