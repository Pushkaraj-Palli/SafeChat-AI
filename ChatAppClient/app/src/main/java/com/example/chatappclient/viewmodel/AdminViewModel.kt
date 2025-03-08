package com.example.chatappclient.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chatappclient.model.MessageViolation
import com.example.chatappclient.model.UserWarning
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AdminViewModel : ViewModel() {
    private val _violations = MutableStateFlow<List<MessageViolation>>(emptyList())
    val violations: StateFlow<List<MessageViolation>> = _violations

    private val _warnings = MutableStateFlow<List<UserWarning>>(emptyList())
    val warnings: StateFlow<List<UserWarning>> = _warnings

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val firestore = FirebaseFirestore.getInstance()

    init {
        loadViolations()
        loadWarnings()
    }

    private fun loadViolations() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                firestore.collection("violations")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .addSnapshotListener { snapshot, e ->
                        if (e != null) {
                            Log.e("AdminViewModel", "Listen failed.", e)
                            _error.value = "Failed to load violations: ${e.message}"
                            return@addSnapshotListener
                        }

                        if (snapshot != null) {
                            val violationList = snapshot.documents.mapNotNull { doc ->
                                doc.toObject(MessageViolation::class.java)
                            }
                            _violations.value = violationList
                        }
                        _isLoading.value = false
                    }
            } catch (e: Exception) {
                Log.e("AdminViewModel", "Error loading violations", e)
                _error.value = "Failed to load violations: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    private fun loadWarnings() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                firestore.collection("warnings")
                    .orderBy("lastWarningDate", Query.Direction.DESCENDING)
                    .addSnapshotListener { snapshot, e ->
                        if (e != null) {
                            Log.e("AdminViewModel", "Listen failed.", e)
                            _error.value = "Failed to load warnings: ${e.message}"
                            return@addSnapshotListener
                        }

                        if (snapshot != null) {
                            val warningList = snapshot.documents.mapNotNull { doc ->
                                doc.toObject(UserWarning::class.java)
                            }
                            _warnings.value = warningList
                        }
                        _isLoading.value = false
                    }
            } catch (e: Exception) {
                Log.e("AdminViewModel", "Error loading warnings", e)
                _error.value = "Failed to load warnings: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
} 