package com.example.myapplication

import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import android.util.Base64
import android.webkit.WebResourceRequest
import android.webkit.WebSettings

class WebViewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val url = intent.getStringExtra("url") ?: "moneygement.o-r.kr/history"
        println(url)
        val userId = intent.getStringExtra("userId") ?: "bobsbeautifulife"

        setContent {
            PostWebView(
                url = url,
                userId = userId,
                onClose = { finish() }
            )
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun PostWebView(url: String, userId: String, onClose: () -> Unit) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { fullHeight -> fullHeight },
            animationSpec = tween(durationMillis = 300)
        ),
        exit = slideOutVertically(
            targetOffsetY = { fullHeight -> fullHeight },
            animationSpec = tween(durationMillis = 300)
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.White
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // WebView with POST request
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { context ->
                        WebView(context).apply {
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                cacheMode = WebSettings.LOAD_NO_CACHE
                                loadsImagesAutomatically = true
                            }

                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(
                                    view: WebView?,
                                    request: WebResourceRequest?
                                ): Boolean {
                                    return false
                                }
                            }

                            // POST 데이터 생성
                            val postData = "userId=$userId"

                            // HTML 폼 생성
                            val html = """
                                <html>
                                    <body onload="document.forms[0].submit()">
                                        <form action="https://$url" method="post">
                                            <input type="hidden" name="userId" value="$userId">
                                        </form>
                                    </body>
                                </html>
                            """.trimIndent()

                            // Base64로 인코딩된 HTML 로드
                            loadData(
                                Base64.encodeToString(html.toByteArray(), Base64.NO_PADDING),
                                "text/html",
                                "base64"
                            )
                        }
                    }
                )

                // Close Button with Container
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    shadowElevation = 4.dp,
                    color = Color.White,
                    shape = androidx.compose.foundation.shape.CircleShape
                ) {
                    IconButton(
                        onClick = {
                            visible = false
                            onClose()
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.Black
                        )
                    }
                }
            }
        }
    }
}