package com.example.evenly

import android.util.Log
import android.util.Patterns
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import com.example.evenly.ui.theme.BottomBackgroundColor
import com.example.evenly.ui.theme.TopBackgroundColor
import com.example.evenly.api.ApiRepository
import com.example.evenly.api.friends.FriendRequest
import com.example.evenly.api.friends.FriendAnalyticsResponse
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

enum class FriendsTab {
    FRIENDS, REQUESTS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(
    userId: String,
    modifier: Modifier = Modifier
) {
    var incomingRequests by remember { mutableStateOf<List<FriendRequest>>(emptyList()) }
    var currentFriends by remember { mutableStateOf<List<FriendRequest>>(emptyList()) }
    var outgoingRequests by remember { mutableStateOf<List<FriendRequest>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var showAddFriendDialog by remember { mutableStateOf(false) }
    var friendEmail by remember { mutableStateOf("") }
    var isAddingFriend by remember { mutableStateOf(false) }
    var currentUserEmail by remember { mutableStateOf<String?>(null) }
    var selectedTab by remember { mutableStateOf(FriendsTab.FRIENDS) }
    var friendNames by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var isLoadingFriendNames by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }
    var selectedFriendForAnalytics by remember { mutableStateOf<FriendRequest?>(null) }
    var showAnalyticsDialog by remember { mutableStateOf(false) }
    var friendAnalytics by remember { mutableStateOf<FriendAnalyticsResponse?>(null) }
    var isLoadingAnalytics by remember { mutableStateOf(false) }

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

    suspend fun loadFriendAnalytics(friend: FriendRequest) {
        isLoadingAnalytics = true
        val friendEmail = if (friend.from_user == currentUserEmail) friend.to_user else friend.from_user
        
        Log.d("FriendsScreen", "Loading analytics for friend: $friendEmail")
        Log.d("FriendsScreen", "Current user email: $currentUserEmail")
        
        try {
            val result = ApiRepository.friends.getFriendAnalytics(friendEmail, currentUserEmail!!)
            result.fold(
                onSuccess = { analytics ->
                    Log.d("FriendsScreen", "Successfully loaded analytics: $analytics")
                    friendAnalytics = analytics
                },
                onFailure = { exception ->
                    Log.e("FriendsScreen", "Failed to load friend analytics: ${exception.message}")
                    Log.e("FriendsScreen", "Exception stack trace: ${exception.stackTraceToString()}")
                    // Show error dialog or handle gracefully
                    showAnalyticsDialog = false
                }
            )
        } catch (e: Exception) {
            Log.e("FriendsScreen", "Exception loading friend analytics: ${e.message}")
            Log.e("FriendsScreen", "Exception stack trace: ${e.stackTraceToString()}")
            // Show error dialog or handle gracefully
            showAnalyticsDialog = false
        }
        
        isLoadingAnalytics = false
    }

    suspend fun refreshAllData() {
        currentUserEmail?.let { email ->
            isLoading = true
            error = null
            
            // Load both incoming requests and current friends
            val requestsResult = ApiRepository.friends.getIncomingRequests(email)
            val friendsResult = ApiRepository.friends.getFriends(email)
            val outgoingRequestsResult = ApiRepository.friends.getOutgoingRequests(email)

            requestsResult.fold(
                onSuccess = { requests ->
                    incomingRequests = requests
                },
                onFailure = { exception ->
                    error = exception.message ?: "Failed to load friend requests"
                }
            )

            outgoingRequestsResult.fold(
                onSuccess = { requests ->
                    outgoingRequests = requests
                },
                onFailure = { exception ->
                    Log.e("FriendsScreen", "Failed to load outgoing requests: ${exception.message}")
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

    suspend fun validateAndSendFriendRequest(email: String): Result<String> {
        // Validate email format
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return Result.failure(Exception("Invalid email format"))
        }
        
        // Check if trying to add yourself
        if (email == currentUserEmail) {
            return Result.failure(Exception("Cannot send a friend request to yourself"))
        }
        
        // Check if user exists in database
        val userExistsResult = ApiRepository.auth.getUserByEmail(email)
        userExistsResult.fold(
            onSuccess = { 
                // User exists, continue with validation
            },
            onFailure = { 
                return Result.failure(Exception("User with this email does not exist"))
            }
        )
        
        // Check if already friends
        val newFriendEmail = email
        val isAlreadyFriend = currentFriends.any { friend ->
            (friend.from_user == currentUserEmail && friend.to_user == newFriendEmail) ||
            (friend.from_user == newFriendEmail && friend.to_user == currentUserEmail)
        }
        
        if (isAlreadyFriend) {
            return Result.failure(Exception("You are already friends with this user"))
        }
        
        // Check if there's already a pending outgoing request
        val hasPendingOutgoingRequest = outgoingRequests.any { request ->
            request.to_user == email && request.from_user == currentUserEmail
        }
        if (hasPendingOutgoingRequest) {
            return Result.failure(Exception("You already have a pending friend request to this user"))
        }
        // Check if there's already a pending incoming request
        val hasPendingIncomingRequest = incomingRequests.any { request ->
            request.from_user == email && request.to_user == currentUserEmail
        }
        if (hasPendingIncomingRequest) {
            return Result.failure(Exception("You already have a pending friend request from this user"))
        }
        
        // Send the friend request
        return try {
            val result = ApiRepository.friends.sendFriendRequest(currentUserEmail!!, email)
            result.fold(
                onSuccess = { response ->
                    if (response.status == "request sent") {
                        Result.success("Friend request sent successfully")
                    } else {
                        Result.failure(Exception("Failed to send friend request"))
                    }
                },
                onFailure = { exception ->
                    Result.failure(exception)
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
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
            val outgoingRequestsResult = ApiRepository.friends.getOutgoingRequests(email)

            requestsResult.fold(
                onSuccess = { requests ->
                    incomingRequests = requests
                },
                onFailure = { exception ->
                    error = exception.message ?: "Failed to load friend requests"
                }
            )

            outgoingRequestsResult.fold(
                onSuccess = { requests ->
                    outgoingRequests = requests
                },
                onFailure = { exception ->
                    Log.e("FriendsScreen", "Failed to load outgoing requests: ${exception.message}")
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

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(TopBackgroundColor, BottomBackgroundColor)
                )
            )
    ) {
        // Main content
        Column(modifier = Modifier.fillMaxSize()) {
            // Tab Row at the very top, outside all padding
            // Custom tab selection with boxes
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Current Friends Tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            color = if (selectedTab == FriendsTab.FRIENDS) Color(0xFF5FB953) else Color.White,
                            shape = MaterialTheme.shapes.small
                        )
                        .clickable { selectedTab = FriendsTab.FRIENDS }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Current Friends",
                        color = if (selectedTab == FriendsTab.FRIENDS) Color.White else Color.Gray,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                // Friend Requests Tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            color = if (selectedTab == FriendsTab.REQUESTS) Color(0xFF5FB953) else Color.White,
                            shape = MaterialTheme.shapes.small
                        )
                        .clickable { selectedTab = FriendsTab.REQUESTS }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Friend Requests",
                        color = if (selectedTab == FriendsTab.REQUESTS) Color.White else Color.Gray,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            // Main content with padding
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                        .padding(top = 8.dp)
                ) {
                    Spacer(modifier = Modifier.height(8.dp))
                    // Content based on selected tab
                    when (selectedTab) {
                        FriendsTab.FRIENDS -> {
                            val pullRefreshState = rememberPullRefreshState(
                                refreshing = isRefreshing,
                                onRefresh = {
                                    currentUserEmail?.let { email ->
                                        coroutineScope.launch {
                                            isRefreshing = true
                                            refreshAllData()
                                            isRefreshing = false
                                        }
                                    }
                                }
                            )
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .pullRefresh(pullRefreshState)
                            ) {
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
                                                            isRefreshing = true
                                                            refreshAllData()
                                                            isRefreshing = false
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
                                        LazyColumn(
                                            modifier = Modifier.fillMaxSize(),
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            item {
                                                Box(
                                                    modifier = Modifier.fillMaxWidth(),
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
                                                            text = "You could hear a pin drop.",
                                                            style = MaterialTheme.typography.headlineSmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                        Spacer(modifier = Modifier.height(8.dp))
                                                        Text(
                                                            text = "Add friends to start splitting expenses",
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        LazyColumn(
                                            verticalArrangement = Arrangement.spacedBy(12.dp),
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            items(currentFriends) { friend ->
                                                FriendCard(
                                                    friend = friend,
                                                    currentUserEmail = currentUserEmail,
                                                    friendName = friendNames[if (friend.from_user == currentUserEmail) friend.to_user else friend.from_user],
                                                    onClick = {
                                                        selectedFriendForAnalytics = friend
                                                        if (currentUserEmail != null) {
                                                            showAnalyticsDialog = true
                                                            coroutineScope.launch {
                                                                try {
                                                                    loadFriendAnalytics(friend)
                                                                } catch (e: Exception) {
                                                                    Log.e("FriendsScreen", "Error loading analytics: ${e.message}")
                                                                    showAnalyticsDialog = false
                                                                }
                                                            }
                                                        } else {
                                                            Log.e("FriendsScreen", "Current user email is null")
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                                
                                PullRefreshIndicator(
                                    refreshing = isRefreshing,
                                    state = pullRefreshState,
                                    modifier = Modifier.align(Alignment.TopCenter)
                                )
                            }
                        }
                        FriendsTab.REQUESTS -> {
                            val pullRefreshState = rememberPullRefreshState(
                                refreshing = isRefreshing,
                                onRefresh = {
                                    currentUserEmail?.let { email ->
                                        coroutineScope.launch {
                                            isRefreshing = true
                                            refreshAllData()
                                            isRefreshing = false
                                        }
                                    }
                                }
                            )
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .pullRefresh(pullRefreshState)
                            ) {
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
                                                            isRefreshing = true
                                                            refreshAllData()
                                                            isRefreshing = false
                                                        }
                                                    }
                                                }
                                            ) {
                                                Text("Retry")
                                            }
                                        }
                                    }
                                } else {
                                    val allRequests = incomingRequests.map { it to true } + outgoingRequests.map { it to false }
                                    
                                    if (allRequests.isEmpty()) {
                                        LazyColumn(
                                            modifier = Modifier.fillMaxSize(),
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            item {
                                                Box(
                                                    modifier = Modifier.fillMaxWidth(),
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
                                                            text = "It's looking dry.",
                                                            style = MaterialTheme.typography.headlineSmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                        Spacer(modifier = Modifier.height(8.dp))
                                                        Text(
                                                            text = "You'll see friend requests here",
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        LazyColumn(
                                            verticalArrangement = Arrangement.spacedBy(12.dp),
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            items(allRequests) { (request, isIncoming) ->
                                                FriendRequestCard(
                                                    request = request,
                                                    isIncoming = isIncoming,
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
                                                    },
                                                    onCancel = { toUser ->
                                                        currentUserEmail?.let { currentEmail ->
                                                            coroutineScope.launch {
                                                                rejectFriendRequest(currentEmail, toUser) { result ->
                                                                    result.fold(
                                                                        onSuccess = {
                                                                            // Refresh all data after canceling
                                                                            coroutineScope.launch {
                                                                                refreshAllData()
                                                                            }
                                                                        },
                                                                        onFailure = { exception ->
                                                                            error = exception.message ?: "Failed to cancel friend request"
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
                                
                                PullRefreshIndicator(
                                    refreshing = isRefreshing,
                                    state = pullRefreshState,
                                    modifier = Modifier.align(Alignment.TopCenter)
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // Floating Action Button - positioned at the bottom right corner
        FloatingActionButton(
            onClick = { showAddFriendDialog = true },
            containerColor = Color(0xFFFF7024), // Orange background
            contentColor = Color.White, // White content
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
                    validationError = null
                }
            },
            containerColor = Color.White,
            title = {
                Text("Send Friend Request")
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = friendEmail,
                        onValueChange = { friendEmail = it },
                        placeholder = { Text("Enter email address") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = Color(0xFFF6F6F6),
                            focusedContainerColor = Color(0xFFF6F6F6),
                            unfocusedBorderColor = Color(0xFF5BBD6C),
                            focusedBorderColor = Color(0xFF5BBD6C)
                        )
                    )
                    if (validationError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = validationError!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (friendEmail.isNotBlank() && currentUserEmail != null) {
                            isAddingFriend = true
                            validationError = null // Clear previous errors
                            coroutineScope.launch {
                                val validationResult = validateAndSendFriendRequest(friendEmail)
                                validationResult.fold(
                                    onSuccess = { message ->
                                        showAddFriendDialog = false
                                        friendEmail = ""
                                        validationError = null
                                        // Refresh data after sending friend request
                                        refreshAllData()
                                    },
                                    onFailure = { exception ->
                                        validationError = exception.message ?: "Failed to send friend request"
                                    }
                                )
                                isAddingFriend = false
                            }
                        }
                    },
                    enabled = friendEmail.isNotBlank() && !isAddingFriend && currentUserEmail != null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF7024),
                        contentColor = Color.White
                    )
                ) {
                    if (isAddingFriend) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White
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
                        validationError = null
                    },
                    enabled = !isAddingFriend,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color(0xFFFF7024)
                    )
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Friend Analytics Dialog
    if (showAnalyticsDialog) {
        if (friendAnalytics != null) {
            FriendAnalyticsDialog(
                analytics = friendAnalytics!!,
                onDismiss = {
                    showAnalyticsDialog = false
                    friendAnalytics = null
                }
            )
        } else if (isLoadingAnalytics) {
            AlertDialog(
                onDismissRequest = { },
                containerColor = Color.White,
                title = {
                    Text("Loading Analytics...")
                },
                text = {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                },
                confirmButton = { }
            )
        }
    }
}

@Composable
fun FriendCard(
    friend: FriendRequest,
    currentUserEmail: String?,
    friendName: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
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
                    text = friendName ?: "Friend",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )
                Text(
                    text = "Email: ${if (friend.from_user == currentUserEmail) friend.to_user else friend.from_user}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun FriendAnalyticsDialog(
    analytics: FriendAnalyticsResponse,
    onDismiss: () -> Unit
) {
    Log.d("FriendAnalyticsDialog", "Rendering dialog with analytics: $analytics")
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = {
            Text(
                text = analytics.user_info.name ?: "Friend Analytics",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                // User Info Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "User Information",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "Name: ${analytics.user_info.name ?: "Not provided"}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                        Text(
                            text = "Email: ${analytics.user_info.email}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                        
                        analytics.user_info.friendship_date?.let { dateStr ->
                            val formattedDate = try {
                                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.getDefault())
                                val outputFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                                val date = inputFormat.parse(dateStr)
                                date?.let { outputFormat.format(it) }
                            } catch (e: Exception) {
                                dateStr.substring(0, 10)
                            }
                            Text(
                                text = "Friends since: $formattedDate",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Credit Score Section
                analytics.credit_score?.let { score ->
                    if (score >= 300 && score <= 850) { // Valid credit score range
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = "Credit Score",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                // Credit score visualization
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(60.dp)
                                        .background(
                                            color = Color(0xFFE8F5E8),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .padding(8.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    val progress = (score - 300) / 550f // Normalize to 0-1
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(progress.toFloat())
                                            .background(
                                                color = when {
                                                    score >= 750 -> Color(0xFF4CAF50) // Excellent
                                                    score >= 650 -> Color(0xFF8BC34A) // Good
                                                    score >= 550 -> Color(0xFFFFC107) // Fair
                                                    else -> Color(0xFFF44336) // Poor
                                                },
                                                shape = RoundedCornerShape(6.dp)
                                            )
                                    )
                                    Text(
                                        text = "$score",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(start = 12.dp)
                                    )
                                }
                                
                                Text(
                                    text = when {
                                        score >= 750 -> "Excellent"
                                        score >= 650 -> "Good"
                                        score >= 550 -> "Fair"
                                        else -> "Poor"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
                
                // Spending Analytics Section
                if (analytics.spending_analytics.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Spending by Category",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                            Text(
                                text = "Total spent: $${String.format("%.2f", analytics.total_spent)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            analytics.spending_analytics.forEach { category ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = category.category ?: "Other",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f),
                                        color = Color.Black
                                    )
                                    Text(
                                        text = "$${String.format("%.2f", category.amount)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.Black
                                    )
                                    Text(
                                        text = "(${String.format("%.1f", category.percentage)}%)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray,
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }
                                
                                // Progress bar for category
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .background(
                                            color = Color(0xFFE0E0E0),
                                            shape = RoundedCornerShape(2.dp)
                                        )
                                        .padding(top = 2.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth((category.percentage / 100f).toFloat())
                                            .background(
                                                color = Color(0xFF5FB953),
                                                shape = RoundedCornerShape(2.dp)
                                            )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF5FB953),
                    contentColor = Color.White
                )
            ) {
                Text("Close")
            }
        }
    )
}

@Composable
fun FriendRequestCard(
    request: FriendRequest,
    isIncoming: Boolean,
    onAccept: (String) -> Unit,
    onReject: (String) -> Unit,
    onCancel: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Request details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = if (isIncoming) "Friend request from" else "Friend request to",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                Text(
                    text = if (isIncoming) request.from_user else request.to_user,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )
                if (!isIncoming) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(
                                    color = Color(0xFFFFD700),
                                    shape = androidx.compose.foundation.shape.CircleShape
                                )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Pending",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
            }
            
            // Action buttons
            if (isIncoming) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onAccept(request.from_user) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF5BBD6C),
                            contentColor = Color.White
                        ),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(
                            text = "Accept",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    
                    OutlinedButton(
                        onClick = { onReject(request.from_user) },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFFF7024)
                        ),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(
                            text = "Reject",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            } else {
                Button(
                    onClick = { onCancel(request.to_user) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF7026),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(
                        text = "Cancel",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

private suspend fun getCurrentUserEmail(userId: String): String? {
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

private suspend fun acceptFriendRequest(fromUser: String, toUser: String, onResult: (Result<com.example.evenly.api.friends.AcceptFriendRequestResponse>) -> Unit) {
    val result = ApiRepository.friends.acceptFriendRequest(fromUser, toUser)
    onResult(result)
}

private suspend fun rejectFriendRequest(fromUser: String, toUser: String, onResult: (Result<com.example.evenly.api.friends.RejectFriendRequestResponse>) -> Unit) {
    val result = ApiRepository.friends.rejectFriendRequest(fromUser, toUser)
    onResult(result)
} 