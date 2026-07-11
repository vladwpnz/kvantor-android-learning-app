package com.bambiloff.kvantor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.net.SocketTimeoutException



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodingTaskView(
    task: Page.CodingTask,
    onSubmitted: (Boolean) -> Unit
) {
    val api       = remember { OllamaApi.create() }
    val coroutine = rememberCoroutineScope()

    val focusManager       = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var userCode  by remember(task) { mutableStateOf("") }
    var submitted by remember(task) { mutableStateOf(false) }
    var isCorrect by remember(task) { mutableStateOf<Boolean?>(null) }
    var aiReview  by remember(task) { mutableStateOf<String?>(null) }
    var isLoading by remember(task) { mutableStateOf(false) }
    var error     by remember(task) { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Default.Code, contentDescription = null, tint = KvCyan)
            Text(
                text = "Task",
                color = KvTextColor,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            text = task.description,
            color = KvMutedText,
            style = MaterialTheme.typography.bodyLarge
        )

        OutlinedTextField(
            value = userCode,
            onValueChange = { userCode = it },
            label = { Text("Your code") },
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp),
            leadingIcon = { Icon(Icons.Default.Code, contentDescription = null, tint = KvCyan) },
            textStyle = LocalTextStyle.current.copy(color = KvTextColor),
            colors = KvOutlinedTextFieldColors()
        )

        KvantorButton(
            text = "Submit",
            onClick = {
                focusManager.clearFocus()
                keyboardController?.hide()

                submitted = true
                isCorrect = userCode.trim() == task.expectedCode.trim()
                onSubmitted(isCorrect == true)

                aiReview = null
                error    = null

                coroutine.launch {
                    isLoading = true
                    try {
                        val response = api.reviewCode(
                            CodeReviewRequest(task.description, userCode)
                        )
                        aiReview = response.response
                    } catch (e: SocketTimeoutException) {
                        error = "Timeout: AI review was not received. Try again later."
                    } catch (e: Exception) {
                        error = "AI review error: ${e.localizedMessage}"
                    } finally {
                        isLoading = false
                    }
                }
            },
            enabled = userCode.isNotBlank() && !isLoading,
            leadingIcon = Icons.AutoMirrored.Filled.Send,
            modifier = Modifier.fillMaxWidth()
        )

        if (submitted) {
            if (isCorrect == true) {
                ResultSurface(
                    icon = Icons.Default.CheckCircle,
                    text = "Correct!",
                    tint = KvSuccess
                )
            } else {
                ResultSurface(
                    icon = Icons.Default.ErrorOutline,
                    text = "Incorrect.\nExpected: ${task.expectedCode}",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }

        if (isLoading) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CircularProgressIndicator(color = KvCyan, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                Text("AI review is preparing...", color = KvMutedText, style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            aiReview?.let {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = KvSurfaceHi.copy(alpha = .62f),
                    border = BorderStroke(1.dp, KvCyan.copy(alpha = .22f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Psychology, contentDescription = null, tint = KvCyan)
                            Text("AI review", color = KvTextColor, style = MaterialTheme.typography.titleSmall)
                        }
                        Text(it, color = KvMutedText, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        error?.let {
            ResultSurface(
                icon = Icons.Default.ErrorOutline,
                text = it,
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun ResultSurface(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    tint: androidx.compose.ui.graphics.Color
) = Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(8.dp),
    color = tint.copy(alpha = .14f),
    border = BorderStroke(1.dp, tint.copy(alpha = .34f))
) {
    Row(
        modifier = Modifier.padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, contentDescription = null, tint = tint)
        Text(text, color = KvTextColor, style = MaterialTheme.typography.bodyMedium)
    }
}
