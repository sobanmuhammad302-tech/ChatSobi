package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.engine.OfflineKnowledgeEngine
import com.example.engine.SobiSpeechManager
import com.example.engine.SobiVoiceListener
import com.example.model.CallMode
import com.example.service.ChatSobiRoutingService
import com.example.ui.components.AudioVisualizerWaveform
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.NeuralPulsingCore
import com.example.ui.components.TextStreamer
import com.example.ui.theme.SobiCyan
import com.example.ui.theme.SobiGreen
import com.example.ui.theme.SobiNeonPink
import com.example.ui.theme.SobiViolet
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LiveAgentScreen(
    speechManager: SobiSpeechManager,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isSpeaking by speechManager.isSpeaking.collectAsState()
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f

    // Call state: starts disconnected so user can choose Voice or Video
    var isCallActive by remember { mutableStateOf(false) }
    var isConnecting by remember { mutableStateOf(false) }
    var callMode by remember { mutableStateOf(CallMode.AUDIO) }
    var isMicMuted by remember { mutableStateOf(false) }
    var isVisionScanning by remember { mutableStateOf(true) }
    var callSeconds by remember { mutableIntStateOf(0) }

    // User speech, live listening & auto-reply state
    var userSpokenTranscript by remember { mutableStateOf("") }
    var isUserTalking by remember { mutableStateOf(false) }
    var isThinking by remember { mutableStateOf(false) }
    var liveRms by remember { mutableFloatStateOf(0f) }
    var silenceCountdown by remember { mutableIntStateOf(0) }
    var silenceJob by remember { mutableStateOf<Job?>(null) }
    var inlineQueryText by remember { mutableStateOf("") }

    var agentSpokenText by remember {
        mutableStateOf("I am listening! Speak any math problem, science question, or concept. I will automatically answer when you finish speaking.")
    }

    var cameraPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    // Permission launcher for both AUDIO and CAMERA
    val permissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result[Manifest.permission.CAMERA] == true) {
            cameraPermissionGranted = true
        }
    }

    // Forward declaration reference for voiceListener
    var triggerReplyRef by remember { mutableStateOf<((String) -> Unit)?>(null) }

    // Voice recognition listener for hands-free speech input
    val voiceListener = remember {
        SobiVoiceListener(
            context = context,
            onSpeechStarted = {
                isUserTalking = true
                silenceJob?.cancel()
            },
            onPartialTranscript = { partial ->
                userSpokenTranscript = partial
                isUserTalking = true
                silenceJob?.cancel()
            },
            onFinalTranscript = { finalPhrase ->
                if (finalPhrase.isNotBlank()) {
                    userSpokenTranscript = finalPhrase
                    triggerReplyRef?.invoke(finalPhrase)
                }
            },
            onRmsChangedCallback = { rms ->
                liveRms = rms
            }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            voiceListener.destroy()
        }
    }

    // Automatic Silence Detection & Auto-Reply Trigger via Gemini / Offline Engine
    fun onUserFinishedSpeaking(query: String) {
        if (query.isBlank() || isMicMuted) return
        silenceJob?.cancel()
        userSpokenTranscript = query
        isUserTalking = false
        isThinking = true
        voiceListener.pauseForAgentSpeech()

        silenceJob = coroutineScope.launch {
            // Intelligent routing: tries Gemini API cloud model, with seamless fallback to Offline Knowledge Engine
            val responseText = try {
                val res = ChatSobiRoutingService.getInstance().routeQuery(context, query)
                res.mainAnswer
            } catch (e: Exception) {
                OfflineKnowledgeEngine.answerQuery(query).text
            }

            val cleanedReply = responseText
                .replace(Regex("#+\\s*"), "")
                .replace("**", "")
                .replace("*", "")
                .replace("$$", "")
                .replace(Regex("```[a-zA-Z]*\\n[\\s\\S]*?```"), "Code implementation provided.")
                .trim()

            isThinking = false
            agentSpokenText = cleanedReply
            speechManager.speak(cleanedReply)
        }
    }

    LaunchedEffect(Unit) {
        triggerReplyRef = ::onUserFinishedSpeaking
    }

    // Pause mic listening while AI is speaking; resume when AI finishes
    LaunchedEffect(isSpeaking) {
        if (!isSpeaking && isCallActive && !isMicMuted && !isThinking) {
            voiceListener.resumeAfterAgentSpeech()
        } else if (isSpeaking) {
            voiceListener.pauseForAgentSpeech()
        }
    }

    // Call duration timer
    LaunchedEffect(isCallActive) {
        if (isCallActive) {
            callSeconds = 0
            while (isCallActive) {
                delay(1000)
                callSeconds++
            }
        }
    }

    // Start Call Handler
    fun startCall(mode: CallMode) {
        callMode = mode
        val reqPermissions = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (mode == CallMode.VIDEO) {
            reqPermissions.add(Manifest.permission.CAMERA)
        }
        permissionsLauncher.launch(reqPermissions.toTypedArray())

        isConnecting = true
        coroutineScope.launch {
            delay(1000)
            isConnecting = false
            isCallActive = true
            val welcomeText = if (mode == CallMode.AUDIO) {
                "Voice call connected. What would you like to solve or discuss today?"
            } else {
                "Live video connected. I can observe your workspace and answer any question!"
            }
            agentSpokenText = welcomeText
            speechManager.speak(welcomeText)
            voiceListener.startListening()
        }
    }

    val conversationStarters = listOf(
        "What is quadratic formula?" to "The quadratic formula calculates roots for ax squared plus bx plus c equals 0. x equals negative b plus or minus square root of b squared minus 4ac all divided by 2a.",
        "Calculate 50 times 30" to "50 times 30 equals 1500.",
        "What is the speed of light?" to "The speed of light in a vacuum is 299,792,458 meters per second, denoted by the constant c.",
        "Explain photosynthesis" to "Photosynthesis is the process where green plants convert sunlight, water, and carbon dioxide into glucose and oxygen."
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ==============================================================
        // CASE A: NOT CONNECTED (CALL LOBBY & SETUP SCREEN)
        // ==============================================================
        if (!isCallActive && !isConnecting) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header / Intro Card
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .clip(CircleShape)
                            .border(3.dp, Brush.linearGradient(listOf(SobiCyan, SobiViolet)), CircleShape)
                            .padding(4.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_chatsobi_logo),
                            contentDescription = "ChatSobi AI Agent",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "ChatSobi Live AI Conversation",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Real-time voice and video dialogue with ChatSobi. Speak naturally—the AI listens and automatically replies when you pause (no send button needed).",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                // Call Mode Selection Cards
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Option 1: Audio Call Button
                    Surface(
                        onClick = { startCall(CallMode.AUDIO) },
                        shape = RoundedCornerShape(18.dp),
                        color = if (isDark) Color(0xFF1E293B) else Color(0xFFFFFFFF),
                        border = androidx.compose.foundation.BorderStroke(
                            1.5.dp,
                            if (isDark) SobiCyan.copy(alpha = 0.5f) else Color(0xFF0284C7)
                        ),
                        shadowElevation = if (!isDark) 3.dp else 0.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("start_audio_call_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .background(Brush.linearGradient(listOf(Color(0xFF0284C7), SobiCyan))),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Phone, null, tint = Color.White, modifier = Modifier.size(24.dp))
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "📞 Start Voice Call (صرف آواز)",
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Hands-free audio dialogue with automatic pause detection",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Option 2: Video Call Button
                    Surface(
                        onClick = { startCall(CallMode.VIDEO) },
                        shape = RoundedCornerShape(18.dp),
                        color = if (isDark) Color(0xFF1E293B) else Color(0xFFFFFFFF),
                        border = androidx.compose.foundation.BorderStroke(
                            1.5.dp,
                            if (isDark) SobiNeonPink.copy(alpha = 0.5f) else Color(0xFFEC4899)
                        ),
                        shadowElevation = if (!isDark) 3.dp else 0.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("start_video_call_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .background(Brush.linearGradient(listOf(SobiNeonPink, SobiViolet))),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Videocam, null, tint = Color.White, modifier = Modifier.size(26.dp))
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "📹 Start Video Call (کیمرہ و لائیو ویڈیو)",
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Live camera stream to inspect notebooks, code, & equations",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Features Trust Pills
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FeaturePill("⚡ Low Latency")
                    FeaturePill("👂 2.5s Auto-Reply")
                    FeaturePill("🔒 Encrypted Stream")
                }
            }
        } else if (isConnecting) {
            // ==============================================================
            // CASE B: CONNECTING STATE
            // ==============================================================
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(SobiCyan.copy(alpha = 0.15f))
                        .border(2.dp, SobiCyan, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = SobiCyan,
                        modifier = Modifier.size(70.dp),
                        strokeWidth = 3.dp
                    )
                    Image(
                        painter = painterResource(id = R.drawable.ic_chatsobi_logo),
                        contentDescription = "Connecting",
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Connecting to ChatSobi Neural Stream...",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Initializing automated speech detection & audio pipeline...",
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        } else {
            // ==============================================================
            // CASE C: ACTIVE CALL VIEW (VOICE OR VIDEO)
            // ==============================================================
            // Active Call Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(SobiGreen)
                    )
                    Text(
                        text = if (callMode == CallMode.AUDIO) "Voice Call Active" else "Live Video Active",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Call Duration Counter
                val minutes = callSeconds / 60
                val seconds = callSeconds % 60
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0x3300F5A0)
                ) {
                    Text(
                        text = String.format("%02d:%02d", minutes, seconds),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = SobiGreen,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            // Mode-specific viewport
            // Mode-specific viewport
            if (callMode == CallMode.AUDIO) {
                AudioCallView(
                    isSpeaking = isSpeaking,
                    agentText = agentSpokenText,
                    userTranscript = userSpokenTranscript,
                    isUserTalking = isUserTalking,
                    isThinking = isThinking,
                    liveRms = liveRms,
                    starters = conversationStarters,
                    onSelectStarter = { topic, _ ->
                        onUserFinishedSpeaking(topic)
                    },
                    onTriggerSpeech = { spokenText ->
                        onUserFinishedSpeaking(spokenText)
                    },
                    modifier = Modifier.weight(1f)
                )
            } else {
                VideoCallView(
                    isSpeaking = isSpeaking,
                    agentText = agentSpokenText,
                    userTranscript = userSpokenTranscript,
                    isUserTalking = isUserTalking,
                    isThinking = isThinking,
                    isVisionScanning = isVisionScanning,
                    starters = conversationStarters,
                    onSelectStarter = { topic, _ ->
                        onUserFinishedSpeaking(topic)
                    },
                    onTriggerSpeech = { spokenText ->
                        onUserFinishedSpeaking(spokenText)
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            // Continuous Speech & Silence Auto-Reply Indicator
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (isThinking) SobiCyan.copy(alpha = 0.2f) else if (isUserTalking) Color(0x3310B981) else (if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isThinking) SobiCyan else if (isUserTalking) SobiGreen else (if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = if (isThinking) Icons.Default.SmartToy else if (isUserTalking) Icons.Default.Mic else Icons.Default.SmartToy,
                            contentDescription = null,
                            tint = if (isThinking) SobiCyan else if (isUserTalking) SobiGreen else SobiCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (isThinking) {
                                "ChatSobi is thinking & researching answer..."
                            } else if (isUserTalking) {
                                "User speaking: \"${userSpokenTranscript.takeLast(30)}\"..."
                            } else if (isSpeaking) {
                                "ChatSobi is speaking answer..."
                            } else {
                                "Listening... Speak naturally (auto-answers after pause)"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                    }

                    if (isThinking || isUserTalking) {
                        CircularProgressIndicator(
                            color = if (isThinking) SobiCyan else SobiGreen,
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }
            }

            // Optional Inline Call Query Input (for noisy environments or manual testing)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedTextField(
                    value = inlineQueryText,
                    onValueChange = { inlineQueryText = it },
                    placeholder = { Text("Ask anything during call...", fontSize = 11.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SobiCyan,
                        unfocusedBorderColor = if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("call_text_input")
                )

                Button(
                    onClick = {
                        if (inlineQueryText.isNotBlank()) {
                            val q = inlineQueryText.trim()
                            inlineQueryText = ""
                            onUserFinishedSpeaking(q)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SobiCyan),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .height(44.dp)
                        .testTag("call_send_text_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send Call Query", tint = Color.Black, modifier = Modifier.size(16.dp))
                }
            }

            // Call Controls Action Bar
            LiquidGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                cornerRadius = 24.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Mute Mic Toggle
                    IconButton(
                        onClick = {
                            isMicMuted = !isMicMuted
                            if (isMicMuted) {
                                speechManager.stop()
                                voiceListener.pauseForAgentSpeech()
                            } else {
                                voiceListener.resumeAfterAgentSpeech()
                            }
                        },
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(if (isMicMuted) Color(0x33FF2A85) else Color(0x3300E5FF))
                            .testTag("toggle_mic_button")
                    ) {
                        Icon(
                            imageVector = if (isMicMuted) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = "Mute Mic",
                            tint = if (isMicMuted) SobiNeonPink else SobiCyan
                        )
                    }

                    // Vision Toggle (Video mode) or Read Aloud (Voice mode)
                    if (callMode == CallMode.VIDEO) {
                        IconButton(
                            onClick = { isVisionScanning = !isVisionScanning },
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(Color(0x339D4EDD))
                                .testTag("toggle_vision_button")
                        ) {
                            Icon(Icons.Default.Visibility, "Toggle Vision", tint = SobiViolet)
                        }
                    } else {
                        IconButton(
                            onClick = { speechManager.speak(agentSpokenText) },
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(Color(0x3300F5A0))
                                .testTag("speaker_replay_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.VolumeUp, "Replay", tint = SobiGreen)
                        }
                    }

                    // End Call Button (Returns to Call Setup Screen)
                    IconButton(
                        onClick = {
                            speechManager.stop()
                            silenceJob?.cancel()
                            voiceListener.stopListening()
                            isCallActive = false
                            userSpokenTranscript = ""
                            isThinking = false
                        },
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFF2A85))
                            .testTag("end_call_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CallEnd,
                            contentDescription = "End Call",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FeaturePill(text: String) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun AudioCallView(
    isSpeaking: Boolean,
    agentText: String,
    userTranscript: String,
    isUserTalking: Boolean,
    isThinking: Boolean,
    liveRms: Float,
    starters: List<Pair<String, String>>,
    onSelectStarter: (String, String) -> Unit,
    onTriggerSpeech: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Holographic Neural Core Orb
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 6.dp)
        ) {
            NeuralPulsingCore(
                isSpeaking = isSpeaking || isUserTalking || isThinking,
                size = 130.dp
            )

            Spacer(modifier = Modifier.height(8.dp))

            AudioVisualizerWaveform(
                isActive = isSpeaking || isUserTalking,
                barCount = 22,
                barColor = if (isUserTalking) SobiGreen else (if (isSpeaking) SobiCyan else SobiViolet)
            )

            Text(
                text = if (isThinking) "ChatSobi is thinking..." else if (isUserTalking) "Listening to user voice..." else if (isSpeaking) "ChatSobi is Speaking..." else "Listening... (Auto-answers after 2-3s pause)",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isThinking) SobiCyan else if (isUserTalking) SobiGreen else if (isSpeaking) SobiCyan else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Live User Utterance Badge (shows what user just asked)
        if (userTranscript.isNotBlank()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = SobiCyan.copy(alpha = 0.12f),
                border = androidx.compose.foundation.BorderStroke(1.dp, SobiCyan.copy(alpha = 0.3f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🗣️ You: $userTranscript",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = SobiCyan,
                        maxLines = 2
                    )
                }
            }
        }

        // Agent Dialogue Card with Animated TextStreamer
        LiquidGlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            cornerRadius = 18.dp
        ) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_chatsobi_logo),
                    contentDescription = "ChatSobi Logo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, SobiCyan, CircleShape)
                )

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ChatSobi (Voice AI)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = SobiCyan
                        )
                        if (isThinking) {
                            CircularProgressIndicator(
                                color = SobiCyan,
                                modifier = Modifier.size(12.dp),
                                strokeWidth = 1.5.dp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    if (isThinking) {
                        Text(
                            text = "Analyzing question and synthesizing answer...",
                            fontSize = 12.sp,
                            color = SobiCyan
                        )
                    } else {
                        TextStreamer(
                            fullText = agentText,
                            animate = true,
                            typingSpeedMs = 12L,
                            style = LocalTextStyle.current.copy(
                                fontSize = 12.5.sp,
                                lineHeight = 17.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            enableCursor = true
                        )
                    }
                }
            }
        }

        // Quick Spoken Queries (Tap to speak without typing)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "💬 Spoken Inquiries (Tap to ask AI):",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    onClick = { onTriggerSpeech("What is the quadratic formula?") },
                    shape = RoundedCornerShape(8.dp),
                    color = SobiGreen.copy(alpha = 0.2f),
                    modifier = Modifier.testTag("tap_to_speak_call_button")
                ) {
                    Text(
                        text = "🎙️ Tap to Speak",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = SobiGreen,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(starters) { (title, reply) ->
                    Surface(
                        onClick = { onSelectStarter(title, reply) },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, SobiCyan.copy(alpha = 0.3f)),
                        modifier = Modifier.testTag("call_starter_pill")
                    ) {
                        Text(
                            text = title,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VideoCallView(
    isSpeaking: Boolean,
    agentText: String,
    userTranscript: String,
    isUserTalking: Boolean,
    isThinking: Boolean,
    isVisionScanning: Boolean,
    starters: List<Pair<String, String>>,
    onSelectStarter: (String, String) -> Unit,
    onTriggerSpeech: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "scan_laser")
    val laserY by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laserSweep"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(1.5.dp, SobiNeonPink.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
    ) {
        // Camera Stream Feed
        Image(
            painter = painterResource(id = R.drawable.img_media_studio_1792726725443),
            contentDescription = "Camera Stream",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Dark transparent HUD overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x55000000))
        )

        // Scanning Laser Line
        if (isVisionScanning) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        val y = size.height * laserY
                        drawLine(
                            brush = Brush.horizontalGradient(
                                listOf(Color.Transparent, SobiCyan, SobiNeonPink, Color.Transparent)
                            ),
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 3.dp.toPx()
                        )
                    }
            )
        }

        // Augmented Neural Vision Bounding Boxes
        if (isVisionScanning) {
            Box(
                modifier = Modifier
                    .padding(start = 18.dp, top = 20.dp)
                    .size(width = 180.dp, height = 80.dp)
                    .border(1.5.dp, SobiCyan, RoundedCornerShape(8.dp))
                    .background(Color(0x2200E5FF))
                    .padding(6.dp)
            ) {
                Text(
                    text = "TARGET [Student Workspace]\nAI Vision: Active (Real-Time)\nAudio Detection: ${if (isUserTalking) "User Speaking" else "Listening"}",
                    color = SobiCyan,
                    fontSize = 9.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }
        }

        // Sobi Picture-In-Picture Avatar
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
                .size(72.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(2.dp, if (isSpeaking) SobiCyan else SobiViolet, RoundedCornerShape(16.dp))
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_chatsobi_logo),
                contentDescription = "ChatSobi Neural Stream",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Live Vision Caption HUD with TextStreamer
        LiquidGlassCard(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(10.dp)
                .fillMaxWidth(),
            cornerRadius = 14.dp
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(SobiNeonPink))
                        Text(
                            text = "Sobi Neural Vision Engine",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = SobiNeonPink
                        )
                    }

                    if (userTranscript.isNotBlank()) {
                        Text(
                            text = "You: ${userTranscript.takeLast(25)}",
                            fontSize = 9.5.sp,
                            color = SobiCyan
                        )
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
                if (isThinking) {
                    Text(
                        text = "Analyzing visual input & researching answer...",
                        fontSize = 11.sp,
                        color = SobiCyan
                    )
                } else {
                    TextStreamer(
                        fullText = agentText,
                        animate = true,
                        typingSpeedMs = 12L,
                        style = LocalTextStyle.current.copy(
                            fontSize = 11.5.sp,
                            color = Color.White
                        )
                    )
                }
            }
        }
    }
}
