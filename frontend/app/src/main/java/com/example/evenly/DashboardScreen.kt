package com.example.evenly

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.evenly.api.ApiRepository
import com.example.evenly.api.dashboard.models.DashboardResponse
import com.example.evenly.api.dashboard.models.Expense
import com.example.evenly.api.dashboard.models.Split
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import androidx.compose.foundation.BorderStroke
import com.example.evenly.ui.theme.TopBackgroundColor
import com.example.evenly.ui.theme.BottomBackgroundColor

// Helper function to format due dates
data class DueDateInfo(
    val formattedDate: String,
    val color: Color
)

fun formatDueDate(dueDateStr: String): DueDateInfo {
    return try {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val dueDate = LocalDate.parse(dueDateStr, formatter)
        val today = LocalDate.now()
        val daysUntilDue = dueDate.toEpochDay() - today.toEpochDay()
        
        val formattedDate = when {
            daysUntilDue < 0 -> "Overdue"
            daysUntilDue == 0L -> "Due today"
            daysUntilDue == 1L -> "Due tomorrow"
            daysUntilDue <= 7L -> "Due in $daysUntilDue days"
            else -> dueDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
        }
        
        val color = when {
            daysUntilDue < 0 -> Color(0xFFD32F2F) // Red for overdue
            daysUntilDue <= 3L -> Color(0xFFFF9800) // Orange for soon
            else -> Color(0xFF2E7D32) // Green for normal
        }
        
        DueDateInfo(formattedDate, color)
    } catch (e: DateTimeParseException) {
        DueDateInfo("Invalid date", Color(0xFFD32F2F))
    }
}

@Composable
fun QuickActionsSection(
        onCreateGroup: () -> Unit,
        modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
                text = "Quick Actions",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        Card(
                modifier = Modifier.fillMaxWidth(),
                colors =
                        CardDefaults.cardColors(
                                containerColor = Color(0xFFFF7024)
                        ),
                onClick = onCreateGroup
        ) {
            Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                        text = "Create Group",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                        text = "Start a new group",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White
                )
            }
        }
    }
}

@Composable
fun SplitCard(
    split: Split, 
    modifier: Modifier = Modifier,
    onPaymentRequested: () -> Unit = {}
) {
    var showPaymentDialog by remember { mutableStateOf(false) }
    var isRequestingPayment by remember { mutableStateOf(false) }
    var currentSplit by remember { mutableStateOf(split) }
    
    Card(
            modifier = modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
    ) {
        Column(
                modifier = Modifier.fillMaxWidth().padding(12.dp)
        ) {
            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                            text = split.expense?.title ?: "Expense #${split.expenseId}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                    )
                    split.expense?.lender?.name?.let { lenderName ->
                        Text(
                                text = "Owed to: $lenderName",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                        text = "$${"%.2f".format(split.amountOwed / 100.0)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                )
            }
            
            // Due date display for owed expenses
            split.expense?.dueDate?.let { dueDateStr ->
                val dueDateInfo = formatDueDate(dueDateStr)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                        text = "Due: ${dueDateInfo.formattedDate}",
                        style = MaterialTheme.typography.bodySmall,
                        color = dueDateInfo.color
                )
            }
            
            // Payment status and action button
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
            ) {
                // Payment status
                when {
                    currentSplit.paidConfirmed != null -> {
                        Text(
                                text = "✅ Payment confirmed",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF2E7D32)
                        )
                    }
                    currentSplit.paidRequest != null -> {
                        Text(
                                text = "⏳ Payment pending confirmation",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFFF9800)
                        )
                    }
                    else -> {
                        Text(
                                text = "❌ Payment not made",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                
                // Action button
                if (currentSplit.paidConfirmed == null) {
                    Button(
                            onClick = { showPaymentDialog = true },
                            enabled = !isRequestingPayment && currentSplit.paidRequest == null,
                            colors = ButtonDefaults.buttonColors(
                                    containerColor = if (currentSplit.paidRequest != null) 
                                        MaterialTheme.colorScheme.surfaceVariant 
                                    else 
                                        MaterialTheme.colorScheme.primary
                            )
                    ) {
                        if (isRequestingPayment) {
                            CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text(
                                    text = if (currentSplit.paidRequest != null) "Pending" else "Mark as Paid",
                                    style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Payment confirmation dialog
    if (showPaymentDialog) {
        AlertDialog(
                onDismissRequest = { showPaymentDialog = false },
                title = { Text("Confirm Payment") },
                text = { 
                    Text("Are you sure you want to mark this payment as completed? This will notify ${currentSplit.expense?.lender?.name ?: "the lender"} to confirm your payment.") 
                },
                confirmButton = {
                    Button(
                            onClick = {
                                showPaymentDialog = false
                                isRequestingPayment = true
                                
                                // Get current user ID
                                val currentUser = FirebaseAuth.getInstance().currentUser
                                if (currentUser != null) {
                                                                    // Launch in coroutine to handle async operation
                                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                                    try {
                                        println("💰 Requesting payment confirmation for split: ${currentSplit.id}")
                                        val userResult = ApiRepository.auth.getUser(currentUser.uid)
                                        userResult.fold(
                                            onSuccess = { user ->
                                                println("👤 Got user ID: ${user.user.id}")
                                                // Request payment confirmation
                                                val paymentResult = ApiRepository.expenses.requestPaymentConfirmation(
                                                    currentSplit.id, 
                                                    user.user.id
                                                )
                                                paymentResult.fold(
                                                    onSuccess = { response ->
                                                        println("✅ Payment request successful: ${response.message}")
                                                        // Update local state
                                                        currentSplit = currentSplit.copy(
                                                            paidRequest = response.split.paidRequest
                                                        )
                                                        println("🔄 Triggering dashboard refresh...")
                                                        onPaymentRequested() // Notify parent to refresh
                                                    },
                                                    onFailure = { exception ->
                                                        // Handle error - you might want to show a snackbar
                                                        println("❌ Payment request failed: ${exception.message}")
                                                    }
                                                )
                                            },
                                            onFailure = { exception ->
                                                println("❌ Failed to get user: ${exception.message}")
                                            }
                                        )
                                    } catch (e: Exception) {
                                        println("❌ Exception during payment request: ${e.message}")
                                    } finally {
                                        isRequestingPayment = false
                                    }
                                }
                                } else {
                                    isRequestingPayment = false
                                }
                            }
                    ) {
                        Text("Confirm")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showPaymentDialog = false }) {
                        Text("Cancel")
                    }
                }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
        userId: String,
        userName: String? = null,
        onLogout: () -> Unit,
        onCreateGroup: () -> Unit,
        modifier: Modifier = Modifier
) {
    var dashboardData by remember { mutableStateOf<DashboardResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var retryCounter by remember { mutableStateOf(0) }

    LaunchedEffect(userId, retryCounter) {
        println("🔄 Refreshing dashboard data... retryCounter: $retryCounter")
        isLoading = true
        error = null
        dashboardData = null

        val id =
                if (userId.isNotEmpty()) {
                    userId
                } else {
                    val currentUser = FirebaseAuth.getInstance().currentUser
                    if (currentUser == null) {
                        error = "User not authenticated"
                        isLoading = false
                        return@LaunchedEffect
                    }
                    try {
                        val userResult = ApiRepository.auth.getUser(currentUser.uid)
                        userResult.fold(
                                onSuccess = { it.user.id },
                                onFailure = {
                                    error = "Failed to get user info: ${it.message}"
                                    null
                                }
                        )
                    } catch (e: Exception) {
                        error = "Exception getting user info: ${e.message}"
                        null
                    }
                }

        if (id != null) {
            try {
                println("📊 Fetching dashboard data for user: $id")
                val dashboardResult = ApiRepository.dashboard.getUserExpenses(id)
                dashboardResult.fold(
                        onSuccess = { data -> 
                            println("✅ Dashboard data loaded successfully")
                            dashboardData = data 
                        },
                        onFailure = { exception ->
                            println("❌ Dashboard data failed: ${exception.message}")
                            error = exception.message ?: "Failed to load dashboard"
                        }
                )
            } catch (e: Exception) {
                println("❌ Exception loading dashboard: ${e.message}")
                error = e.message ?: "Failed to load dashboard"
            }
        } else {
            if (error == null) {
                error = "Could not retrieve user ID"
            }
        }
        isLoading = false
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(TopBackgroundColor, BottomBackgroundColor)
                )
            )
    ) {
        Column(
                modifier =
                        Modifier.fillMaxSize()
                                .padding(horizontal = 16.dp)
                                .padding(top = 8.dp)
        ) {
            // Personalized header
            if (userName != null) {
                Text(
                        text = "Hi $userName! Ready to split Evenly?",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 16.dp)
                )
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
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
                dashboardData?.let { data ->
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                        item {
                            TotalSummaryCard(
                                    totalLent = data.lent.totalAmount,
                                    totalOwed = data.owed.totalAmount
                            )
                        }

                        // Quick Actions Section
                        item { QuickActionsSection(onCreateGroup = onCreateGroup) }

                        // Lent Section
                        item { 
                            LentSection(
                                expenses = data.lent.expenses,
                                onPaymentConfirmed = { retryCounter++ },
                                onPaymentRejected = { retryCounter++ }
                            ) 
                        }

                        // Owed Section
                        item { 
                            OwedSection(
                                splits = data.owed.splits,
                                onPaymentRequested = { 
                                    println("🔄 onPaymentRequested callback triggered!")
                                    // Force a complete refresh of dashboard data
                                    retryCounter++ 
                                    println("🔄 retryCounter incremented to: $retryCounter")
                                }
                            ) 
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun TotalSummaryCard(totalLent: Long, totalOwed: Long, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // You are owed card
        Card(
            modifier = Modifier.weight(1f),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "You are owed", 
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$${"%.2f".format(totalLent / 100.0)}",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E7D32)
                )
            }
        }
        
        // You owe card
        Card(
            modifier = Modifier.weight(1f),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "You owe", 
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$${"%.2f".format(totalOwed / 100.0)}",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun LentSection(
    expenses: List<Expense>, 
    onPaymentConfirmed: () -> Unit = {},
    onPaymentRejected: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
                text = "Lent",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        if (expenses.isEmpty()) {
            Text(
                    text = "You haven't lent any money",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                expenses.forEach { expense -> 
                    ExpenseCard(
                        expense = expense,
                        onPaymentConfirmed = onPaymentConfirmed,
                        onPaymentRejected = onPaymentRejected
                    ) 
                }
            }
        }
    }
}

@Composable
fun OwedSection(
    splits: List<Split>, 
    onPaymentRequested: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
                text = "Owed",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        if (splits.isEmpty()) {
            Text(
                    text = "You don't owe any money",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                splits.forEach { split -> 
                    SplitCard(
                        split = split,
                        onPaymentRequested = onPaymentRequested
                    ) 
                }
            }
        }
    }
}

@Composable
fun ExpenseCard(
    expense: Expense, 
    modifier: Modifier = Modifier,
    onPaymentConfirmed: () -> Unit = {},
    onPaymentRejected: () -> Unit = {}
) {
    var showConfirmDialog by remember { mutableStateOf<String?>(null) }
    var showRejectDialog by remember { mutableStateOf<String?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    
    // Get current user ID for lender verification
    val currentUser = FirebaseAuth.getInstance().currentUser
    val currentUserId = remember {
        if (currentUser != null) {
            try {
                runBlocking {
                    val userResult = ApiRepository.auth.getUser(currentUser.uid)
                    userResult.fold(
                        onSuccess = { it.user.id },
                        onFailure = { null }
                    )
                }
            } catch (e: Exception) {
                null
            }
        } else null
    }
    
    // Calculate pending payment requests
    val pendingCount = expense.splits.count { split ->
        split.paidRequest != null && split.paidConfirmed == null
    }

    Box(
        modifier = modifier.fillMaxWidth()
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = 8.dp,
                    end = 8.dp
                ),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = if (pendingCount > 0) 4.dp else 2.dp
            ),
            onClick = {
                if (pendingCount > 0) {
                    showConfirmDialog = "all"
                }
            }
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = expense.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "$${"%.2f".format(expense.totalAmount / 100.0)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                // Due date display
                expense.dueDate?.let { dueDateStr ->
                    val dueDateInfo = formatDueDate(dueDateStr)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Due: ${dueDateInfo.formattedDate}",
                        style = MaterialTheme.typography.bodySmall,
                        color = dueDateInfo.color
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))

                // List of people who owe money
                if (expense.splits.isEmpty()) {
                    Text(
                        text = "No one has been added to this expense yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    expense.splits.forEach { split ->
                        split.debtor?.name?.let { debtorName ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Owed by: $debtorName",
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                    
                                    // Payment status icon
                                    split.debtor?.paymentStatus?.let { status ->
                                        when (status) {
                                            "paid" -> {
                                                Text(
                                                    text = "✅",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontSize = 16.sp
                                                )
                                            }
                                            "pending" -> {
                                                Text(
                                                    text = "⏳",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontSize = 16.sp
                                                )
                                            }
                                            else -> {
                                                // No icon for no request
                                            }
                                        }
                                    }
                                }
                                Text(
                                    text = "$${"%.2f".format(split.amountOwed / 100.0)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }
            }
        }
        
        // Notification badge positioned absolutely within the outer Box
        if (pendingCount > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(20.dp)
                    .background(
                        color = Color(0xFFFF7024),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = pendingCount.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                )
            }
        }
    }
    
    // Confirm payment dialog with individual actions
    showConfirmDialog?.let { dialogType ->
        if (dialogType == "all") {
            val pendingSplits = expense.splits.filter { split ->
                split.paidRequest != null && split.paidConfirmed == null
            }
            
            AlertDialog(
                onDismissRequest = { showConfirmDialog = null },
                title = { Text("Pending Payment Requests") },
                text = { 
                    Column {
                        Text(
                            text = "The following people have requested payment confirmation:",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        pendingSplits.forEach { split ->
                            split.debtor?.name?.let { name ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surface
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = name,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                Text(
                                                    text = "$${"%.2f".format(split.amountOwed / 100.0)}",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                            
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                // Reject button
                                                OutlinedButton(
                                                    onClick = { 
                                                        showConfirmDialog = null
                                                        showRejectDialog = name
                                                    },
                                                    colors = ButtonDefaults.outlinedButtonColors(
                                                        contentColor = MaterialTheme.colorScheme.error
                                                    ),
                                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                                                ) {
                                                    Text("Reject", fontSize = 12.sp)
                                                }
                                                
                                                // Confirm button
                                                Button(
                                                    onClick = {
                                                        showConfirmDialog = null
                                                        isProcessing = true
                                                        
                                                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                                                            try {
                                                                if (currentUserId != null) {
                                                                    val result = ApiRepository.expenses.confirmPayment(split.id, currentUserId)
                                                                    result.fold(
                                                                        onSuccess = { onPaymentConfirmed() },
                                                                        onFailure = { /* Handle error */ }
                                                                    )
                                                                }
                                                            } catch (e: Exception) {
                                                                println("Exception during payment confirmation: ${e.message}")
                                                            } finally {
                                                                isProcessing = false
                                                            }
                                                        }
                                                    }
                                                ) {
                                                    Text("Confirm", fontSize = 12.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Bulk actions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    showConfirmDialog = null
                                    isProcessing = true
                                    
                                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                                        try {
                                            var successCount = 0
                                            for (split in pendingSplits) {
                                                if (currentUserId != null) {
                                                    val result = ApiRepository.expenses.rejectPayment(split.id, currentUserId)
                                                    result.fold(
                                                        onSuccess = { successCount++ },
                                                        onFailure = { /* Handle error */ }
                                                    )
                                                }
                                            }
                                            if (successCount > 0) {
                                                onPaymentRejected()
                                            }
                                        } catch (e: Exception) {
                                            println("Exception during bulk rejection: ${e.message}")
                                        } finally {
                                            isProcessing = false
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                            ) {
                                Text("Reject All")
                            }
                            
                            Button(
                                onClick = {
                                    showConfirmDialog = null
                                    isProcessing = true
                                    
                                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                                        try {
                                            var successCount = 0
                                            for (split in pendingSplits) {
                                                if (currentUserId != null) {
                                                    val result = ApiRepository.expenses.confirmPayment(split.id, currentUserId)
                                                    result.fold(
                                                        onSuccess = { successCount++ },
                                                        onFailure = { /* Handle error */ }
                                                    )
                                                }
                                            }
                                            if (successCount > 0) {
                                                onPaymentConfirmed()
                                            }
                                        } catch (e: Exception) {
                                            println("Exception during bulk confirmation: ${e.message}")
                                        } finally {
                                            isProcessing = false
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Confirm All")
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showConfirmDialog = null }) {
                        Text("Close")
                    }
                }
            )
        }
    }
    
    // Individual reject payment dialog
    showRejectDialog?.let { debtorName ->
        AlertDialog(
            onDismissRequest = { showRejectDialog = null },
            title = { Text("Reject Payment") },
            text = { 
                Text("Are you sure you want to reject this payment request from $debtorName? This will notify them that you haven't received the payment.") 
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRejectDialog = null
                        isProcessing = true
                        
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                            try {
                                val split = expense.splits.find { it.debtor?.name == debtorName }
                                
                                if (split != null && currentUserId != null) {
                                    val result = ApiRepository.expenses.rejectPayment(split.id, currentUserId)
                                    result.fold(
                                        onSuccess = { onPaymentRejected() },
                                        onFailure = { exception ->
                                            println("Payment rejection failed: ${exception.message}")
                                        }
                                    )
                                }
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
                TextButton(onClick = { showRejectDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}