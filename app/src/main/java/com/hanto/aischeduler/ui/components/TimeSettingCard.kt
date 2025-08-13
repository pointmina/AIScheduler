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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeSettingCard(
    startTime: String,
    endTime: String,
    includeBreaks: Boolean,
    breakDuration: Int,
    onStartTimeChange: (String) -> Unit,
    onEndTimeChange: (String) -> Unit,
    onToggleBreaks: () -> Unit,
    onBreakDurationChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    AppCard(modifier = modifier) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 제목
            Text(
                text = "⏰ 시간 설정",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.OnSurface
            )

            // 시간 선택 영역
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 시작 시간
                TimePickerButton(
                    label = "시작",
                    time = startTime,
                    onClick = { showStartTimePicker = true },
                    modifier = Modifier.weight(1f)
                )

                // 종료 시간
                TimePickerButton(
                    label = "종료",
                    time = endTime,
                    onClick = { showEndTimePicker = true },
                    modifier = Modifier.weight(1f)
                )
            }

            // 프리셋 버튼들
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PresetButton(
                    text = "오전",
                    onClick = {
                        onStartTimeChange("09:00")
                        onEndTimeChange("12:00")
                    },
                    modifier = Modifier.weight(1f)
                )
                PresetButton(
                    text = "오후",
                    onClick = {
                        onStartTimeChange("13:00")
                        onEndTimeChange("18:00")
                    },
                    modifier = Modifier.weight(1f)
                )
                PresetButton(
                    text = "저녁",
                    onClick = {
                        onStartTimeChange("19:00")
                        onEndTimeChange("22:00")
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            // 구분선
            HorizontalDivider(
                color = AppColors.TextSecondary.copy(alpha = 0.2f),
                thickness = 1.dp
            )

            // 휴식시간 설정
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 휴식시간 포함 여부
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "☕ 휴식시간 포함",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = AppColors.OnSurface
                        )
                        Text(
                            text = if (includeBreaks) "작업 사이에 휴식시간이 추가됩니다" else "연속적인 스케줄로 생성됩니다",
                            fontSize = 12.sp,
                            color = AppColors.TextSecondary
                        )
                    }

                    Switch(
                        checked = includeBreaks,
                        onCheckedChange = { onToggleBreaks() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = AppColors.Primary,
                            checkedTrackColor = AppColors.Primary.copy(alpha = 0.3f)
                        )
                    )
                }

                // 휴식시간 길이 선택 (휴식시간 포함일 때만 표시)
                if (includeBreaks) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "휴식시간 길이",
                            fontSize = 12.sp,
                            color = AppColors.TextSecondary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            BreakDurationChip(
                                text = "15분",
                                isSelected = breakDuration == 15,
                                onClick = { onBreakDurationChange(15) },
                                modifier = Modifier.weight(1f)
                            )
                            BreakDurationChip(
                                text = "30분",
                                isSelected = breakDuration == 30,
                                onClick = { onBreakDurationChange(30) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }

    // 시작 시간 피커 다이얼로그
    if (showStartTimePicker) {
        TimePickerDialog(
            initialTime = startTime,
            onTimeSelected = { selectedTime ->
                onStartTimeChange(selectedTime)
                showStartTimePicker = false
            },
            onDismiss = { showStartTimePicker = false }
        )
    }

    // 종료 시간 피커 다이얼로그
    if (showEndTimePicker) {
        TimePickerDialog(
            initialTime = endTime,
            onTimeSelected = { selectedTime ->
                onEndTimeChange(selectedTime)
                showEndTimePicker = false
            },
            onDismiss = { showEndTimePicker = false }
        )
    }
}

@Composable
private fun TimePickerButton(
    label: String,
    time: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = AppColors.Surface
        )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                color = AppColors.TextSecondary
            )
            Text(
                text = time,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.OnSurface
            )
        }
    }
}

@Composable
private fun PresetButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.outlinedButtonColors()
    ) {
        Text(
            text = text,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun BreakDurationChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        onClick = onClick,
        label = { Text(text) },
        selected = isSelected,
        modifier = modifier,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = AppColors.Primary.copy(alpha = 0.2f),
            selectedLabelColor = AppColors.Primary
        )
    )
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