package com.bambiloff.kvantor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bambiloff.kvantor.ui.*
import com.bambiloff.kvantor.ui.theme.KvantorTheme


class ShopActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                LessonViewModel() as T
        }

        setContent {
            KvantorTheme {
                val vm: LessonViewModel = viewModel(factory = factory)

                val lives  by vm.lives.collectAsState()
                val hints  by vm.hints.collectAsState()
                val coins  by vm.coins.collectAsState()

                val snack  = remember { SnackbarHostState() }
                LaunchedEffect(Unit) {
                    vm.events.collect { e ->
                        when (e) {
                            LessonViewModel.UiEvent.NoCoins ->
                                snack.showSnackbar("Not enough Coins")
                            is LessonViewModel.UiEvent.PurchaseFinished -> {
                                val message = when (e.result) {
                                    PurchaseResult.SUCCESS -> "Purchase complete"
                                    PurchaseResult.INSUFFICIENT_COINS -> "Not enough Coins"
                                    PurchaseResult.FULL_LIVES -> "Lives are already full"
                                    PurchaseResult.FAILURE -> "Could not complete purchase"
                                }
                                snack.showSnackbar(message)
                            }
                            else -> Unit
                        }
                    }
                }

                Scaffold(
                    snackbarHost   = { SnackbarHost(snack) },
                    containerColor = KvBg,
                    topBar = {
                        TopAppBar(
                            title          = { Text("Shop", color = KvTextColor) },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = KvAccent)
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = KvBg)
                        )
                    }
                ) { pad ->
                    KvGradientBackground(modifier = Modifier.padding(pad)) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 20.dp, vertical = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            StatusRow(lives, hints, coins)

                            KvGlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp)) {
                                Text(
                                    "How the Shop works",
                                    color = KvTextColor,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                ExplanationRow(
                                    icon = Icons.Default.Favorite,
                                    text = "Lives keep tasks available while your supply is not empty."
                                )
                                ExplanationRow(
                                    icon = Icons.Default.Lightbulb,
                                    text = "Hints help with quiz questions."
                                )
                                ExplanationRow(
                                    icon = Icons.Default.MonetizationOn,
                                    text = "Coins are earned for correct answers and spent here."
                                )
                            }

                            ShopItem(
                                icon    = Icons.Default.Favorite,
                                label   = "Buy 1 Life (${GameConfig.LIFE_COST}₵)",
                                enabled = coins >= GameConfig.LIFE_COST && lives < GameConfig.MAX_LIVES,
                                onClick = vm::buyLife
                            )

                            ShopItem(
                                icon    = Icons.Default.Lightbulb,
                                label   = "Buy 1 Hint (${GameConfig.HINT_COST}₵)",
                                enabled = coins >= GameConfig.HINT_COST,
                                onClick = vm::buyHint
                            )
                        }
                    }
                }
            }
        }
    }
}

/* ---------------- допоміжні компоненти ---------------- */

@Composable
private fun StatusRow(lives: Int, hints: Int, coins: Int) = Row(
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    modifier              = Modifier
        .fillMaxWidth()
        .padding(bottom = 8.dp)
) {
    StatusChip(Icons.Default.Favorite, lives, "Lives", Modifier.weight(1f))
    StatusChip(Icons.Default.Lightbulb, hints, "Hints", Modifier.weight(1f))
    StatusChip(Icons.Default.MonetizationOn, coins, "Coins", Modifier.weight(1f))
}

@Composable
private fun StatusChip(icon: ImageVector, value: Int, label: String, modifier: Modifier = Modifier) =
    KvMetricChip(icon, value.toString(), label, modifier = modifier, accent = KvCyan)

@Composable
private fun ExplanationRow(icon: ImageVector, text: String) = Row(
    verticalAlignment     = Alignment.Top,
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    modifier              = Modifier.fillMaxWidth()
) {
    Icon(icon, null, tint = KvCyan, modifier = Modifier.size(18.dp))
    Text(text, color = KvMutedText, style = MaterialTheme.typography.bodySmall)
}

@Composable
private fun ShopItem(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit
) = Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(8.dp),
    color = if (enabled) KvSurface.copy(alpha = .9f) else KvSurfaceHi.copy(alpha = .62f),
    border = BorderStroke(1.dp, if (enabled) KvAccentSoft.copy(alpha = .2f) else KvMutedText.copy(alpha = .22f))
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(14.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (enabled) KvCyan.copy(alpha = .14f) else KvMutedText.copy(alpha = .12f)
        ) {
            Icon(icon, null, tint = if (enabled) KvCyan else KvMutedText, modifier = Modifier.padding(10.dp))
        }
        KvantorButton(
            text     = label,
            enabled  = enabled,
            onClick  = onClick,
            modifier = Modifier.weight(1f)
        )
    }
}
