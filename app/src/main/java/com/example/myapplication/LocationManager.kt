package com.example.myapplication

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

class LocationManager(private val context: Context) {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    var currentLatitude: Double = 0.0
    var currentLongitude: Double = 0.0

    init {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    }

    fun requestPermissions() {
        if (android.os.Build.VERSION.SDK_INT >= 23) {
            val permissions = arrayOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )

            permissions.forEach { permission ->
                if (ContextCompat.checkSelfPermission(context, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(
                        context as MainActivity,
                        permissions,
                        0
                    )
                }
            }
        }
    }

    fun getCurrentLocation() {
        if (checkLocationPermission()) {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    location?.let {
                        currentLatitude = it.latitude
                        currentLongitude = it.longitude
                        Toast.makeText(
                            context,
                            "위치: $currentLatitude, $currentLongitude",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        }
    }

    private fun checkLocationPermission(): Boolean {
        return (ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED)
    }
}