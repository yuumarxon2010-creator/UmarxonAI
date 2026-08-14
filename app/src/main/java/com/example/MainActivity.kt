package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.ApiKeyDialog
import com.example.ui.components.UmarxonTopBar
import com.example.ui.screens.chat.ChatScreen
import com.example.ui.screens.history.HistoryScreen
import com.example.ui.screens.image.ImageStudioScreen
import com.example.ui.screens.tools.ToolsScreen
import com.example.ui.screens.video.VideoStudioScreen
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.UmarxonViewModel

enum class NavigationTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    CHAT("Suhbat", Icons.Filled.ChatBubble, Icons.Outlined.ChatBubbleOutline, "tab_chat"),
    IMAGE("Imagen", Icons.Filled.Image, Icons.Outlined.Image, "tab_image"),
    VIDEO("Video", Icons.Filled.Movie, Icons.Outlined.Movie, "tab_video"),
    TOOLS("Asboblar", Icons.Filled.Psychology, Icons.Outlined.Psychology, "tab_tools"),
    HISTORY("Tarix", Icons.Filled.History, Icons.Outlined.History, "tab_history")
}

class MainActivity : ComponentActivity() {
    private val viewModel: UmarxonViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                UmarxonAiApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun UmarxonAiApp(viewModel: UmarxonViewModel) {
    var currentTab by rememberSaveable { mutableIntStateOf(0) }

    val chatState by viewModel.chatState.collectAsStateWithLifecycle()
    val imageState by viewModel.imageState.collectAsStateWithLifecycle()
    val videoState by viewModel.videoState.collectAsStateWithLifecycle()
    val settingsState by viewModel.settingsState.collectAsStateWithLifecycle()

    val sessions by viewModel.allSessions.collectAsStateWithLifecycle()
    val savedImages by viewModel.allGeneratedImages.collectAsStateWithLifecycle()
    val savedVideos by viewModel.allVideoProjects.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            UmarxonTopBar(
                title = "UmarxonAI",
                subtitle = when (currentTab) {
                    0 -> "Gemini Universal Intellekt"
                    1 -> "Google Imagen 3.0 Studiyasi"
                    2 -> "Veo Video Rejissyor"
                    3 -> "Bilimlar va Asboblar"
                    else -> "Saqlangan Ma'lumotlar"
                },
                onApiKeyClick = { viewModel.showApiKeyDialog(true) }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                NavigationTab.entries.forEachIndexed { index, tab ->
                    val isSelected = currentTab == index
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentTab = index },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = tab.title
                            )
                        },
                        label = {
                            Text(text = tab.title, fontSize = 11.sp)
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = IndigoPrimary,
                            selectedTextColor = IndigoPrimary,
                            indicatorColor = IndigoPrimary.copy(alpha = 0.15f),
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.testTag(tab.testTag)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                0 -> ChatScreen(
                    state = chatState,
                    personas = viewModel.personas,
                    onInputChanged = viewModel::onChatInputChanged,
                    onSendMessage = viewModel::sendMessage,
                    onPersonaSelected = viewModel::onPersonaSelected,
                    onAttachImage = { uri -> viewModel.attachImage(viewModel.getApplication(), uri) },
                    onAttachDocument = { uri -> viewModel.attachDocument(viewModel.getApplication(), uri) },
                    onRemoveAttachment = viewModel::removeAttachedImage,
                    onNewChat = viewModel::createNewChat,
                    onToggleSpeak = viewModel::toggleSpeakMessage,
                    onQuickPromptClick = { prompt ->
                        when {
                            prompt.startsWith("🎨 /imagine") || prompt.startsWith("/imagine") -> {
                                viewModel.generateImagenImageFromChat(prompt.removePrefix("🎨").trim())
                            }
                            prompt.startsWith("🎬 /video") || prompt.startsWith("/video") -> {
                                viewModel.generateVideoFromChat(prompt.removePrefix("🎬").trim())
                            }
                            else -> {
                                viewModel.onChatInputChanged(prompt)
                                viewModel.sendMessage()
                            }
                        }
                    },
                    onSpeechRecognized = viewModel::appendSpeechText,
                    onSelectPromptTemplate = { template ->
                        viewModel.applyPromptTemplate(template)
                        when (template.type) {
                            com.example.data.model.TemplateType.CHAT -> { currentTab = 0 }
                            com.example.data.model.TemplateType.IMAGE -> { currentTab = 1 }
                            com.example.data.model.TemplateType.VIDEO -> { currentTab = 2 }
                        }
                    },
                    onTogglePromptGallery = viewModel::togglePromptGallery,
                    onToggleAttachMenu = viewModel::toggleAttachMenu,
                    onExportPdf = { msg -> viewModel.exportMessageToPdf(viewModel.getApplication(), msg) },
                    onShareMessage = { msg -> viewModel.shareChatMessage(viewModel.getApplication(), msg) },
                    onGenerateImagen = viewModel::generateImagenImageFromChat,
                    onGenerateVideo = { prompt, genre -> viewModel.generateVideoFromChat(prompt, genre) }
                )
                1 -> ImageStudioScreen(
                    state = imageState,
                    savedImages = savedImages,
                    stylePresets = viewModel.stylePresets,
                    samplePrompts = viewModel.sampleImagePrompts,
                    engines = viewModel.imagenEngines,
                    onPromptChanged = viewModel::onImagePromptChanged,
                    onStyleSelected = viewModel::onImageStyleSelected,
                    onAspectSelected = viewModel::onImageAspectSelected,
                    onEngineSelected = viewModel::onImageEngineSelected,
                    onNegativePromptChanged = viewModel::onNegativePromptChanged,
                    onToggleNegativePrompt = viewModel::toggleNegativePromptField,
                    onEnhancePrompt = viewModel::enhanceImagePrompt,
                    onGenerateImage = viewModel::generateImage,
                    onOpenPreview = viewModel::openImagePreview,
                    onDeleteImage = viewModel::deleteImage
                )
                2 -> VideoStudioScreen(
                    state = videoState,
                    savedProjects = savedVideos,
                    genrePresets = viewModel.genrePresets,
                    onPromptChanged = viewModel::onVideoPromptChanged,
                    onGenreSelected = viewModel::onVideoGenreSelected,
                    onAspectSelected = viewModel::onVideoAspectSelected,
                    onGenerateVideo = viewModel::generateVideoDirectorProject,
                    onTogglePlay = viewModel::toggleVideoPlayback,
                    onSceneSelected = viewModel::setVideoSceneIndex,
                    onSelectSavedProject = viewModel::selectSavedVideoProject,
                    onDeleteProject = viewModel::deleteVideoProject
                )
                3 -> ToolsScreen(
                    settingsState = settingsState,
                    personas = viewModel.personas,
                    currentPersonaId = chatState.selectedPersonaId,
                    onPersonaSelected = viewModel::onPersonaSelected,
                    onSpeechRateChanged = viewModel::setSpeechRate,
                    onSpeechPitchChanged = viewModel::setSpeechPitch,
                    onRefreshCacheSize = { viewModel.refreshCacheSize(viewModel.getApplication()) },
                    onClearCache = { viewModel.clearAppCache(viewModel.getApplication()) },
                    onClearAllHistory = viewModel::clearAllHistory,
                    onPromptSelected = { prompt ->
                        currentTab = 0
                        viewModel.onChatInputChanged(prompt)
                        viewModel.sendMessage()
                    },
                    onOpenImageStudioWithPrompt = { prompt ->
                        viewModel.onImagePromptChanged(prompt)
                        currentTab = 1
                    },
                    onOpenApiKeyDialog = { viewModel.showApiKeyDialog(true) },
                    onSelectPromptTemplate = { template ->
                        viewModel.applyPromptTemplate(template)
                        when (template.type) {
                            com.example.data.model.TemplateType.CHAT -> { currentTab = 0 }
                            com.example.data.model.TemplateType.IMAGE -> { currentTab = 1 }
                            com.example.data.model.TemplateType.VIDEO -> { currentTab = 2 }
                        }
                    }
                )
                4 -> HistoryScreen(
                    sessions = sessions,
                    images = savedImages,
                    videos = savedVideos,
                    onSelectSession = { session ->
                        viewModel.selectSession(session)
                        currentTab = 0
                    },
                    onDeleteSession = viewModel::deleteChatSession,
                    onOpenImagePreview = viewModel::openImagePreview,
                    onDeleteImage = viewModel::deleteImage,
                    onSelectVideo = { video ->
                        viewModel.selectSavedVideoProject(video)
                        currentTab = 2
                    },
                    onDeleteVideo = viewModel::deleteVideoProject,
                    onClearAll = viewModel::clearAllHistory
                )
            }
        }
    }

    if (settingsState.showApiKeyDialog) {
        ApiKeyDialog(
            initialKey = settingsState.customApiKey,
            onSave = viewModel::saveCustomApiKey,
            onDismiss = { viewModel.showApiKeyDialog(false) }
        )
    }
}
