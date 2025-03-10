package com.example.chatappadmin.model

import com.google.firebase.Timestamp

data class MessageViolation(
    var id: String = "",
    var messageId: String = "",
    var senderId: String = "",
    var receiverId: String = "",
    var message: String = "",
    var timestamp: Timestamp = Timestamp.now(),
    var hasBullyWords: Boolean = false,
    var hasSexualHarassmentWords: Boolean = false,
    var hasBadWords: Boolean = false,
    var status: String = "pending",
    var reviewedBy: String? = null,
    var reviewedAt: Timestamp? = null,
    var action: String? = null
) {
    companion object {
        fun fromMap(map: Map<String, Any?>): MessageViolation {
            return MessageViolation(
                id = map["id"] as? String ?: "",
                messageId = map["messageId"] as? String ?: "",
                senderId = map["senderId"] as? String ?: "",
                receiverId = map["receiverId"] as? String ?: "",
                message = map["message"] as? String ?: "",
                timestamp = map["timestamp"] as? Timestamp ?: Timestamp.now(),
                hasBullyWords = map["hasBullyWords"] as? Boolean ?: false,
                hasSexualHarassmentWords = map["hasSexualHarassmentWords"] as? Boolean ?: false,
                hasBadWords = map["hasBadWords"] as? Boolean ?: false,
                status = map["status"] as? String ?: "pending",
                reviewedBy = map["reviewedBy"] as? String,
                reviewedAt = map["reviewedAt"] as? Timestamp,
                action = map["action"] as? String
            )
        }
    }
} 