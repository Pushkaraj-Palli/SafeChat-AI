package com.example.chatappclient.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.chatappclient.model.Message
import com.example.chatappclient.model.User
import com.example.chatappclient.ui.theme.*
import com.example.chatappclient.viewmodel.ChatViewModel
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    user: User,
    onBackClick: () -> Unit,
    chatViewModel: ChatViewModel = viewModel()
) {
    val colorScheme = MaterialTheme.colorScheme
    var messageText by remember { mutableStateOf("") }
    val messages by chatViewModel.messages.collectAsState()
    val error by chatViewModel.error.collectAsState()
    val currentUser = FirebaseAuth.getInstance().currentUser
    val listState = rememberLazyListState()

    // Start listening to messages
    LaunchedEffect(user.uid) {
        chatViewModel.startListeningToMessages(user.uid)
        chatViewModel.markMessagesAsRead(user.uid)
    }

    // Scroll to bottom when new message is added
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.scrollToItem(0)
        }
    }

    Scaffold(
        containerColor = colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AsyncImage(
                            model = user.photoUrl ?: "https://i.pravatar.cc/150?u=${user.uid}",
                            contentDescription = "Profile picture of ${user.displayName}",
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Text(
                            text = "${user.firstName} ${user.lastName}",
                            style = MaterialTheme.typography.titleMedium,
                            color = colorScheme.onPrimary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colorScheme.primary,
                    titleContentColor = colorScheme.onPrimary,
                    navigationIconContentColor = colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = colorScheme.surface,
                tonalElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        placeholder = { 
                            Text(
                                "Type a message",
                                color = colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                        maxLines = 4,
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            containerColor = colorScheme.surface,
                            unfocusedBorderColor = colorScheme.onSurface.copy(alpha = 0.12f),
                            focusedBorderColor = colorScheme.primary
                        )
                    )
                    FloatingActionButton(
                        onClick = {
                            if (messageText.isNotBlank()) {
                                chatViewModel.sendMessage(user.uid, messageText)
                                messageText = ""
                            }
                        },
                        containerColor = colorScheme.primary,
                        contentColor = colorScheme.onPrimary
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send message")
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(colorScheme.background)
        ) {
            if (error != null) {
                Text(
                    text = error ?: "",
                    color = colorScheme.error,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                )
            }
            
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                reverseLayout = true,
                state = listState,
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(messages) { message ->
                    MessageBubble(
                        message = message,
                        isFromCurrentUser = message.senderId == currentUser?.uid
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
fun MessageBubble(
    message: Message,
    isFromCurrentUser: Boolean
) {
    val colorScheme = MaterialTheme.colorScheme
    val dateFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val isDark = isSystemInDarkTheme()
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = if (isFromCurrentUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            color = if (isFromCurrentUser) 
                if (isDark) DarkSentBubble else LightSentBubble
            else 
                if (isDark) DarkReceivedBubble else LightReceivedBubble,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isFromCurrentUser) 16.dp else 4.dp,
                bottomEnd = if (isFromCurrentUser) 4.dp else 16.dp
            ),
            modifier = Modifier.widthIn(max = 340.dp)
        ) {
            Column(
                modifier = Modifier.padding(8.dp)
            ) {
                Text(
                    text = message.text,
                    color = if (isDark) DarkTextPrimary else LightTextPrimary,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = dateFormat.format(Date(message.timestamp)),
                    color = if (isDark) DarkTextSecondary else LightTextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.align(Alignment.End),
                    textAlign = TextAlign.End
                )
            }
        }
    }
} 