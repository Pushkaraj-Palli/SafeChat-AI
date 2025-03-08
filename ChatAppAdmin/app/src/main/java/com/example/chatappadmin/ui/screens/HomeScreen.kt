package com.example.chatappadmin.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth

@Composable
fun HomeScreen(
    navController: NavController,
    auth: FirebaseAuth
) {
    val currentUser = auth.currentUser

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Welcome ${currentUser?.email ?: "User"}!",
            style = MaterialTheme.typography.headlineMedium
        )

        Button(
            onClick = {
                auth.signOut()
                navController.navigate("login") {
                    popUpTo("home") { inclusive = true }
                }
            }
        ) {
            Text("Sign Out")
        }
    }
} 