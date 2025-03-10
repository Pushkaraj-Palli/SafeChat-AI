package com.example.chatappadmin.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch

@Composable
fun RegisterScreen(
    navController: NavController,
    auth: FirebaseAuth
) {
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val scope = rememberCoroutineScope()
    val db = FirebaseFirestore.getInstance()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Create New Account",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = firstName,
            onValueChange = { firstName = it.trim() },
            label = { Text("First Name", color = MaterialTheme.colorScheme.onBackground) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = lastName,
            onValueChange = { lastName = it.trim() },
            label = { Text("Last Name", color = MaterialTheme.colorScheme.onBackground) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = email,
            onValueChange = { email = it.trim() },
            label = { Text("Email", color = MaterialTheme.colorScheme.onBackground) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password", color = MaterialTheme.colorScheme.onBackground) },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Confirm Password", color = MaterialTheme.colorScheme.onBackground) },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            isError = password != confirmPassword && confirmPassword.isNotEmpty()
        )

        if (errorMessage != null) {
            Text(
                text = errorMessage!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                scope.launch {
                    try {
                        if (password != confirmPassword) {
                            errorMessage = "Passwords do not match"
                            return@launch
                        }
                        
                        if (password.length < 6) {
                            errorMessage = "Password must be at least 6 characters long"
                            return@launch
                        }

                        if (!email.contains("@")) {
                            errorMessage = "Please enter a valid email address"
                            return@launch
                        }

                        isLoading = true
                        errorMessage = null
                        
                        // Create user with email and password
                        val result = auth.createUserWithEmailAndPassword(email, password).await()
                        
                        // Update display name
                        result.user?.updateProfile(
                            com.google.firebase.auth.UserProfileChangeRequest.Builder()
                                .setDisplayName("$firstName $lastName")
                                .build()
                        )?.await()

                        // Create user document in Firestore
                        val user = hashMapOf(
                            "uid" to result.user?.uid,
                            "firstName" to firstName,
                            "lastName" to lastName,
                            "email" to email,
                            "createdAt" to com.google.firebase.Timestamp.now(),
                            "displayName" to "$firstName $lastName"
                        )

                        // Add user to Firestore
                        db.collection("users")
                            .document(result.user?.uid ?: "")
                            .set(user)
                            .await()
                        
                        // Navigate to home screen
                        navController.navigate("home") {
                            popUpTo("register") { inclusive = true }
                        }
                    } catch (e: Exception) {
                        errorMessage = e.message
                    } finally {
                        isLoading = false
                    }
                }
            },
            enabled = !isLoading && 
                     email.isNotBlank() && 
                     password.isNotBlank() && 
                     confirmPassword.isNotBlank() &&
                     firstName.isNotBlank() && 
                     lastName.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Create Account")
            }
        }

        TextButton(
            onClick = { navController.navigate("login") },
            colors = ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.colorScheme.onBackground
            )
        ) {
            Text("Already have an account? Sign In")
        }
    }
}