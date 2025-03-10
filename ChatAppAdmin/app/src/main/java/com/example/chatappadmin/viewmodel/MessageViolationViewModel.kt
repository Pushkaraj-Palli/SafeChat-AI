package com.example.chatappadmin.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

import com.example.chatappadmin.model.MessageViolation

class MessageViolationViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val violationsCollection = db.collection("violations")

    private val _violations = MutableStateFlow<List<MessageViolation>>(emptyList())
    val violations: StateFlow<List<MessageViolation>> = _violations

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        loadViolations()
    }

    private fun loadViolations() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                violationsCollection
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .addSnapshotListener { snapshot, e ->
                        if (e != null) {
                            _error.value = e.message
                            return@addSnapshotListener
                        }

                        val violationsList = snapshot?.documents?.mapNotNull { doc ->
                            doc.toObject(MessageViolation::class.java)?.copy(id = doc.id)
                        } ?: emptyList()

                        _violations.value = violationsList
                        _isLoading.value = false
                    }
            } catch (e: Exception) {
                _error.value = e.message
                _isLoading.value = false
            }
        }
    }

    fun updateViolationStatus(violationId: String, status: String, action: String?) {
        viewModelScope.launch {
            try {
                val updates = hashMapOf<String, Any?>(
                    "status" to status,
                    "action" to action,
                    "reviewedAt" to com.google.firebase.Timestamp.now(),
                    "reviewedBy" to FirebaseFirestore.getInstance().collection("users").document().id
                )

                violationsCollection.document(violationId)
                    .update(updates)
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun refreshViolations() {
        loadViolations()
    }
}