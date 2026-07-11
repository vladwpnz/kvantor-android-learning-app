package com.bambiloff.kvantor

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bambiloff.kvantor.ui.theme.KvantorTheme
import com.bambiloff.kvantor.ui.theme.Rubik
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d("SPLASH", "SplashActivity запустилась")

        setContent {
            KvantorTheme {
                var startupError by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    routeFromSplash(onProfileCheckFailed = { startupError = true })
                }

                SplashScreenContent(
                    showError = startupError,
                    onRetry = {
                        startupError = false
                        routeFromSplash(onProfileCheckFailed = { startupError = true })
                    }
                )
            }
        }
    }

    private fun routeFromSplash(onProfileCheckFailed: () -> Unit) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            navigateTo(AuthActivity::class.java)
            return
        }

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(user.uid)
            .get()
            .addOnSuccessListener { doc ->
                val hasProfile = doc.exists() &&
                    !doc.getString("nickname").isNullOrBlank() &&
                    !doc.getString("avatarName").isNullOrBlank()

                navigateTo(
                    if (hasProfile) CourseSelectionActivity::class.java
                    else ProfileSetupActivity::class.java
                )
            }
            .addOnFailureListener {
                onProfileCheckFailed()
            }
    }

    private fun navigateTo(target: Class<*>) {
        startActivity(Intent(this, target))
        finish()
    }
}

@Composable
fun SplashScreenContent(
    showError: Boolean = false,
    onRetry: () -> Unit = {}
) {
    KvGradientBackground(contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = KvSurface.copy(alpha = .88f),
                tonalElevation = 3.dp
            ) {
                Image(
                    painter = painterResource(id = R.drawable.kvantor_logo),
                    contentDescription = "Kvantor Logo",
                    modifier = Modifier
                        .size(168.dp)
                        .padding(18.dp)
                )
            }

            Text(
                text = "KVANTOR",
                color = KvCyan,
                style = MaterialTheme.typography.displayMedium,
                fontFamily = Rubik
            )
            Text(
                text = "Your code. Your quest.",
                color = KvMutedText,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                fontFamily = Rubik
            )
            CircularProgressIndicator(
                color = KvCyan,
                trackColor = KvAccent.copy(alpha = .22f),
                strokeWidth = 3.dp,
                modifier = Modifier.size(34.dp)
            )
        }

        if (showError) {
            KvGlassCard(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(24.dp)
            ) {
                Text(
                    text = "Could not check your profile",
                    color = KvTextColor,
                    style = MaterialTheme.typography.titleSmall,
                    fontFamily = Rubik
                )
                KvantorButton(
                    text = "Retry",
                    onClick = onRetry,
                    leadingIcon = Icons.Default.Refresh,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
