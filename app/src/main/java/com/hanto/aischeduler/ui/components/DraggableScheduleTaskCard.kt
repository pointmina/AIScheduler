package com.hanto.aischeduler.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
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
    onTimeEdit: (String, String) -> Unit,
    onConflictDetected: (String) -> Unit = {}, // 충돌 감지 콜백 추가
    modifier: Modifier = Modifier
) {
    var showTimeEditDialog by remember { mutableStateOf(false) }

    val cardScale by animateFloatAsState(
        targetValue = if (isDragging) 1.05f else 1f,
        label = "cardScale"
    )

    AppCard(
        modifier = modifier
            .scale(cardScale)
            .let { mod ->
                if (isEditMode) {
                    mod.combinedClickable(
                        onClick = { showTimeEditDialog = true },
                        onLongClick = {
                            // 길게 누르면 드래그 모드 시작 (현재는 비활성화)
                            // onDragStart()
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
            // 편집 모드 표시 (드래그 핸들 대신)
            if (isEditMode) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "편집 가능",
                    tint = AppColors.Warning,
                    modifier = Modifier
                        .size(20.dp)
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
                    text = task.title,
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

            // 충돌 경고 표시
            if (task.description.contains("자동 조정됨") || task.description.contains("압축됨")) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "시간 조정됨",
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
            onDismiss = { showTimeEditDialog = false },
            onConflictDetected = { message ->
                onConflictDetected(message)
                showTimeEditDialog = false
            }
        )
    }
}