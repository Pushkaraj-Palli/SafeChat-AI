package com.example.chatappclient

import android.app.Application
import com.google.firebase.firestore.FirebaseFirestore

class ChatApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Enable Firestore offline persistence
        FirebaseFirestore.getInstance().firestoreSettings = 
            com.google.firebase.firestore.FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build()
    }
} 