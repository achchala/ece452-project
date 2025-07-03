package com.example.evenly

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.evenly.api.ApiRepository
import com.example.evenly.api.friends.FriendRequest
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

enum class FriendsTab {
    FRIENDS, REQUESTS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(
    userId: Int,
    modifier: Modifier = Modifier
) {
    var incomingRequests by remember { mutableStateOf<List<FriendRequest>>(emptyList()) }
    var currentFriends by remember { mutableStateOf<List<FriendRequest>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var showAddFriendDialog by remember { mutableStateOf(false) }
    var friendEmail by remember { mutableStateOf("") }
    var isAddingFriend by remember { mutableStateOf(false) }
    var currentUserEmail by remember { mutableStateOf<String?>(null) }
    var selectedTab by remember { mutableStateOf(FriendsTab.FRIENDS) }
    var friendNames by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var isLoadingFriendNames by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    suspend fun fetchFriendNames(friends: List<FriendRequest>, currentUserEmail: String) {
        isLoadingFriendNames = true
        val namesMap = mutableMapOf<String, String>()
        for (friend in friends) {
            val friendEmail = if (friend.from_user == currentUserEmail) friend.to_user else friend.from_user
            try {
                val userResult = ApiRepository.auth.getUserByEmail(friendEmail)
                userResult.fold(
                    onSuccess = { response ->
                        response.user.name?.let { name ->
                            namesMap[friendEmail] = name
                        }
                    },
                    onFailure = { exception ->
                        Log.e("FriendsScreen", "Failed to get friend name for $friendEmail: ${exception.message}")
                    }
                )
            } catch (e: Exception) {
                Log.e("FriendsScreen", "Exception getting friend name for $friendEmail: ${e.message}")
            }
        }
        friendNames = namesMap
        isLoadingFriendNames = false
    }

    suspend fun refreshAllData() {
        currentUserEmail?.let { email ->
            isLoading = true
            error = null
            
            // Load both incoming requests and current friends
            val requestsResult = ApiRepository.friends.getIncomingRequests(email)
            val friendsResult = ApiRepository.friends.getFriends(email)

            requestsResult.fold(
                onSuccess = { requests ->
                    incomingRequests = requests
                },
                onFailure = { exception ->
                    error = exception.message ?: "Failed to load friend requests"
                }
            )

            friendsResult.fold(
                onSuccess = { friends ->
                    currentFriends = friends
                    // Fetch names for all friends
                    coroutineScope.launch {
                        fetchFriendNames(friends, email)
                    }
                },
                onFailure = { exception ->
                    Log.e("FriendsScreen", "Failed to load friends: ${exception.message}")
                }
            )

            isLoading = false
        }
    }

    // Load current user email and incoming requests on first launch
    LaunchedEffect(userId) {
        val email = getCurrentUserEmail(userId)
        currentUserEmail = email
        if (email != null) {
            // Load both incoming requests and current friends
            val requestsResult = ApiRepository.friends.getIncomingRequests(email)
            val friendsResult = ApiRepository.friends.getFriends(email)

            requestsResult.fold(
                onSuccess = { requests ->
                    incomingRequests = requests
                },
                onFailure = { exception ->
                    error = exception.message ?: "Failed to load friend requests"
                }
            )

            friendsResult.fold(
                onSuccess = { friends ->
                    currentFriends = friends
                    // Fetch names for all friends
                    coroutineScope.launch {
                        fetchFriendNames(friends, email)
                    }
                },
                onFailure = { exception ->
                    // Don't set error for friends loading failure, just log it
                    Log.e("FriendsScreen", "Failed to load friends: ${exception.message}")
                }
            )

            isLoading = false
        } else {
            error = "Could not retrieve user email"
            isLoading = false
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp)
        ) {
            // Tab Row
            TabRow(
                selectedTabIndex = selectedTab.ordinal,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedTab == FriendsTab.FRIENDS,
                    onClick = { selectedTab = FriendsTab.FRIENDS },
                    text = { Text("Current Friends") },
                    modifier = Modifier.weight(1f)
                )
                Tab(
                    selected = selectedTab == FriendsTab.REQUESTS,
                    onClick = { selectedTab = FriendsTab.REQUESTS },
                    text = { Text("Friend Requests") },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Content based on selected tab
            when (selectedTab) {
                FriendsTab.FRIENDS -> {
                    if (isLoading || isLoadingFriendNames) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else if (error != null) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
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
                                Button(
                                    onClick = {
                                        isLoading = true
                                        error = null
                                        currentUserEmail?.let { email ->
                                            coroutineScope.launch {
                                                val requestsResult = ApiRepository.friends.getIncomingRequests(email)
                                                val friendsResult = ApiRepository.friends.getFriends(email)

                                                requestsResult.fold(
                                                    onSuccess = { requests ->
                                                        incomingRequests = requests
                                                    },
                                                    onFailure = { exception ->
                                                        error = exception.message ?: "Failed to load friend requests"
                                                    }
                                                )

                                                friendsResult.fold(
                                                    onSuccess = { friends ->
                                                        currentFriends = friends
                                                        // Fetch names for all friends
                                                        coroutineScope.launch {
                                                            fetchFriendNames(friends, email)
                                                        }
                                                    },
                                                    onFailure = { exception ->
                                                        Log.e("FriendsScreen", "Failed to load friends: ${exception.message}")
                                                    }
                                                )

                                                isLoading = false
                                            }
                                        }
                                    }
                                ) {
                                    Text("Retry")
                                }
                            }
                        }
                    } else {
                        if (currentFriends.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        modifier = Modifier.size(64.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "No friends yet",
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Accept friend requests to see your friends here",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(currentFriends) { friend ->
                                    val friendEmail = if (friend.from_user == currentUserEmail) friend.to_user else friend.from_user
                                    val friendName = friendNames[friendEmail]
                                    FriendCard(
                                        friend = friend,
                                        currentUserEmail = currentUserEmail,
                                        friendName = friendName
                                    )
                                }
                            }
                        }
                    }
                }
                FriendsTab.REQUESTS -> {
                    if (isLoading) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else if (error != null) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
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
                                Button(
                                    onClick = {
                                        isLoading = true
                                        error = null
                                        currentUserEmail?.let { email ->
                                            coroutineScope.launch {
                                                val requestsResult = ApiRepository.friends.getIncomingRequests(email)
                                                val friendsResult = ApiRepository.friends.getFriends(email)

                                                requestsResult.fold(
                                                    onSuccess = { requests ->
                                                        incomingRequests = requests
                                                    },
                                                    onFailure = { exception ->
                                                        error = exception.message ?: "Failed to load friend requests"
                                                    }
                                                )

                                                friendsResult.fold(
                                                    onSuccess = { friends ->
                                                        currentFriends = friends
                                                    },
                                                    onFailure = { exception ->
                                                        Log.e("FriendsScreen", "Failed to load friends: ${exception.message}")
                                                    }
                                                )

                                                isLoading = false
                                            }
                                        }
                                    }
                                ) {
                                    Text("Retry")
                                }
                            }
                        }
                    } else {
                        if (incomingRequests.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        modifier = Modifier.size(64.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "No friend requests yet",
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Accept friend requests here to add new friends",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(incomingRequests) { request ->
                                    FriendRequestCard(
                                        request = request,
                                        onAccept = { fromUser ->
                                            currentUserEmail?.let { currentEmail ->
                                                coroutineScope.launch {
                                                    acceptFriendRequest(fromUser, currentEmail) { result ->
                                                        result.fold(
                                                            onSuccess = {
                                                                // Refresh all data after accepting
                                                                coroutineScope.launch {
                                                                    refreshAllData()
                                                                }
                                                            },
                                                            onFailure = { exception ->
                                                                error = exception.message ?: "Failed to accept friend request"
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        },
                                        onReject = { fromUser ->
                                            currentUserEmail?.let { currentEmail ->
                                                coroutineScope.launch {
                                                    rejectFriendRequest(fromUser, currentEmail) { result ->
                                                        result.fold(
                                                            onSuccess = {
                                                                // Refresh all data after rejecting
                                                                coroutineScope.launch {
                                                                    refreshAllData()
                                                                }
                                                            },
                                                            onFailure = { exception ->
                                                                error = exception.message ?: "Failed to reject friend request"
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Floating Action Button
        FloatingActionButton(
            onClick = { showAddFriendDialog = true },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Send Friend Request"
            )
        }
    }

    // Send Friend Request Dialog
    if (showAddFriendDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isAddingFriend) {
                    showAddFriendDialog = false
                    friendEmail = ""
                }
            },
            title = {
                Text("Send Friend Request")
            },
            text = {
                Column {
                    Text(
                        text = "Enter your friend's email address",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = friendEmail,
                        onValueChange = { friendEmail = it },
                        label = { Text("Email") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (friendEmail.isNotBlank() && currentUserEmail != null) {
                            isAddingFriend = true
                            coroutineScope.launch {
                                sendFriendRequest(currentUserEmail!!, friendEmail) { result ->
                                    result.fold(
                                        onSuccess = { response ->
                                            if (response.status == "request sent") {
                                                showAddFriendDialog = false
                                                friendEmail = ""
                                                // Refresh data after sending friend request
                                                coroutineScope.launch {
                                                    refreshAllData()
                                                }
                                            } else {
                                                error = "Failed to send friend request"
                                            }
                                        },
                                        onFailure = { exception ->
                                            error = exception.message ?: "Failed to send friend request"
                                        }
                                    )
                                    isAddingFriend = false
                                }
                            }
                        }
                    },
                    enabled = friendEmail.isNotBlank() && !isAddingFriend && currentUserEmail != null
                ) {
                    if (isAddingFriend) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Send Request")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAddFriendDialog = false
                        friendEmail = ""
                    },
                    enabled = !isAddingFriend
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun FriendRequestCard(
    request: FriendRequest,
    onAccept: (String) -> Unit,
    onReject: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Friend Request",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "From: ${request.from_user}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onAccept(request.from_user) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Accept",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Accept")
                }
                Button(
                    onClick = { onReject(request.from_user) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Reject",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reject")
                }
            }
        }
    }
}

@Composable
fun FriendCard(
    friend: FriendRequest,
    currentUserEmail: String?,
    friendName: String?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = friendName ?: "Friend",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Email: ${if (friend.from_user == currentUserEmail) friend.to_user else friend.from_user}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private suspend fun getCurrentUserEmail(userId: Int): String? {
    val currentUser = FirebaseAuth.getInstance().currentUser
    Log.d("FriendsScreen", "getCurrentUserEmail called with userId: $userId")
    Log.d("FriendsScreen", "Current Firebase user: $currentUser")
    if (currentUser == null) {
        Log.e("FriendsScreen", "No Firebase user available")
        return null
    }

    Log.d("FriendsScreen", "Firebase user found with UID: ${currentUser.uid}")
    Log.d("FriendsScreen", "About to call API with firebaseId: ${currentUser.uid}")
    return try {
        val userResult = ApiRepository.auth.getUser(currentUser.uid)
        Log.d("FriendsScreen", "API call completed, result: $userResult")
        userResult.fold(
            onSuccess = {
                Log.d("FriendsScreen", "Successfully got user email: ${it.user.email}")
                it.user.email
            },
            onFailure = {
                Log.e("FriendsScreen", "Failed to get user from API: ${it.message}")
                null
            }
        )
    } catch (e: Exception) {
        Log.e("FriendsScreen", "Exception getting user from API: ${e.message}")
        Log.e("FriendsScreen", "Exception stack trace: ${e.stackTraceToString()}")
        null
    }
}

private suspend fun sendFriendRequest(fromUser: String, toUser: String, onResult: (Result<com.example.evenly.api.friends.SendFriendRequestResponse>) -> Unit) {
    val result = ApiRepository.friends.sendFriendRequest(fromUser, toUser)
    onResult(result)
}

private suspend fun acceptFriendRequest(fromUser: String, toUser: String, onResult: (Result<com.example.evenly.api.friends.AcceptFriendRequestResponse>) -> Unit) {
    val result = ApiRepository.friends.acceptFriendRequest(fromUser, toUser)
    onResult(result)
}

private suspend fun rejectFriendRequest(fromUser: String, toUser: String, onResult: (Result<com.example.evenly.api.friends.RejectFriendRequestResponse>) -> Unit) {
    val result = ApiRepository.friends.rejectFriendRequest(fromUser, toUser)
    onResult(result)
} 