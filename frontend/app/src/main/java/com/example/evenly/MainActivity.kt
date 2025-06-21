package com.example.evenly

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.example.evenly.api.ApiRepository
import com.example.evenly.ui.theme.HelloWorldTheme
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Get the user name from intent if passed from NameCollection
        val userName = intent.getStringExtra("user_name")
        
        setContent {
            HelloWorldTheme {
                MainScreen(
                    initialUserName = userName,
                    onLogout = {
                        lifecycleScope.launch {
//                            ApiRepository.auth.logout()
                            val intent = Intent(this@MainActivity, Login::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    initialUserName: String?,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    var userName by remember { mutableStateOf<String?>(initialUserName) }
    var isLoading by remember { mutableStateOf(initialUserName == null) }
    
    // Fetch user name from API only if not provided via intent
    LaunchedEffect(initialUserName) {
        if (initialUserName == null) {
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser != null) {
                try {
                    val result = ApiRepository.auth.getUser(currentUser.uid)
                    result.fold(
                        onSuccess = { response ->
                            userName = response.user.name ?: "User"
                            isLoading = false
                        },
                        onFailure = { exception ->
                            userName = "User"
                            isLoading = false
                        }
                    )
                } catch (e: Exception) {
                    userName = "User"
                    isLoading = false
                }
            } else {
                userName = "User"
                isLoading = false
            }
        }
    }
    
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Evenly") }
            )
        }
    ) { innerPadding ->
        NameListScreen(
            userName = userName,
            isLoading = isLoading,
            onLogout = onLogout,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NameListScreen(
    userName: String? = null,
    isLoading: Boolean = false,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    var nameText by remember { mutableStateOf("") }
    var names by remember { mutableStateOf(listOf<String>()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Personalized header
        if (isLoading) {
            Text(
                text = "Loading...",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 24.dp)
            )
        } else if (userName != null) {
            Text(
                text = "Hi $userName! Ready to split evenly?",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }
        
        TextField(
            value = nameText,
            onValueChange = { nameText = it },
            label = { Text("Enter a name") },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Button(
            onClick = {
                if (nameText.isNotBlank()) {
                    names = names + nameText
                    nameText = ""
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Add Name")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyColumn {
            items(names) { name ->
                Greeting(name = name)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        Button(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Logout")
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = "Hello $name!",
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun NameListScreenPreview() {
    HelloWorldTheme {
        NameListScreen(onLogout = {})
    }
}