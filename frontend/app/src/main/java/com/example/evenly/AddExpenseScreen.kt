package com.example.evenly

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import com.example.evenly.api.ApiRepository
import com.example.evenly.api.expenses.models.ExpenseSplit
import com.example.evenly.api.group.models.GroupMember
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreen(
    groupId: String,
    groupName: String,
    groupMembers: List<GroupMember>,
    onNavigateBack: () -> Unit,
    onExpenseAdded: () -> Unit,
    modifier: Modifier = Modifier
) {
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<ExpenseCategory?>(null) }
    var selectedMembers by remember { mutableStateOf<Set<String>>(emptySet()) }
    var splitType by remember { mutableStateOf(SplitType.EQUAL) }
    var customAmounts by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var dueDateType by remember { mutableStateOf(DueDateType.NONE) }
    var customDueDate by remember { mutableStateOf<LocalDate?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var dueDateDropdownExpanded by remember { mutableStateOf(false) }
    
    val coroutineScope = rememberCoroutineScope()
    val firebaseUser = FirebaseAuth.getInstance().currentUser

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Add Expense") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
            // Header
            Text(
                text = "Add Expense to $groupName",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            // Expense Details Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
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
                            singleLine = true
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
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
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
                                .menuAnchor()
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
                                readOnly = true
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { showDatePicker = true },
                                modifier = Modifier.height(56.dp)
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
                                        initialSelectedDateMillis = today.plusDays(1).toEpochDay() * 24 * 60 * 60 * 1000
                                    ),
                                    showModeToggle = false
                                )
                            }
                        }
                    }
                }
            }

            // Split Type and Member Selection Combined
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Split & Members",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )

                    // Split Type Selection (compact)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        SplitType.values().forEach { splitTypeOption ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = splitType == splitTypeOption,
                                    onClick = { splitType = splitTypeOption }
                                )
                                Text(
                                    text = splitTypeOption.displayName,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }

                    Divider()

                    // Member Selection (compact)
                    Text(
                        text = "Select Members",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )

                    Text(
                        text = "You won't be charged - you're paying for everyone else.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    groupMembers.forEach { member ->
                        val memberId = member.userId
                        val isSelected = selectedMembers.contains(memberId)
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { checked ->
                                    selectedMembers = if (checked) {
                                        selectedMembers + memberId
                                    } else {
                                        selectedMembers - memberId
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = member.user?.name ?: "User #$memberId",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                member.user?.email?.let { email ->
                                    Text(
                                        text = email,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Custom Amounts (if split type is custom)
            if (splitType == SplitType.CUSTOM && selectedMembers.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Custom Amounts",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )

                        val currentUserEmail = firebaseUser?.email
                        val borrowers = selectedMembers.filter { memberId ->
                            val member = groupMembers.find { it.userId == memberId }
                            member?.user?.email != currentUserEmail
                        }

                        Text(
                            text = "Total: $${amount.toDoubleOrNull() ?: 0.0}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        borrowers.forEach { memberId ->
                            val member = groupMembers.find { it.userId == memberId }
                            val memberName = member?.user?.name ?: "User #$memberId"
                            val currentAmount = customAmounts[memberId] ?: ""
                            
                            OutlinedTextField(
                                value = currentAmount,
                                onValueChange = { newAmount ->
                                    if (newAmount.isEmpty() || newAmount.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                                        customAmounts = customAmounts + (memberId to newAmount)
                                    }
                                },
                                label = { Text("Amount for $memberName") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                            )
                        }
                    }
                }
            }

            // Summary Section (compact)
            if (title.isNotBlank() && amount.isNotBlank() && selectedMembers.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "You will pay: $${amount.toDoubleOrNull() ?: 0.0}",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            
                            val currentUserEmail = firebaseUser?.email
                            val borrowers = selectedMembers.filter { memberId ->
                                val member = groupMembers.find { it.userId == memberId }
                                member?.user?.email != currentUserEmail
                            }
                            
                            if (borrowers.isNotEmpty()) {
                                Text(
                                    text = "Split between ${borrowers.size} people",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            }

            // Error Display
            error?.let { errorMessage ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = errorMessage,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            // Add Expense Button
            Button(
                onClick = {
                    if (validateInputs(title, amount, selectedMembers, splitType, customAmounts, firebaseUser?.email, groupMembers)) {
                        coroutineScope.launch {
                            addExpense(
                                title = title,
                                amount = amount,
                                groupId = groupId,
                                selectedCategory = selectedCategory,
                                selectedMembers = selectedMembers,
                                splitType = splitType,
                                customAmounts = customAmounts,
                                dueDateType = dueDateType,
                                customDueDate = customDueDate,
                                groupMembers = groupMembers,
                                firebaseUser = firebaseUser,
                                onSuccess = {
                                    onExpenseAdded()
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
                        error = "Please fill in all required fields and select at least one member"
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF7024), // Orange background
                    contentColor = Color.White // White text/icon
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White
                    )
                } else {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Expense", color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

enum class SplitType(val displayName: String) {
    EQUAL("Split Equally"),
    CUSTOM("Custom Amounts")
}

enum class DueDateType(val displayName: String, val days: Int?) {
    NONE("No Due Date", null),
    ONE_DAY("1 Day", 1),
    THREE_DAYS("3 Days", 3),
    ONE_WEEK("1 Week", 7),
    CUSTOM("Custom Date", null)
}

private fun validateInputs(
    title: String,
    amount: String,
    selectedMembers: Set<String>,
    splitType: SplitType,
    customAmounts: Map<String, String>,
    currentUserEmail: String? = null,
    groupMembers: List<GroupMember>? = null
): Boolean {
    if (title.isBlank() || amount.isBlank() || selectedMembers.isEmpty()) {
        return false
    }
    
    if (splitType == SplitType.CUSTOM) {
        val totalAmount = amount.toDoubleOrNull() ?: 0.0
        
        // Calculate total from borrowers only (excluding the expense creator)
        val borrowers = selectedMembers.filter { memberId ->
            val member = groupMembers?.find { it.userId == memberId }
            member?.user?.email != currentUserEmail
        }
        
        val customTotal = borrowers.sumOf { memberId ->
            customAmounts[memberId]?.toDoubleOrNull() ?: 0.0
        }
        
        if (customTotal != totalAmount) {
            return false
        }
    }
    
    return true
}

private suspend fun addExpense(
    title: String,
    amount: String,
    groupId: String,
    selectedCategory: ExpenseCategory?,
    selectedMembers: Set<String>,
    splitType: SplitType,
    customAmounts: Map<String, String>,
    dueDateType: DueDateType,
    customDueDate: LocalDate?,
    groupMembers: List<GroupMember>,
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
        val splits = mutableListOf<ExpenseSplit>()

        // Calculate due date
        val dueDate = when (dueDateType) {
            DueDateType.NONE -> null
            DueDateType.ONE_DAY -> LocalDate.now().plusDays(1)
            DueDateType.THREE_DAYS -> LocalDate.now().plusDays(3)
            DueDateType.ONE_WEEK -> LocalDate.now().plusDays(7)
            DueDateType.CUSTOM -> customDueDate
        }
        
        val dueDateString = dueDate?.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

        // Get current user's email to exclude them from splits
        val currentUserEmail = firebaseUser.email
        val borrowers = selectedMembers.filter { memberId ->
            val member = groupMembers.find { it.userId == memberId }
            member?.user?.email != currentUserEmail
        }

        when (splitType) {
            SplitType.EQUAL -> {
                if (borrowers.isNotEmpty()) {
                    val amountPerPerson = totalAmountCents / borrowers.size
                    val remainder = totalAmountCents % borrowers.size
                    
                    borrowers.forEachIndexed { index, memberId ->
                        val member = groupMembers.find { it.userId == memberId }
                        val memberEmail = member?.user?.email
                        if (memberEmail != null) {
                            val splitAmount = amountPerPerson + if (index < remainder) 1 else 0
                            splits.add(ExpenseSplit(memberEmail, splitAmount))
                        }
                    }
                }
            }
            SplitType.CUSTOM -> {
                borrowers.forEach { memberId ->
                    val member = groupMembers.find { it.userId == memberId }
                    val memberEmail = member?.user?.email
                    val customAmount = customAmounts[memberId] ?: "0"
                    if (memberEmail != null) {
                        val splitAmount = (customAmount.toDouble() * 100).toInt()
                        splits.add(ExpenseSplit(memberEmail, splitAmount))
                    }
                }
            }
        }

        val result = ApiRepository.expenses.createExpense(
            title = title,
            totalAmount = totalAmountCents,
            firebaseId = firebaseUser.uid,
            groupId = groupId,
            dueDate = dueDateString,
            category = selectedCategory?.name,
            splits = splits
        )

        result.fold(
            onSuccess = {
                onSuccess()
            },
            onFailure = { exception ->
                onError("Failed to create expense: ${exception.message}")
            }
        )
    } catch (e: Exception) {
        onError("Exception creating expense: ${e.message}")
    } finally {
        setLoading(false)
    }
} 