package com.example.chatappadmin.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.chatappadmin.components.CustomTextField
import com.example.chatappadmin.components.PrimaryButton
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    navController: NavController,
    auth: FirebaseAuth
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Welcome back Admin!\nGlad to see you, Again!",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Start,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp)
        )

        CustomTextField(
            value = email,
            onValueChange = { email = it },
            label = "Enter your email",
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next
        )

        CustomTextField(
            value = password,
            onValueChange = { password = it },
            label = "Enter your password",
            isPassword = true,
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done
        )

        Text(
            text = "Forgot Password?",
            modifier = Modifier
                .align(Alignment.End)
                .padding(end = 16.dp, top = 8.dp)
                .clickable { /* TODO: Implement forgot password */ },
            color = MaterialTheme.colorScheme.primary
        )

        if (errorMessage != null) {
            Text(
                text = errorMessage!!,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        PrimaryButton(
            text = if (isLoading) "Logging in..." else "Login",
            onClick = {
                isLoading = true
                errorMessage = null
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        isLoading = false
                        if (task.isSuccessful) {
                            navController.navigate("home") {
                                popUpTo("login") { inclusive = true }
                            }
                        } else {
                            errorMessage = task.exception?.message ?: "Login failed"
                        }
                    }
            },
            enabled = !isLoading && email.isNotEmpty() && password.isNotEmpty()
        )

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Don't have an account? ",
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Register Now",
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { navController.navigate("register") }
            )
        }
    }
}