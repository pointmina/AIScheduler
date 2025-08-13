package com.hanto.aischeduler.ui.components

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hanto.aischeduler.ui.theme.AppColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeEditDialog(
    initialStartTime: String,
    initialEndTime: String,
    onTimeConfirm: (String, String) -> Unit,
    onDismiss: () -> Unit,
    onConflictDetected: (String) -> Unit = {} // 충돌 감지 콜백 추가
) {
    var startTime by remember { mutableStateOf(initialStartTime) }
    var endTime by remember { mutableStateOf(initialEndTime) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var warningMessage by remember { mutableStateOf("") }

    // 시간 검증 및 충돌 감지 함수
    fun validateTimes(): Boolean {
        val startMinutes = timeToMinutes(startTime)
        val endMinutes = timeToMinutes(endTime)

        return when {
            endMinutes <= startMinutes -> {
                errorMessage = "종료 시간은 시작 시간보다 늦어야 합니다"
                warningMessage = ""
                false
            }

            endMinutes - startMinutes < 30 -> {
                errorMessage = "최소 30분 이상의 시간이 필요합니다"
                warningMessage = ""
                false
            }

            endMinutes - startMinutes > 180 -> {
                errorMessage = ""
                warningMessage = "3시간을 초과하면 다른 일정과 충돌할 수 있습니다"
                true
            }

            else -> {
                errorMessage = ""
                warningMessage = ""
                true
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "⏰ 시간 수정",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 시작 시간 선택
                TimePickerField(
                    label = "시작 시간",
                    time = startTime,
                    onClick = { showStartTimePicker = true }
                )

                // 종료 시간 선택
                TimePickerField(
                    label = "종료 시간",
                    time = endTime,
                    onClick = { showEndTimePicker = true }
                )

                // 예상 소요시간 표시
                val duration = calculateDuration(startTime, endTime)
                if (duration > 0) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = when {
                            duration > 180 -> AppColors.Warning.copy(alpha = 0.1f)
                            else -> AppColors.Primary.copy(alpha = 0.1f)
                        }
                    ) {
                        Text(
                            text = "⏱️ 소요시간: ${formatDuration(duration)}${if (duration > 180) " (장시간)" else ""}",
                            modifier = Modifier.padding(8.dp),
                            fontSize = 12.sp,
                            color = if (duration > 180) AppColors.Warning else AppColors.Primary
                        )
                    }
                }

                // 충돌 가능성 경고
                if (warningMessage.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = AppColors.Warning.copy(alpha = 0.1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("⚠️", fontSize = 14.sp)
                            Text(
                                text = warningMessage,
                                fontSize = 12.sp,
                                color = AppColors.Warning
                            )
                        }
                    }
                }

                // 에러 메시지
                if (errorMessage.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = AppColors.Warning.copy(alpha = 0.1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("❌", fontSize = 14.sp)
                            Text(
                                text = errorMessage,
                                fontSize = 12.sp,
                                color = AppColors.Warning
                            )
                        }
                    }
                }

                // 스마트 제안
                if (duration > 180 && errorMessage.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = AppColors.Primary.copy(alpha = 0.1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "💡 제안",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = AppColors.Primary
                            )
                            Text(
                                text = "긴 작업은 여러 개로 나누거나 중간에 휴식을 추가하는 것이 좋습니다.",
                                fontSize = 11.sp,
                                color = AppColors.TextSecondary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (validateTimes()) {
                        if (warningMessage.isNotEmpty()) {
                            // 충돌 가능성이 있을 때 콜백 호출
                            onConflictDetected("시간 변경으로 인해 다른 일정과 충돌할 수 있습니다.")
                        }
                        onTimeConfirm(startTime, endTime)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = when {
                        errorMessage.isNotEmpty() -> AppColors.TextSecondary
                        warningMessage.isNotEmpty() -> AppColors.Warning
                        else -> AppColors.Primary
                    }
                ),
                enabled = errorMessage.isEmpty()
            ) {
                Text(
                    if (warningMessage.isNotEmpty()) "주의해서 적용" else "확인"
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )

    // 시작 시간 피커
    if (showStartTimePicker) {
        TimePickerDialog(
            initialTime = startTime,
            onTimeSelected = { selectedTime ->
                startTime = selectedTime
                showStartTimePicker = false
                validateTimes()
            },
            onDismiss = { showStartTimePicker = false }
        )
    }

    // 종료 시간 피커
    if (showEndTimePicker) {
        TimePickerDialog(
            initialTime = endTime,
            onTimeSelected = { selectedTime ->
                endTime = selectedTime
                showEndTimePicker = false
                validateTimes()
            },
            onDismiss = { showEndTimePicker = false }
        )
    }
}

@Composable
private fun TimePickerField(
    label: String,
    time: String,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = AppColors.Surface
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = AppColors.TextSecondary,
                fontSize = 14.sp
            )
            Text(
                text = time,
                color = AppColors.OnSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@SuppressLint("DefaultLocale")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    initialTime: String,
    onTimeSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val timeParts = initialTime.split(":")
    val initialHour = timeParts.getOrNull(0)?.toIntOrNull() ?: 9
    val initialMinute = timeParts.getOrNull(1)?.toIntOrNull() ?: 0

    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("시간 선택") },
        text = {
            TimePicker(
                state = timePickerState,
                modifier = Modifier.padding(16.dp)
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val selectedTime = String.format(
                        "%02d:%02d",
                        timePickerState.hour,
                        timePickerState.minute
                    )
                    onTimeSelected(selectedTime)
                }
            ) {
                Text("확인")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
}

// 유틸리티 함수들
private fun timeToMinutes(time: String): Int {
    val parts = time.split(":")
    return try {
        val hour = parts[0].toInt()
        val minute = parts[1].toInt()
        hour * 60 + minute
    } catch (e: Exception) {
        0
    }
}

private fun calculateDuration(startTime: String, endTime: String): Int {
    val startMinutes = timeToMinutes(startTime)
    val endMinutes = timeToMinutes(endTime)
    return if (endMinutes > startMinutes) endMinutes - startMinutes else 0
}

private fun formatDuration(minutes: Int): String {
    val hours = minutes / 60
    val remainingMinutes = minutes % 60

    return when {
        hours == 0 -> "${remainingMinutes}분"
        remainingMinutes == 0 -> "${hours}시간"
        else -> "${hours}시간 ${remainingMinutes}분"
    }
}