package com.example.chatappclient.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chatappclient.model.LastMessage
import com.example.chatappclient.model.Message
import com.example.chatappclient.model.MessageViolation
import com.example.chatappclient.model.User
import com.example.chatappclient.service.WarningService
import com.example.chatappclient.utils.MessageFilter
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

    private val warningService = WarningService()
    private val _warningMessage = MutableStateFlow<String?>(null)
    val warningMessage: StateFlow<String?> = _warningMessage

    private val firestore = FirebaseFirestore.getInstance()
    private val currentUser = FirebaseAuth.getInstance().currentUser
    private var context: Context? = null

    init {
        viewModelScope.launch {
            try {
                MessageFilter.initialize()
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error initializing MessageFilter", e)
                _error.value = "Failed to initialize message filter: ${e.message}"
            }
        }
    }

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
                                    // Mark message as read since we're in the chat
                                    markMessagesAsRead(otherUserId)
                                    // Still show notification in case app is in background
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
                    // Check if user is blocked
                    if (warningService.isUserBlocked(user.uid)) {
                        _warningMessage.value = "You are temporarily blocked from sending messages due to violations"
                        return@launch
                    }

                    val chatId = getChatId(receiverId)
                    
                    // Check message for violations
                    val violations = MessageFilter.checkMessage(text)
                    
                    val message = Message(
                        id = firestore.collection("chats").document().id,
                        senderId = user.uid,
                        receiverId = receiverId,
                        text = text,
                        timestamp = System.currentTimeMillis()
                    )

                    // Handle violations if found
                    if (violations.hasViolation()) {
                        val messageViolation = MessageViolation(
                            messageId = message.id,
                            senderId = user.uid,
                            receiverId = receiverId,
                            message = text,
                            hasBullyWords = violations.hasBullyWords,
                            hasSexualHarassmentWords = violations.hasSexualHarassmentWords,
                            hasBadWords = violations.hasBadWords
                        )

                        // Check if message should be blocked
                        val allowMessage = warningService.handleViolation(messageViolation)
                        
                        if (!allowMessage) {
                            _warningMessage.value = "Your message was blocked due to inappropriate content"
                            return@launch
                        }

                        // Store violation in Firebase
                        firestore.collection("violations")
                            .document(message.id)
                            .set(messageViolation.toMap())
                            .await()

                        // Show warning to user
                        val warningCount = warningService.getViolationCount(user.uid)
                        _warningMessage.value = "Warning: Your message contains inappropriate content. Warning ${warningCount}/${WarningService.MAX_WARNINGS}"
                        
                        Log.w("ChatViewModel", "Message contains violations: $violations")
                    }

                    // Send the message if not blocked
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

    fun markMessagesAsRead(otherUserId: String) {
        viewModelScope.launch {
            try {
                val currentUserId = currentUser?.uid ?: return@launch
                
                // Create a LastMessage object with isRead set to true
                val lastMessageUpdate = mapOf(
                    "lastMessage.isRead" to true
                )

                // Get both users' data
                val currentUserDoc = firestore.collection("users")
                    .document(currentUserId)
                    .get()
                    .await()
                
                val otherUserDoc = firestore.collection("users")
                    .document(otherUserId)
                    .get()
                    .await()

                val currentUser = currentUserDoc.toObject(User::class.java)
                val otherUser = otherUserDoc.toObject(User::class.java)

                // Update current user's last message if it's from the other user
                if (currentUser?.lastMessage?.senderId == otherUserId) {
                    firestore.collection("users")
                        .document(currentUserId)
                        .update(lastMessageUpdate)
                        .await()
                }

                // Update other user's last message if it's from the current user
                if (otherUser?.lastMessage?.senderId == currentUserId) {
                    firestore.collection("users")
                        .document(otherUserId)
                        .update(lastMessageUpdate)
                        .await()
                }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error marking messages as read", e)
            }
        }
    }

    fun clearWarningMessage() {
        _warningMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        // Clean up listeners
        MessageFilter.cleanup()
    }
} 