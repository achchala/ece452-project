package com.example.evenly

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Home
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
import com.example.evenly.api.group.models.GroupMember
import com.example.evenly.ui.theme.BottomBackgroundColor
import com.example.evenly.ui.theme.TopBackgroundColor

sealed class Screen(val route: String, val title: String) {
    object Dashboard : Screen("dashboard", "Dashboard")
    object Friends : Screen("friends", "Friends")
    object Groups : Screen("groups", "Groups")
    object CreateGroup : Screen("create_group", "Create Group")
    object GroupDetail : Screen("group_detail", "Group Details")
    object AddExpense : Screen("add_expense", "Add Expense")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
        userId: String,
        userName: String? = null,
        onLogout: () -> Unit,
        modifier: Modifier = Modifier
) {
    var selectedScreen by remember { mutableStateOf<Screen>(Screen.Dashboard) }
    var selectedGroupId by remember { mutableStateOf<String?>(null) }
    var selectedGroupName by remember { mutableStateOf<String?>(null) }
    var selectedGroupMembers by remember { mutableStateOf<List<GroupMember>?>(null) }
    var selectedExpense by remember { mutableStateOf<com.example.evenly.api.expenses.models.Expense?>(null) }
    var showExpenseDetailModal by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(TopBackgroundColor, BottomBackgroundColor)
                )
            )
    ) {
        Scaffold(
                modifier = modifier,
                topBar = {
                    TopAppBar(
                            title = {
                                Text(
                                        text = selectedScreen.title,
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.SemiBold
                                )
                            },
                            actions = { LogoutButton(onLogout = onLogout) },
                            colors =
                                    TopAppBarDefaults.topAppBarColors(
                                            containerColor = Color(0xFFE2F0E8),
                                            titleContentColor = MaterialTheme.colorScheme.onSurface,
                                            actionIconContentColor = MaterialTheme.colorScheme.onSurface
                                    )
                    )
                },
                bottomBar = {
                    // Only show bottom navigation for main screens
                    if (selectedScreen in listOf(Screen.Dashboard, Screen.Friends, Screen.Groups)) {
                        NavigationBar(
                            containerColor = Color.White
                        ) {
                            NavigationBarItem(
                                    icon = {
                                        Icon(
                                                Icons.Default.Home,
                                                contentDescription = Screen.Dashboard.title,
                                                tint = if (selectedScreen == Screen.Dashboard) Color(0xFF5BBD6C) else Color.Gray
                                        )
                                    },
                                    label = { 
                                        Text(
                                            Screen.Dashboard.title,
                                            color = if (selectedScreen == Screen.Dashboard) Color(0xFF5BBD6C) else Color.Gray
                                        ) 
                                    },
                                    selected = selectedScreen == Screen.Dashboard,
                                    onClick = { selectedScreen = Screen.Dashboard },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Color(0xFF5BBD6C),
                                        selectedTextColor = Color(0xFF5BBD6C),
                                        unselectedIconColor = Color.Gray,
                                        unselectedTextColor = Color.Gray,
                                        indicatorColor = Color.Transparent
                                    )
                            )
                            NavigationBarItem(
                                    icon = {
                                        Icon(
                                                Icons.Default.Person,
                                                contentDescription = Screen.Friends.title,
                                                tint = if (selectedScreen == Screen.Friends) Color(0xFF5BBD6C) else Color.Gray
                                        )
                                    },
                                    label = { 
                                        Text(
                                            Screen.Friends.title,
                                            color = if (selectedScreen == Screen.Friends) Color(0xFF5BBD6C) else Color.Gray
                                        ) 
                                    },
                                    selected = selectedScreen == Screen.Friends,
                                    onClick = { selectedScreen = Screen.Friends },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Color(0xFF5BBD6C),
                                        selectedTextColor = Color(0xFF5BBD6C),
                                        unselectedIconColor = Color.Gray,
                                        unselectedTextColor = Color.Gray,
                                        indicatorColor = Color.Transparent
                                    )
                            )
                            NavigationBarItem(
                                    icon = {
                                        Icon(
                                                painter = painterResource(id = R.drawable.group_24px),
                                                contentDescription = Screen.Groups.title,
                                                tint = if (selectedScreen == Screen.Groups) Color(0xFF5BBD6C) else Color.Gray
                                        )
                                    },
                                    label = { 
                                        Text(
                                            Screen.Groups.title,
                                            color = if (selectedScreen == Screen.Groups) Color(0xFF5BBD6C) else Color.Gray
                                        ) 
                                    },
                                    selected = selectedScreen == Screen.Groups,
                                    onClick = { selectedScreen = Screen.Groups },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Color(0xFF5BBD6C),
                                        selectedTextColor = Color(0xFF5BBD6C),
                                        unselectedIconColor = Color.Gray,
                                        unselectedTextColor = Color.Gray,
                                        indicatorColor = Color.Transparent
                                    )
                            )
                        }
                    }
                }
        ) { innerPadding ->
        when (selectedScreen) {
                Screen.Dashboard -> {
                    DashboardScreen(
                            userId = userId,
                            userName = userName,
                            onLogout = onLogout,
                            onCreateGroup = { selectedScreen = Screen.CreateGroup },
                            modifier = Modifier.padding(innerPadding)
                    )
                }
                Screen.Friends -> {
                    FriendsScreen(userId = userId, modifier = Modifier.padding(innerPadding))
                }
                Screen.CreateGroup -> {
                    CreateGroupScreen(
                            onNavigateBack = { selectedScreen = Screen.Dashboard },
                            onGroupCreated = { groupId ->
                                selectedGroupId = groupId
                                selectedScreen = Screen.GroupDetail
                            },
                            modifier = Modifier.padding(innerPadding)
                    )
                }
                Screen.Groups -> {
                    GroupsScreen(
                            onCreateGroup = { selectedScreen = Screen.CreateGroup },
                            onGroupClick = { groupId ->
                                selectedGroupId = groupId
                                selectedScreen = Screen.GroupDetail
                            },
                            modifier = Modifier.padding(innerPadding)
                    )
                }
                Screen.GroupDetail -> {
                    selectedGroupId?.let { groupId ->
                        GroupDetailScreen(
                                groupId = groupId,
                                onNavigateBack = { selectedScreen = Screen.Groups },
                                onAddExpense = { groupId, groupName, groupMembers ->
                                    selectedGroupId = groupId
                                    selectedGroupName = groupName
                                    selectedGroupMembers = groupMembers
                                    selectedScreen = Screen.AddExpense
                                },
                                onExpenseClick = { expense, groupMembers ->
                                    selectedExpense = expense
                                    selectedGroupMembers = groupMembers
                                    showExpenseDetailModal = true
                                },
                                modifier = Modifier.padding(innerPadding),
                                key = selectedExpense // Force refresh when selectedExpense changes
                        )
                    }
                }
                Screen.AddExpense -> {
                    selectedGroupId?.let { groupId ->
                        selectedGroupName?.let { groupName ->
                            selectedGroupMembers?.let { groupMembers ->
                                AddExpenseScreen(
                                    groupId = groupId,
                                    groupName = groupName,
                                    groupMembers = groupMembers,
                                    onNavigateBack = { selectedScreen = Screen.GroupDetail },
                                    onExpenseAdded = { selectedScreen = Screen.GroupDetail },
                                    modifier = Modifier.padding(innerPadding)
                                )
                            }
                        }
                    }
                }
            }

        }
    }

    // Expense Detail Modal
    if (showExpenseDetailModal && selectedExpense != null) {
        selectedGroupMembers?.let { groupMembers ->
            ExpenseDetailModal(
                expense = selectedExpense!!,
                groupMembers = groupMembers,
                onDismiss = { showExpenseDetailModal = false },
                onExpenseUpdated = { 
                    showExpenseDetailModal = false
                    // Force refresh of the group detail screen by updating the selectedExpense
                    selectedExpense = null
                },
                onExpenseDeleted = { 
                    showExpenseDetailModal = false
                    // Force refresh of the group detail screen by updating the selectedExpense
                    selectedExpense = null
                }
            )
        }
    }
}

@Composable
fun LogoutButton(onLogout: () -> Unit, modifier: Modifier = Modifier) {
    Button(
            onClick = onLogout,
            modifier = modifier.padding(end = 8.dp).height(40.dp),
            colors =
                    ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF7026),
                            contentColor = Color.White
                    ),
            shape = MaterialTheme.shapes.medium
    ) {
        Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                    imageVector = Icons.Default.ExitToApp,
                    contentDescription = "Logout",
                    modifier = Modifier.size(18.dp),
                    tint = Color.White
            )
            Text(
                    text = "Logout",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
            )
        }
    }
}
