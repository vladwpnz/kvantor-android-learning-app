package com.bambiloff.kvantor

import android.app.Activity
import android.os.Build
import android.view.View
import android.view.Window
import android.view.WindowInsetsController
import androidx.compose.runtime.SideEffect
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bambiloff.kvantor.ui.theme.Rubik

val KvBg = Color(0xFF210B36)
val KvPurple = Color(0xFF390D58)
val KvSurface = Color(0xFF2B1245)
val KvSurfaceHi = Color(0xFF35175A)
val KvAccent = Color(0xFF8C52FF)
val KvAccentSoft = Color(0xFFA987FF)
val KvCyan = Color(0xFF1DE0FF)
val KvGold = Color(0xFFFFD166)
val KvSuccess = Color(0xFF40DDB4)
val KvMutedText = Color(0xFFCBBCE8)
val KvTextColor = Color.White
val KvInk = Color(0xFF11071C)

private val KvCardShape = RoundedCornerShape(8.dp)

fun kvBackgroundBrush(darkTheme: Boolean = true): Brush =
    if (darkTheme) {
        Brush.verticalGradient(
            listOf(Color(0xFF16051F), KvBg, KvPurple)
        )
    } else {
        Brush.verticalGradient(
            listOf(Color(0xFFFDFBFF), Color(0xFFF4ECFF), Color(0xFFEFFBFF))
        )
    }

@Composable
fun KvGradientBackground(
    modifier: Modifier = Modifier,
    darkTheme: Boolean = true,
    contentAlignment: Alignment = Alignment.TopCenter,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(kvBackgroundBrush(darkTheme)),
        contentAlignment = contentAlignment,
        content = content
    )
}

@Composable
fun KvantorSystemBars() {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val systemBarColor = KvBg.toArgbCompat()
            window.statusBarColor = systemBarColor
            window.navigationBarColor = systemBarColor
            window.setContentBelowSystemBars()
            window.setLightSystemBarIcons(lightIcons = true)
        }
    }
}

private fun Color.toArgbCompat(): Int = android.graphics.Color.argb(
    (alpha * 255).toInt(),
    (red * 255).toInt(),
    (green * 255).toInt(),
    (blue * 255).toInt()
)

private fun Window.setContentBelowSystemBars() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        setDecorFitsSystemWindows(true)
    } else {
        @Suppress("DEPRECATION")
        decorView.systemUiVisibility =
            decorView.systemUiVisibility and
                View.SYSTEM_UI_FLAG_FULLSCREEN.inv() and
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN.inv() and
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION.inv()
    }
}

private fun Window.setLightSystemBarIcons(lightIcons: Boolean) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val lightAppearance = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
            WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
        insetsController?.setSystemBarsAppearance(
            if (lightIcons) 0 else lightAppearance,
            lightAppearance
        )
    } else {
        @Suppress("DEPRECATION")
        var flags = decorView.systemUiVisibility
        flags = if (lightIcons) {
            flags and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
        } else {
            flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            flags = if (lightIcons) {
                flags and View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv()
            } else {
                flags or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            }
        }
        decorView.systemUiVisibility = flags
    }
}

@Composable
fun KvGlassCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(20.dp),
    content: @Composable ColumnScope.() -> Unit
) = Surface(
    modifier = modifier,
    shape = KvCardShape,
    color = KvSurface.copy(alpha = .9f),
    contentColor = KvTextColor,
    tonalElevation = 2.dp,
    border = BorderStroke(1.dp, KvAccentSoft.copy(alpha = .22f))
) {
    Column(
        modifier = Modifier.padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content
    )
}

@Composable
fun KvantorButton(
    text: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    containerColor: Color = KvAccent,
    contentColor: Color = KvTextColor
) = Button(
    onClick = onClick,
    enabled = enabled,
    modifier = modifier,
    colors = ButtonDefaults.buttonColors(
        containerColor = containerColor,
        contentColor = contentColor,
        disabledContainerColor = KvSurfaceHi.copy(alpha = .88f),
        disabledContentColor = KvMutedText.copy(alpha = .72f)
    ),
    shape = KvCardShape,
    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 13.dp)
) {
    if (leadingIcon != null) {
        Icon(leadingIcon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
    }
    Text(text, fontFamily = Rubik, style = MaterialTheme.typography.labelLarge)
}

@Composable
fun KvantorOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null
) = OutlinedButton(
    onClick = onClick,
    modifier = modifier,
    shape = KvCardShape,
    border = BorderStroke(1.dp, KvCyan.copy(alpha = .5f)),
    colors = ButtonDefaults.outlinedButtonColors(contentColor = KvTextColor),
    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 13.dp)
) {
    if (leadingIcon != null) {
        Icon(leadingIcon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
    }
    Text(text, fontFamily = Rubik, style = MaterialTheme.typography.labelLarge)
}

@Composable
fun KvOutlinedTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = KvTextColor,
    unfocusedTextColor = KvTextColor,
    cursorColor = KvCyan,
    focusedBorderColor = KvCyan,
    unfocusedBorderColor = KvAccentSoft.copy(alpha = .45f),
    focusedLabelColor = KvCyan,
    unfocusedLabelColor = KvMutedText,
    focusedPlaceholderColor = KvMutedText.copy(alpha = .75f),
    unfocusedPlaceholderColor = KvMutedText.copy(alpha = .75f),
    focusedContainerColor = KvSurface.copy(alpha = .72f),
    unfocusedContainerColor = KvSurface.copy(alpha = .56f)
)

@Composable
fun KvMetricChip(
    icon: ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    accent: Color = KvCyan
) = Surface(
    modifier = modifier,
    shape = KvCardShape,
    color = KvSurfaceHi.copy(alpha = .82f),
    border = BorderStroke(1.dp, accent.copy(alpha = .24f))
) {
    Row(
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(17.dp))
        Text(value, color = KvTextColor, style = MaterialTheme.typography.labelLarge)
        Text(label, color = KvMutedText, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun KvStateCard(
    icon: ImageVector,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null
) = KvGlassCard(modifier = modifier.fillMaxWidth()) {
    Icon(icon, contentDescription = null, tint = KvCyan, modifier = Modifier.size(34.dp))
    Text(
        title,
        color = KvTextColor,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
    )
    Text(
        body,
        color = KvMutedText,
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Start
    )
    action?.invoke()
}

@Composable
fun PageContainer(
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable BoxScope.() -> Unit
) = Box(
    modifier = Modifier
        .fillMaxSize()
        .background(kvBackgroundBrush())
        .then(modifier)
        .padding(24.dp),
    contentAlignment = contentAlignment,
    content = content
)
