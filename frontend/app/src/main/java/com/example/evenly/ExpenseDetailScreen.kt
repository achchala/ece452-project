package com.example.evenly

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.evenly.api.ApiRepository
import com.example.evenly.api.expenses.models.Expense
import com.example.evenly.api.expenses.models.ExpenseSplit
import com.example.evenly.api.group.models.GroupMember
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseDetailScreen(
    expense: Expense,
    groupMembers: List<GroupMember>,
    onNavigateBack: () -> Unit,
    onExpenseUpdated: () -> Unit,
    onExpenseDeleted: () -> Unit,
    modifier: Modifier = Modifier
) {
    var title by remember { mutableStateOf(expense.title) }
    var amount by remember { mutableStateOf((expense.totalAmount / 100.0).toString()) }
    var selectedCategory by remember { mutableStateOf(expense.category?.let { ExpenseCategory.fromString(it) }) }
    var dueDateType by remember { mutableStateOf(DueDateType.NONE) }
    var customDueDate by remember { mutableStateOf<LocalDate?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var dueDateDropdownExpanded by remember { mutableStateOf(false) }
    
    val coroutineScope = rememberCoroutineScope()
    val firebaseUser = FirebaseAuth.getInstance().currentUser

    // Initialize due date type based on expense data
    LaunchedEffect(expense.dueDate) {
        expense.dueDate?.let { dueDateStr ->
            if (dueDateStr.isNotBlank()) {
                try {
                    val dueDate = LocalDate.parse(dueDateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                    val today = LocalDate.now()
                    val daysDiff = dueDate.toEpochDay() - today.toEpochDay()
                    
                    dueDateType = when (daysDiff) {
                        1L -> DueDateType.ONE_DAY
                        3L -> DueDateType.THREE_DAYS
                        7L -> DueDateType.ONE_WEEK
                        else -> {
                            customDueDate = dueDate
                            DueDateType.CUSTOM
                        }
                    }
                } catch (e: Exception) {
                    dueDateType = DueDateType.NONE
                }
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Expense Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Delete button
                    IconButton(
                        onClick = { showDeleteDialog = true }
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Error message
            error?.let { errorMessage ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            // Expense Details Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                )
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Expense Details",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )

                    // Title and Amount in a row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Title") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = Color(0xFFFF7024),
                                focusedBorderColor = Color(0xFFFF7024)
                            )
                        )
                        
                        OutlinedTextField(
                            value = amount,
                            onValueChange = { 
                                if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                                    amount = it
                                }
                            },
                            label = { Text("Amount") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = Color(0xFFFF7024),
                                focusedBorderColor = Color(0xFFFF7024)
                            )
                        )
                    }

                    // Category Selector
                    CategorySelector(
                        selectedCategory = selectedCategory,
                        onCategorySelected = { selectedCategory = it }
                    )

                    // Due Date Dropdown
                    ExposedDropdownMenuBox(
                        expanded = dueDateDropdownExpanded,
                        onExpandedChange = { dueDateDropdownExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = dueDateType.displayName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Due Date") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dueDateDropdownExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = Color(0xFFFF7024),
                                focusedBorderColor = Color(0xFFFF7024)
                            )
                        )
                        
                        ExposedDropdownMenu(
                            expanded = dueDateDropdownExpanded,
                            onDismissRequest = { dueDateDropdownExpanded = false }
                        ) {
                            DueDateType.values().forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.displayName) },
                                    onClick = {
                                        dueDateType = option
                                        dueDateDropdownExpanded = false
                                        if (option != DueDateType.CUSTOM) {
                                            customDueDate = null
                                        }
                                    }
                                )
                            }
                        }
                    }

                    // Custom date picker (compact)
                    if (dueDateType == DueDateType.CUSTOM) {
                        var showDatePicker by remember { mutableStateOf(false) }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = customDueDate?.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")) ?: "",
                                onValueChange = { },
                                label = { Text("Custom Date") },
                                modifier = Modifier.weight(1f),
                                readOnly = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedBorderColor = Color(0xFFFF7024),
                                    focusedBorderColor = Color(0xFFFF7024)
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { showDatePicker = true },
                                modifier = Modifier.height(56.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFF7024)
                                )
                            ) {
                                Text("Pick")
                            }
                        }

                        if (showDatePicker) {
                            val today = LocalDate.now()
                            DatePickerDialog(
                                onDismissRequest = { showDatePicker = false },
                                confirmButton = {
                                    TextButton(onClick = { showDatePicker = false }) {
                                        Text("OK")
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showDatePicker = false }) {
                                        Text("Cancel")
                                    }
                                }
                            ) {
                                DatePicker(
                                    state = rememberDatePickerState(
                                        initialSelectedDateMillis = customDueDate?.toEpochDay()?.let { it * 24 * 60 * 60 * 1000 } 
                                            ?: (today.plusDays(1).toEpochDay() * 24 * 60 * 60 * 1000)
                                    ),
                                    showModeToggle = false
                                )
                            }
                        }
                    }
                }
            }

            // Current Splits Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                )
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Current Splits",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )

                    if (expense.splits.isEmpty()) {
                        Text(
                            text = "No splits for this expense.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        expense.splits.forEach { split ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "User ID: ${split.userId}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "$${"%.2f".format(split.amountOwed / 100.0)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFFFF7024) // Orange color
                                )
                            }
                        }
                    }
                }
            }

            // Update Button
            Button(
                onClick = {
                    if (validateInputs(title, amount)) {
                        coroutineScope.launch {
                            updateExpense(
                                expenseId = expense.id,
                                title = title,
                                amount = amount,
                                selectedCategory = selectedCategory,
                                dueDateType = dueDateType,
                                customDueDate = customDueDate,
                                firebaseUser = firebaseUser,
                                onSuccess = {
                                    onExpenseUpdated()
                                },
                                onError = { errorMessage ->
                                    error = errorMessage
                                },
                                setLoading = { loading ->
                                    isLoading = loading
                                }
                            )
                        }
                    } else {
                        error = "Please fill in all required fields"
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF7024)
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color(0xFFFF7024) // Orange color
                    )
                } else {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Update Expense")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Expense") },
            text = { Text("Are you sure you want to delete this expense? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        coroutineScope.launch {
                            deleteExpense(
                                expenseId = expense.id,
                                firebaseUser = firebaseUser,
                                onSuccess = {
                                    onExpenseDeleted()
                                },
                                onError = { errorMessage ->
                                    error = errorMessage
                                },
                                setLoading = { loading ->
                                    isLoading = loading
                                }
                            )
                        }
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

private fun validateInputs(title: String, amount: String): Boolean {
    return title.isNotBlank() && amount.isNotBlank()
}

private suspend fun updateExpense(
    expenseId: String,
    title: String,
    amount: String,
    selectedCategory: ExpenseCategory?,
    dueDateType: DueDateType,
    customDueDate: LocalDate?,
    firebaseUser: com.google.firebase.auth.FirebaseUser?,
    onSuccess: () -> Unit,
    onError: (String) -> Unit,
    setLoading: (Boolean) -> Unit
) {
    setLoading(true)
    
    try {
        if (firebaseUser == null) {
            onError("User not authenticated")
            return
        }

        val totalAmountCents = (amount.toDouble() * 100).toInt()

        // Calculate due date
        val dueDate = when (dueDateType) {
            DueDateType.NONE -> null
            DueDateType.ONE_DAY -> LocalDate.now().plusDays(1)
            DueDateType.THREE_DAYS -> LocalDate.now().plusDays(3)
            DueDateType.ONE_WEEK -> LocalDate.now().plusDays(7)
            DueDateType.CUSTOM -> customDueDate
        }
        
        val dueDateString = dueDate?.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

        val result = ApiRepository.expenses.updateExpense(
            expenseId = expenseId,
            title = title,
            totalAmount = totalAmountCents,
            category = selectedCategory?.name,
            dueDate = dueDateString,
            firebaseId = firebaseUser.uid
        )

        result.fold(
            onSuccess = {
                onSuccess()
            },
            onFailure = { exception ->
                onError("Failed to update expense: ${exception.message}")
            }
        )
    } catch (e: Exception) {
        onError("Exception updating expense: ${e.message}")
    } finally {
        setLoading(false)
    }
}

private suspend fun deleteExpense(
    expenseId: String,
    firebaseUser: com.google.firebase.auth.FirebaseUser?,
    onSuccess: () -> Unit,
    onError: (String) -> Unit,
    setLoading: (Boolean) -> Unit
) {
    setLoading(true)
    
    try {
        if (firebaseUser == null) {
            onError("User not authenticated")
            return
        }

        val result = ApiRepository.expenses.deleteExpense(
            expenseId = expenseId,
            firebaseId = firebaseUser.uid
        )

        result.fold(
            onSuccess = {
                onSuccess()
            },
            onFailure = { exception ->
                onError("Failed to delete expense: ${exception.message}")
            }
        )
    } catch (e: Exception) {
        onError("Exception deleting expense: ${e.message}")
    } finally {
        setLoading(false)
    }
} 