package com.bambiloff.kvantor

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bambiloff.kvantor.ui.theme.KvantorTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
/**  MAIN ACTIVITY — вибір курсу  */
class CourseSelectionActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var isDarkTheme by remember { mutableStateOf(true) }

            KvantorTheme(darkTheme = isDarkTheme) {
                CourseSelectionScreen(
                    onSelect      = ::openCourse,
                    isDarkTheme   = isDarkTheme,
                    onToggleTheme = { isDarkTheme = it }
                )
            }
        }
    }

    /**  Натискання кнопки курсу  */
    private fun openCourse(courseId: String) {

        /* ── DEBUG-збірка: минаємо Firestore, одразу стартуємо цільову Activity ── */
        if (BuildConfig.DEBUG) {
            launchTarget(courseId)
            return
        }

        /* ── Release: зберігаємо вибір у Firestore, після успіху відкриваємо курс ── */
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .update("selectedCourse", courseId)
            .addOnSuccessListener { launchTarget(courseId) }
    }

    /**  Відкриває потрібну Activity та (у Release) закриває поточну  */
    private fun launchTarget(courseId: String) {
        val target = when (courseId) {
            "python"     -> MainActivity::class.java
            "javascript" -> JavaScriptMainActivity::class.java
            else         -> MainActivity::class.java
        }
        startActivity(Intent(this, target))

        if (!BuildConfig.DEBUG) {
            finish()                    // Закриваємо тільки в релізі
        }
    }
}

@SuppressLint("DiscouragedApi")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseSelectionScreen(
    onSelect: (String) -> Unit,
    isDarkTheme: Boolean,
    onToggleTheme: (Boolean) -> Unit
) {
    val ctx = LocalContext.current

    /* --------- Аватар з Firestore --------- */
    var avatarResId by remember { mutableStateOf(R.drawable.default_avatar) }
    LaunchedEffect(FirebaseAuth.getInstance().currentUser?.uid) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@LaunchedEffect
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { doc ->
                val name = doc.getString("avatarName") ?: "default_avatar"
                val id   = ctx.resources.getIdentifier(name, "drawable", ctx.packageName)
                avatarResId = if (id != 0) id else R.drawable.default_avatar
            }
    }

    KvGradientBackground(darkTheme = isDarkTheme) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        /* ─────────── Top Row ─────────── */
        Row(
            Modifier
                .fillMaxWidth()
                .padding(bottom = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = KvSurface.copy(alpha = .78f),
                border = BorderStroke(1.dp, KvAccentSoft.copy(alpha = .22f))
            ) {
                IconToggleButton(
                    modifier = Modifier.testTag("toggle_theme"),
                    checked         = isDarkTheme,
                    onCheckedChange = onToggleTheme
                ) {
                    Icon(
                        imageVector       = if (isDarkTheme) Icons.Default.DarkMode else Icons.Default.LightMode,
                        contentDescription = "Toggle theme",
                        tint               = KvCyan
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = KvSurface.copy(alpha = .78f),
                    border = BorderStroke(1.dp, KvAccentSoft.copy(alpha = .22f))
                ) {
                    IconButton(
                        modifier = Modifier.testTag("btn_shop"),
                        onClick = {
                            ctx.startActivity(Intent(ctx, ShopActivity::class.java))
                        }
                    ) {
                        Icon(
                            Icons.Default.ShoppingCart,
                            contentDescription = "Shop",
                            tint               = KvCyan
                        )
                    }
                }

                Spacer(Modifier.width(8.dp))

                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = KvSurface.copy(alpha = .78f),
                    border = BorderStroke(1.dp, KvCyan.copy(alpha = .35f))
                ) {
                    Image(
                        painter            = painterResource(id = avatarResId),
                        contentDescription = "Profile",
                        modifier           = Modifier
                            .size(46.dp)
                            .padding(5.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .testTag("avatar")
                            .clickable {
                                ctx.startActivity(Intent(ctx, ProfileActivity::class.java))
                            }
                    )
                }
            }
        }

        /* ─────────── Заголовок ─────────── */
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
                        imageVector = Icons.Default.School,
                        contentDescription = null,
                        tint = KvCyan,
                        modifier = Modifier
                            .size(64.dp)
                            .padding(14.dp)
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Choose a course",
                        style = MaterialTheme.typography.headlineSmall,
                        color = KvTextColor,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Build skills, complete modules, and unlock achievements.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = KvMutedText
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        /* ─────────── Кнопки курсів ─────────── */
        CourseButton(
            title       = "Python",
            description = "Core concepts and practical coding tasks",
            icon        = Icons.Default.Code,
            accent      = KvCyan,
            onClick     = { onSelect("python") },
            tag         = "btn_python"
        )
        Spacer(Modifier.height(10.dp))
        CourseButton(
            title       = "JavaScript",
            description = "Web basics, logic, and DOM practice",
            icon        = Icons.Default.DataObject,
            accent      = KvGold,
            onClick     = { onSelect("javascript") },
            tag         = "btn_js"
        )
        Spacer(Modifier.height(10.dp))
        CourseButton(
            title       = "AI Assistant",
            description = "Chat, hints, and code review",
            icon        = Icons.Default.Psychology,
            accent      = KvAccentSoft,
            onClick     = {
                ctx.startActivity(Intent(ctx, AiAssistantActivity::class.java))
            },
            tag         = "btn_ai"
        )
    }
    }
}

/* ───────── helper Composable ───────── */
@Suppress("SameParameterValue")
@Composable
private fun CourseButton(
    title: String,
    description: String,
    icon: ImageVector,
    accent: Color,
    onClick: () -> Unit,
    tag: String
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale  by animateFloatAsState(if (pressed) 0.95f else 1f)

    Button(
        onClick           = onClick,
        interactionSource = interaction,
        modifier          = Modifier
            .fillMaxWidth()
            .testTag(tag)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .padding(vertical = 4.dp),
        shape  = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = .3f)),
        contentPadding = PaddingValues(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = KvSurface.copy(alpha = .92f),
            contentColor   = KvTextColor
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = accent.copy(alpha = .16f)
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier
                        .size(48.dp)
                        .padding(11.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = KvTextColor)
                Text(
                    text  = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = KvMutedText,
                    textAlign = TextAlign.Start
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
