package com.hanto.aischeduler.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hanto.aischeduler.ui.components.AppCard
import com.hanto.aischeduler.ui.components.SolidBackground
import com.hanto.aischeduler.ui.theme.AppColors
import com.hanto.aischeduler.ui.viewModel.SavedScheduleItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedSchedulesScreen(
    savedSchedules: List<SavedScheduleItem>,
    onBack: () -> Unit,
    onScheduleClick: (String) -> Unit,
    onNewSchedule: () -> Unit,
    onDeleteSchedule: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    SolidBackground {
        Scaffold(
            modifier = modifier
                .fillMaxSize()
                .statusBarsPadding(),
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "📋 내 계획",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = "뒤로가기",
                                tint = Color.Black
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = onNewSchedule,
                    containerColor = AppColors.Primary,
                    contentColor = Color.White
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "새 계획 만들기"
                    )
                }
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 안내 메시지
                item {
                    if (savedSchedules.isEmpty()) {
                        EmptySchedulesMessage()
                    } else {
                        Text(
                            text = "최근 7일간 저장된 계획 ${savedSchedules.size}개",
                            fontSize = 14.sp,
                            color = AppColors.TextSecondary,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // 저장된 계획 목록
                items(savedSchedules) { scheduleItem ->
                    SavedScheduleCard(
                        scheduleItem = scheduleItem,
                        onClick = { onScheduleClick(scheduleItem.id) },
                        onDelete = { onDeleteSchedule(scheduleItem.id) }
                    )
                }

                // 하단 여백
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

@Composable
private fun SavedScheduleCard(
    scheduleItem: SavedScheduleItem,
    onClick: () -> Unit,
    onDelete: () -> Unit = {}
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    AppCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 제목과 날짜 + 삭제 버튼
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = scheduleItem.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.OnSurface
                    )
                    Text(
                        text = formatDateKorean(scheduleItem.date),
                        fontSize = 12.sp,
                        color = AppColors.TextSecondary
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 완료 상태 뱃지
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = when {
                            scheduleItem.isCompleted -> AppColors.Primary.copy(alpha = 0.1f)
                            scheduleItem.completionRate >= 0.5f -> AppColors.Secondary.copy(alpha = 0.1f)
                            else -> AppColors.Warning.copy(alpha = 0.1f)
                        }
                    ) {
                        Text(
                            text = when {
                                scheduleItem.isCompleted -> "완료 ✅"
                                scheduleItem.completionRate >= 0.5f -> "진행중 💪"
                                else -> "시작 🚀"
                            },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                scheduleItem.isCompleted -> AppColors.Primary
                                scheduleItem.completionRate >= 0.5f -> AppColors.Secondary
                                else -> AppColors.Warning
                            }
                        )
                    }

                    // 삭제 버튼
                    IconButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "계획 삭제",
                            tint = AppColors.TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // 시간과 작업 정보
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "⏰ ${scheduleItem.startTime} ~ ${scheduleItem.endTime}",
                    fontSize = 12.sp,
                    color = AppColors.TextSecondary
                )
                Text(
                    text = "📝 ${scheduleItem.totalTasks}개 작업",
                    fontSize = 12.sp,
                    color = AppColors.TextSecondary
                )
            }

            // 진행률 바
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "진행률",
                        fontSize = 11.sp,
                        color = AppColors.TextSecondary
                    )
                    Text(
                        text = "${scheduleItem.completedTasks}/${scheduleItem.totalTasks} (${(scheduleItem.completionRate * 100).toInt()}%)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.OnSurface
                    )
                }

                LinearProgressIndicator(
                    progress = { scheduleItem.completionRate },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    color = when {
                        scheduleItem.isCompleted -> AppColors.Primary
                        scheduleItem.completionRate >= 0.5f -> AppColors.Secondary
                        else -> AppColors.Warning
                    },
                    trackColor = AppColors.Border
                )
            }
        }
    }

    // 삭제 확인 다이얼로그
    if (showDeleteDialog) {
        DeleteConfirmDialog(
            scheduleName = scheduleItem.title,
            onConfirm = {
                onDelete()
                showDeleteDialog = false
            },
            onDismiss = { showDeleteDialog = false }
        )
    }
}

@Composable
private fun EmptySchedulesMessage() {
    AppCard {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "📝",
                fontSize = 48.sp
            )
            Text(
                text = "아직 저장된 계획이 없어요",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.OnSurface
            )
            Text(
                text = "할 일을 추가하고 AI 스케줄을 만든 후\n저장 버튼을 눌러보세요!",
                fontSize = 14.sp,
                color = AppColors.TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

// 삭제 확인 다이얼로그
@Composable
private fun DeleteConfirmDialog(
    scheduleName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("🗑️", fontSize = 20.sp)
                Text(
                    "계획 삭제",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "정말로 이 계획을 삭제하시겠어요?",
                    fontSize = 14.sp
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = AppColors.Warning.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = "📋 $scheduleName",
                        modifier = Modifier.padding(12.dp),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.Warning
                    )
                }

                Text(
                    text = "삭제된 계획은 복구할 수 없습니다.",
                    fontSize = 12.sp,
                    color = AppColors.TextSecondary
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.Warning
                )
            ) {
                Text("삭제")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
}

private fun formatDateKorean(date: String): String {
    return try {
        val formatter = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val displayFormatter = java.text.SimpleDateFormat("M월 d일 (E)", java.util.Locale.KOREAN)
        val dateObj = formatter.parse(date)
        displayFormatter.format(dateObj ?: java.util.Date())
    } catch (e: Exception) {
        date
    }
}