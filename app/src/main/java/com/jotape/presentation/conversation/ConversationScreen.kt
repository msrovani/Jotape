package com.jotape.presentation.conversation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jotape.domain.model.Interaction
import com.jotape.presentation.auth.AuthViewModel
import kotlinx.coroutines.launch
import androidx.compose.ui.tooling.preview.Preview
import com.jotape.ui.theme.JotapeTheme
import java.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationScreen(
    conversationViewModel: ConversationViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by conversationViewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Gerenciar o estado do TextField localmente
    var inputText by remember { mutableStateOf("") }

    // Scroll to bottom when new messages arrive
    LaunchedEffect(uiState.interactions.size) {
        if (uiState.interactions.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(uiState.interactions.lastIndex)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Jotape Assistant") },
                actions = {
                    // Remover o botão de limpar histórico
                    // IconButton(onClick = { viewModel.clearConversationHistory() }) {
                    //     Icon(Icons.Default.ClearAll, contentDescription = "Clear History")
                    // }
                }
            )
        },
        bottomBar = {
            InputBar(
                text = inputText,
                onTextChanged = { inputText = it },
                onSendClick = {
                    // Chamar sendMessage com o texto local e limpar o campo
                    if (inputText.isNotBlank() && !uiState.isSending) {
                        conversationViewModel.sendMessage(messageText = inputText)
                        inputText = "" // Limpar campo após envio
                    }
                },
                isLoading = uiState.isSending
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 8.dp)
        ) {
            if (uiState.isLoading && uiState.interactions.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(uiState.interactions, key = { it.id }) { interaction ->
                        MessageBubble(interaction = interaction)
                    }
                }
            }

            if (uiState.error != null) {
                Text(
                    text = "Erro: ${uiState.error}",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun MessageBubble(interaction: Interaction) {
    val alignment = if (interaction.isFromUser) Alignment.CenterEnd else Alignment.CenterStart
    val backgroundColor = if (interaction.isFromUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = if (interaction.isFromUser) 40.dp else 0.dp,
                end = if (interaction.isFromUser) 0.dp else 40.dp
            )
    ) {
        Card(
            modifier = Modifier.align(alignment),
            colors = CardDefaults.cardColors(containerColor = backgroundColor)
        ) {
            Text(
                text = interaction.text,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
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
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    Icon(Icons.Filled.Send, contentDescription = "Send Message")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun ConversationScreenPreview() {
    JotapeTheme {
        // Mock data for preview
        val previewState = ConversationViewModel.ConversationUiState(
            interactions = listOf(
                Interaction("1", true, "Olá!", Instant.now()),
                Interaction("2", false, "Oi! Como posso ajudar?", Instant.now().plusSeconds(1)),
                Interaction("3", true, "Gostaria de saber mais sobre IA.", Instant.now().plusSeconds(2))
            ),
            isLoading = false,
            isSending = false,
            error = null
        )
        // Need a way to preview the screen without a real ViewModel
        // For now, just show the basic layout structure
        Scaffold(
            topBar = { TopAppBar(title = { Text("Jotape Assistant Preview") }) }
        ) { paddingValues ->
            Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
                Text("Preview Area - List would go here", modifier = Modifier.weight(1f))
                Text("Error Area (if any)")
                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(value = "", onValueChange = {}, modifier = Modifier.weight(1f))
                    IconButton(onClick = { /*TODO*/ }) {
                        Icon(Icons.Filled.Send, contentDescription = null)
                    }
                }
            }
        }
    }
} 