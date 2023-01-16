package com.ss.challengetask.utils

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun CaptureBtn(modifier: Modifier,callback: () -> Unit) {
    val gradient = Brush.horizontalGradient(listOf(Color(0xFF56ab2f), Color(0xFFa8e063)))
    GradientButton(
        onClick = callback,
        text = "Capture",
        gradient = gradient,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp)
    )
}