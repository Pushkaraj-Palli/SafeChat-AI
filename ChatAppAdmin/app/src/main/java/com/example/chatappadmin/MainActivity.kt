package com.example.chatappadmin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.chatappadmin.ui.screens.*
import com.example.chatappadmin.ui.theme.ChatAppAdminTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class MainActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth

        setContent {
            ChatAppAdminTheme {
                val navController = rememberNavController()
                val startDestination = if (auth.currentUser != null) "home" else "login"

                NavHost(
                    navController = navController,
                    startDestination = startDestination
                ) {
                    composable("login") {
                        LoginScreen(navController = navController, auth = auth)
                    }
                    composable("register") {
                        RegisterScreen(navController = navController, auth = auth)
                    }
                    composable("home") {
                        HomeScreen(navController = navController, auth = auth)
                    }
                }
            }
        }
    }
}