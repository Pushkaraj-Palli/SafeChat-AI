package com.example.chatappclient.model

data class User(
    val uid: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String? = null
) 