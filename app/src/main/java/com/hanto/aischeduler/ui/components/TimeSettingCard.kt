package com.hanto.aischeduler.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hanto.aischeduler.ui.theme.AppColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeSettingCard(
    startTime: String,
    endTime: String,
    onStartTimeChange: (String) -> Unit,
    onEndTimeChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    AppCard(modifier = modifier) {
        Text(
            text = "시간 설정",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = AppColors.OnSurface
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 시작 시간
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "시작 시간",
                    fontSize = 12.sp,
                    color = AppColors.TextSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))

                TimePickerButton(
                    time = startTime,
                    onClick = { showStartTimePicker = true }
                )
            }

            // 구분선
            Text(
                text = "~",
                fontSize = 18.sp,
                color = AppColors.TextSecondary,
                modifier = Modifier.padding(top = 16.dp)
            )

            // 종료 시간
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "종료 시간",
                    fontSize = 12.sp,
                    color = AppColors.TextSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))

                TimePickerButton(
                    time = endTime,
                    onClick = { showEndTimePicker = true }
                )
            }
        }

        // 추천 시간대
        Spacer(modifier = Modifier.height(12.dp))
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 첫 번째 줄
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TimePresetButton(
                    text = "풀타임 (8-22시)",
                    onClick = {
                        onStartTimeChange("08:00")
                        onEndTimeChange("22:00")
                    },
                    modifier = Modifier.weight(1f)
                )
                TimePresetButton(
                    text = "데이타임 (9-18시)",
                    onClick = {
                        onStartTimeChange("09:00")
                        onEndTimeChange("18:00")
                    },
                    modifier = Modifier.weight(1f)
                )
                TimePresetButton(
                    text = "나이트 (19-22시)",
                    onClick = {
                        onStartTimeChange("19:00")
                        onEndTimeChange("22:00")
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    // 시작 시간 피커
    if (showStartTimePicker) {
        TimePickerDialog(
            initialTime = startTime,
            onTimeSelected = { time ->
                onStartTimeChange(time)
                showStartTimePicker = false
            },
            onDismiss = { showStartTimePicker = false }
        )
    }

    // 종료 시간 피커
    if (showEndTimePicker) {
        TimePickerDialog(
            initialTime = endTime,
            onTimeSelected = { time ->
                onEndTimeChange(time)
                showEndTimePicker = false
            },
            onDismiss = { showEndTimePicker = false }
        )
    }
}

@Composable
private fun TimePickerButton(
    time: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(
                width = 1.dp,
                color = AppColors.Border,
                shape = RoundedCornerShape(8.dp)
            )
            .background(Color.White)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = time,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = AppColors.OnSurface
        )
    }
}

@Composable
private fun TimePresetButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = AppColors.Primary.copy(alpha = 0.1f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                fontSize = 10.sp,
                color = AppColors.Primary,
                maxLines = 1
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    initialTime: String,
    onTimeSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    // 시간 파싱
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
                    val hour = String.format("%02d", timePickerState.hour)
                    val minute = String.format("%02d", timePickerState.minute)
                    onTimeSelected("$hour:$minute")
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