package com.example.chatappclient.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chatappclient.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class UserViewModel : ViewModel() {
    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val firestore = FirebaseFirestore.getInstance()
    private val currentUser = FirebaseAuth.getInstance().currentUser

    init {
        Log.d("UserViewModel", "Initializing UserViewModel. Current user: ${currentUser?.uid}")
        loadUsers()
    }

    private fun loadUsers() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                
                Log.d("UserViewModel", "Starting to load users. Current user ID: ${currentUser?.uid}")
                
                // Try to get cached data first
                val usersRef = firestore.collection("users")
                val snapshot = try {
                    usersRef.get(Source.CACHE).await()
                } catch (e: Exception) {
                    Log.d("UserViewModel", "No cached data available, fetching from server")
                    usersRef.get(Source.SERVER).await()
                }
                
                Log.d("UserViewModel", "Total documents in users collection: ${snapshot.documents.size}")
                
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
            } catch (e: Exception) {
                Log.e("UserViewModel", "Error loading users", e)
                _error.value = "Failed to load users: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateUserProfile() {
        currentUser?.let { user ->
            viewModelScope.launch {
                try {
                    val docRef = firestore.collection("users").document(user.uid)
                    
                    // Try to get cached data first
                    val docSnapshot = try {
                        docRef.get(Source.CACHE).await()
                    } catch (e: Exception) {
                        Log.d("UserViewModel", "No cached profile data available, fetching from server")
                        docRef.get(Source.SERVER).await()
                    }
                    
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

                    if (docSnapshot.exists()) {
                        Log.d("UserViewModel", "User document exists, updating...")
                    } else {
                        Log.d("UserViewModel", "User document doesn't exist, creating new...")
                    }

                    // Enable offline persistence for this operation
                    docRef.set(userProfile)
                        .await()
                    
                    Log.d("UserViewModel", "User profile updated/created successfully")
                    
                    // Try to verify the update
                    try {
                        val updatedDoc = docRef.get(Source.CACHE).await()
                        Log.d("UserViewModel", "Verified user document (from cache): ${updatedDoc.data}")
                    } catch (e: Exception) {
                        Log.d("UserViewModel", "Could not verify update from cache: ${e.message}")
                    }
                    
                    loadUsers()
                } catch (e: Exception) {
                    val errorMessage = when {
                        e.message?.contains("offline") == true -> 
                            "You're offline. Changes will sync when you're back online."
                        else -> "Failed to update profile: ${e.message}"
                    }
                    Log.e("UserViewModel", "Error updating user profile", e)
                    _error.value = errorMessage
                }
            }
        } ?: run {
            Log.e("UserViewModel", "No current user found")
            _error.value = "No user logged in"
        }
    }

    fun refreshUsers() {
        Log.d("UserViewModel", "Manual refresh triggered")
        loadUsers()
    }
} 