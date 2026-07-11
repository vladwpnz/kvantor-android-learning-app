/* ───────────────────────── JavaScriptMainActivity.kt ───────────────────────── */
package com.bambiloff.kvantor

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DataObject
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bambiloff.kvantor.ui.theme.KvantorTheme
import com.bambiloff.kvantor.ui.theme.Rubik
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import androidx.compose.ui.platform.testTag


class JavaScriptMainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /* апаратна «Назад» → вибір курсу */
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    startActivity(
                        Intent(this@JavaScriptMainActivity, CourseSelectionActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    )
                    finish()
                }
            }
        )

        setContent {
            var dark by remember { mutableStateOf(true) }
            KvantorTheme(darkTheme = dark) {
                JavaScriptMenu(
                    dark,
                    onToggle = { dark = it },
                    onStartFromBeginning = { openJavaScriptLesson(resetProgress = true) },
                    onContinueCourse = { openJavaScriptLesson(resetProgress = false) }
                )
            }
        }
    }

    private fun openJavaScriptLesson(resetProgress: Boolean) {
        val intent = Intent(this, LessonActivity::class.java).putExtra("courseType", "javascript")
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
                        "javascript" to mapOf(
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
fun JavaScriptScreen() {
    Text(
        "JavaScript course",
        modifier = Modifier.testTag("js_header")
    )
}

/* ------------------------- сам екран ------------------------- */
@Composable
private fun JavaScriptMenu(
    dark: Boolean,
    onToggle: (Boolean) -> Unit,
    onStartFromBeginning: () -> Unit,
    onContinueCourse: () -> Unit
) {
    val context = LocalContext.current
    val uid  = FirebaseAuth.getInstance().currentUser?.uid
    val db   = FirebaseFirestore.getInstance()

    /* аватар без reflection */
    var avatar by remember { mutableStateOf(R.drawable.default_avatar) }
    LaunchedEffect(uid) {
        uid?.let { u ->
            db.collection("users").document(u).get()
                .addOnSuccessListener { d ->
                    avatar = when (d.getString("avatarName")) {
                        "avatar1" -> R.drawable.avatar1
                        "avatar2" -> R.drawable.avatar2
                        "avatar3" -> R.drawable.avatar3
                        "avatar4" -> R.drawable.avatar4
                        else      -> R.drawable.default_avatar
                    }
                }
        }
    }

    /* список модулів */
    val topics = listOf(
        "Introduction"        to "What JavaScript is and how it powers the web.",
        "Variables and types" to "var, let, const, and core data types.",
        "Functions"           to "Declarations, calls, and arrow functions.",
        "Loops and conditions" to "for, while, and if/else flow control.",
        "Arrays and objects"  to "Array methods and object properties.",
        "DOM and events"      to "DOM manipulation and event handlers."
    )

    KvGradientBackground(
        modifier = Modifier.testTag("js_header"),
        darkTheme = dark
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
                verticalAlignment = Alignment.CenterVertically
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
                    IconToggleButton(dark, onToggle) {
                        Icon(
                            if (dark) Icons.Default.DarkMode else Icons.Default.LightMode,
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
                        painter = painterResource(id = avatar),
                        contentDescription = "Profile",
                        modifier = Modifier
                            .size(46.dp)
                            .padding(5.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                context.startActivity(Intent(context, ProfileActivity::class.java))
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
                        color = KvGold.copy(alpha = .18f)
                    ) {
                        Icon(
                            Icons.Default.DataObject,
                            contentDescription = null,
                            tint = KvGold,
                            modifier = Modifier
                                .size(62.dp)
                                .padding(14.dp)
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "JavaScript",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            fontFamily = Rubik,
                            color = KvTextColor
                        )
                        Text(
                            text = "6 topics, DOM practice, and web logic",
                            style = MaterialTheme.typography.bodyMedium,
                            color = KvMutedText,
                            fontFamily = Rubik
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KvMetricChip(Icons.Default.School, "6", "topics", modifier = Modifier.weight(1f), accent = KvGold)
                    KvMetricChip(Icons.Default.DoneAll, "2", "formats", modifier = Modifier.weight(1f), accent = KvCyan)
                }
            }

            Spacer(Modifier.height(16.dp))

            topics.forEachIndexed { index, (title, descr) ->
                var expand by remember { mutableStateOf(false) }
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { expand = !expand },
                    shape = RoundedCornerShape(8.dp),
                    color = KvSurface.copy(alpha = .86f),
                    border = BorderStroke(1.dp, KvGold.copy(alpha = .2f))
                ) {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "${index + 1}".padStart(2, '0'),
                                color = KvGold,
                                style = MaterialTheme.typography.labelLarge,
                                fontFamily = Rubik
                            )
                            Text(
                                title,
                                color = KvTextColor,
                                style = MaterialTheme.typography.titleSmall,
                                fontFamily = Rubik,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        AnimatedVisibility(expand) {
                            Text(
                                descr,
                                color = KvMutedText,
                                style = MaterialTheme.typography.bodyMedium,
                                fontFamily = Rubik,
                                textAlign = TextAlign.Start,
                                modifier = Modifier.padding(top = 8.dp)
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
                KvantorOutlinedButton(
                    text = "Start from beginning",
                    onClick = onStartFromBeginning,
                    leadingIcon = Icons.Default.RestartAlt,
                    modifier = Modifier.weight(1f)
                )

                KvantorButton(
                    text = "Continue",
                    onClick = onContinueCourse,
                    leadingIcon = Icons.Default.PlayArrow,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}


