package com.bambiloff.kvantor

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bambiloff.kvantor.ui.theme.KvantorTheme
import com.bambiloff.kvantor.ui.*
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/* ───────────── Activity ───────────── */
class LessonActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val courseType = intent.getStringExtra("courseType") ?: "python"
        val uid        = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                LessonViewModel(courseType).apply { loadModules() } as T
        }

        setContent {
            KvantorTheme {
                val vm: LessonViewModel = viewModel(factory = factory)
                LessonScreen(
                    viewModel    = vm,
                    courseType   = courseType,
                    uid          = uid,
                    onBackToMenu = { finish() }
                )
            }
        }
    }
}

/* ───────────── Екран з модулями ───────────── */
@SuppressLint("DefaultLocale")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LessonScreen(
    viewModel: LessonViewModel,
    courseType: String,
    uid: String,
    onBackToMenu: () -> Unit
) {
    /* ----------- стани ------------- */
    val modules        by viewModel.modules.collectAsState()
    val currentModIdx  by viewModel.currentModuleIndex.collectAsState()
    val currentPageIdx by viewModel.currentPageIndex.collectAsState()
    val courseCompleted by viewModel.courseCompleted.collectAsState()

    val lives          by viewModel.lives.collectAsState()
    val hints          by viewModel.hints.collectAsState()
    val coins          by viewModel.coins.collectAsState()
    val showHint       by viewModel.showHint.collectAsState()
    val timeLeft   by viewModel.timeToNextLife.collectAsState()

    val livesLabel = if (timeLeft > 0 && lives < GameConfig.MAX_LIVES)
        "$lives (${String.format("%02d:%02d", timeLeft/60, timeLeft%60)})"
    else "$lives"

    val snack = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var savingProgress by remember { mutableStateOf(false) }


    /* ловимо повідомлення-події від VM */
    LaunchedEffect(Unit) {
        viewModel.events.collectLatest {
            when (it) {
                LessonViewModel.UiEvent.NoLives ->
                    snack.showSnackbar("You are out of Lives. Try again later")
                LessonViewModel.UiEvent.NoHints ->
                    snack.showSnackbar("No Hints left")
                LessonViewModel.UiEvent.NoCoins ->      // ← нова гілка
                    snack.showSnackbar("Not enough Coins to buy this")
                LessonViewModel.UiEvent.SaveFailed ->
                    snack.showSnackbar("Could not save progress")
                LessonViewModel.UiEvent.AchievementUnlockFailed ->
                    snack.showSnackbar("Progress saved, but the Achievement was not unlocked")
                is LessonViewModel.UiEvent.PurchaseFinished -> {
                    val message = when (it.result) {
                        PurchaseResult.SUCCESS -> "Purchase complete"
                        PurchaseResult.INSUFFICIENT_COINS -> "Not enough Coins"
                        PurchaseResult.FULL_LIVES -> "Lives are already full"
                        PurchaseResult.FAILURE -> "Could not complete purchase"
                    }
                    snack.showSnackbar(message)
                }
            }
        }
    }

    /* ----------- прогрес ------------- */
    val pageCount = modules.getOrNull(currentModIdx)?.pages?.size ?: 1
    val progress  = ((currentPageIdx + 1).coerceAtMost(pageCount)).toFloat() / pageCount

    Scaffold(
        snackbarHost = { SnackbarHost(snack) },

        /* ---- TOP ---- */
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Kvantor",
                            color = KvTextColor,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            if (courseType == "javascript") "JavaScript quest" else "Python quest",
                            color = KvMutedText,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                },
                actions = {
                    TextButton(
                        enabled = !savingProgress,
                        onClick = {
                            scope.launch {
                                savingProgress = true
                                val saved = viewModel.saveProgressNow()
                                savingProgress = false
                                if (saved) onBackToMenu()
                            }
                        }
                    ) {
                        Text("Menu", color = KvCyan)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = KvBg)
            )
        },

        /* ---- BOTTOM ---- */
        bottomBar = {
            Column {
                /* статус-бар з життя/підказки/монети */
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(KvBg)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusChip(Icons.Default.Favorite, livesLabel, "Lives", modifier = Modifier.weight(1f))
                    StatusChip(Icons.Default.Lightbulb, hints.toString(), "Hints", modifier = Modifier.weight(1f))
                    StatusChip(Icons.Default.MonetizationOn, coins.toString(), "Coins", modifier = Modifier.weight(1f))
                }

                /* прогрес */
                HorizontalDivider(color = KvAccent.copy(.3f), thickness = 1.dp)
                LinearProgressIndicator(
                    progress   = { progress },
                    modifier   = Modifier.fillMaxWidth().height(4.dp),
                    color      = KvAccent,
                    trackColor = KvAccent.copy(.1f)
                )
            }
        },
        containerColor = KvBg
    ) { padding ->
        PageContainer(Modifier.padding(padding)) {
            when {
                modules.isEmpty() -> KvStateCard(
                    icon = Icons.Default.HourglassEmpty,
                    title = "Loading modules",
                    body = "Preparing lessons, quizzes, and coding tasks."
                ) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = KvCyan,
                        trackColor = KvAccent.copy(alpha = .18f)
                    )
                }

                courseCompleted -> CourseFinishedScreen(onBackToMenu)

                currentModIdx < modules.size -> LessonModuleContent(
                    module       = modules[currentModIdx],
                    pageIndex    = currentPageIdx,
                    isLastModule = currentModIdx == modules.lastIndex,
                    courseType   = courseType,
                    uid          = uid,
                    vm           = viewModel,          // ← передаємо VM
                    onNext       = viewModel::next,
                    onBackToMenu = onBackToMenu
                )

                else -> CourseFinishedScreen(onBackToMenu)
            }

            /* показуємо підказку під усім контентом (як snackbar) */
            showHint?.let { hint ->
                LaunchedEffect(hint) {
                    snack.showSnackbar("Hint: $hint")
                    viewModel.clearHint()
                }
            }
        }
    }
}

/* ───────────── Контент модуля ───────────── */
@Composable
fun LessonModuleContent(
    module: Module,
    pageIndex: Int,
    isLastModule: Boolean,
    courseType: String,
    uid: String,
    vm: LessonViewModel,          // отримали
    onNext: () -> Unit,
    onBackToMenu: () -> Unit
) {
    val page = module.pages.getOrNull(pageIndex)
    var done by remember(module.id, pageIndex, page) { mutableStateOf(page is Page.Theory) }
    val accent = if (courseType == "javascript") KvGold else KvCyan

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        AnimatedVisibility(visible = true, enter = fadeIn()) {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = accent.copy(alpha = .16f),
                border = BorderStroke(1.dp, accent.copy(alpha = .28f))
            ) {
                Icon(
                    Icons.Filled.Code,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier
                        .size(72.dp)
                        .padding(16.dp)
                )
            }
        }

        Text(
            text      = "Module: ${module.title}",
            style     = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color     = KvTextColor,
            textAlign = TextAlign.Center
        )

        KvGlassCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(20.dp)
        ) {
            when (page) {
                is Page.Theory -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, tint = accent)
                        Text("Theory", color = KvTextColor, style = MaterialTheme.typography.titleMedium)
                    }
                    Text(
                        page.text,
                        color = KvMutedText,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Start
                    )
                }

                is Page.Test -> TestPage(
                    page,
                    vm,
                    CourseProgressRules.rewardPageId(courseType, module.id, pageIndex)
                ) { done = it }
                is Page.CodingTask -> CodingTaskView(page) { done = it }

                is Page.Final -> {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = KvSuccess, modifier = Modifier.size(42.dp))
                    Text(cleanGamifiedText(page.message), color = KvMutedText, textAlign = TextAlign.Center)
                    KvantorButton(
                        text    = if (isLastModule) "Back to menu" else "Next module",
                        onClick = onNext,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                null -> {
                    Text("Page not found", color = KvMutedText)
                }
            }
        }

        if (done && page !is Page.Final) {
            KvantorButton(
                "Continue",
                onClick = onNext,
                leadingIcon = Icons.Default.CheckCircle,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/* ───────────── Фінальний екран ───────────── */
@Composable
fun CourseFinishedScreen(onBackToMenu: () -> Unit) {
    KvGlassCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(24.dp)
    ) {
        Icon(
            Icons.Default.EmojiEvents,
            contentDescription = null,
            tint = KvGold,
            modifier = Modifier
                .size(56.dp)
                .align(Alignment.CenterHorizontally)
        )
        Text(
            "Congratulations!\nYou completed all modules.",
            style     = MaterialTheme.typography.titleLarge,
            color     = KvTextColor,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        KvantorButton(
            "Back to menu",
            onClick = onBackToMenu,
            leadingIcon = Icons.Default.CheckCircle,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/* ───────────── Сторінка-тест ───────────── */
@Composable
fun TestPage(
    test: Page.Test,
    vm: LessonViewModel,
    pageId: String,
    onDone: (Boolean) -> Unit
) {
    var selected by remember(pageId) { mutableStateOf(-1) }
    var checked  by remember(pageId) { mutableStateOf(false) }
    var answeredCorrect by remember(pageId) { mutableStateOf(false) }
    var lastQuizResult by remember(pageId) { mutableStateOf<QuizAttemptResult?>(null) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(Icons.Default.Quiz, contentDescription = null, tint = KvCyan)
        Text("Quiz", color = KvTextColor, style = MaterialTheme.typography.titleMedium)
    }
    Text(
        test.question,
        color = KvMutedText,
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Start
    )

    test.answers.forEachIndexed { idx, ans ->
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = !answeredCorrect) {
                    selected = idx
                    checked = false
                    lastQuizResult = null
                    onDone(false)
                },
            shape = RoundedCornerShape(8.dp),
            color = if (selected == idx) KvAccent.copy(alpha = .22f) else KvSurfaceHi.copy(alpha = .62f),
            border = BorderStroke(
                1.dp,
                if (selected == idx) KvCyan.copy(alpha = .55f) else KvAccentSoft.copy(alpha = .18f)
            )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selected == idx,
                    onClick  = {
                        if (!answeredCorrect) {
                            selected = idx
                            checked = false
                            lastQuizResult = null
                            onDone(false)
                        }
                    },
                    colors   = RadioButtonDefaults.colors(
                        selectedColor   = KvCyan,
                        unselectedColor = KvMutedText
                    )
                )
                Spacer(Modifier.width(8.dp))
                Text(ans, color = KvTextColor, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }

    /* ---- ПІДКАЗКА ---- */
    if (test.hint != null) {
        Spacer(Modifier.height(8.dp))
        KvantorButton(
            text    = "Hint (${vm.hints.collectAsState().value})",
            enabled = vm.hints.collectAsState().value > 0,
            onClick = { vm.requestHint(test) },
            leadingIcon = Icons.Default.Lightbulb,
            modifier = Modifier.fillMaxWidth()
        )
    }

    /* ---- Перевірка ---- */
    KvantorButton(
        text    = "Check answer",
        enabled = selected != -1 && !answeredCorrect,
        onClick = {
            val result = vm.checkAnswer(test, selected, pageId)
            checked = true
            answeredCorrect = result.correct
            lastQuizResult = result
            onDone(result.canProceed)
        },
        leadingIcon = Icons.Default.Quiz,
        modifier = Modifier.fillMaxWidth()
    )

    /* ---- Результат ---- */
    val lastResult = lastQuizResult
    if (checked && lastResult != null) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (lastResult.correct) KvSuccess.copy(alpha = .16f) else MaterialTheme.colorScheme.error.copy(alpha = .14f),
            border = BorderStroke(
                1.dp,
                if (lastResult.correct) KvSuccess.copy(alpha = .4f) else MaterialTheme.colorScheme.error.copy(alpha = .38f)
            )
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    if (lastResult.correct) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = if (lastResult.correct) KvSuccess else MaterialTheme.colorScheme.error
                )
                Text(
                    cleanGamifiedText(QuizProgressRules.resultMessage(lastResult)),
                    color = KvTextColor,
                    textAlign = TextAlign.Start,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

/* ───────────── Допоміжне: статус-чіп ───────────── */
@Composable
private fun StatusChip(
    icon: ImageVector,
    valueText: String,
    label: String,
    modifier: Modifier = Modifier
) = KvMetricChip(
    icon = icon,
    value = valueText,
    label = label,
    modifier = modifier,
    accent = KvCyan
)

private fun cleanGamifiedText(text: String): String = text
    .replace("✅ ", "")
    .replace("❌ ", "")
    .replace("🎉 ", "")
    .replace("❤️", "Life")
    .replace("Правильно", "Correct")
    .replace("Неправильно", "Incorrect")
