package com.example.gemmimobileclient.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.NavKey
import com.example.gemmimobileclient.service.GemmiGpsLocationService
import com.example.gemmimobileclient.service.GemmiMeshNetClient
import com.example.gemmimobileclient.service.GpsTelemetry

// Studio Palette
val DarkBg = Color(0xFF0A0D14)
val PanelBg = Color(0xFF121722)
val PurpleAccent = Color(0xFF8B5CF6)
val CyanAccent = Color(0xFF06B6D4)
val EmeraldGreen = Color(0xFF10B981)
val BorderColor = Color(0xFF1F293D)
val SubtextColor = Color(0xFF9CA3AF)

@Composable
fun MainScreen(
    onItemClick: (NavKey) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var gpsTelemetry by remember { mutableStateOf(GpsTelemetry()) }
    val gpsService = remember { GemmiGpsLocationService(context) }
    val meshClient = remember { GemmiMeshNetClient("100.64.0.50") }

    LaunchedEffect(Unit) {
        gpsService.startGpsUpdates { updated ->
            gpsTelemetry = updated
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(12.dp)
    ) {
        HeaderCard()

        Spacer(modifier = Modifier.height(10.dp))

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = PanelBg,
            contentColor = Color.White
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("🎙️ Perception", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("🗺️ GPS Guide", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("🌐 NetBird", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                0 -> AmbientPerceptionTab()
                1 -> GpsTourGuideTab(gpsTelemetry)
                2 -> NetBirdMeshTab(gpsTelemetry, meshClient)
            }
        }
    }
}

@Composable
fun HeaderCard() {
    Surface(
        color = PanelBg,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "⬡ Gemmi Mobile Client",
                    color = PurpleAccent,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    color = EmeraldGreen,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "● Mobile Node Online",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }
            Text(
                text = "24/7 Asynchronous Second Brain & Real-Time GPS Tour Guide",
                color = SubtextColor,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
fun AmbientPerceptionTab() {
    var alertsLog by remember { mutableStateOf("System Started: Gemmi Mobile Node connected to NetBird mesh (mesh.barrer.net)\nListening for ambient audio & vision stream...\n") }
    var initiationScore by remember { mutableFloatStateOf(0.42f) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Surface(
            color = PanelBg,
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("🎙️ Ambient Perception Telemetry", color = PurpleAccent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(10.dp))

                StatusRow("Continuous VAD Audio Stream", "● Active (Full-Duplex)", EmeraldGreen)
                Spacer(modifier = Modifier.height(6.dp))
                StatusRow("Mobile Camera Vision Stream", "● Active (2 FPS Ingest)", CyanAccent)
                Spacer(modifier = Modifier.height(10.dp))

                Text("Spontaneous Trigger Score (θ > 0.85):", color = SubtextColor, fontSize = 11.sp)
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { initiationScore },
                    modifier = Modifier.fillMaxWidth().height(10.dp),
                    color = PurpleAccent,
                    trackColor = BorderColor
                )
                Text(
                    text = "Initiation Score: ${(initiationScore * 100).toInt()}%",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = {
                alertsLog += "[Spontaneous Initiation] Hey John, I just verified the LPDDR5X RAM training latency on Rev 3 is down to 0.31s!\n"
                initiationScore = 0.92f
            },
            colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent),
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("⚡ Trigger Manual Vocal Alert", fontWeight = FontWeight.Bold, color = Color.White)
        }

        Spacer(modifier = Modifier.height(10.dp))

        Surface(
            color = PanelBg,
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
            modifier = Modifier.fillMaxWidth().height(220.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("🤖 Second Brain Spontaneous Alerts Log", color = EmeraldGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = alertsLog,
                    color = EmeraldGreen,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun GpsTourGuideTab(telemetry: GpsTelemetry) {
    var tourGuideLog by remember { mutableStateOf("🗺️ Real-Time Sub-Meter GPS Tour Guide Active\nTracking location coordinates & compass bearing...\n") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Surface(
            color = PanelBg,
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("🗺️ Live Sub-Meter GPS AI Walking Tour Guide", color = CyanAccent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(10.dp))

                StatusRow("Live GPS Coordinates", "${String.format("%.4f", telemetry.latitude)}° N, ${String.format("%.4f", telemetry.longitude)}° W", Color.White)
                Spacer(modifier = Modifier.height(6.dp))
                StatusRow("Compass Bearing & Speed", "${telemetry.bearing.toInt()}° NW (${String.format("%.1f", telemetry.speed)} m/s)", CyanAccent)
                Spacer(modifier = Modifier.height(6.dp))
                StatusRow("Nearest Landmark", telemetry.landmarkName, EmeraldGreen)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = {
                tourGuideLog += "[Tour Guide Audio]: 'You are now passing ${telemetry.landmarkName} at ${String.format("%.4f", telemetry.latitude)}, ${String.format("%.4f", telemetry.longitude)}. Historic waterfront view.'\n"
            },
            colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("🔊 Trigger Live AI Tour Audio", fontWeight = FontWeight.Bold, color = Color.White)
        }

        Spacer(modifier = Modifier.height(10.dp))

        Surface(
            color = PanelBg,
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
            modifier = Modifier.fillMaxWidth().height(220.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("🔊 AI Tour Guide Vocal Narration Stream", color = CyanAccent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = tourGuideLog,
                    color = CyanAccent,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun NetBirdMeshTab(telemetry: GpsTelemetry, meshClient: GemmiMeshNetClient) {
    var meshLog by remember { mutableStateOf("NetBird P2P Mesh Connected: mesh.barrer.net\nTarget Node: DeepHorizon-Node-01 (100.64.0.50:18799)\n") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Surface(
            color = PanelBg,
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("🌐 NetBird P2P Mesh Overlay (mesh.barrer.net)", color = EmeraldGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(10.dp))

                StatusRow("Mobile Node NetBird IP", "100.64.0.88", Color.White)
                Spacer(modifier = Modifier.height(6.dp))
                StatusRow("Primary Desktop Host", "DeepHorizon-Node-01 (100.64.0.50)", EmeraldGreen)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = {
                meshClient.hydrateMeshState(telemetry) { success, msg ->
                    meshLog += "[${if (success) "SUCCESS" else "P2P MESH TRACE"}]: $msg\n"
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("🌐 Hydrate Mobile State to Desktop Node via OkHttp", fontWeight = FontWeight.Bold, color = Color.White)
        }

        Spacer(modifier = Modifier.height(10.dp))

        Surface(
            color = PanelBg,
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
            modifier = Modifier.fillMaxWidth().height(220.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("🌐 Mesh Hydration & REST API Telemetry", color = EmeraldGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = meshLog,
                    color = EmeraldGreen,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun StatusRow(label: String, value: String, valueColor: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(label, color = SubtextColor, fontSize = 12.sp)
        Text(value, color = valueColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}
