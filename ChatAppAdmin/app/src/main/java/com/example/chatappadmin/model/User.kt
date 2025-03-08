package com.example.chatappadmin.model

import com.google.firebase.Timestamp

data class User(
    val uid: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val displayName: String = "",
    val createdAt: Timestamp = Timestamp.now()
) 