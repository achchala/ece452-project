package com.example.evenly

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.evenly.api.ApiRepository
import com.example.evenly.api.dashboard.models.DashboardResponse
import com.example.evenly.api.dashboard.models.Expense
import com.example.evenly.api.dashboard.models.Split
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
        userId: Int,
        userName: String? = null,
        onLogout: () -> Unit,
        onCreateGroup: () -> Unit,
        onViewGroups: () -> Unit,
        modifier: Modifier = Modifier
) {
    var dashboardData by remember { mutableStateOf<DashboardResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var retryCounter by remember { mutableStateOf(0) }

    LaunchedEffect(userId, retryCounter) {
        isLoading = true
        error = null
        dashboardData = null

        val id =
                if (userId != -1) {
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
                val dashboardResult = ApiRepository.dashboard.getUserExpenses(id)
                dashboardResult.fold(
                        onSuccess = { data -> dashboardData = data },
                        onFailure = { exception ->
                            error = exception.message ?: "Failed to load dashboard"
                        }
                )
            } catch (e: Exception) {
                error = e.message ?: "Failed to load dashboard"
            }
        } else {
            if (error == null) {
                error = "Could not retrieve user ID"
            }
        }
        isLoading = false
    }

    Scaffold(
            modifier = modifier.fillMaxSize(),
            floatingActionButton = {
                FloatingActionButton(
                        onClick = onCreateGroup,
                        containerColor = MaterialTheme.colorScheme.primary
                ) { Text("+", style = MaterialTheme.typography.headlineMedium) }
            }
    ) { paddingValues ->
        Column(
                modifier =
                        Modifier.fillMaxSize()
                                .padding(horizontal = 16.dp)
                                .padding(top = 8.dp)
                                .padding(paddingValues)
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
                        item {
                            QuickActionsSection(
                                    onCreateGroup = onCreateGroup,
                                    onViewGroups = onViewGroups
                            )
                        }

                        // Lent Section
                        item { LentSection(expenses = data.lent.expenses) }

                        // Owed Section
                        item { OwedSection(splits = data.owed.splits) }
                    }
                }
            }
        }
    }
}

@Composable
fun TotalSummaryCard(totalLent: Long, totalOwed: Long, modifier: Modifier = Modifier) {
    Card(
            modifier = modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceAround
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "You are owed", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                        text = "$${"%.2f".format(totalLent / 100.0)}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "You owe", style = MaterialTheme.typography.labelLarge)
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
fun LentSection(expenses: List<Expense>, modifier: Modifier = Modifier) {
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
                expenses.forEach { expense -> ExpenseCard(expense = expense) }
            }
        }
    }
}

@Composable
fun OwedSection(splits: List<Split>, modifier: Modifier = Modifier) {
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
                splits.forEach { split -> SplitCard(split = split) }
            }
        }
    }
}

@Composable
fun ExpenseCard(expense: Expense, modifier: Modifier = Modifier) {
    Card(
            modifier = modifier.fillMaxWidth(),
            colors =
                    CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
    ) {
        Column(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
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
                                horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                    text = "Owed by: $debtorName",
                                    style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(
                                    text = "$${"%.2f".format(split.amountOwed / 100.0)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SplitCard(split: Split, modifier: Modifier = Modifier) {
    Card(
            modifier = modifier.fillMaxWidth(),
            colors =
                    CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
    ) {
        Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
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
    }
}

@Composable
fun QuickActionsSection(
        onCreateGroup: () -> Unit,
        onViewGroups: () -> Unit,
        modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
                text = "Quick Actions",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                    modifier = Modifier.weight(1f),
                    colors =
                            CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
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
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                            text = "Start a new group",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Card(
                    modifier = Modifier.weight(1f),
                    colors =
                            CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                            ),
                    onClick = onViewGroups
            ) {
                Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                            text = "My Groups",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                            text = "View all groups",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}
