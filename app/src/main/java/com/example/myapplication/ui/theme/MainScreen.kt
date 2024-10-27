package com.example.myapplication

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.myapplication.api.ApiService

@Composable
fun MainScreen(
    context: MainActivity,
    apiService: ApiService,
    locationManager: LocationManager
) {
    var isListening by remember { mutableStateOf(false) }
    var robotState by remember { mutableStateOf(RobotState.IDLE) }
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
                robotState = robotState,
                onRobotStateChange = { robotState = it }
            )
        }
    }
}

@Composable
private fun ButtonSection(
    context: MainActivity,
    isListening: Boolean,
    onListeningChange: (Boolean) -> Unit,
    robotState: RobotState,
    onRobotStateChange: (RobotState) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier = Modifier.fillMaxWidth()
    ) {
        CustomButton(
            onClick = {
                onListeningChange(!isListening)
                if (!isListening) {
                    onRobotStateChange(RobotState.LISTEN)
                    val intent = Intent(context, WordDetectionService::class.java)
                    context.startService(intent)
                    Toast.makeText(context, "음성인식 시작", Toast.LENGTH_SHORT).show()
                } else {
                    onRobotStateChange(RobotState.IDLE)
                    val intent = Intent(context, WordDetectionService::class.java)
                    context.stopService(intent)
                    Toast.makeText(context, "음성인식 종료", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp),
            text = if (isListening) "STOP LISTEN" else "LISTEN"
        )

        CustomButton(
            onClick = {
                val intent = Intent(context, WebViewActivity::class.java).apply {
                    putExtra("url", "www.naver.com")
                }
                context.startActivity(intent)
            },
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp),
            text = "HISTORY"
        )
    }

    Spacer(modifier = Modifier.height(32.dp))
}