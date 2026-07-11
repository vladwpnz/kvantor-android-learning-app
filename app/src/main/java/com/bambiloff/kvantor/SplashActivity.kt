package com.bambiloff.kvantor

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bambiloff.kvantor.ui.theme.Rubik // ← додаємо імпорт шрифту
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d("SPLASH", "SplashActivity запустилась")

        setContent {
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
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF390D58)),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.kvantor_logo),
            contentDescription = "Kvantor Logo",
            modifier = Modifier.size(220.dp)
        )

        Text(
            text = "Your code. Your quest.",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = Rubik, // ← ось тут наш кастомний шрифт
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 36.dp)
        )

        if (showError) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 72.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Не вдалося перевірити профіль",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontFamily = Rubik
                )
                Spacer(Modifier.height(8.dp))
                Button(onClick = onRetry) {
                    Text("Повторити")
                }
            }
        }
    }
}
