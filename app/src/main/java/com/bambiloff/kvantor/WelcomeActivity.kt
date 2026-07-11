package com.bambiloff.kvantor

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bambiloff.kvantor.ui.theme.KvantorTheme
import com.bambiloff.kvantor.ui.theme.Rubik

class WelcomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val nickname = intent.getStringExtra("nickname") ?: "Guest"
        val avatarResId = intent.getIntExtra("avatarResId", R.drawable.default_avatar)

        setContent {
            KvantorTheme {
                WelcomeScreen(nickname, avatarResId) {
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            }
        }
    }
}

@Composable
fun WelcomeScreen(nickname: String, avatarResId: Int, onStartClick: () -> Unit) {
    KvGradientBackground(contentAlignment = Alignment.Center) {
        KvGlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            contentPadding = PaddingValues(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = KvSurfaceHi.copy(alpha = .9f)
                ) {
                    Image(
                        painter = painterResource(id = avatarResId),
                        contentDescription = "Avatar",
                        modifier = Modifier
                            .size(128.dp)
                            .padding(10.dp)
                    )
                }

                Text(
                    text = "Hi, $nickname!",
                    color = KvCyan,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    fontFamily = Rubik,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Welcome to Kvantor!\nLearn programming through quests, practice, and rewards.",
                    color = KvMutedText,
                    style = MaterialTheme.typography.bodyLarge,
                    fontFamily = Rubik,
                    textAlign = TextAlign.Center
                )

                KvantorButton(
                    text = "Start",
                    onClick = onStartClick,
                    leadingIcon = Icons.Default.PlayArrow,
                    containerColor = KvCyan,
                    contentColor = KvInk,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
