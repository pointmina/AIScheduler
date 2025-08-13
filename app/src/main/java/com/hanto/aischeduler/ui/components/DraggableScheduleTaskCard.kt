package com.hanto.aischeduler.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hanto.aischeduler.data.model.Task
import com.hanto.aischeduler.ui.theme.AppColors

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DraggableScheduleTaskCard(
    task: Task,
    isEditMode: Boolean,
    isDragging: Boolean,
    onDragStart: () -> Unit,
    onDragEnd: () -> Unit,
    onTimeEdit: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showTimeEditDialog by remember { mutableStateOf(false) }

    val cardElevation by animateDpAsState(
        targetValue = if (isDragging) 8.dp else 2.dp,
        label = "cardElevation"
    )

    val cardScale by animateFloatAsState(
        targetValue = if (isDragging) 1.05f else 1f,
        label = "cardScale"
    )

    AppCard(
        modifier = modifier
            .scale(cardScale)
            .let { mod ->
                if (isEditMode) {
                    mod
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { onDragStart() },
                                onDragEnd = { onDragEnd() }
                            ) { _, _ -> }
                        }
                        .combinedClickable(
                            onClick = { showTimeEditDialog = true },
                            onLongClick = {
                                // 길게 누르면 드래그 모드 시작
                                onDragStart()
                            }
                        )
                } else {
                    mod
                }
            }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 드래그 핸들 (편집 모드일 때만 표시)
            if (isEditMode) {
                Icon(
                    imageVector = Icons.Default.DragHandle,
                    contentDescription = "드래그하여 순서 변경",
                    tint = AppColors.TextSecondary,
                    modifier = Modifier
                        .size(24.dp)
                        .padding(end = 8.dp)
                )
            }

            // 시간 표시
            Column(
                modifier = Modifier.width(80.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = task.startTime,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isEditMode) AppColors.Warning else AppColors.Primary
                )
                Text(
                    text = "━",
                    fontSize = 8.sp,
                    color = AppColors.TextSecondary
                )
                Text(
                    text = task.endTime,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isEditMode) AppColors.Warning else AppColors.Primary
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // 작업 내용
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = getTaskEmoji(task.title) + " " + task.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = AppColors.OnSurface
                )
                if (task.description.isNotEmpty()) {
                    Text(
                        text = task.description,
                        fontSize = 12.sp,
                        color = AppColors.TextSecondary
                    )
                }
            }

            // 편집 모드 표시
            if (isEditMode) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "편집 가능",
                    tint = AppColors.Warning,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }

    // 시간 편집 다이얼로그
    if (showTimeEditDialog) {
        TimeEditDialog(
            initialStartTime = task.startTime,
            initialEndTime = task.endTime,
            onTimeConfirm = { startTime, endTime ->
                onTimeEdit(startTime, endTime)
                showTimeEditDialog = false
            },
            onDismiss = { showTimeEditDialog = false }
        )
    }
}

// 작업에 맞는 이모지 반환
private fun getTaskEmoji(title: String): String {
    return when {
        title.contains("운동", ignoreCase = true) -> "💪"
        title.contains("점심", ignoreCase = true) || title.contains("식사", ignoreCase = true) -> "🍽️"
        title.contains("회의", ignoreCase = true) || title.contains("미팅", ignoreCase = true) -> "💼"
        title.contains("공부", ignoreCase = true) || title.contains("학습", ignoreCase = true) -> "📚"
        title.contains("휴식", ignoreCase = true) || title.contains("커피", ignoreCase = true) -> "☕"
        title.contains("프로젝트", ignoreCase = true) || title.contains("작업", ignoreCase = true) -> "💻"
        title.contains("쇼핑", ignoreCase = true) -> "🛒"
        else -> "📝"
    }
}