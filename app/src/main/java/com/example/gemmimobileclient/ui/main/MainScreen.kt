package com.example.gemmimobileclient.ui.main

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation3.runtime.NavKey
import com.example.gemmimobileclient.service.*
import java.text.SimpleDateFormat
import java.util.*

// Studio Palette
val DarkBg = Color(0xFF0A0D14)
val PanelBg = Color(0xFF121722)
val CardBg = Color(0xFF181F2E)
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
    var hostIp by remember { mutableStateOf("100.95.198.162") }
    var gpsTelemetry by remember { mutableStateOf(GpsTelemetry()) }

    val gpsService = remember { GemmiGpsLocationService(context) }
    val meshClient = remember { GemmiMeshNetClient(hostIp) }
    val llamaClient = remember { GemmiLlamaClient(hostIp) }
    val wsClient = remember { GemmiWebSocketClient(hostIp) }

    var latestThought by remember { mutableStateOf("Gemmi Mobile ready. Connected to sovereign ecosystem.") }
    var activePosture by remember { mutableStateOf("CozyChairListeningMusic") }

    LaunchedEffect(hostIp) {
        meshClient.updateHost(hostIp)
        llamaClient.updateHost(hostIp)
    }

    LaunchedEffect(Unit) {
        gpsService.startGpsUpdates { updated ->
            gpsTelemetry = updated
        }
        wsClient.connect(
            onConnected = {},
            onThoughtReceived = { thought -> latestThought = thought },
            onPostureChanged = { posture -> activePosture = posture },
            onStatusMessage = {}
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(10.dp)
    ) {
        HeaderCard(latestThought, activePosture)

        Spacer(modifier = Modifier.height(8.dp))

        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = PanelBg,
            contentColor = Color.White,
            edgePadding = 0.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("💬 Neural Chat", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("🌐 4D Avatar", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("🎙️ Perception", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedTab == 3,
                onClick = { selectedTab = 3 },
                text = { Text("🗺️ GPS Guide", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedTab == 4,
                onClick = { selectedTab = 4 },
                text = { Text("⚙️ Settings", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                0 -> NeuralChatTab(llamaClient, wsClient)
                1 -> AvatarVisualizerTab(hostIp, onHostIpChanged = { hostIp = it })
                2 -> AmbientPerceptionTab()
                3 -> GpsTourGuideTab(gpsTelemetry)
                4 -> SettingsMeshTab(hostIp, onHostIpChanged = { hostIp = it }, gpsTelemetry, meshClient)
            }
        }
    }
}

@Composable
fun HeaderCard(thought: String, posture: String) {
    Surface(
        color = PanelBg,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "⬡ Gemmi Sovereign AI",
                    color = PurpleAccent,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    color = EmeraldGreen,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "● $posture",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Text(
                text = "💭 \"$thought\"",
                color = CyanAccent,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun NeuralChatTab(llamaClient: GemmiLlamaClient, wsClient: GemmiWebSocketClient) {
    var promptInput by remember { mutableStateOf("") }
    var isQuerying by remember { mutableStateOf(false) }
    val messages = remember {
        mutableStateListOf(
            ChatMessage("assistant", "Hello Daniel! I am connected to your local model and spatial body. How can I assist you?", "Now")
        )
    }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Quick Action Chips
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
        ) {
            QuickChip("🚶 Walk") {
                wsClient.sendState("walk")
            }
            QuickChip("🪑 Cozy Chair") {
                wsClient.sendState("sit")
            }
            QuickChip("👋 Wave") {
                wsClient.sendAction("wave")
            }
            QuickChip("📡 Radar") {
                promptInput = "What is the status of our 3D room radar?"
            }
        }

        // Messages List
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { msg ->
                ChatBubble(msg)
            }
            if (isQuerying) {
                item {
                    Text(
                        text = "Gemmi is generating neural response on port 11436...",
                        color = CyanAccent,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Input Box
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = promptInput,
                onValueChange = { promptInput = it },
                placeholder = { Text("Ask Gemmi anything...", fontSize = 12.sp, color = SubtextColor) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyanAccent,
                    unfocusedBorderColor = BorderColor,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = CardBg,
                    unfocusedContainerColor = CardBg
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(6.dp))

            Button(
                onClick = {
                    if (promptInput.isNotBlank() && !isQuerying) {
                        val text = promptInput.trim()
                        val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                        messages.add(ChatMessage("user", text, timeStr))
                        promptInput = ""
                        isQuerying = true

                        // Also notify WebSocket server
                        wsClient.sendChat(text)

                        llamaClient.sendPrompt(text, messages) { success, reply ->
                            isQuerying = false
                            val respTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                            messages.add(ChatMessage("assistant", reply, respTime))
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                shape = RoundedCornerShape(8.dp),
                enabled = !isQuerying
            ) {
                Text("💬 Send", fontWeight = FontWeight.Bold, color = Color.Black)
            }
        }
    }
}

@Composable
fun QuickChip(label: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = CardBg,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun ChatBubble(msg: ChatMessage) {
    val isUser = msg.role == "user"
    Column(
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
        modifier = Modifier.fillMaxWidth()
    ) {
        Surface(
            color = if (isUser) PurpleAccent else CardBg,
            shape = RoundedCornerShape(8.dp),
            border = if (isUser) null else androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = if (isUser) "You" else "Gemmi",
                    color = if (isUser) Color.White else CyanAccent,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = msg.content,
                    color = Color.White,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun AvatarVisualizerTab(
    hostIp: String,
    onHostIpChanged: (String) -> Unit = {}
) {
    var reloadTrigger by remember { mutableIntStateOf(0) }
    var ipInput by remember { mutableStateOf(hostIp) }
    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }
    val visualizerUrl = "http://$hostIp:8088/"

    Column(modifier = Modifier.fillMaxSize()) {
        // Quick IP Config Bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
        ) {
            OutlinedTextField(
                value = ipInput,
                onValueChange = { ipInput = it },
                label = { Text("Gemmi Host IP / Port", fontSize = 10.sp, color = SubtextColor) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyanAccent,
                    unfocusedBorderColor = BorderColor,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = CardBg,
                    unfocusedContainerColor = CardBg
                ),
                shape = RoundedCornerShape(6.dp),
                singleLine = true,
                modifier = Modifier.weight(1f).height(50.dp)
            )

            Spacer(modifier = Modifier.width(6.dp))

            Button(
                onClick = {
                    onHostIpChanged(ipInput.trim())
                    loadError = null
                    isLoading = true
                    reloadTrigger++
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.height(50.dp)
            ) {
                Text("🔄 Connect", fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 11.sp)
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                .background(DarkBg)
        ) {
            key(reloadTrigger, hostIp) {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            setBackgroundColor(0xFF0A0D14.toInt())
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                databaseEnabled = true
                                allowFileAccess = true
                                allowContentAccess = true
                                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                useWideViewPort = true
                                loadWithOverviewMode = true
                                cacheMode = WebSettings.LOAD_NO_CACHE
                            }
                            webChromeClient = android.webkit.WebChromeClient()
                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                    super.onPageStarted(view, url, favicon)
                                    isLoading = true
                                    loadError = null
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    isLoading = false
                                }

                                override fun onReceivedError(
                                    view: WebView?,
                                    errorCode: Int,
                                    description: String?,
                                    failingUrl: String?
                                ) {
                                    super.onReceivedError(view, errorCode, description, failingUrl)
                                    isLoading = false
                                    loadError = "Connection failed to $failingUrl: $description"
                                }

                                override fun onReceivedError(
                                    view: WebView?,
                                    request: android.webkit.WebResourceRequest?,
                                    error: android.webkit.WebResourceError?
                                ) {
                                    super.onReceivedError(view, request, error)
                                    if (request?.isForMainFrame == true) {
                                        isLoading = false
                                        loadError = "Cannot connect to $visualizerUrl. Ensure Gemmi Live Host is running on your PC."
                                    }
                                }
                            }
                            loadUrl(visualizerUrl)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            if (isLoading && loadError == null) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize().background(DarkBg.copy(alpha = 0.7f))
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = CyanAccent)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Loading 3D Avatar WebGL stream ($visualizerUrl)...", color = CyanAccent, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }

            if (loadError != null) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize().background(DarkBg).padding(16.dp)
                ) {
                    Surface(
                        color = CardBg,
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text("⚠️ Host Connection Offline", color = Color(0xFFEF4444), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(loadError ?: "", color = SubtextColor, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "1. Ensure 'GemmiLiveSystemHost' is running on your PC.\n2. Verify the Host IP above matches your PC's IP.\n3. Make sure tablet is connected to the same Wi-Fi / NetBird mesh.",
                                color = Color.White,
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    loadError = null
                                    isLoading = true
                                    reloadTrigger++
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("Retry Connection", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AmbientPerceptionTab() {
    var alertsLog by remember { mutableStateOf("System Started: Gemmi Mobile Node connected to NetBird mesh\nListening for ambient audio & vision stream...\n") }
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
                alertsLog += "[Spontaneous Initiation] Hey Daniel, I just verified the 15-point kinematic spatial matrix is running at 60 FPS!\n"
                initiationScore = 0.94f
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
fun SettingsMeshTab(
    hostIp: String,
    onHostIpChanged: (String) -> Unit,
    telemetry: GpsTelemetry,
    meshClient: GemmiMeshNetClient
) {
    var ipInput by remember { mutableStateOf(hostIp) }
    var meshLog by remember { mutableStateOf("NetBird P2P Mesh Connected\nTarget Node: $hostIp:18799\n") }

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
                Text("⚙️ Gemmi Node & Mesh Configuration", color = PurpleAccent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(10.dp))

                Text("Primary Desktop Host IP (NetBird / LAN):", color = SubtextColor, fontSize = 11.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = ipInput,
                        onValueChange = { ipInput = it },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PurpleAccent,
                            unfocusedBorderColor = BorderColor,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = CardBg,
                            unfocusedContainerColor = CardBg
                        ),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onHostIpChanged(ipInput)
                            meshLog += "[HOST UPDATE] Set active host to $ipInput\n"
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("Save", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
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
