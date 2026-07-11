package com.bambiloff.kvantor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val KvBg = Color(0xFF210B36)
val KvSurface = Color(0xFF2B1245)
val KvAccent = Color(0xFF8C52FF)
val KvCyan = Color(0xFF1DE0FF)
val KvTextColor = Color.White

@Composable
fun KvButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) = Button(
    onClick = onClick,
    enabled = enabled,
    modifier = modifier,
    colors = ButtonDefaults.buttonColors(
        containerColor = KvAccent,
        contentColor = Color.White
    ),
    shape = RoundedCornerShape(8.dp),
    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 13.dp)
) { Text(text) }

@Composable
fun PageContainer(
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable ColumnScope.() -> Unit
) = Box(
    modifier = modifier
        .fillMaxSize()
        .background(Brush.verticalGradient(listOf(Color(0xFF16051F), KvBg, Color(0xFF390D58))))
        .padding(24.dp),
    contentAlignment = contentAlignment
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content
    )
}
