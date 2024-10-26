package com.example.myapplication

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CustomButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier
) {
    val gradientColors = listOf(
        Color(0xFF4169E1),  // 시작 색상 (로얄 블루)
        Color(0xFF1E90FF)   // 끝 색상 (깊은 하늘색)
    )

    Button(
        onClick = onClick,
        modifier = modifier
            .shadow(
                elevation = 8.dp,
                ambientColor = Color.White,
                spotColor = Color.White
            )
            .drawBehind {
                // White border
                drawCircle(
                    color = Color.White,
                    radius = size.maxDimension / 2,
                    style = Stroke(width = 2f)
                )
                // Outer glow
                for (i in 1..5) {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.1f - (i * 0.02f)),
                        radius = size.maxDimension / 2 + i.dp.toPx(),
                        style = Stroke(width = 2f)
                    )
                }
            },
        colors = ButtonDefaults.buttonColors(
            containerColor = gradientColors[0]
        ),
        shape = RoundedCornerShape(12.dp),  // 덜 둥근 모서리
        contentPadding = ButtonDefaults.ButtonWithIconContentPadding,  // 기본 패딩
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 12.dp)  // 버튼 높이 증가
        )
    }
}