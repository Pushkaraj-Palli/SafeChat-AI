package com.example.chatappclient.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
    private val _isAuthenticated = MutableStateFlow<Boolean?>(null)
    val isAuthenticated: StateFlow<Boolean?> = _isAuthenticated

    init {
        checkAuthState()
    }

    fun checkAuthState() {
        viewModelScope.launch {
            val currentUser = FirebaseAuth.getInstance().currentUser
            _isAuthenticated.value = currentUser != null
        }
    }

    fun signOut() {
        FirebaseAuth.getInstance().signOut()
        _isAuthenticated.value = false
    }
} 