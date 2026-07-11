package com.bambiloff.kvantor

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bambiloff.kvantor.ui.theme.Rubik
import androidx.compose.material.icons.filled.ArrowBack

//@Composable
//fun AiAssistantScreen() {
//    Text(
//        "AI Assistant",
//        modifier = Modifier.testTag("ai_header")
//    )
//}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAssistantScreen(vm: AiAssistantViewModel = viewModel()) {
    val chat by vm.chat.collectAsState()
    var input by remember { mutableStateOf("") }
    val ctx = LocalContext.current

    Scaffold(
        containerColor = KvBg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "AI Assistant",
                        fontFamily = Rubik,
                        color = KvTextColor,
                        modifier = Modifier.testTag("ai_header")
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { (ctx as? Activity)?.finish() }) {
                        @Suppress("DEPRECATION")  // suppress deprecated ArrowBack warning
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = KvCyan
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = KvBg)
            )
        }
    ) { pad ->
        KvGradientBackground(modifier = Modifier.padding(pad)) {
            Column(
                Modifier.fillMaxSize()
            ) {
                if (chat.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        KvStateCard(
                            icon = Icons.Default.Psychology,
                            title = "Ready to help",
                            body = "Ask about code, a lesson, or an error."
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        reverseLayout = true,
                        contentPadding = PaddingValues(bottom = 8.dp)
                    ) {
                        items(chat.reversed()) { msg ->
                            MessageBubble(msg)
                        }
                    }
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    shape = MaterialTheme.shapes.large,
                    color = KvSurface.copy(alpha = .92f),
                    border = BorderStroke(1.dp, KvAccentSoft.copy(alpha = .22f))
                ) {
                    Row(
                        Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = input,
                            onValueChange = { input = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Ask a question...") },
                            colors = KvOutlinedTextFieldColors(),
                            textStyle = LocalTextStyle.current.copy(color = KvTextColor, fontFamily = Rubik),
                            minLines = 1,
                            maxLines = 3
                        )
                        IconButton(
                            onClick = {
                                if (input.isNotBlank()) {
                                    vm.send(input.trim())
                                    input = ""
                                }
                            }
                        ) {
                            @Suppress("DEPRECATION")  // suppress deprecated Send warning
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Send",
                                tint = if (input.isBlank()) KvMutedText else KvCyan
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(msg: ChatMessage) {
    val isUser = msg.role == ChatMessage.Role.USER
    val bg = if (isUser) KvAccent.copy(alpha = .92f) else KvSurfaceHi.copy(alpha = .92f)
    val border = if (isUser) KvAccentSoft.copy(alpha = .32f) else KvCyan.copy(alpha = .22f)

    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = bg,
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 2.dp,
            border = BorderStroke(1.dp, border)
        ) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = if (isUser) "You" else "AI",
                    color = if (isUser) KvTextColor.copy(alpha = .82f) else KvCyan,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    fontFamily = Rubik
                )
                Text(
                    text = msg.text,
                    color = KvTextColor,
                    fontFamily = Rubik,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
