package com.hanto.aischeduler.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hanto.aischeduler.data.model.Task
import com.hanto.aischeduler.ui.theme.AppColors

@Composable
fun ScheduleTaskCard(
    modifier: Modifier = Modifier,
    task: Task,
    isEditMode: Boolean = false,
    onCompletionToggle: (Boolean) -> Unit,
    onTimeEdit: (String, String) -> Unit = { _, _ -> },
    ) {
    AppCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 완료 체크박스
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = onCompletionToggle,
                colors = CheckboxDefaults.colors(
                    checkedColor = AppColors.Primary,
                    uncheckedColor = AppColors.TextSecondary
                )
            )

            // 시간 표시
            Column(
                modifier = Modifier.width(80.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = task.startTime,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (task.isCompleted) AppColors.TextSecondary else AppColors.Primary,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null
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
                    color = if (task.isCompleted) AppColors.TextSecondary else AppColors.Primary,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null
                )
            }

            // 작업 내용
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = task.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (task.isCompleted) AppColors.TextSecondary else AppColors.OnSurface,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null
                )

                if (task.description.isNotEmpty()) {
                    Spacer(modifier = Modifier.padding(top = 4.dp))
                    Text(
                        text = task.description,
                        fontSize = 12.sp,
                        color = AppColors.TextSecondary,
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null
                    )
                }
            }

            // 완료 상태 표시
            if (task.isCompleted) {
                Text(
                    text = "✅",
                    fontSize = 16.sp
                )
            }
        }
    }
}