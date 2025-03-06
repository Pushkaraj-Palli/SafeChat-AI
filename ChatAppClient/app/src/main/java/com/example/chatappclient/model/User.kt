package com.example.chatappclient.model

data class User(
    val uid: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String? = null,
    val lastMessage: LastMessage? = null
)

data class LastMessage(
    val text: String = "",
    val timestamp: Long = 0L,
    val senderId: String = "",
    val isRead: Boolean = false
) 