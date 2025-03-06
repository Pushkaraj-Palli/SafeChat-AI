package com.example.chatappclient.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chatappclient.model.LastMessage
import com.example.chatappclient.model.Message
import com.example.chatappclient.model.User
import com.example.chatappclient.utils.NotificationHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ChatViewModel : ViewModel() {
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val firestore = FirebaseFirestore.getInstance()
    private val currentUser = FirebaseAuth.getInstance().currentUser
    private var context: Context? = null

    fun setContext(context: Context) {
        this.context = context
        NotificationHelper.createNotificationChannel(context)
    }

    fun startListeningToMessages(otherUserId: String) {
        viewModelScope.launch {
            try {
                val chatId = getChatId(otherUserId)
                
                firestore.collection("chats")
                    .document(chatId)
                    .collection("messages")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .addSnapshotListener { snapshot, e ->
                        if (e != null) {
                            Log.e("ChatViewModel", "Listen failed.", e)
                            _error.value = "Failed to load messages: ${e.message}"
                            return@addSnapshotListener
                        }

                        if (snapshot != null) {
                            val messageList = snapshot.documents.mapNotNull { doc ->
                                doc.toObject(Message::class.java)
                            }
                            _messages.value = messageList

                            // Check for new messages and show notifications
                            messageList.firstOrNull()?.let { latestMessage ->
                                if (latestMessage.senderId != currentUser?.uid) {
                                    showNotificationForNewMessage(latestMessage)
                                }
                            }
                        }
                    }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error starting message listener", e)
                _error.value = "Failed to start message updates: ${e.message}"
            }
        }
    }

    fun sendMessage(receiverId: String, text: String) {
        viewModelScope.launch {
            try {
                currentUser?.let { user ->
                    val chatId = getChatId(receiverId)
                    
                    val message = Message(
                        id = firestore.collection("chats").document().id,
                        senderId = user.uid,
                        receiverId = receiverId,
                        text = text,
                        timestamp = System.currentTimeMillis()
                    )

                    // Send the message
                    firestore.collection("chats")
                        .document(chatId)
                        .collection("messages")
                        .document(message.id)
                        .set(message)
                        .await()

                    // Update last message for both users
                    updateLastMessage(message)

                    Log.d("ChatViewModel", "Message sent successfully")
                }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error sending message", e)
                _error.value = "Failed to send message: ${e.message}"
            }
        }
    }

    private suspend fun updateLastMessage(message: Message) {
        try {
            val lastMessage = LastMessage(
                text = message.text,
                timestamp = message.timestamp,
                senderId = message.senderId,
                isRead = false
            )

            // Update sender's last message
            firestore.collection("users")
                .document(message.receiverId)
                .update("lastMessage", lastMessage)
                .await()

            // Update receiver's last message
            firestore.collection("users")
                .document(message.senderId)
                .update("lastMessage", lastMessage)
                .await()
        } catch (e: Exception) {
            Log.e("ChatViewModel", "Error updating last message", e)
        }
    }

    private fun showNotificationForNewMessage(message: Message) {
        context?.let { ctx ->
            viewModelScope.launch {
                try {
                    // Get sender's name from Firestore
                    val senderDoc = firestore.collection("users")
                        .document(message.senderId)
                        .get()
                        .await()
                    
                    val sender = senderDoc.toObject(User::class.java)
                    val senderName = "${sender?.firstName} ${sender?.lastName}"
                    
                    NotificationHelper.showMessageNotification(
                        context = ctx,
                        senderName = senderName,
                        messageText = message.text,
                        notificationId = message.senderId.hashCode()
                    )
                } catch (e: Exception) {
                    Log.e("ChatViewModel", "Error showing notification", e)
                }
            }
        }
    }

    private fun getChatId(otherUserId: String): String {
        val currentUserId = currentUser?.uid ?: throw IllegalStateException("No user logged in")
        // Create a consistent chat ID by sorting user IDs
        return if (currentUserId < otherUserId) {
            "${currentUserId}_${otherUserId}"
        } else {
            "${otherUserId}_${currentUserId}"
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Clean up any listeners if needed
    }
} 