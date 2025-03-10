package com.example.chatappadmin.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chatappadmin.ui.theme.*

@Composable
fun Header(
    title: String,
    onThemeChange: () -> Unit,
    onSignOut: () -> Unit,
    isDarkTheme: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = LightPrimary,
        tonalElevation = 3.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = title,
                color = LightBackground,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.align(Alignment.CenterStart)
            )

            Box(modifier = Modifier.align(Alignment.CenterEnd)) {
                var showMenu by remember { mutableStateOf(false) }
                var showSettingsDialog by remember { mutableStateOf(false) }

                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More options",
                        tint = LightBackground
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Settings") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = "Settings"
                            )
                        },
                        onClick = {
                            showMenu = false
                            showSettingsDialog = true
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Sign Out") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Logout,
                                contentDescription = "Sign Out"
                            )
                        },
                        onClick = {
                            showMenu = false
                            onSignOut()
                        }
                    )
                }

                if (showSettingsDialog) {
                    AlertDialog(
                        onDismissRequest = { showSettingsDialog = false },
                        title = { Text("Settings") },
                        text = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Dark Theme")
                                Switch(
                                    checked = isDarkTheme,
                                    onCheckedChange = { 
                                        onThemeChange()
                                        showSettingsDialog = false
                                    }
                                )
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showSettingsDialog = false }) {
                                Text("Close")
                            }
                        }
                    )
                }
            }
        }
    }
}