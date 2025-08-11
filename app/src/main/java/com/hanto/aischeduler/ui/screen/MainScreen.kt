package com.hanto.aischeduler.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hanto.aischeduler.ui.components.AIGenerateButton
import com.hanto.aischeduler.ui.components.SolidBackground
import com.hanto.aischeduler.ui.components.TaskInputCard
import com.hanto.aischeduler.ui.components.TaskListCard
import com.hanto.aischeduler.ui.theme.AISchedulerTheme
import com.hanto.aischeduler.ui.theme.AppColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    var tasks by remember { mutableStateOf(listOf<String>()) }
    var newTask by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    SolidBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // 상단 앱바
            TopAppBar(
                title = {
                    Text(
                        "오늘의 할일!",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    Text("🔔 ⚙️", color = Color.Black, fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(16.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )

            // 스크롤 가능한 콘텐츠
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 날짜 표시
                Text(
                    text = "📅 ${getCurrentDate()}",
                    color = Color.Black,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                // 할 일 입력 카드
                TaskInputCard(
                    taskText = newTask,
                    onTaskTextChange = { newTask = it },
                    onAddTask = {
                        if (newTask.isNotBlank()) {
                            tasks = tasks + newTask
                            newTask = ""
                        }
                    }
                )

                // 할 일 목록 카드
                TaskListCard(
                    tasks = tasks,
                    onDeleteTask = { taskToDelete ->
                        tasks = tasks.filter { it != taskToDelete }
                    }
                )

                // AI 생성 버튼
                AIGenerateButton(
                    onClick = {
                        isLoading = true
                        // TODO: AI API 호출
                        // 임시로 3초 후 로딩 종료
                        // viewModel.generateSchedule()
                    },
                    isLoading = isLoading,
                    enabled = tasks.isNotEmpty()
                )

                // 임시 에러 메시지
                if (tasks.isEmpty()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = AppColors.Warning.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = "⚠️ 할 일을 하나 이상 추가해주세요",
                            modifier = Modifier.padding(12.dp),
                            color = AppColors.Warning,
                            fontSize = 14.sp
                        )
                    }
                }

                // 하단 여백
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun getCurrentDate(): String {
    val formatter = SimpleDateFormat(" M월 d일 EEEE", Locale.KOREAN)
    return formatter.format(Date())
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun MainScreenPreview() {
    AISchedulerTheme {
        MainScreen()
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
fun MainScreenMobilePreview() {
    AISchedulerTheme {
        MainScreen()
    }
}