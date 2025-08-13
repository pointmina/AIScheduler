package com.hanto.aischeduler.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hanto.aischeduler.data.model.Task
import com.hanto.aischeduler.ui.components.AppCard
import com.hanto.aischeduler.ui.components.DraggableScheduleTaskCard
import com.hanto.aischeduler.ui.components.SolidBackground
import com.hanto.aischeduler.ui.components.TimeConflictDialog
import com.hanto.aischeduler.ui.theme.AppColors

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ScheduleResultScreen(
    tasks: List<Task>,
    isEditMode: Boolean,
    errorMessage: String?,
    onBack: () -> Unit,
    onToggleEditMode: () -> Unit,
    onReorderTasks: (Int, Int) -> Unit,
    onUpdateTaskTime: (String, String, String) -> Unit,
    onCompressSchedule: () -> Unit,
    onExtendEndTime: () -> Unit,
    onClearError: () -> Unit,
    onSave: () -> Unit = {},
    onSetAlarm: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var draggedTaskIndex by remember { mutableIntStateOf(-1) }
    var showConflictDialog by remember { mutableStateOf(false) }
    var conflictMessage by remember { mutableStateOf("") }

    // 에러 메시지가 충돌 관련이면 다이얼로그 표시
    LaunchedEffect(errorMessage) {
        if (errorMessage?.contains("초과") == true || errorMessage?.contains("충돌") == true) {
            conflictMessage = errorMessage
            showConflictDialog = true
        }
    }

    SolidBackground {
        Column(
            modifier = modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // 상단 앱바 (편집 버튼 추가)
            TopAppBar(
                title = {
                    Text(
                        if (isEditMode) "스케줄 편집 중" else "오늘의 완벽한 스케줄",
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
                actions = {
                    // 편집 모드 토글 버튼
                    IconButton(onClick = onToggleEditMode) {
                        Icon(
                            imageVector = if (isEditMode) Icons.Default.Done else Icons.Default.Edit,
                            contentDescription = if (isEditMode) "편집 완료" else "편집 모드",
                            tint = if (isEditMode) AppColors.Warning else Color.Black
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )

            // 편집 모드 안내
            if (isEditMode) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = AppColors.Warning.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = "✏️ 작업을 탭해서 시간을 편집할 수 있습니다",
                        modifier = Modifier.padding(12.dp),
                        color = AppColors.Warning,
                        fontSize = 12.sp
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // 에러 메시지 (충돌이 아닌 일반 에러)
            if (errorMessage != null && !errorMessage.contains("초과") && !errorMessage.contains("충돌")) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = AppColors.Warning.copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = errorMessage,
                            color = AppColors.Warning,
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = onClearError,
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "닫기",
                                tint = AppColors.Warning,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // 스크롤 가능한 콘텐츠
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 통계 카드
                item {
                    ScheduleStatsCard(
                        taskCount = tasks.size,
                        totalHours = calculateTotalHours(tasks)
                    )
                }

                // 스케줄 아이템들
                itemsIndexed(
                    items = tasks,
                    key = { _, task -> task.id }
                ) { index, task ->
                    DraggableScheduleTaskCard(
                        task = task,
                        isEditMode = isEditMode,
                        isDragging = draggedTaskIndex == index,
                        onDragStart = { draggedTaskIndex = index },
                        onDragEnd = { draggedTaskIndex = -1 },
                        onTimeEdit = { startTime, endTime ->
                            onUpdateTaskTime(task.id, startTime, endTime)
                        },
                        onConflictDetected = { message ->
                            conflictMessage = message
                            showConflictDialog = true
                        },
                        modifier = Modifier.animateItemPlacement()
                    )
                }

                // 액션 버튼들
                item {
                    if (!isEditMode) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = onSave,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AppColors.Secondary
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("💾 저장")
                            }

                            Button(
                                onClick = onSetAlarm,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AppColors.Warning
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("🔔 알림 설정")
                            }
                        }
                    }
                }

                // 하단 여백
                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }

    // 충돌 해결 다이얼로그
    if (showConflictDialog) {
        TimeConflictDialog(
            conflictMessage = conflictMessage,
            onCompressSchedule = {
                onCompressSchedule()
                showConflictDialog = false
                onClearError()
            },
            onExtendEndTime = {
                onExtendEndTime()
                showConflictDialog = false
                onClearError()
            },
            onCancel = {
                showConflictDialog = false
                onClearError()
            },
            onDismiss = {
                showConflictDialog = false
            }
        )
    }
}

@Composable
private fun ScheduleStatsCard(
    taskCount: Int,
    totalHours: Float
) {
    AppCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "🎯 오늘의 일정",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.OnSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "총 ${taskCount}개 작업 • ${formatTotalHours(totalHours)}",
                    fontSize = 14.sp,
                    color = AppColors.TextSecondary
                )
            }

            // 효율성 지표
            Surface(
                shape = RoundedCornerShape(50),
                color = when {
                    totalHours <= 4 -> AppColors.Primary.copy(alpha = 0.1f)
                    totalHours <= 8 -> AppColors.Secondary.copy(alpha = 0.1f)
                    else -> AppColors.Warning.copy(alpha = 0.1f)
                },
                modifier = Modifier.size(60.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = when {
                                totalHours <= 4 -> "⚡"
                                totalHours <= 8 -> "💪"
                                else -> "🔥"
                            },
                            fontSize = 16.sp
                        )
                        Text(
                            text = when {
                                totalHours <= 4 -> "가벼움"
                                totalHours <= 8 -> "적당함"
                                else -> "빡빡함"
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                totalHours <= 4 -> AppColors.Primary
                                totalHours <= 8 -> AppColors.Secondary
                                else -> AppColors.Warning
                            }
                        )
                    }
                }
            }
        }
    }
}

// 총 시간 계산 (실제 Task 시간 기반)
private fun calculateTotalHours(tasks: List<Task>): Float {
    if (tasks.isEmpty()) return 0f

    var totalMinutes = 0
    tasks.forEach { task ->
        val startMinutes = timeToMinutes(task.startTime)
        val endMinutes = timeToMinutes(task.endTime)
        val duration = endMinutes - startMinutes
        if (duration > 0) {
            totalMinutes += duration
        }
    }

    return totalMinutes / 60f // 분을 시간으로 변환
}

// 시간을 분으로 변환하는 헬퍼 함수
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

// 시간 포맷팅 함수
private fun formatTotalHours(hours: Float): String {
    val totalMinutes = (hours * 60).toInt()
    val h = totalMinutes / 60
    val m = totalMinutes % 60

    return when {
        h == 0 -> "${m}분"
        m == 0 -> "${h}시간"
        else -> "${h}시간 ${m}분"
    }
}
