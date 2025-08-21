package com.hanto.aischeduler.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hanto.aischeduler.ui.theme.AppColors

enum class ErrorType {
    WARNING, ERROR, INFO
}

@Composable
fun ErrorDisplay(
    modifier: Modifier = Modifier,
    message: String,
    type: ErrorType = ErrorType.ERROR,
    onDismiss: (() -> Unit)? = null,
    onRetry: (() -> Unit)? = null
) {
    val (backgroundColor, iconColor, icon) = when (type) {
        ErrorType.WARNING -> Triple(
            AppColors.Warning.copy(alpha = 0.1f),
            AppColors.Warning,
            Icons.Default.Warning
        )

        ErrorType.ERROR -> Triple(
            Color.Red.copy(alpha = 0.1f),
            Color.Red,
            Icons.Default.Warning
        )

        ErrorType.INFO -> Triple(
            AppColors.Primary.copy(alpha = 0.1f),
            AppColors.Primary,
            Icons.Default.Info
        )
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = type.name,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )

            Text(
                text = message,
                modifier = Modifier.weight(1f),
                fontSize = 14.sp,
                color = AppColors.OnSurface
            )

            // 재시도 버튼
            if (onRetry != null) {
                IconButton(
                    onClick = onRetry,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "다시 시도",
                        tint = iconColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // 닫기 버튼
            if (onDismiss != null) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "닫기",
                        tint = iconColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun LoadingIndicator(
    modifier: Modifier = Modifier,
    message: String = "로딩 중...",
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = AppColors.Primary.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = AppColors.Primary,
                strokeWidth = 2.dp
            )

            Text(
                text = message,
                fontSize = 14.sp,
                color = AppColors.Primary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}