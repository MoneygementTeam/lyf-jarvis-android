package com.example.myapplication

import ApiService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : ComponentActivity() {
    private lateinit var apiService: ApiService
    private lateinit var locationManager: LocationManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // LocationManager 초기화
        locationManager = LocationManager(this)
        locationManager.requestPermissions()
        locationManager.getCurrentLocation()

        // Retrofit 설정
        val retrofit = Retrofit.Builder()
            .baseUrl("https://www.naver.com")  // 실제 base URL로 변경 필요
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        apiService = retrofit.create(ApiService::class.java)

        enableEdgeToEdge()
        setContent {
            var showSplash by remember { mutableStateOf(true) }

            if (showSplash) {
                SplashScreen {
                    showSplash = false
                }
            } else {
                MainScreen(
                    context = this,
                    apiService = apiService,
                    locationManager = locationManager
                )
            }
        }
    }
}