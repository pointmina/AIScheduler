package com.hanto.aischeduler.ui.components

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hanto.aischeduler.ui.theme.AppColors
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeEditDialog(
    initialStartTime: String,
    initialEndTime: String,
    onTimeConfirm: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var startTime by remember { mutableStateOf(initialStartTime) }
    var endTime by remember { mutableStateOf(initialEndTime) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    // 시간 검증 함수
    fun validateTimes(): Boolean {
        val startMinutes = timeToMinutes(startTime)
        val endMinutes = timeToMinutes(endTime)

        return when {
            endMinutes <= startMinutes -> {
                errorMessage = "종료 시간은 시작 시간보다 늦어야 합니다"
                false
            }
            endMinutes - startMinutes < 30 -> {
                errorMessage = "최소 30분 이상의 시간이 필요합니다"
                false
            }
            else -> {
                errorMessage = ""
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
                        color = AppColors.Primary.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = "⏱️ 소요시간: ${formatDuration(duration)}",
                            modifier = Modifier.padding(8.dp),
                            fontSize = 12.sp,
                            color = AppColors.Primary
                        )
                    }
                }

                // 에러 메시지
                if (errorMessage.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = AppColors.Warning.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = "⚠️ $errorMessage",
                            modifier = Modifier.padding(8.dp),
                            fontSize = 12.sp,
                            color = AppColors.Warning
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (validateTimes()) {
                        onTimeConfirm(startTime, endTime)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.Primary
                )
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