package com.bambiloff.kvantor

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bambiloff.kvantor.ui.theme.KvantorTheme
import com.bambiloff.kvantor.ui.theme.Rubik
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import androidx.compose.ui.platform.testTag


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var isDarkTheme by remember { mutableStateOf(true) }

            KvantorTheme(darkTheme = isDarkTheme) {
                PythonCourseScreen(
                    isDarkTheme   = isDarkTheme,
                    onToggleTheme = { isDarkTheme = it },
                    onStartFromBeginning = { openPythonLesson(resetProgress = true) },
                    onContinueCourse = { openPythonLesson(resetProgress = false) }
                )
            }
        }
    }

    private fun openPythonLesson(resetProgress: Boolean) {
        val intent = Intent(this, LessonActivity::class.java).putExtra("courseType", "python")
        val uid = FirebaseAuth.getInstance().currentUser?.uid

        if (!resetProgress || uid == null) {
            startActivity(intent)
            return
        }

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .set(
                mapOf(
                    "progress" to mapOf(
                        "python" to mapOf(
                            "moduleIndex" to 0,
                            "pageIndex" to 0,
                            "completedModuleIds" to emptyList<String>(),
                            "rewardedQuizPageIds" to emptyList<String>(),
                            "courseCompleted" to false
                        )
                    )
                ),
                SetOptions.merge()
            )
            .addOnCompleteListener {
                startActivity(intent)
            }
    }
}

@Composable
fun PythonScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("python_header")   // ← ЯКІР ДЛЯ ТЕСТУ
    ) {
        Text("Python: first lesson")
    }
}

@Suppress("DiscouragedApi")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PythonCourseScreen(
    isDarkTheme: Boolean,
    onToggleTheme: (Boolean) -> Unit,
    onStartFromBeginning: () -> Unit = {},
    onContinueCourse: () -> Unit = {}
) {
    val context = LocalContext.current
    val uid     = FirebaseAuth.getInstance().currentUser?.uid
    val db      = FirebaseFirestore.getInstance()
    var avatarResId by remember { mutableStateOf(R.drawable.default_avatar) }
    LaunchedEffect(uid) {
        uid?.let { user ->
            db.collection("users").document(user).get()
                .addOnSuccessListener { doc ->
                    val name = doc.getString("avatarName") ?: "default_avatar"
                    val id   = context.resources.getIdentifier(
                        name, "drawable", context.packageName
                    )
                    avatarResId = if (id != 0) id else R.drawable.default_avatar
                }
        }
    }

    KvGradientBackground(
        modifier = Modifier.testTag("python_header"),
        darkTheme = isDarkTheme
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 18.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = KvSurface.copy(alpha = .78f),
                    border = BorderStroke(1.dp, KvAccentSoft.copy(alpha = .22f))
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_back),
                        contentDescription = "Back",
                        tint = KvTextColor,
                        modifier = Modifier
                            .size(46.dp)
                            .padding(10.dp)
                            .clickable {
                                context.startActivity(
                                    Intent(context, CourseSelectionActivity::class.java)
                                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                )
                                (context as Activity).finish()
                            }
                    )
                }

                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = KvSurface.copy(alpha = .78f),
                    border = BorderStroke(1.dp, KvAccentSoft.copy(alpha = .22f))
                ) {
                    IconToggleButton(
                        checked = isDarkTheme,
                        onCheckedChange = onToggleTheme
                    ) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Default.DarkMode else Icons.Default.LightMode,
                            contentDescription = null,
                            tint = KvCyan
                        )
                    }
                }

                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = KvSurface.copy(alpha = .78f),
                    border = BorderStroke(1.dp, KvCyan.copy(alpha = .35f))
                ) {
                    Image(
                        painter = painterResource(id = avatarResId),
                        contentDescription = "Profile",
                        modifier = Modifier
                            .size(46.dp)
                            .padding(5.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                context.startActivity(
                                    Intent(context, ProfileActivity::class.java)
                                )
                            }
                    )
                }
            }

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
                        shape = MaterialTheme.shapes.large,
                        color = KvCyan.copy(alpha = .16f)
                    ) {
                        Icon(
                            Icons.Default.Code,
                            contentDescription = null,
                            tint = KvCyan,
                            modifier = Modifier
                                .size(62.dp)
                                .padding(14.dp)
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "PYTHON",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            fontFamily = Rubik,
                            color = KvTextColor
                        )
                        Text(
                            text = "6 topics, practice, quizzes, and AI code review",
                            style = MaterialTheme.typography.bodyMedium,
                            color = KvMutedText,
                            fontFamily = Rubik
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KvMetricChip(Icons.Default.School, "6", "topics", modifier = Modifier.weight(1f), accent = KvCyan)
                    KvMetricChip(Icons.Default.DoneAll, "2", "formats", modifier = Modifier.weight(1f), accent = KvAccentSoft)
                }
            }

            Spacer(Modifier.height(16.dp))

            // Список тем з фоном 0xFF8C52FF
            val topics = listOf(
                "Introduction" to "Meet Python and write your first program.",
                "Variables" to "What variables are, data types, and declarations.",
                "Loops" to "Practice with for and while loops.",
                "Conditions" to "if, else, and elif for decision-making.",
                "Functions" to "Create functions and pass parameters.",
                "Lists and dictionaries" to "Python collection basics for real tasks."
            )

            topics.forEachIndexed { index, (title, description) ->
                var expanded by remember { mutableStateOf(false) }
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { expanded = !expanded },
                    shape = RoundedCornerShape(8.dp),
                    color = KvSurface.copy(alpha = .86f),
                    border = BorderStroke(1.dp, KvCyan.copy(alpha = .18f))
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "${index + 1}".padStart(2, '0'),
                                color = KvCyan,
                                style = MaterialTheme.typography.labelLarge,
                                fontFamily = Rubik
                            )
                            Text(
                                text       = title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium,
                                fontFamily = Rubik,
                                color      = KvTextColor,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        AnimatedVisibility(expanded) {
                            Text(
                                text       = description,
                                style = MaterialTheme.typography.bodyMedium,
                                fontFamily = Rubik,
                                color      = KvMutedText,
                                modifier   = Modifier.padding(top = 8.dp),
                                textAlign = TextAlign.Start
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val interaction = remember { MutableInteractionSource() }
                val pressed by interaction.collectIsPressedAsState()
                val scale by animateFloatAsState(if (pressed) 0.95f else 1f)

                KvantorOutlinedButton(
                    text = "Start from beginning",
                    onClick = onStartFromBeginning,
                    leadingIcon = Icons.Default.RestartAlt,
                    modifier = Modifier
                        .weight(1f)
                        .graphicsLayer { scaleX = scale; scaleY = scale }
                )

                val interaction2 = remember { MutableInteractionSource() }
                val pressed2 by interaction2.collectIsPressedAsState()
                val scale2 by animateFloatAsState(if (pressed2) 0.95f else 1f)

                KvantorButton(
                    text = "Continue",
                    onClick = onContinueCourse,
                    leadingIcon = Icons.Default.PlayArrow,
                    modifier = Modifier
                        .weight(1f)
                        .graphicsLayer { scaleX = scale2; scaleY = scale2 }
                )
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
