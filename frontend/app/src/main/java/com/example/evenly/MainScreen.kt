package com.example.evenly

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

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
    var selectedGroupMembers by remember { mutableStateOf<List<com.example.evenly.api.group.models.GroupMember>?>(null) }

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
                                        containerColor = MaterialTheme.colorScheme.surface,
                                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                                        actionIconContentColor = MaterialTheme.colorScheme.onSurface
                                )
                )
            },
            bottomBar = {
                // Only show bottom navigation for main screens
                if (selectedScreen in listOf(Screen.Dashboard, Screen.Friends, Screen.Groups)) {
                    NavigationBar {
                        NavigationBarItem(
                                icon = {
                                    Icon(
                                            Icons.Default.Home,
                                            contentDescription = Screen.Dashboard.title
                                    )
                                },
                                label = { Text(Screen.Dashboard.title) },
                                selected = selectedScreen == Screen.Dashboard,
                                onClick = { selectedScreen = Screen.Dashboard }
                        )
                        NavigationBarItem(
                                icon = {
                                    Icon(
                                            Icons.Default.Person,
                                            contentDescription = Screen.Friends.title
                                    )
                                },
                                label = { Text(Screen.Friends.title) },
                                selected = selectedScreen == Screen.Friends,
                                onClick = { selectedScreen = Screen.Friends }
                        )
                        NavigationBarItem(
                                icon = {
                                    Icon(
                                            painter = painterResource(id = R.drawable.group_24px),
                                            contentDescription = Screen.Groups.title
                                    )
                                },
                                label = { Text(Screen.Groups.title) },
                                selected = selectedScreen == Screen.Groups,
                                onClick = { selectedScreen = Screen.Groups }
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
                            modifier = Modifier.padding(innerPadding)
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

@Composable
fun LogoutButton(onLogout: () -> Unit, modifier: Modifier = Modifier) {
    Button(
            onClick = onLogout,
            modifier = modifier.padding(end = 8.dp).height(40.dp),
            colors =
                    ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
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
                    modifier = Modifier.size(18.dp)
            )
            Text(
                    text = "Logout",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium
            )
        }
    }
}
