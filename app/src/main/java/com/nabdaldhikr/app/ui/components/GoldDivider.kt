package com.nabdaldhikr.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun GoldDivider() {
    val goldColor = MaterialTheme.colorScheme.primary
    val brush = remember(goldColor) {
        Brush.horizontalGradient(listOf(Color.Transparent, goldColor, Color.Transparent))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(2.dp)
            .background(brush)
    )
}
