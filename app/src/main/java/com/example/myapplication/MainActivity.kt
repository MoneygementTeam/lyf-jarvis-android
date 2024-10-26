package com.example.myapplication

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import androidx.compose.ui.platform.LocalContext
import coil.compose.rememberAsyncImagePainter
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import androidx.compose.foundation.Image
import coil.ImageLoader

class MainActivity : ComponentActivity() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    @Composable
    fun MainScreen() {
        var robotState by remember { mutableStateOf(RobotState.IDLE) }

        MyApplicationTheme {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                // Background Animation
                BackgroundAnimation(robotState = robotState)

                // Buttons overlay
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {

                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CustomButton(
                            onClick = {
                                robotState = RobotState.LISTEN
                                Intent(applicationContext, WordDetectionService::class.java).also {
                                    Toast.makeText(this@MainActivity, "음성인식 시작", Toast.LENGTH_SHORT).show()
                                    val intent = Intent(this@MainActivity, WordDetectionService::class.java)
                                    startService(intent)
                                }
                            },
                            modifier = Modifier.weight(1f).padding(end = 8.dp),
                            text = "LISTEN"
                        )
                        CustomButton(
                            onClick = {
                                robotState = RobotState.IDLE
                                Intent(applicationContext, WordDetectionService::class.java).also {
                                    Toast.makeText(this@MainActivity, "음성인식 종료", Toast.LENGTH_SHORT).show()
                                    val intent = Intent(this@MainActivity, WordDetectionService::class.java)
                                    stopService(intent)
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
                            onClick = { robotState = RobotState.GO
                                Intent(applicationContext, WordDetectionService::class.java).also {
                                    Toast.makeText(this@MainActivity, "음성인식 종료", Toast.LENGTH_SHORT).show()
                                    val intent = Intent(this@MainActivity, WordDetectionService::class.java)
                                    stopService(intent)
                                }
                            },
                            text = "EAT",
                            modifier = Modifier.weight(1f).padding(end = 8.dp)
                        )
                        CustomButton(
                            onClick = { robotState = RobotState.GO
                                Intent(applicationContext, WordDetectionService::class.java).also {
                                    Toast.makeText(this@MainActivity, "음성인식 종료", Toast.LENGTH_SHORT).show()
                                    val intent = Intent(this@MainActivity, WordDetectionService::class.java)
                                    stopService(intent)
                                }
                            },
                            text = "ACTIVITY",
                            modifier = Modifier.weight(1f).padding(start = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        requestPermissions()
        getCurrentLocation()

        enableEdgeToEdge()
        setContent {
            var showSplash by remember { mutableStateOf(true) }

            if (showSplash) {
                SplashScreen {
                    showSplash = false
                }
            } else {
                MainScreen()
            }
        }
    }

    private fun requestPermissions() {
        if (Build.VERSION.SDK_INT >= 23) {
            val permissions = arrayOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )

            permissions.forEach { permission ->
                if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, permissions, 0)
                }
            }
        }
    }

    private fun getCurrentLocation() {
        if (checkLocationPermission()) {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    location?.let {
                        val latitude = it.latitude
                        val longitude = it.longitude
                        Toast.makeText(
                            this,
                            "위치: $latitude, $longitude",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        }
    }

    private fun checkLocationPermission(): Boolean {
        return (ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED)
    }
}