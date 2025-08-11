package com.hanto.aischeduler.ui.screen

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hanto.aischeduler.data.model.Task
import com.hanto.aischeduler.ui.components.AppCard
import com.hanto.aischeduler.ui.components.SolidBackground
import com.hanto.aischeduler.ui.theme.AISchedulerTheme
import com.hanto.aischeduler.ui.theme.AppColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleResultScreen(
    tasks: List<Task>,
    onBack: () -> Unit,
    onSave: () -> Unit = {},
    onSetAlarm: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    SolidBackground {
        Column(
            modifier = modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // 상단 앱바
            TopAppBar(
                title = {
                    Text(
                        "오늘의 완벽한 스케줄",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "뒤로가기",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    Text("⭐", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(16.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )

            // 스크롤 가능한 콘텐츠
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 통계 카드
                item {
                    ScheduleStatsCard(
                        taskCount = tasks.size,
                        totalHours = calculateTotalHours(tasks)
                    )
                }

                // 스케줄 아이템들
                items(tasks) { task ->
                    ScheduleTaskCard(task = task)
                }

                // 액션 버튼들
                item {
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

                // 하단 여백
                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
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
                    text = "총 ${taskCount}개 작업 • ${totalHours}시간",
                    fontSize = 14.sp,
                    color = AppColors.TextSecondary
                )
            }

            // 진행률 원형 표시 (임시)
            Surface(
                shape = RoundedCornerShape(50),
                color = AppColors.Primary.copy(alpha = 0.1f),
                modifier = Modifier.size(60.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "100%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.Primary
                    )
                }
            }
        }
    }
}

@Composable
private fun ScheduleTaskCard(
    task: Task
) {
    AppCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 시간 표시
            Column(
                modifier = Modifier.width(80.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = task.startTime,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.Primary
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
                    color = AppColors.Primary
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
        }
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

// 총 시간 계산
private fun calculateTotalHours(tasks: List<Task>): Float {
    return tasks.size * 1.5f // 임시로 작업당 1.5시간으로 계산
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ScheduleResultScreenPreview() {
    AISchedulerTheme {
        ScheduleResultScreen(
            tasks = listOf(
                Task(
                    id = "1",
                    title = "프로젝트 완성하기",
                    description = "2시간 집중 작업",
                    startTime = "09:00",
                    endTime = "11:00",
                    date = "2025-01-28"
                ),
                Task(
                    id = "2",
                    title = "커피 타임 & 휴식",
                    description = "30분",
                    startTime = "11:00",
                    endTime = "11:30",
                    date = "2025-01-28"
                ),
                Task(
                    id = "3",
                    title = "점심 약속",
                    description = "1시간",
                    startTime = "12:00",
                    endTime = "13:00",
                    date = "2025-01-28"
                ),
                Task(
                    id = "4",
                    title = "운동하기",
                    description = "1시간 30분",
                    startTime = "14:00",
                    endTime = "15:30",
                    date = "2025-01-28"
                )
            ),
            onBack = {}
        )
    }
}