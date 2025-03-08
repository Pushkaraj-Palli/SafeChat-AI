package com.example.chatappclient.viewmodel

import android.util.Log
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chatappclient.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack

class UserViewModel : ViewModel() {
    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val firestore = FirebaseFirestore.getInstance()
    private val currentUser = FirebaseAuth.getInstance().currentUser
    private var usersListener: ListenerRegistration? = null

    init {
        Log.d("UserViewModel", "Initializing UserViewModel. Current user: ${currentUser?.uid}")
        // Enable offline persistence
        try {
            val settings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build()
            firestore.firestoreSettings = settings
            Log.d("UserViewModel", "Offline persistence enabled successfully")
        } catch (e: Exception) {
            Log.e("UserViewModel", "Error enabling offline persistence", e)
        }
        startListeningToUsers()
    }

    private fun startListeningToUsers() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                
                Log.d("UserViewModel", "Starting to listen for users. Current user ID: ${currentUser?.uid}")
                
                usersListener?.remove() // Remove any existing listener
                
                usersListener = firestore.collection("users")
                    .addSnapshotListener { snapshot, e ->
                        if (e != null) {
                            Log.e("UserViewModel", "Listen failed.", e)
                            _error.value = "Failed to load users: ${e.message}"
                            return@addSnapshotListener
                        }

                        if (snapshot != null) {
                            val usersList = snapshot.documents
                                .mapNotNull { document ->
                                    try {
                                        val user = document.toObject(User::class.java)
                                        Log.d("UserViewModel", "Document ID: ${document.id}, User: $user")
                                        
                                        when {
                                            user == null -> null
                                            user.uid == currentUser?.uid -> {
                                                Log.d("UserViewModel", "Skipping current user: ${user.uid}")
                                                null
                                            }
                                            else -> user
                                        }
                                    } catch (e: Exception) {
                                        Log.e("UserViewModel", "Error parsing user document: ${document.id}", e)
                                        null
                                    }
                                }
                            
                            Log.d("UserViewModel", "Final users list size: ${usersList.size}")
                            Log.d("UserViewModel", "Users found: ${usersList.joinToString { user -> 
                                "${user.firstName} ${user.lastName} (${user.email})" 
                            }}")
                            
                            _users.value = usersList
                        }
                        _isLoading.value = false
                    }
            } catch (e: Exception) {
                Log.e("UserViewModel", "Error setting up users listener", e)
                _error.value = "Failed to load users: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    fun updateUserProfile() {
        currentUser?.let { user ->
            viewModelScope.launch {
                try {
                    val docRef = firestore.collection("users").document(user.uid)
                    val docSnapshot = docRef.get().await()
                    
                    val existingUser = docSnapshot.toObject(User::class.java)
                    val userProfile = User(
                        uid = user.uid,
                        firstName = existingUser?.firstName ?: "",
                        lastName = existingUser?.lastName ?: "",
                        displayName = user.displayName ?: "${existingUser?.firstName} ${existingUser?.lastName}".trim(),
                        email = user.email ?: "",
                        photoUrl = user.photoUrl?.toString()
                    )

                    Log.d("UserViewModel", "Updating user profile in Firestore. User: $userProfile")

                    docRef.set(userProfile)
                        .await()
                    
                    Log.d("UserViewModel", "User profile updated successfully")
                } catch (e: Exception) {
                    Log.e("UserViewModel", "Error updating user profile", e)
                    _error.value = "Failed to update profile: ${e.message}"
                }
            }
        } ?: run {
            Log.e("UserViewModel", "No current user found")
            _error.value = "No user logged in"
        }
    }

    fun refreshUsers() {
        Log.d("UserViewModel", "Manual refresh triggered")
        startListeningToUsers()
    }

    override fun onCleared() {
        super.onCleared()
        usersListener?.remove()
    }
} 