package com.example.chatappadmin.ui.components.cards

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.chatappadmin.model.MessageViolation
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViolationCard(violation: MessageViolation) {
    var showMenu by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault()) }
    var showDialog by remember { mutableStateOf(false) }
    var selectedAction by remember { mutableStateOf<String?>(null) }
    var senderName by remember { mutableStateOf("Loading...") }
    var receiverName by remember { mutableStateOf("Loading...") }

    // Create violations list at a higher scope
    val violations = remember(violation) {
        buildList {
            if (violation.hasBullyWords) add("Bullying")
            if (violation.hasSexualHarassmentWords) add("Sexual Harassment")
            if (violation.hasBadWords) add("Bad Words")
        }
    }

    // Fetch user details
    LaunchedEffect(violation) {
        try {
            val firestore = FirebaseFirestore.getInstance()
            
            // Fetch sender details
            val senderDoc = firestore.collection("users")
                .document(violation.senderId)
                .get()
                .await()
            
            val senderFirstName = senderDoc.getString("firstName") ?: ""
            val senderLastName = senderDoc.getString("lastName") ?: ""
            senderName = if (senderFirstName.isNotEmpty() || senderLastName.isNotEmpty()) {
                "$senderFirstName $senderLastName"
            } else {
                "Unknown User"
            }

            // Fetch receiver details
            val receiverDoc = firestore.collection("users")
                .document(violation.receiverId)
                .get()
                .await()
            
            val receiverFirstName = receiverDoc.getString("firstName") ?: ""
            val receiverLastName = receiverDoc.getString("lastName") ?: ""
            receiverName = if (receiverFirstName.isNotEmpty() || receiverLastName.isNotEmpty()) {
                "$receiverFirstName $receiverLastName"
            } else {
                "Unknown User"
            }
        } catch (e: Exception) {
            android.util.Log.e("ViolationCard", "Error fetching user details", e)
            senderName = "Unknown User"
            receiverName = "Unknown User"
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showDialog = true },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "From: $senderName",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = dateFormat.format(violation.timestamp.toDate()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = violation.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Violation Categories with consistent colors
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(violations) { violationType ->
                    AssistChip(
                        onClick = { },
                        label = { Text(text = violationType) },
                        leadingIcon = { 
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = violationType
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            labelColor = MaterialTheme.colorScheme.onErrorContainer,
                            leadingIconContentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "To: $receiverName",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (showDialog) {
        Dialog(onDismissRequest = { showDialog = false }) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 4.dp
                )
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        text = "Message Details",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Message: ${violation.message}",
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "From: $senderName",
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "To: $receiverName",
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Violations:",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(violations) { violationType ->
                            AssistChip(
                                onClick = { },
                                label = { Text(text = violationType) },
                                leadingIcon = { 
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = violationType
                                    )
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    labelColor = MaterialTheme.colorScheme.onErrorContainer,
                                    leadingIconContentColor = MaterialTheme.colorScheme.onErrorContainer
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showDialog = false }) {
                            Text(text = "Close")
                        }
                    }
                }
            }
        }
    }
} 