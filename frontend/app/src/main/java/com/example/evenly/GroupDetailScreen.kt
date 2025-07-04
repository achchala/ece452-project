package com.example.evenly

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.evenly.api.ApiRepository
import com.example.evenly.api.friends.FriendRequest
import com.example.evenly.api.group.models.Group
import com.example.evenly.api.group.models.GroupMember
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(groupId: Int, onNavigateBack: () -> Unit, modifier: Modifier = Modifier) {
    var group by remember { mutableStateOf<Group?>(null) }
    var showAddUserDialog by remember { mutableStateOf(false) }
    var currentUserEmail by remember { mutableStateOf<String?>(null) }
    var friends by remember { mutableStateOf<List<FriendRequest>>(emptyList()) }
    var friendNames by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var isLoadingFriends by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

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
                                    onSuccess = { groupData -> group = groupData },
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
                FloatingActionButton(
                        onClick = {
                            if (getAvailableFriends().isNotEmpty()) {
                                showAddUserDialog = true
                            }
                        },
                        containerColor =
                                if (getAvailableFriends().isNotEmpty())
                                        MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                ) { Icon(Icons.Default.Add, contentDescription = "Add Friend") }
            }
    ) { innerPadding ->
        if (group == null) {
            Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center
            ) { Text("Group not found") }
        } else {
            LazyColumn(
                    modifier =
                            Modifier.fillMaxSize()
                                    .padding(innerPadding)
                                    .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Group Info Card
                item { GroupInfoCard(group = group!!) }

                // Members Section
                item {
                    Text(
                            text = "Members (${group!!.members.size})",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                    )
                }

                // Member Cards
                items(group!!.members) { member -> MemberCard(member = member) }

                // Empty state for no members
                if (group!!.members.isEmpty()) {
                    item {
                        Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                        ) {
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
                                        text = "No members yet",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                        text = "Tap the + button to add friends",
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
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                    text = group.name,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
            )

            if (!group.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                        text = group.description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                        text = "Created ${group.createdAt.substring(0, 10)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                        text = "${group.members.size} members",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun MemberCard(member: GroupMember, modifier: Modifier = Modifier) {
    Card(
            modifier = modifier.fillMaxWidth(),
            colors =
                    CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
    ) {
        Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                        text = member.user?.name ?: "User #${member.userId}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                )
                Text(
                        text = member.user?.email ?: "Joined ${member.joinedAt.substring(0, 10)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (member.user?.email != null) {
                    Text(
                            text = "Joined ${member.joinedAt.substring(0, 10)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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
            title = { Text("Add Friend to Group") },
            text = {
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
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
                                        availableFriends.isNotEmpty()
                ) { Text("Add Friend") }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
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
            Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
            )

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
                        tint = MaterialTheme.colorScheme.primary
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
