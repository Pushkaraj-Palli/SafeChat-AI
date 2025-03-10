package com.example.chatappadmin.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.example.chatappadmin.ui.components.Header
import com.example.chatappadmin.ui.components.AdminDashboardScreen

@Composable
fun HomeScreen(
    navController: NavController,
    auth: FirebaseAuth,
    isDarkTheme: Boolean,
    onThemeChange: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Header(
                title = "Chat Admin",
                onThemeChange = { onThemeChange(!isDarkTheme) },
                onSignOut = {
                    auth.signOut()
                    navController.navigate("login") {
                        popUpTo("home") { inclusive = true }
                    }
                },
                isDarkTheme = isDarkTheme
            )

            // Content area
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                AdminDashboardScreen()
            }
        }
    }
}