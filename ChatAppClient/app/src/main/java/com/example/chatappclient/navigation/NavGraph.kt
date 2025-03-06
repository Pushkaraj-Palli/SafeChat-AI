package com.example.chatappclient.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.chatappclient.screens.*
import com.example.chatappclient.model.User
import com.google.gson.Gson
import com.example.chatappclient.viewmodel.AuthViewModel
import com.example.chatappclient.viewmodel.SettingsViewModel

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object Home : Screen("home")
    object Chat : Screen("chat/{user}") {
        fun createRoute(user: User): String {
            val userJson = Gson().toJson(user)
            return "chat/${userJson}"
        }
    }
}

@Composable
fun NavGraph(
    navController: NavHostController,
    authViewModel: AuthViewModel = viewModel(),
    settingsViewModel: SettingsViewModel
) {
    val isAuthenticated by authViewModel.isAuthenticated.collectAsState()

    NavHost(
        navController = navController,
        startDestination = if (isAuthenticated == true) Screen.Home.route else Screen.Login.route
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onRegisterClick = { navController.navigate(Screen.Register.route) },
                onLoginSuccess = { 
                    authViewModel.checkAuthState()
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Register.route) {
            RegisterScreen(
                onBackClick = { navController.navigateUp() },
                onRegisterSuccess = {
                    authViewModel.checkAuthState()
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Home.route) {
            HomeScreen(
                onSignOut = {
                    authViewModel.signOut()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                settingsViewModel = settingsViewModel,
                onUserClick = { user ->
                    navController.navigate(Screen.Chat.createRoute(user))
                }
            )
        }
        composable(
            route = Screen.Chat.route
        ) { backStackEntry ->
            val userJson = backStackEntry.arguments?.getString("user")
            val user = Gson().fromJson(userJson, User::class.java)
            ChatScreen(
                user = user,
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
    }
} 