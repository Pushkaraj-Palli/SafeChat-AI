package com.example.chatappclient.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.chatappclient.model.User
import com.example.chatappclient.viewmodel.SettingsViewModel
import com.example.chatappclient.viewmodel.UserViewModel
import com.google.firebase.auth.FirebaseAuth

private val LightBlue = Color(0xFF90CAF9)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onSignOut: () -> Unit,
    settingsViewModel: SettingsViewModel,
    onUserClick: (User) -> Unit,
    userViewModel: UserViewModel = viewModel()
) {
    var showMenu by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf(TextFieldValue()) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    val isDarkMode by settingsViewModel.isDarkMode.collectAsState()
    val users by userViewModel.users.collectAsState()
    val isLoading by userViewModel.isLoading.collectAsState()
    val error by userViewModel.error.collectAsState()
    
    // Update current user's profile in Firestore when screen is first shown
    LaunchedEffect(Unit) {
        userViewModel.updateUserProfile()
    }

    val filteredUsers = users.filter {
        it.displayName.contains(searchQuery.text, ignoreCase = true) ||
        it.email.contains(searchQuery.text, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chat App") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = LightBlue,
                    titleContentColor = Color.Black,
                    actionIconContentColor = Color.Black
                ),
                actions = {
                    IconButton(onClick = { userViewModel.refreshUsers() }) {
                        Icon(Icons.Default.Refresh, "Refresh users")
                    }
                    IconButton(onClick = { showMenu = !showMenu }) {
                        Icon(Icons.Default.MoreVert, "More options")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Settings") },
                            onClick = {
                                showSettingsDialog = true
                                showMenu = false
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Settings, "Settings")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Sign Out") },
                            onClick = {
                                showMenu = false
                                onSignOut()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.ExitToApp, "Sign Out")
                            }
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Search users...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, "Search")
                },
                singleLine = true
            )

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
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(error ?: "Unknown error occurred")
                        Button(onClick = { userViewModel.refreshUsers() }) {
                            Text("Retry")
                        }
                    }
                }
            } else if (users.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("No users found")
                        Button(onClick = { userViewModel.refreshUsers() }) {
                            Text("Refresh")
                        }
                    }
                }
            } else {
                LazyColumn {
                    items(filteredUsers) { user ->
                        UserItem(
                            user = user,
                            onClick = { onUserClick(user) }
                        )
                    }
                }
            }
        }

        if (showSettingsDialog) {
            AlertDialog(
                onDismissRequest = { showSettingsDialog = false },
                title = { Text("Settings") },
                text = {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Dark Mode")
                            Switch(
                                checked = isDarkMode,
                                onCheckedChange = { settingsViewModel.toggleTheme() }
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSettingsDialog = false }) {
                        Text("Close")
                    }
                }
            )
        }
    }
}

@Composable
fun UserItem(
    user: User,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = user.photoUrl ?: "https://i.pravatar.cc/150?u=${user.uid}",
                contentDescription = "Profile picture of ${user.displayName}",
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                Text(
                    text = "${user.firstName} ${user.lastName}",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = user.email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
} 