package com.example.chatappclient.model

import com.google.firebase.Timestamp

data class UserWarning(
    val userId: String = "",
    val warningCount: Int = 0,
    val isBlocked: Boolean = false,
    val lastWarningDate: Timestamp = Timestamp.now(),
    val violationHistory: List<String> = emptyList(), // List of violation IDs
    val blockExpiryDate: Timestamp? = null
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "userId" to userId,
            "warningCount" to warningCount,
            "isBlocked" to isBlocked,
            "lastWarningDate" to lastWarningDate,
            "violationHistory" to violationHistory,
            "blockExpiryDate" to blockExpiryDate
        )
    }
} 