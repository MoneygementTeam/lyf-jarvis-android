package com.example.myapplication

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.myapplication.api.ApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// 서비스와 액티비티 간 상태 공유를 위한 오브젝트
object SharedState {
    private val _robotState = MutableStateFlow(RobotState.IDLE)
    val robotState: StateFlow<RobotState> = _robotState

    fun updateRobotState(newState: RobotState) {
        _robotState.value = newState
    }
}

@Composable
fun MainScreen(
    context: MainActivity,
    apiService: ApiService,
    locationManager: LocationManager
) {
    var isListening by remember { mutableStateOf(false) }

    // SharedState의 robotState를 observe
    val robotState by SharedState.robotState.collectAsState()

    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        BackgroundAnimation(robotState = robotState)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom
        ) {
            ButtonSection(
                context = context,
                isListening = isListening,
                onListeningChange = { isListening = it },
                robotState = robotState
            )
        }
    }
}

@Composable
private fun ButtonSection(
    context: MainActivity,
    isListening: Boolean,
    onListeningChange: (Boolean) -> Unit,
    robotState: RobotState
) {
    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier = Modifier.fillMaxWidth()
    ) {
        CustomButton(
            onClick = {
                // WAIT 상태일 때는 버튼 동작 막기
                if (robotState != RobotState.WAIT) {
                    onListeningChange(!isListening)
                    if (!isListening) {
                        val intent = Intent(context, WordDetectionService::class.java)
                        context.startService(intent)
                        Toast.makeText(context, "음성인식 시작", Toast.LENGTH_SHORT).show()
                    } else {
                        SharedState.updateRobotState(RobotState.IDLE)
                        val intent = Intent(context, WordDetectionService::class.java)
                        context.stopService(intent)
                        Toast.makeText(context, "음성인식 종료", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "요청 처리 중입니다. 잠시만 기다려주세요.", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp),
            text = if (isListening) "STOP LISTEN" else "LISTEN",
            enabled = robotState != RobotState.WAIT  // WAIT 상태일 때 버튼 비활성화
        )

        CustomButton(
            onClick = {
                val intent = Intent(context, WebViewActivity::class.java).apply {
                    putExtra("url", "moneygement.o-r.kr/history/")
                    putExtra("userId", "bobsbeautifulife")  // userId 전달
                }
                context.startActivity(intent)
            },
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp),
            text = "HISTORY",
            enabled = robotState != RobotState.WAIT  // WAIT 상태일 때 버튼 비활성화
        )
    }

    Spacer(modifier = Modifier.height(32.dp))
}