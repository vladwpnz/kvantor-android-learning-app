package com.bambiloff.kvantor

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import com.bambiloff.kvantor.ui.theme.KvantorTheme
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bambiloff.kvantor.ui.theme.Rubik
import androidx.compose.material3.LocalTextStyle



class ProfileSetupActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            KvantorTheme {
                ProfileSetupScreen { nickname, avatarName ->
                    val uid = FirebaseAuth.getInstance().currentUser?.uid
                    if (uid == null) {
                        Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show()
                        return@ProfileSetupScreen
                    }

                    lifecycleScope.launch {
                        try {
                            // Створюємо профіль + ініціалізуємо ачівки
                            UserBootstrapper.createUserSkeleton(uid, nickname, avatarName)

                            // Сповіщення про першу ачівку
                            Toast.makeText(
                                this@ProfileSetupActivity,
                                "Achievement unlocked: First step",
                                Toast.LENGTH_SHORT
                            ).show()

                            // Переходимо до вибору курсу
                            startActivity(
                                Intent(
                                    this@ProfileSetupActivity,
                                    CourseSelectionActivity::class.java
                                ).apply {
                                    putExtra("nickname", nickname)
                                    putExtra("avatarName", avatarName)
                                }
                            )
                            finish()
                        } catch (e: Exception) {
                            Toast.makeText(
                                this@ProfileSetupActivity,
                                "Profile save error: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileSetupScreen(
    onContinue: (nickname: String, avatar: String) -> Unit
) {
    val context = LocalContext.current

    var nickname by remember { mutableStateOf("") }
    var selectedAvatar by remember { mutableStateOf("avatar1") }
    var showAvatarOptions by remember { mutableStateOf(false) }

    fun drawableId(name: String): Int =
        context.resources.getIdentifier(name, "drawable", context.packageName)

    KvGradientBackground(contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            KvGlassCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(24.dp)
            ) {
                Text(
                    text = "Hi! What should we call you?",
                    style = MaterialTheme.typography.headlineSmall,
                    color = KvTextColor,
                    fontWeight = FontWeight.Bold,
                    fontFamily = Rubik,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = nickname,
                    onValueChange = { nickname = it },
                    label = { Text("Nickname", fontFamily = Rubik) },
                    textStyle = LocalTextStyle.current.copy(color = KvTextColor, fontFamily = Rubik),
                    leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null, tint = KvCyan) },
                    colors = KvOutlinedTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Choose an avatar",
                    color = KvMutedText,
                    style = MaterialTheme.typography.titleSmall,
                    fontFamily = Rubik,
                    modifier = Modifier.clickable { showAvatarOptions = !showAvatarOptions }
                )

                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = KvSurfaceHi.copy(alpha = .8f),
                    border = BorderStroke(1.dp, KvCyan.copy(alpha = .35f)),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Image(
                        painter = painterResource(id = drawableId(selectedAvatar)),
                        contentDescription = "Avatar",
                        modifier = Modifier
                            .size(104.dp)
                            .padding(10.dp)
                            .clip(CircleShape)
                            .clickable { showAvatarOptions = !showAvatarOptions }
                    )
                }

                if (showAvatarOptions) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf("avatar1", "avatar2", "avatar3").forEach { name ->
                            val selected = selectedAvatar == name
                            Surface(
                                shape = CircleShape,
                                color = if (selected) KvCyan.copy(alpha = .18f) else KvSurface,
                                border = BorderStroke(
                                    1.dp,
                                    if (selected) KvCyan else KvAccentSoft.copy(alpha = .35f)
                                )
                            ) {
                                Box(contentAlignment = Alignment.BottomEnd) {
                                    Image(
                                        painter = painterResource(id = drawableId(name)),
                                        contentDescription = name,
                                        modifier = Modifier
                                            .size(62.dp)
                                            .padding(4.dp)
                                            .clip(CircleShape)
                                            .clickable {
                                                selectedAvatar = name
                                                showAvatarOptions = false
                                            }
                                    )
                                    if (selected) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = KvCyan,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                KvantorButton(
                    text = "Continue",
                    onClick = {
                        if (nickname.isBlank()) {
                            Toast.makeText(context, "Enter a nickname", Toast.LENGTH_SHORT).show()
                        } else {
                            onContinue(nickname, selectedAvatar)
                        }
                    },
                    leadingIcon = Icons.Default.CheckCircle,
                    containerColor = KvCyan,
                    contentColor = KvInk,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
