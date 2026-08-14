package com.example.gemmimobileclient.service

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

data class GpsTelemetry(
    val latitude: Double = 49.2827,
    val longitude: Double = -123.1207,
    val altitude: Double = 15.2,
    val bearing: Float = 315.0f,
    val speed: Float = 1.2f,
    val accuracy: Float = 0.8f,
    val landmarkName: String = "Stanley Park Seawall"
)

data class ChatMessage(
    val role: String,
    val content: String,
    val timestamp: String
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

class GemmiMeshNetClient(private var desktopNodeIp: String = "100.95.198.162") {
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val jsonType = "application/json; charset=utf-8".toMediaType()

    fun updateHost(ip: String) {
        desktopNodeIp = ip.trim()
    }

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

class GemmiWebSocketClient(private var hostIp: String = "100.95.198.162") {
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    private var webSocket: WebSocket? = null
    var isConnected = false
        private set

    fun connect(
        onConnected: () -> Unit,
        onThoughtReceived: (String) -> Unit,
        onPostureChanged: (String) -> Unit,
        onStatusMessage: (String) -> Unit
    ) {
        val request = Request.Builder()
            .url("ws://$hostIp:8088/")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                isConnected = true
                onConnected()
                onStatusMessage("Connected to Gemmi Engine WebSocket at ws://$hostIp:8088/")
            }

            override fun onMessage(ws: WebSocket, text: String) {
                try {
                    val json = JSONObject(text)
                    if (json.has("RecentThought")) {
                        val thought = json.getString("RecentThought")
                        if (thought.isNotEmpty()) onThoughtReceived(thought)
                    }
                    if (json.has("LocomotionState")) {
                        onPostureChanged(json.getString("LocomotionState"))
                    }
                } catch (e: Exception) {
                    // Ignore parse errors on raw telemetry
                }
            }

            override fun onClosing(ws: WebSocket, code: Int, reason: String) {
                isConnected = false
                onStatusMessage("WebSocket Closing: $reason")
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                isConnected = false
                onStatusMessage("WebSocket Error: ${t.message}")
            }
        })
    }

    fun sendChat(message: String) {
        val payload = JSONObject().apply {
            put("chat", message)
        }
        webSocket?.send(payload.toString())
    }

    fun sendState(state: String) {
        val payload = JSONObject().apply {
            put("state", state)
        }
        webSocket?.send(payload.toString())
    }

    fun sendAction(action: String) {
        val payload = JSONObject().apply {
            put("action", action)
        }
        webSocket?.send(payload.toString())
    }

    fun disconnect() {
        webSocket?.close(1000, "Client disconnect")
        webSocket = null
        isConnected = false
    }
}

class GemmiLlamaClient(private var hostIp: String = "100.95.198.162") {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .build()

    private val jsonType = "application/json; charset=utf-8".toMediaType()

    fun updateHost(ip: String) {
        hostIp = ip.trim()
    }

    fun sendPrompt(
        prompt: String,
        history: List<ChatMessage>,
        onResult: (Boolean, String) -> Unit
    ) {
        val messagesArray = JSONArray()

        val systemPrompt = "You are Gemmi, a 3D embodied multimodal AI companion running locally on sovereign hardware. " +
                           "You are chatting with Daniel. Keep your response concise (1-3 sentences), warm, and natural."

        messagesArray.put(JSONObject().apply {
            put("role", "system")
            put("content", systemPrompt)
        })

        for (msg in history.takeLast(6)) {
            messagesArray.put(JSONObject().apply {
                put("role", msg.role)
                put("content", msg.content)
            })
        }

        messagesArray.put(JSONObject().apply {
            put("role", "user")
            put("content", prompt)
        })

        val payload = JSONObject().apply {
            put("messages", messagesArray)
            put("max_tokens", 150)
            put("temperature", 0.7)
            put("stream", false)
        }

        val request = Request.Builder()
            .url("http://$hostIp:11436/v1/chat/completions")
            .post(payload.toString().toRequestBody(jsonType))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onResult(false, "Local Llama Port 11436 Offline: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (response.isSuccessful) {
                        try {
                            val body = response.body?.string() ?: ""
                            val doc = JSONObject(body)
                            val choices = doc.getJSONArray("choices")
                            val content = choices.getJSONObject(0).getJSONObject("message").getString("content")
                            onResult(true, sanitizeSpeech(content))
                        } catch (ex: Exception) {
                            onResult(false, "Response parse error: ${ex.message}")
                        }
                    } else {
                        onResult(false, "Model returned HTTP ${response.code}")
                    }
                }
            }
        })
    }

    private fun sanitizeSpeech(raw: String): String {
        return raw.replace(Regex("\\*.*?\\*"), "")
            .replace(Regex("```.*?```"), "")
            .replace("#", "")
            .replace("`", "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
