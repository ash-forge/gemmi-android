package com.example.gemmimobileclient.service

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

data class GpsTelemetry(
    val latitude: Double = 49.2827,
    val longitude: Double = -123.1207,
    val altitude: Double = 15.2,
    val bearing: Float = 315.0f,
    val speed: Float = 1.2f,
    val accuracy: Float = 0.8f,
    val landmarkName: String = "Stanley Park Seawall"
)

class GemmiGpsLocationService(private val context: Context) {
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private var currentLocation = GpsTelemetry()

    @SuppressLint("MissingPermission")
    fun startGpsUpdates(onLocationUpdated: (GpsTelemetry) -> Unit) {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000)
            .setMinUpdateIntervalMillis(1000)
            .build()

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc: Location? = result.lastLocation
                if (loc != null) {
                    currentLocation = GpsTelemetry(
                        latitude = loc.latitude,
                        longitude = loc.longitude,
                        altitude = loc.altitude,
                        bearing = loc.bearing,
                        speed = loc.speed,
                        accuracy = loc.accuracy,
                        landmarkName = getNearestLandmark(loc.latitude, loc.longitude)
                    )
                    onLocationUpdated(currentLocation)
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        } catch (ex: Exception) {
            // Permission fallback simulation
            onLocationUpdated(currentLocation)
        }
    }

    private fun getNearestLandmark(lat: Double, lng: Double): String {
        return if (lat in 49.2..49.3 && lng in -123.2..-123.1) {
            "Stanley Park Seawall (Historic Granite Seawall)"
        } else {
            "Urban Exploration Landmark Sector Alpha"
        }
    }
}

class GemmiMeshNetClient(private val desktopNodeIp: String = "100.64.0.50") {
    private val client = OkHttpClient()
    private val jsonType = "application/json; charset=utf-8".toMediaType()

    fun hydrateMeshState(
        telemetry: GpsTelemetry,
        onResult: (Boolean, String) -> Unit
    ) {
        val jsonBody = JSONObject().apply {
            put("nodeId", "Gemmi-Mobile-Android")
            put("latitude", telemetry.latitude)
            put("longitude", telemetry.longitude)
            put("bearing", telemetry.bearing.toDouble())
            put("speed", telemetry.speed.toDouble())
            put("landmark", telemetry.landmarkName)
            put("timestamp", System.currentTimeMillis())
        }

        val request = Request.Builder()
            .url("http://$desktopNodeIp:18799/api/mesh/state")
            .post(jsonBody.toString().toRequestBody(jsonType))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onResult(false, "NetBird Mesh P2P Hydration Offline: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (response.isSuccessful) {
                        onResult(true, "NetBird Mesh Hydrated to $desktopNodeIp in ${response.receivedResponseAtMillis - response.sentRequestAtMillis}ms")
                    } else {
                        onResult(false, "NetBird Node Returned HTTP ${response.code}")
                    }
                }
            }
        })
    }
}
