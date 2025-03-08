package com.example.chatappclient.model

import com.google.firebase.Timestamp

data class MessageViolation(
    val messageId: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val message: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val hasBullyWords: Boolean = false,
    val hasSexualHarassmentWords: Boolean = false,
    val hasBadWords: Boolean = false
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "messageId" to messageId,
            "senderId" to senderId,
            "receiverId" to receiverId,
            "message" to message,
            "timestamp" to timestamp,
            "hasBullyWords" to hasBullyWords,
            "hasSexualHarassmentWords" to hasSexualHarassmentWords,
            "hasBadWords" to hasBadWords
        )
    }
} 