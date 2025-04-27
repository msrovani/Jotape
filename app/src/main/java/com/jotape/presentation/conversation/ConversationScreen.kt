package com.jotape.presentation.conversation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jotape.domain.model.Interaction
import com.jotape.presentation.auth.AuthViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationScreen(
    conversationViewModel: ConversationViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by conversationViewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Scroll to bottom when a new message arrives
    LaunchedEffect(uiState.interactions.size) {
        if (uiState.interactions.isNotEmpty()) {
            coroutineScope.launch {
                // Scroll to the first item (which is the newest due to reversed list/query)
                listState.animateScrollToItem(index = 0)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Jotape Chat") },
                actions = {
                    IconButton(onClick = { conversationViewModel.clearChatHistory() }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Limpar Histórico")
                    }
                    IconButton(onClick = { authViewModel.signOut() }) {
                        Icon(Icons.Filled.Logout, contentDescription = "Sair")
                    }
                }
            )
        },
        bottomBar = {
            InputBar(
                text = uiState.inputText,
                onTextChanged = { conversationViewModel.onInputTextChanged(it) },
                onSendClick = { conversationViewModel.sendMessage() },
                isLoading = uiState.isLoading // Pass loading state if needed for UI feedback
            )
        }
    ) { paddingValues ->
        MessageList(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            interactions = uiState.interactions,
            listState = listState
        )
    }
}

@Composable
fun MessageList(
    modifier: Modifier = Modifier,
    interactions: List<Interaction>,
    listState: androidx.compose.foundation.lazy.LazyListState
) {
    LazyColumn(
        modifier = modifier.padding(horizontal = 8.dp),
        state = listState,
        reverseLayout = true // Show newest messages at the bottom
    ) {
        items(interactions, key = { it.id }) { interaction ->
            MessageItem(interaction = interaction)
        }
    }
}

@Composable
fun MessageItem(interaction: Interaction) {
    val alignment = if (interaction.isFromUser) Alignment.CenterEnd else Alignment.CenterStart
    val backgroundColor = if (interaction.isFromUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
    val textColor = if (interaction.isFromUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer

    // Row to hold the bubble and the sync indicator
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = if (interaction.isFromUser) Arrangement.End else Arrangement.Start
    ) {
        // Sync Indicator for User messages (before bubble)
        if (interaction.isFromUser) {
            Text(
                text = if (interaction.isSynced) "S" else "R", // Show S or R
                fontSize = 10.sp,
                color = if (interaction.isSynced) Color.Gray else MaterialTheme.colorScheme.error, // Gray for Synced, Error color for Unsynced (R)
                modifier = Modifier.padding(end = 4.dp).align(Alignment.Bottom)
            )
        }

        // Message Bubble (Card)
        Card(
            modifier = Modifier.widthIn(max = 300.dp),
            colors = CardDefaults.cardColors(containerColor = backgroundColor)
        ) {
            Text(
                text = interaction.text,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                color = textColor
            )
        }

        // Sync Indicator for Assistant messages (after bubble)
        if (!interaction.isFromUser) {
            Text(
                text = if (interaction.isSynced) "S" else "R", // Show S or R
                fontSize = 10.sp,
                color = if (interaction.isSynced) Color.Gray else MaterialTheme.colorScheme.error, // Gray for Synced, Error color for Unsynced (R)
                modifier = Modifier.padding(start = 4.dp).align(Alignment.Bottom)
            )
        }
    }
}

@Composable
fun InputBar(
    text: String,
    onTextChanged: (String) -> Unit,
    onSendClick: () -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shadowElevation = 8.dp // Add some shadow to separate from list
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChanged,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Digite sua mensagem...") },
                maxLines = 4 // Allow some vertical expansion
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = onSendClick, enabled = text.isNotBlank() && !isLoading) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send Message",
                    tint = if (text.isNotBlank() && !isLoading) MaterialTheme.colorScheme.primary else Color.Gray
                )
            }
            // Optional: Show progress indicator when loading
            // if (isLoading) {
            //     Spacer(modifier = Modifier.width(8.dp))
            //     CircularProgressIndicator(modifier = Modifier.size(24.dp))
            // }
        }
    }
} 