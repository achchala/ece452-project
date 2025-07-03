package com.example.evenly

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.lifecycle.lifecycleScope
import com.example.evenly.api.ApiRepository
import com.example.evenly.ui.theme.HelloWorldTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

sealed class Screen {
    object Dashboard : Screen()
    object CreateGroup : Screen()
    object Groups : Screen()
    data class GroupDetail(val groupId: Int) : Screen()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Get the user name and ID from intent if passed
        val userName = intent.getStringExtra("user_name")
        val userId = intent.getIntExtra("user_id", -1) // Use -1 to indicate not provided
        
        setContent {
            HelloWorldTheme {
                var currentScreen by remember { mutableStateOf<Screen>(Screen.Dashboard) }
                
                when (currentScreen) {
                    Screen.Dashboard -> {
                        DashboardScreen(
                            userId = userId,
                            userName = userName,
                            onLogout = {
                                Firebase.auth.signOut()
                                val intent = Intent(this, Login::class.java)
                                startActivity(intent)
                            },
                            onCreateGroup = {
                                currentScreen = Screen.CreateGroup
                            },
                            onViewGroups = {
                                currentScreen = Screen.Groups
                            }
                        )
                    }
                    Screen.CreateGroup -> {
                        CreateGroupScreen(
                            onNavigateBack = {
                                currentScreen = Screen.Dashboard
                            },
                            onGroupCreated = { groupId ->
                                // Navigate to groups screen to show the newly created group
                                currentScreen = Screen.Groups
                            }
                        )
                    }
                    Screen.Groups -> {
                        GroupsScreen(
                            onNavigateBack = {
                                currentScreen = Screen.Dashboard
                            },
                            onCreateGroup = {
                                currentScreen = Screen.CreateGroup
                            },
                            onGroupClick = { groupId ->
                                currentScreen = Screen.GroupDetail(groupId)
                            }
                        )
                    }
                    is Screen.GroupDetail -> {
                        val groupDetail = currentScreen as Screen.GroupDetail
                        GroupDetailScreen(
                            groupId = groupDetail.groupId,
                            onNavigateBack = {
                                currentScreen = Screen.Groups
                            }
                        )
                    }
                }
            }
        }
    }
}