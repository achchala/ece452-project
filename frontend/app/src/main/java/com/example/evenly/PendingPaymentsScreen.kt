package com.example.evenly

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.evenly.api.expenses.models.PendingPaymentRequest
import com.example.evenly.api.expenses.models.PendingPaymentRequestsResponse
import com.example.evenly.api.ApiRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PendingPaymentsScreen(
    userId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var pendingRequests by remember { mutableStateOf<List<PendingPaymentRequest>?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var retryCounter by remember { mutableStateOf(0) }

    LaunchedEffect(userId, retryCounter) {
        isLoading = true
        error = null
        pendingRequests = null

        try {
            val result = ApiRepository.expenses.getPendingPaymentRequests(userId)
            result.fold(
                onSuccess = { response -> pendingRequests = response.pendingRequests },
                onFailure = { exception ->
                    error = exception.message ?: "Failed to load pending payments"
                }
            )
        } catch (e: Exception) {
            error = e.message ?: "Failed to load pending payments"
        }
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pending Payments") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        // You can add a back arrow icon here
                        Text("←")
                    }
                }
            )
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .padding(paddingValues)
        ) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        color = Color(0xFFFF7024) // Orange color
                    )
                }
            } else if (error != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Error",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = error!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { retryCounter++ }) { Text("Retry") }
                    }
                }
            } else {
                pendingRequests?.let { requests ->
                    if (requests.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "No Pending Payments",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "You don't have any pending payment requests",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(requests) { request ->
                                PendingPaymentCard(
                                    request = request,
                                    lenderId = userId,
                                    onConfirm = { 
                                        // Refresh the list after confirming
                                        retryCounter++ 
                                    },
                                    onReject = { 
                                        // Refresh the list after rejecting
                                        retryCounter++ 
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PendingPaymentCard(
    request: PendingPaymentRequest,
    lenderId: String,
    onConfirm: () -> Unit,
    onReject: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showConfirmDialog by remember { mutableStateOf(false) }
    var showRejectDialog by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = request.expense.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "From: ${request.debtor.name}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "$${"%.2f".format(request.amountOwed / 100.0)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { showRejectDialog = true },
                    enabled = !isProcessing,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Reject")
                }

                Button(
                    onClick = { showConfirmDialog = true },
                    enabled = !isProcessing,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF55BF6E)
                    )
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Color(0xFFFF7024) // Orange color
                        )
                    } else {
                        Text("Confirm Payment")
                    }
                }
            }
        }
    }

    // Confirm payment dialog
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Confirm Payment") },
            text = { 
                Text("Are you sure you want to confirm that ${request.debtor.name} has paid you $${"%.2f".format(request.amountOwed / 100.0)} for '${request.expense.title}'?") 
            },
            containerColor = Color.White,
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        isProcessing = true
                        
                        // Launch in coroutine to handle async operation
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                            try {
                                val result = ApiRepository.expenses.confirmPayment(request.id, lenderId)
                                result.fold(
                                    onSuccess = { 
                                        onConfirm() // Refresh the list
                                    },
                                    onFailure = { exception ->
                                        // Handle error - you might want to show a snackbar
                                        println("Payment confirmation failed: ${exception.message}")
                                    }
                                )
                            } catch (e: Exception) {
                                println("Exception during payment confirmation: ${e.message}")
                            } finally {
                                isProcessing = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF55BF6E)
                    )
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showConfirmDialog = false },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color.Black
                    )
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Reject payment dialog
    if (showRejectDialog) {
        AlertDialog(
            onDismissRequest = { showRejectDialog = false },
            title = { Text("Reject Payment") },
            text = { 
                Text("Are you sure you want to reject this payment request? This will notify ${request.debtor.name} that you haven't received the payment.") 
            },
            containerColor = Color.White,
            confirmButton = {
                Button(
                    onClick = {
                        showRejectDialog = false
                        isProcessing = true
                        
                        // Launch in coroutine to handle async operation
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                            try {
                                val result = ApiRepository.expenses.rejectPayment(request.id, lenderId)
                                result.fold(
                                    onSuccess = { 
                                        onReject() // Refresh the list
                                    },
                                    onFailure = { exception ->
                                        // Handle error - you might want to show a snackbar
                                        println("Payment rejection failed: ${exception.message}")
                                    }
                                )
                            } catch (e: Exception) {
                                println("Exception during payment rejection: ${e.message}")
                            } finally {
                                isProcessing = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Reject")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showRejectDialog = false },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color.Black
                    )
                ) {
                    Text("Cancel")
                }
            }
        )
    }
} 