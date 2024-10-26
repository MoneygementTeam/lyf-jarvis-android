package com.example.myapplication

import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.ui.zIndex

enum class RobotState {
    IDLE, GO, LISTEN
}


@Composable
fun BackgroundAnimation(robotState: RobotState) {
    val context = LocalContext.current
    val imageLoader = ImageLoader.Builder(context)
        .components {
            if (Build.VERSION.SDK_INT >= 28) {
                add(ImageDecoderDecoder.Factory())
            } else {
                add(GifDecoder.Factory())
            }
        }
        .crossfade(true)
        .build()

    val animationResource = when (robotState) {
        RobotState.IDLE -> R.drawable.robot_idle
        RobotState.GO -> R.drawable.robot_go
        RobotState.LISTEN -> R.drawable.robot_listen
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 정적 배경 이미지
        Image(
            painter = painterResource(id = R.drawable.robot), // 기본 로봇 이미지를 정적 배경으로 사용
            contentDescription = "Background",
            modifier = Modifier
                .fillMaxSize()
                .zIndex(0f),
            contentScale = ContentScale.Crop,
            alignment = Alignment.Center
        )

        // 애니메이션 GIF
        Crossfade(
            targetState = animationResource,
            animationSpec = tween(durationMillis = 300),
            modifier = Modifier.zIndex(1f)
        ) { targetResource ->
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(targetResource)
                    .crossfade(true)
                    .build(),
                contentDescription = "Robot Animation",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alignment = Alignment.Center,
                imageLoader = imageLoader
            )
        }
    }
}