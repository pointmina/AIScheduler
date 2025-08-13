package com.hanto.aischeduler.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hanto.aischeduler.ui.theme.AppColors

@Composable
fun TimeConflictDialog(
    conflictMessage: String,
    onCompressSchedule: () -> Unit,
    onExtendEndTime: () -> Unit,
    onCancel: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "경고",
                    tint = AppColors.Warning
                )
                Text(
                    "⏰ 시간 충돌 감지",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 충돌 상황 설명
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = AppColors.Warning.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = conflictMessage,
                        modifier = Modifier.padding(12.dp),
                        fontSize = 14.sp,
                        color = AppColors.OnSurface
                    )
                }

                // 해결 옵션 설명
                Text(
                    text = "다음 중 하나를 선택해주세요:",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = AppColors.OnSurface
                )

                // 옵션들
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ConflictResolutionOption(
                        icon = "⚡",
                        title = "스케줄 압축",
                        description = "각 작업 시간을 줄여서 맞춰요",
                        onClick = onCompressSchedule
                    )

                    ConflictResolutionOption(
                        icon = "⏰",
                        title = "종료시간 연장",
                        description = "전체 종료시간을 늦춰요",
                        onClick = onExtendEndTime
                    )
                }
            }
        },
        confirmButton = {
            // 별도 버튼 없음 (옵션 버튼들로 대체)
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text("취소")
            }
        }
    )
}

@Composable
private fun ConflictResolutionOption(
    icon: String,
    title: String,
    description: String,
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
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = icon,
                fontSize = 20.sp
            )
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = AppColors.OnSurface
                )
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = AppColors.TextSecondary
                )
            }
        }
    }
}

// 압축 완료 알림 다이얼로그
@Composable
fun ScheduleCompressedDialog(
    originalDuration: String,
    compressedDuration: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("⚡", fontSize = 20.sp)
                Text(
                    "스케줄 압축 완료",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "스케줄이 성공적으로 압축되었습니다!",
                    fontSize = 14.sp
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = AppColors.Primary.copy(alpha = 0.1f)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "압축 전: $originalDuration",
                            fontSize = 12.sp,
                            color = AppColors.TextSecondary
                        )
                        Text(
                            text = "압축 후: $compressedDuration",
                            fontSize = 12.sp,
                            color = AppColors.Primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.Primary
                )
            ) {
                Text("확인")
            }
        }
    )
}