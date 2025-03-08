package com.example.chatappclient.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.chatappclient.viewmodel.AdminViewModel
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    adminViewModel: AdminViewModel = viewModel(),
    onNavigateBack: () -> Unit
) {
    val violations by adminViewModel.violations.collectAsState()
    val warnings by adminViewModel.warnings.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }
    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Dashboard") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Violations") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Warned Users") }
                )
            }

            when (selectedTab) {
                0 -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        items(violations) { violation ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Violation Type:",
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (violation.hasBullyWords) Text("• Bullying")
                                    if (violation.hasSexualHarassmentWords) Text("• Sexual Harassment")
                                    if (violation.hasBadWords) Text("• Inappropriate Language")
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Message: ${violation.message}")
                                    Text("Date: ${dateFormat.format(violation.timestamp.toDate())}")
                                    Text("From: ${violation.senderId}")
                                    Text("To: ${violation.receiverId}")
                                }
                            }
                        }
                    }
                }
                1 -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        items(warnings) { warning ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .fillMaxWidth()
                                ) {
                                    Text(
                                        text = "User: ${warning.userId}",
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text("Warning Count: ${warning.warningCount}")
                                    Text("Status: ${if (warning.isBlocked) "Blocked" else "Active"}")
                                    if (warning.isBlocked && warning.blockExpiryDate != null) {
                                        Text("Block Expires: ${dateFormat.format(warning.blockExpiryDate.toDate())}")
                                    }
                                    Text("Last Warning: ${dateFormat.format(warning.lastWarningDate.toDate())}")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
} 