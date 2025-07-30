package com.example.evenly

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.evenly.api.ApiRepository
import com.example.evenly.api.expenses.models.Expense
import com.example.evenly.api.friends.FriendRequest
import com.example.evenly.api.group.models.Group
import com.example.evenly.api.group.models.GroupMember
import com.example.evenly.CategoryChip
import com.example.evenly.ExpenseCategory
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    groupId: String,
    onNavigateBack: () -> Unit,
    onAddExpense: (String, String, List<GroupMember>) -> Unit,
    onExpenseClick: (Expense, List<GroupMember>) -> Unit,
    modifier: Modifier = Modifier,
    key: Any? = null
) {
    var group by remember { mutableStateOf<Group?>(null) }
    var showAddUserDialog by remember { mutableStateOf(false) }
    var currentUserEmail by remember { mutableStateOf<String?>(null) }
    var friends by remember { mutableStateOf<List<FriendRequest>>(emptyList()) }
    var friendNames by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var isLoadingFriends by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var expenses by remember { mutableStateOf<List<Expense>>(emptyList()) }
    var isLoadingExpenses by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    // Function to update error state
    fun updateError(errorMessage: String?) {
        error = errorMessage
    }

    // Function to fetch friend names
    suspend fun fetchFriendNames(friends: List<FriendRequest>, currentUserEmail: String) {
        val namesMap = mutableMapOf<String, String>()
        for (friend in friends) {
            val friendEmail =
                    if (friend.from_user == currentUserEmail) friend.to_user else friend.from_user
            try {
                val userResult = ApiRepository.auth.getUserByEmail(friendEmail)
                userResult.fold(
                        onSuccess = { response ->
                            response.user.name?.let { name -> namesMap[friendEmail] = name }
                        },
                        onFailure = { exception ->
                            // Use email as fallback if name not available
                            namesMap[friendEmail] = friendEmail
                        }
                )
            } catch (e: Exception) {
                // Use email as fallback if name not available
                namesMap[friendEmail] = friendEmail
            }
        }
        friendNames = namesMap
    }

    // Function to fetch group expenses
    suspend fun fetchGroupExpenses(groupId: String) {
        isLoadingExpenses = true
        try {
            val result = ApiRepository.expenses.getGroupExpenses(groupId)
            result.fold(
                onSuccess = { response ->
                    expenses = response.expenses
                },
                onFailure = { exception ->
                    updateError("Failed to load expenses: ${exception.message}")
                }
            )
        } catch (e: Exception) {
            updateError("Exception loading expenses: ${e.message}")
        } finally {
            isLoadingExpenses = false
        }
    }

    // Force refresh when key changes
    LaunchedEffect(key) {
        // Refresh expenses when key changes (after update/delete)
        if (groupId.isNotEmpty()) {
            coroutineScope.launch {
                fetchGroupExpenses(groupId)
            }
        }
    }

    // Load group data and current user info
    LaunchedEffect(groupId) {
        // Load group from backend API
        val firebaseUser = FirebaseAuth.getInstance().currentUser
        if (firebaseUser != null) {
            try {
                // Get current user info first
                val userResult = ApiRepository.auth.getUser(firebaseUser.uid)
                userResult.fold(
                        onSuccess = { userResponse ->
                            currentUserEmail = userResponse.user.email

                            // Load group details from backend
                            val groupResult = ApiRepository.group.getGroupById(groupId)
                            groupResult.fold(
                                    onSuccess = { groupData ->
                                        group = groupData
                                        // Load expenses for this group
                                        coroutineScope.launch {
                                            fetchGroupExpenses(groupId)
                                        }
                                    },
                                    onFailure = { exception ->
                                        updateError("Failed to load group: ${exception.message}")
                                    }
                            )

                            // Load friends for this user
                            isLoadingFriends = true
                            val friendsResult =
                                    ApiRepository.friends.getFriends(userResponse.user.email)
                            friendsResult.fold(
                                    onSuccess = { friendsList ->
                                        friends = friendsList
                                        // Fetch friend names
                                        coroutineScope.launch {
                                            fetchFriendNames(friendsList, userResponse.user.email)
                                        }
                                    },
                                    onFailure = { exception ->
                                        updateError("Failed to load friends: ${exception.message}")
                                    }
                            )
                            isLoadingFriends = false
                        },
                        onFailure = { exception ->
                            updateError("Failed to get user info: ${exception.message}")
                        }
                )
            } catch (e: Exception) {
                updateError("Exception getting user info: ${e.message}")
            }
        }
    }

    // Function to check if a friend is already in the group
    fun isFriendInGroup(friendEmail: String): Boolean {
        return group?.members?.any { member ->
            // For now, we'll check by email. In a real implementation,
            // you'd want to store user emails in the group members
            member.user?.email == friendEmail
        }
                ?: false
    }

    // Function to get available friends (not already in group)
    fun getAvailableFriends(): List<Pair<String, String>> {
        return friends.mapNotNull { friend ->
            val friendEmail =
                    if (friend.from_user == currentUserEmail) friend.to_user else friend.from_user
            val friendName = friendNames[friendEmail] ?: friendEmail
            if (!isFriendInGroup(friendEmail)) {
                Pair(friendEmail, friendName)
            } else {
                null
            }
        }
    }

    Scaffold(
            modifier = modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                        title = { Text(group?.name ?: "Group Details") },
                        navigationIcon = {
                            IconButton(onClick = onNavigateBack) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                            }
                        }
                )
            },
            floatingActionButton = {
                Column {
                    // Add Expense FAB
                    FloatingActionButton(
                            onClick = {
                                group?.let { groupData ->
                                    onAddExpense(groupData.id, groupData.name, groupData.members)
                                }
                            },
                            containerColor = Color(0xFFFF7024), // Orange background
                            contentColor = Color.White, // White content
                            modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.attach_money_24px),
                            contentDescription = "Add Expense"
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    // Add Friend FAB
                    FloatingActionButton(
                            onClick = { showAddUserDialog = true },
                            containerColor = Color(0xFFFF7024), // Orange background
                            contentColor = Color.White, // White content
                            modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.person_add_24px),
                            contentDescription = "Add Friend"
                        )
                    }
                }
            }
    ) { innerPadding ->
        LazyColumn(
                modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Group Info Card
            group?.let { groupData ->
                item {
                    GroupInfoCard(group = groupData)
                }
            }

            // Members Section
            group?.members?.let { members ->
                item {
                    Text(
                            text = "Members",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                items(members) { member ->
                    MemberCard(member = member)
                }
            }

            // Expenses Section
            item {
                Text(
                        text = "Expenses",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            if (isLoadingExpenses) {
                item {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            color = Color(0xFFFF7024) // Orange color
                        )
                    }
                }
            } else if (expenses.isEmpty()) {
                item {
                    Text(
                            text = "No expenses yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
            } else {
                items(expenses) { expense ->
                    ExpenseCard(
                        expense = expense,
                        onClick = { onExpenseClick(expense, group?.members ?: emptyList()) }
                    )
                }
            }
        }
    }

    // Add Friend Dialog
    if (showAddUserDialog) {
        AddFriendDialog(
                availableFriends = getAvailableFriends(),
                isLoading = isLoadingFriends,
                onDismiss = { showAddUserDialog = false },
                onAddFriend = { friendEmail, friendName ->
                    addFriendToGroup(group!!, friendEmail, friendName, coroutineScope) {
                            errorMessage ->
                        updateError(errorMessage)
                    }
                    showAddUserDialog = false
                }
        )
    }
}

@Composable
fun GroupInfoCard(group: Group, modifier: Modifier = Modifier) {
    Card(
            modifier = modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Text(
                    text = group.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
            )

            if (!group.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                        text = group.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF9E9E9E) // Light gray color
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Budget information
            group.totalBudget?.let { budget ->
                val budgetColor = if (budget > 0) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.attach_money_24px),
                        contentDescription = "Budget",
                        modifier = Modifier.size(16.dp),
                        tint = budgetColor
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Budget: $${"%.2f".format(budget)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = budgetColor
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                        text = "Created ${group.createdAt.substring(0, 10)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF9E9E9E) // Light gray color
                )
                Text(
                        text = "${group.members.size} members",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF9E9E9E) // Light gray color
                )
            }
        }
    }
}

@Composable
fun MemberCard(member: GroupMember, modifier: Modifier = Modifier) {
    Card(
            modifier = modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                    containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = Color(0xFFA6DB93),
                        shape = androidx.compose.foundation.shape.CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = member.user?.name ?: "User #${member.userId}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )
                Text(
                    text = member.user?.email ?: "Joined ${member.joinedAt.substring(0, 10)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFriendDialog(
        availableFriends: List<Pair<String, String>>,
        isLoading: Boolean,
        onDismiss: () -> Unit,
        onAddFriend: (String, String) -> Unit
) {
    var selectedFriendEmail by remember { mutableStateOf<String?>(null) }

    AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = Color.White,
            title = { Text("Add Friend to Group") },
            text = {
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            color = Color(0xFFFF7024) // Orange color
                        )
                    }
                } else if (availableFriends.isEmpty()) {
                    Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                                text = "No friends available",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                                text = "All your friends are already in this group",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                            modifier = Modifier.heightIn(max = 300.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(availableFriends) { (email, name) ->
                            FriendOptionCard(
                                    email = email,
                                    name = name,
                                    isSelected = selectedFriendEmail == email,
                                    onSelect = { selectedFriendEmail = email }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                        onClick = {
                            selectedFriendEmail?.let { email ->
                                val friendName =
                                        availableFriends.find { it.first == email }?.second ?: email
                                onAddFriend(email, friendName)
                            }
                        },
                        enabled =
                                selectedFriendEmail != null &&
                                        !isLoading &&
                                        availableFriends.isNotEmpty(),
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = Color(0xFFFF7024)
                        )
                ) { Text("Add Friend") }
            },
            dismissButton = { 
                TextButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color(0xFFFF7024)
                    )
                ) { Text("Cancel") } 
            }
    )
}

@Composable
fun FriendOptionCard(
        email: String,
        name: String,
        isSelected: Boolean,
        onSelect: () -> Unit,
        modifier: Modifier = Modifier
) {
    Card(
            modifier = modifier.fillMaxWidth(),
            colors =
                    CardDefaults.cardColors(
                            containerColor =
                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant
                    ),
            onClick = onSelect
    ) {
        Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = Color(0xFFA6DB93),
                        shape = androidx.compose.foundation.shape.CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                        text = name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                )
                Text(
                        text = email,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isSelected) {
                Icon(
                        Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = Color(0xFFFF7024)
                )
            }
        }
    }
}

// Function to add a friend to a group
fun addFriendToGroup(
    group: Group,
    friendEmail: String,
    friendName: String,
    coroutineScope: CoroutineScope,
    onError: (String) -> Unit
) {
    // Use backend API to add member to group
    val firebaseUser = FirebaseAuth.getInstance().currentUser
    if (firebaseUser != null) {
        coroutineScope.launch {
            try {
                val result = ApiRepository.group.addMemberToGroup(group.id, friendEmail)
                result.fold(
                        onSuccess = {
                            // Refresh the group data to show the new member
                            val groupResult = ApiRepository.group.getGroupById(group.id)
                            groupResult.fold(
                                    onSuccess = { updatedGroup ->
                                        // Update the group state
                                        // Note: In a real app, you might want to use a state
                                        // management solution
                                        // For now, we'll just show a success message
                                    },
                                    onFailure = { exception ->
                                        onError("Failed to refresh group: ${exception.message}")
                                    }
                            )
                        },
                        onFailure = { exception ->
                            onError("Failed to add member: ${exception.message}")
                        }
                )
            } catch (e: Exception) {
                onError("Exception adding member: ${e.message}")
            }
        }
    }
}

@Composable
fun ExpenseCard(
    expense: Expense, 
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Check if expense is fully paid
    val isFullyPaid = expense.splits.isNotEmpty() && expense.splits.all { split ->
        split.paidConfirmed != null
    }
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    tint = if (isFullyPaid) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = expense.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = if (isFullyPaid) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                        
                        // Category chip (only show if category exists and is valid)
                        expense.category?.let { categoryStr ->
                            if (categoryStr.isNotBlank() && categoryStr != "null") {
                                val category = ExpenseCategory.fromString(categoryStr)
                                CategoryChip(category = category)
                            }
                        }
                        
                        // Show paid indicator
                        if (isFullyPaid) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Fully Paid",
                                    tint = Color(0xFF2E7D32), // Green
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Paid",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF2E7D32), // Green
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                    Text(
                        text = "$${"%.2f".format(expense.totalAmount / 100.0)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isFullyPaid) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                    )
                    
                    // Creator information
                    expense.creator?.let { creator ->
                        Text(
                            text = "Created by: ${creator.name}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = expense.createdAt.substring(0, 10),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    // Due date with color coding (only show if not fully paid)
                    if (!isFullyPaid) {
                        expense.dueDate?.let { dueDate ->
                            val dueDateLocal = java.time.LocalDate.parse(dueDate)
                            val today = java.time.LocalDate.now()
                            val isOverdue = dueDateLocal.isBefore(today)
                            val isDueToday = dueDateLocal.isEqual(today)
                            
                            val dueDateColor = when {
                                isOverdue -> MaterialTheme.colorScheme.error
                                isDueToday -> MaterialTheme.colorScheme.error
                                else -> Color(0xFF2E7D32) // Green
                            }
                            
                            val dueDateText = when {
                                isOverdue -> "Overdue: $dueDate"
                                isDueToday -> "Due today: $dueDate"
                                else -> "Due: $dueDate"
                            }
                            
                            Text(
                                text = dueDateText,
                                style = MaterialTheme.typography.bodySmall,
                                color = dueDateColor,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
            
            // Show payment status summary
            if (expense.splits.isNotEmpty()) {
                val paidCount = expense.splits.count { it.paidConfirmed != null }
                val totalCount = expense.splits.size
                
                if (isFullyPaid) {
                    Text(
                        text = "✅ All ${totalCount} people have paid back",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF2E7D32), // Green
                        fontWeight = FontWeight.Medium
                    )
                } else {
                    Text(
                        text = "Split between ${totalCount} people (${paidCount}/${totalCount} paid)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
