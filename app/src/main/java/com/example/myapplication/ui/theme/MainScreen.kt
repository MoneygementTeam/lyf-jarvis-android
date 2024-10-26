package com.example.myapplication

import ActivityRequest
import ApiService
import Location
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    context: MainActivity,
    apiService: ApiService,
    locationManager: LocationManager
) {
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
                robotState = robotState,
                onRobotStateChange = { robotState = it },
                scope = scope,
                apiService = apiService,
                locationManager = locationManager
            )
        }
    }
}

@Composable
private fun ButtonSection(
    context: MainActivity,
    robotState: RobotState,
    onRobotStateChange: (RobotState) -> Unit,
    scope: CoroutineScope,
    apiService: ApiService,
    locationManager: LocationManager
) {
    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier = Modifier.fillMaxWidth()
    ) {
        CustomButton(
            onClick = {
                onRobotStateChange(RobotState.LISTEN)
                Intent(context.applicationContext, WordDetectionService::class.java).also {
                    Toast.makeText(context, "음성인식 시작", Toast.LENGTH_SHORT).show()
                    val intent = Intent(context, WordDetectionService::class.java)
                    context.startService(intent)
                }
            },
            modifier = Modifier.weight(1f).padding(end = 8.dp),
            text = "LISTEN"
        )
        CustomButton(
            onClick = {
                onRobotStateChange(RobotState.IDLE)
                Intent(context.applicationContext, WordDetectionService::class.java).also {
                    Toast.makeText(context, "음성인식 종료", Toast.LENGTH_SHORT).show()
                    val intent = Intent(context, WordDetectionService::class.java)
                    context.stopService(intent)
                }
            },
            modifier = Modifier.weight(1f).padding(start = 8.dp),
            text = "STOP LISTEN"
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier = Modifier.fillMaxWidth()
    ) {
        CustomButton(
            onClick = {
                onRobotStateChange(RobotState.GO)
                Intent(context.applicationContext, WordDetectionService::class.java).also {
                    Toast.makeText(context, "음성인식 종료", Toast.LENGTH_SHORT).show()
                    val intent = Intent(context, WordDetectionService::class.java)
                    context.stopService(intent)
                }
                scope.launch {
                    sendActivityRequest(
                        context,
                        apiService,
                        locationManager,
                        "food"
                    )
                }
            },
            text = "EAT",
            modifier = Modifier.weight(1f).padding(end = 8.dp)
        )
        CustomButton(
            onClick = {
                onRobotStateChange(RobotState.GO)
                Intent(context.applicationContext, WordDetectionService::class.java).also {
                    Toast.makeText(context, "음성인식 종료", Toast.LENGTH_SHORT).show()
                    val intent = Intent(context, WordDetectionService::class.java)
                    context.stopService(intent)
                }
                scope.launch {
                    sendActivityRequest(
                        context,
                        apiService,
                        locationManager,
                        "activity"
                    )
                }
            },
            text = "ACTIVITY",
            modifier = Modifier.weight(1f).padding(start = 8.dp)
        )
    }

    Spacer(modifier = Modifier.height(32.dp))
}

private suspend fun sendActivityRequest(
    context: MainActivity,
    apiService: ApiService,
    locationManager: LocationManager,
    voiceText: String
) {
    try {
        val request = ActivityRequest(
            startPoint = Location(
                name = "lyf funan",
                latitude = locationManager.currentLatitude,
                longitude = locationManager.currentLongitude
            ),
            voiceText = voiceText
        )

        val response = apiService.sendActivityRequest(request)
        if (response.isSuccessful) {
            Toast.makeText(context, "요청이 성공적으로 전송되었습니다", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "요청 전송 실패", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        Toast.makeText(context, "오류 발생: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}