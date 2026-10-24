package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MovieFilter
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.auth.AuthManager
import com.example.engine.SobiSpeechManager
import com.example.model.CloudFileItem
import com.example.model.CloudFileType
import com.example.ui.screens.AssistantScreen
import com.example.ui.screens.CameraCaptureScreen
import com.example.ui.screens.CloudStorageScreen
import com.example.ui.screens.LiveAgentScreen
import com.example.ui.screens.MediaStudioScreen
import com.example.ui.theme.SobiCyan
import com.example.ui.theme.SobiGreen
import com.example.ui.theme.SobiNeonPink
import com.example.ui.theme.SobiViolet

enum class AppNavDestination(val title: String, val icon: ImageVector) {
    ASSISTANT("Assistant", Icons.Default.AutoAwesome),
    CAMERA_AI("Camera AI", Icons.Default.CameraAlt),
    CLOUD_VAULT("30GB Cloud", Icons.Default.CloudQueue),
    MEDIA_STUDIO("Studio", Icons.Default.MovieFilter),
    LIVE_AGENT("Live Sobi", Icons.Default.RecordVoiceOver)
}

@Composable
fun ChatSobiMainScreen(
    isDarkMode: Boolean,
    onToggleTheme: () -> Unit
) {
    val context = LocalContext.current
    val speechManager = remember { SobiSpeechManager(context) }
    val currentUser by AuthManager.currentUser.collectAsState()

    DisposableEffect(Unit) {
        onDispose {
            speechManager.destroy()
        }
    }

    var currentScreen by remember { mutableStateOf(AppNavDestination.ASSISTANT) }
    var isCloudModeEnabled by remember { mutableStateOf(false) }

    // Cloud Vault Files List (Emptied when user logs in to provide fresh 30GB clean space)
    var vaultFiles by remember {
        mutableStateOf(
            listOf(
                CloudFileItem(
                    name = "Calculus_Lecture_Notes.pdf",
                    type = CloudFileType.DOCUMENT,
                    sizeBytes = 2_150_000L
                ),
                CloudFileItem(
                    name = "Lab_Experiment_Reaction.mp4",
                    type = CloudFileType.VIDEO,
                    sizeBytes = 18_400_000L
                ),
                CloudFileItem(
                    name = "Whiteboard_Architecture.jpg",
                    type = CloudFileType.PHOTO,
                    sizeBytes = 4_200_000L
                )
            )
        )
    }

    // Reset storage and files when user signs in with Google
    androidx.compose.runtime.LaunchedEffect(currentUser) {
        if (currentUser != null) {
            vaultFiles = emptyList()
        }
    }

    BackHandler(enabled = currentScreen != AppNavDestination.ASSISTANT) {
        currentScreen = AppNavDestination.ASSISTANT
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            ChatSobiTopAppBar(
                isDarkMode = isDarkMode,
                onToggleTheme = onToggleTheme,
                isCloudMode = isCloudModeEnabled,
                onToggleCloudMode = { isCloudModeEnabled = !isCloudModeEnabled },
                onProfileClick = { currentScreen = AppNavDestination.CLOUD_VAULT }
            )
        },
        bottomBar = {
            ChatSobiBottomBar(
                currentDestination = currentScreen,
                onNavigate = { screen ->
                    speechManager.stop()
                    currentScreen = screen
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Crossfade(
                targetState = currentScreen,
                label = "screen_transition"
            ) { screen ->
                when (screen) {
                    AppNavDestination.ASSISTANT -> AssistantScreen(
                        speechManager = speechManager,
                        isCloudModeEnabled = isCloudModeEnabled,
                        onToggleCloudMode = { isCloudModeEnabled = !isCloudModeEnabled }
                    )
                    AppNavDestination.CAMERA_AI -> CameraCaptureScreen(
                        onNavigateToAssistantWithAnalysis = { currentScreen = AppNavDestination.ASSISTANT },
                        onSaveToVault = { newFile ->
                            vaultFiles = listOf(newFile) + vaultFiles
                        }
                    )
                    AppNavDestination.CLOUD_VAULT -> CloudStorageScreen(
                        files = vaultFiles,
                        onUploadFile = { newFile ->
                            vaultFiles = listOf(newFile) + vaultFiles
                        },
                        onDeleteFile = { fileToDelete ->
                            vaultFiles = vaultFiles.filter { it.id != fileToDelete.id }
                        },
                        onInspectWithAI = {
                            currentScreen = AppNavDestination.ASSISTANT
                        }
                    )
                    AppNavDestination.MEDIA_STUDIO -> MediaStudioScreen(
                        onSaveToVaultCallback = { fileName, isVideo ->
                            val newFile = CloudFileItem(
                                name = fileName,
                                type = if (isVideo) CloudFileType.VIDEO else CloudFileType.PHOTO,
                                sizeBytes = if (isVideo) 15_000_000L else 3_500_000L
                            )
                            vaultFiles = listOf(newFile) + vaultFiles
                            AuthManager.updateUsedStorage(newFile.sizeBytes)
                        }
                    )
                    AppNavDestination.LIVE_AGENT -> LiveAgentScreen(
                        speechManager = speechManager
                    )
                }
            }
        }
    }
}

@Composable
fun ChatSobiTopAppBar(
    isDarkMode: Boolean,
    onToggleTheme: () -> Unit,
    isCloudMode: Boolean,
    onToggleCloudMode: () -> Unit,
    onProfileClick: () -> Unit = {}
) {
    val currentUser by AuthManager.currentUser.collectAsState()

    Surface(
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Brand Logo & Title with user provided Logo
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_chatsobi_logo),
                    contentDescription = "ChatSobi Official Logo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .border(1.2.dp, SobiCyan.copy(alpha = 0.6f), CircleShape)
                )

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "ChatSobi",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(SobiCyan, SobiViolet)
                                    )
                                )
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "30GB VAULT",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    }
                    Text(
                        text = "Universal AI • Safe Cloud • CameraX",
                        fontSize = 9.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Top Controls: Cloud Account & Theme Toggle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Account Badge (Tapping opens 30GB Cloud Vault)
                Surface(
                    onClick = onProfileClick,
                    shape = RoundedCornerShape(12.dp),
                    color = if (currentUser != null) SobiGreen.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (currentUser != null) SobiGreen.copy(alpha = 0.5f) else Color.Transparent
                    ),
                    modifier = Modifier.testTag("top_account_pill")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (currentUser != null) Icons.Default.CloudDone else Icons.Default.AccountCircle,
                            contentDescription = "Account Status",
                            tint = if (currentUser != null) SobiGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (currentUser != null) "30GB Safe" else "Sign In",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (currentUser != null) SobiGreen else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Theme Toggle
                IconButton(
                    onClick = onToggleTheme,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("theme_toggle_button")
                ) {
                    Icon(
                        imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = "Toggle Theme",
                        tint = if (isDarkMode) SobiCyan else SobiViolet,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ChatSobiBottomBar(
    currentDestination: AppNavDestination,
    onNavigate: (AppNavDestination) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        tonalElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        NavigationBar(
            containerColor = Color.Transparent,
            tonalElevation = 0.dp
        ) {
            AppNavDestination.values().forEach { destination ->
                val isSelected = destination == currentDestination
                NavigationBarItem(
                    selected = isSelected,
                    onClick = { onNavigate(destination) },
                    icon = {
                        Icon(
                            imageVector = destination.icon,
                            contentDescription = destination.title,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    label = {
                        Text(
                            text = destination.title,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = SobiCyan,
                        indicatorColor = SobiCyan,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.testTag("nav_${destination.name.lowercase()}")
                )
            }
        }
    }
}
