package com.hanto.aischeduler.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.hanto.aischeduler.ui.theme.*

@Composable
fun SolidBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val isDark = isSystemInDarkTheme()

    val backgroundColor = if (isDark) {
        // 다크모드 - 어두운 배경
        DarkBackground
    } else {
        // 라이트모드 - 흰색 배경
        Color.White
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(color = backgroundColor)
    ) {
        content()
    }
}