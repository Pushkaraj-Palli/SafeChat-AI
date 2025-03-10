package com.example.chatappclient.service

import com.example.chatappclient.model.MessageViolation
import com.example.chatappclient.model.UserWarning
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit
import java.util.Date

class WarningService {
    private val firestore = FirebaseFirestore.getInstance()
    
    companion object {
        const val MAX_WARNINGS = 1000
        private const val BLOCK_DURATION_HOURS = 24L
    }

    suspend fun handleViolation(violation: MessageViolation): Boolean {
        // Get or create user warning record
        val userWarning = getUserWarning(violation.senderId)
        
        // Check if user is currently blocked
        if (isUserBlocked(userWarning)) {
            return false // Message should be blocked
        }

        // Update warning count and check if user should be blocked
        val updatedWarning = updateWarningCount(userWarning, violation.messageId)
        
        // Return whether the message should be blocked
        return !updatedWarning.isBlocked
    }

    private suspend fun getUserWarning(userId: String): UserWarning {
        val doc = firestore.collection("warnings")
            .document(userId)
            .get()
            .await()

        return doc.toObject(UserWarning::class.java) ?: UserWarning(userId = userId)
    }

    private fun isUserBlocked(warning: UserWarning): Boolean {
        if (!warning.isBlocked) return false
        
        // Check if block has expired
        warning.blockExpiryDate?.let { expiryDate ->
            return Timestamp.now().seconds < expiryDate.seconds
        }
        
        return false
    }

    private suspend fun updateWarningCount(warning: UserWarning, violationId: String): UserWarning {
        val newWarningCount = warning.warningCount + 1
        val shouldBlock = newWarningCount >= MAX_WARNINGS
        
        val blockExpiryDate = if (shouldBlock) {
            val currentTime = Timestamp.now()
            val expiryTimeSeconds = currentTime.seconds + TimeUnit.HOURS.toSeconds(BLOCK_DURATION_HOURS)
            Timestamp(expiryTimeSeconds, 0)
        } else {
            null
        }

        val updatedWarning = warning.copy(
            warningCount = newWarningCount,
            isBlocked = shouldBlock,
            lastWarningDate = Timestamp.now(),
            blockExpiryDate = blockExpiryDate,
            violationHistory = warning.violationHistory + violationId
        )

        // Update in Firestore
        firestore.collection("warnings")
            .document(warning.userId)
            .set(updatedWarning.toMap())
            .await()

        return updatedWarning
    }

    suspend fun getViolationCount(userId: String): Int {
        val warning = getUserWarning(userId)
        return warning.warningCount
    }

    suspend fun isUserBlocked(userId: String): Boolean {
        val warning = getUserWarning(userId)
        return isUserBlocked(warning)
    }
} 