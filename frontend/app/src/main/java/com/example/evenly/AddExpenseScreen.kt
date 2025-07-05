package com.example.evenly

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.evenly.api.ApiRepository
import com.example.evenly.api.expenses.models.ExpenseSplit
import com.example.evenly.api.group.models.GroupMember
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

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
    var selectedMembers by remember { mutableStateOf<Set<String>>(emptySet()) }
    var splitType by remember { mutableStateOf(SplitType.EQUAL) }
    var customAmounts by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    
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
            verticalArrangement = Arrangement.spacedBy(24.dp)
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
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Expense Details",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )

                    // Title Input
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Expense Title") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Amount Input
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { 
                            // Only allow numbers and decimal point
                            if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                                amount = it
                            }
                        },
                        label = { Text("Amount ($)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                }
            }

            // Split Type Selection
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Split Type",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )

                    SplitType.values().forEach { splitTypeOption ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = splitType == splitTypeOption,
                                onClick = { splitType = splitTypeOption }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = splitTypeOption.displayName,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }

            // Member Selection
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Select Members",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )

                    // Note about expense creator
                    Text(
                        text = "Note: You (the expense creator) won't be charged - you're paying for everyone else.",
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
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = member.user?.name ?: "User #$memberId",
                                    style = MaterialTheme.typography.bodyLarge,
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
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Custom Amounts",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )

                        // Get borrowers (excluding expense creator)
                        val currentUserEmail = firebaseUser?.email
                        val borrowers = selectedMembers.filter { memberId ->
                            val member = groupMembers.find { it.userId == memberId }
                            member?.user?.email != currentUserEmail
                        }

                        Text(
                            text = "Enter amounts for each person (total should equal $${amount.toDoubleOrNull() ?: 0.0})",
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

            // Summary Section
            if (title.isNotBlank() && amount.isNotBlank() && selectedMembers.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Expense Summary",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        
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
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
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
                                selectedMembers = selectedMembers,
                                splitType = splitType,
                                customAmounts = customAmounts,
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
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Expense")
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
    selectedMembers: Set<String>,
    splitType: SplitType,
    customAmounts: Map<String, String>,
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