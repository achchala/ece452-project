package com.example.evenly

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Create
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.evenly.api.ApiRepository
import com.example.evenly.api.auth.models.User as AuthUser
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupScreen(
        onNavigateBack: () -> Unit,
        onGroupCreated: (String) -> Unit,
        modifier: Modifier = Modifier
) {
        var groupName by remember { mutableStateOf("") }
        var groupDescription by remember { mutableStateOf("") }
        var groupBudget by remember { mutableStateOf("") }
        var isLoading by remember { mutableStateOf(false) }
        var error by remember { mutableStateOf<String?>(null) }
        var currentUser by remember { mutableStateOf<AuthUser?>(null) }

        // Get current user info
        LaunchedEffect(Unit) {
                val firebaseUser = FirebaseAuth.getInstance().currentUser
                if (firebaseUser != null) {
                        try {
                                val userResult = ApiRepository.auth.getUser(firebaseUser.uid)
                                userResult.fold(
                                        onSuccess = { currentUser = it.user },
                                        onFailure = {
                                                error = "Failed to get user info: ${it.message}"
                                        }
                                )
                        } catch (e: Exception) {
                                error = "Exception getting user info: ${e.message}"
                        }
                } else {
                        error = "User not authenticated"
                }
        }

        // Handle group creation
        LaunchedEffect(isLoading) {
                if (isLoading && groupName.isNotBlank() && currentUser != null) {
                        val firebaseUser = FirebaseAuth.getInstance().currentUser
                        if (firebaseUser != null) {
                                try {
                                        val result =
                                                ApiRepository.group.createGroup(
                                                        name = groupName,
                                                        description =
                                                                groupDescription.takeIf {
                                                                        it.isNotBlank()
                                                                },
                                                        totalBudget = groupBudget.toDoubleOrNull(),
                                                        firebaseId = firebaseUser.uid
                                                )

                                        result.fold(
                                                onSuccess = { response ->
                                                        isLoading = false
                                                        onGroupCreated(response.group.id)
                                                },
                                                onFailure = { exception ->
                                                        isLoading = false
                                                        error =
                                                                exception.message
                                                                        ?: "Failed to create group"
                                                }
                                        )
                                } catch (e: Exception) {
                                        isLoading = false
                                        error = e.message ?: "Failed to create group"
                                }
                        } else {
                                isLoading = false
                                error = "User not authenticated"
                        }
                }
        }

        Scaffold(
                modifier = modifier.fillMaxSize(),
                topBar = {
                        TopAppBar(
                                title = { Text("Create Group") },
                                navigationIcon = {
                                        IconButton(onClick = onNavigateBack) {
                                                Icon(
                                                        Icons.Default.ArrowBack,
                                                        contentDescription = "Back"
                                                )
                                        }
                                }
                        )
                }
        ) { innerPadding ->
                Column(
                        modifier =
                                Modifier.fillMaxSize()
                                        .padding(innerPadding)
                                        .padding(horizontal = 16.dp)
                                        .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                        // Header
                        Text(
                                text = "Create a New Group",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                        )

                        Text(
                                text =
                                        "Create a group to easily split expenses with friends and family.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Group Name Field
                        OutlinedTextField(
                                value = groupName,
                                onValueChange = { groupName = it },
                                label = { Text("Group Name *") },
                                placeholder = { Text("e.g., Roommates, Trip to Europe") },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions =
                                        KeyboardOptions(
                                                keyboardType = KeyboardType.Text,
                                                imeAction = ImeAction.Next
                                        ),
                                singleLine = true,
                                isError = groupName.isBlank() && !isLoading
                        )

                        // Group Description Field
                        OutlinedTextField(
                                value = groupDescription,
                                onValueChange = { groupDescription = it },
                                label = { Text("Description (Optional)") },
                                placeholder = { Text("Add a description for your group...") },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions =
                                        KeyboardOptions(
                                                keyboardType = KeyboardType.Text,
                                                imeAction = ImeAction.Next
                                        ),
                                minLines = 3,
                                maxLines = 5
                        )

                        // Group Budget Field
                        OutlinedTextField(
                                value = groupBudget,
                                onValueChange = { 
                                    // Only allow numbers and decimal point
                                    if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                                        groupBudget = it
                                    }
                                },
                                label = { Text("Total Budget (Optional)") },
                                placeholder = { Text("e.g., 1000.00") },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions =
                                        KeyboardOptions(
                                                keyboardType = KeyboardType.Decimal,
                                                imeAction = ImeAction.Done
                                        ),
                                singleLine = true
                        )

                        // Error Message
                        if (error != null) {
                                Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors =
                                                CardDefaults.cardColors(
                                                        containerColor =
                                                                MaterialTheme.colorScheme
                                                                        .errorContainer
                                                )
                                ) {
                                        Text(
                                                text = error!!,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onErrorContainer,
                                                modifier = Modifier.padding(16.dp)
                                        )
                                }
                        }

                        // Create Button
                        Button(
                                onClick = {
                                        if (groupName.isNotBlank() && currentUser != null) {
                                                isLoading = true
                                                error = null
                                        }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled =
                                        groupName.isNotBlank() && !isLoading && currentUser != null
                        ) {
                                if (isLoading) {
                                        CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp),
                                                color = Color(0xFFFF7024) // Orange color
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                }
                                Icon(Icons.Default.Create, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Create Group")
                        }

                        // Info Card
                        Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors =
                                        CardDefaults.cardColors(
                                                containerColor =
                                                        MaterialTheme.colorScheme.surfaceVariant
                                        )
                        ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                                text = "What happens next?",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Medium
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                                text =
                                                        "• You'll be automatically added as a member\n" +
                                                                "• You can invite friends to join the group\n" +
                                                                "• Start adding expenses to split with the group",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                }
                        }
                }
        }
}
