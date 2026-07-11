package com.bambiloff.kvantor

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bambiloff.kvantor.ui.theme.KvantorTheme
import com.bambiloff.kvantor.ui.theme.Rubik
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Help






class ProfileActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KvantorTheme {
                ProfileScreen()
            }
        }
    }
}

@SuppressLint("DiscouragedApi")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ProfileScreen() {
    val context = LocalContext.current
    val uid     = FirebaseAuth.getInstance().currentUser?.uid
    val db      = FirebaseFirestore.getInstance()

    // стани UI
    var nickname    by remember { mutableStateOf("Loading...") }
    var avatarResId by remember { mutableIntStateOf(R.drawable.default_avatar) }
    var achievements by remember { mutableStateOf<List<Achievement>>(emptyList()) }

    // Додаємо мапу зі списком відомих аватарів, щоб уникнути getIdentifier
    fun getAvatarId(name: String): Int = when (name) {
        "avatar1" -> R.drawable.avatar1
        "avatar2" -> R.drawable.avatar2
        "avatar3" -> R.drawable.avatar3
        else      -> R.drawable.default_avatar
    }

    // Завантажуємо профіль і список ачівок
    LaunchedEffect(uid) {
        uid?.let { id ->
            try {
                // — Профіль
                val userDoc = db.collection("users").document(id).get().await()
                nickname = userDoc.getString("nickname") ?: "Unnamed"
                avatarResId = getAvatarId(userDoc.getString("avatarName") ?: "")

                // — Підколекція achievements
                val snap = db.collection("users")
                    .document(id)
                    .collection("achievements")
                    .get()
                    .await()

                achievements = snap.documents.map { d ->
                    Achievement(
                        id       = d.id,
                        unlocked = (d.getBoolean("unlocked") == true)
                    )
                }
            } catch (_: Exception) {
                // логувати за потреби
            }
        }
    }

    val unlockedCount = achievements.count { it.unlocked }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Profile",
                        color = KvTextColor,
                        fontSize = 20.sp,
                        fontFamily = Rubik
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { (context as? ComponentActivity)?.finish() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = KvCyan
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = KvBg)
            )
        },
        containerColor = KvBg
    ) { padding ->
        KvGradientBackground(modifier = Modifier.padding(padding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                KvGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(22.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = KvSurfaceHi.copy(alpha = .9f),
                            border = BorderStroke(1.dp, KvCyan.copy(alpha = .35f))
                        ) {
                            Image(
                                painter = painterResource(id = avatarResId),
                                contentDescription = "Avatar",
                                modifier = Modifier
                                    .size(92.dp)
                                    .padding(6.dp)
                                    .clip(CircleShape)
                            )
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = nickname,
                                color = KvTextColor,
                                style = MaterialTheme.typography.headlineSmall,
                                fontFamily = Rubik,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = "Your personal profile",
                                color = KvMutedText,
                                style = MaterialTheme.typography.bodyMedium,
                                fontFamily = Rubik
                            )

                            Text(
                                text = "Achievements: $unlockedCount / ${achievements.size}",
                                color = KvCyan,
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                    }

                    LinearProgressIndicator(
                        progress = {
                            if (achievements.isEmpty()) 0f else unlockedCount.toFloat() / achievements.size
                        },
                        modifier = Modifier.fillMaxWidth(),
                        color = KvCyan,
                        trackColor = KvAccent.copy(alpha = .18f)
                    )
                }

                Text(
                    text = "Achievements",
                    color = KvTextColor,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.fillMaxWidth()
                )

                if (achievements.isEmpty()) {
                    KvStateCard(
                        icon = Icons.Filled.EmojiEvents,
                        title = "Achievements are waiting",
                        body = "Complete lessons and quizzes to unlock rewards."
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.fillMaxHeight(),
                        contentPadding = PaddingValues(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(achievements) { ach ->
                            val icon = when (ach.id) {
                                "WELCOME"    -> Icons.Filled.EmojiEvents
                                "FIRST_STEPS_IN_PY"  -> Icons.Filled.Code
                                "FIRST_STEPS_IN_JS" -> Icons.Filled.Code
                                else         -> Icons.AutoMirrored.Filled.Help
                            }
                            val title = when (ach.id) {
                                "WELCOME"    -> "First step"
                                "FIRST_STEPS_IN_PY"  -> "First Python step"
                                "FIRST_STEPS_IN_JS" -> "First JS step"
                                else         -> ach.id
                            }

                            Surface(
                                shape = MaterialTheme.shapes.medium,
                                color = if (ach.unlocked) KvSurface.copy(alpha = .92f) else KvSurfaceHi.copy(alpha = .66f),
                                border = BorderStroke(
                                    1.dp,
                                    if (ach.unlocked) KvGold.copy(alpha = .38f) else KvAccentSoft.copy(alpha = .28f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(136.dp)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 8.dp, vertical = 10.dp)
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = title,
                                            modifier = Modifier.size(36.dp),
                                            tint = if (ach.unlocked) KvGold else KvMutedText.copy(alpha = .82f)
                                        )
                                        Text(
                                            text = title,
                                            color = if (ach.unlocked) KvTextColor else KvMutedText.copy(alpha = .9f),
                                            textAlign = TextAlign.Center,
                                            style = MaterialTheme.typography.labelMedium.copy(lineHeight = 15.sp),
                                            fontWeight = FontWeight.SemiBold,
                                            minLines = 2,
                                            maxLines = 3,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Text(
                                        text = if (ach.unlocked) "Unlocked" else "Locked",
                                        color = if (ach.unlocked) KvCyan else KvAccentSoft.copy(alpha = .82f),
                                        textAlign = TextAlign.Center,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
