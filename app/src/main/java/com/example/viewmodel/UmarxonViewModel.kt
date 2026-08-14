package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.speech.tts.TextToSpeech
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.ChatMessageEntity
import com.example.data.local.ChatSessionEntity
import com.example.data.local.GeneratedImageEntity
import com.example.data.local.VideoProjectEntity
import com.example.data.model.AiPersona
import com.example.data.model.VideoScene
import com.example.data.model.VideoScriptProject
import com.example.data.model.PromptCategory
import com.example.data.model.PromptGalleryData
import com.example.data.model.PromptTemplateItem
import com.example.data.model.TemplateType
import com.example.data.remote.UmarxonAiRepository
import com.example.util.FileUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.Locale

data class ChatState(
    val currentSessionId: Long? = null,
    val currentSessionTitle: String = "Yangi suhbat",
    val messages: List<ChatMessageEntity> = emptyList(),
    val inputText: String = "",
    val attachedImageBase64: String? = null,
    val attachedImageUri: Uri? = null,
    val attachedDocumentName: String? = null,
    val attachedDocumentPageCount: Int = 1,
    val selectedPersonaId: String = "universal",
    val isLoading: Boolean = false,
    val isSpeaking: Boolean = false,
    val speakingMessageId: Long? = null,
    val showPromptGallery: Boolean = false,
    val showAttachMenu: Boolean = false,
    val errorMessage: String? = null
)

data class ImageStudioState(
    val prompt: String = "",
    val selectedStyle: String = "Fotorealistik 8K",
    val selectedAspect: String = "1:1",
    val selectedEngine: String = "Imagen 3.0 Ultra (HD)",
    val negativePrompt: String = "",
    val showNegativePromptField: Boolean = false,
    val isEnhancingPrompt: Boolean = false,
    val isGenerating: Boolean = false,
    val currentGeneratedImage: GeneratedImageEntity? = null,
    val previewModalImage: GeneratedImageEntity? = null,
    val errorMessage: String? = null
)

data class VideoStudioState(
    val prompt: String = "",
    val selectedGenre: String = "Kinematik Blokbaster",
    val selectedAspect: String = "16:9",
    val isGenerating: Boolean = false,
    val currentProject: VideoScriptProject? = null,
    // Player State
    val isPlaying: Boolean = false,
    val activeSceneIndex: Int = 0,
    val sceneProgress: Float = 0f,
    val errorMessage: String? = null
)

data class AppSettingsState(
    val customApiKey: String = "",
    val selectedModel: String = "gemini-3.5-flash",
    val isDarkMode: Boolean = true,
    val speechRate: Float = 1.0f,
    val speechPitch: Float = 1.0f,
    val cacheSize: String = "0.0 MB",
    val showApiKeyDialog: Boolean = false,
    val feedbackMessage: String? = null
)

typealias SettingsState = AppSettingsState

class UmarxonViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getInstance(application)
    val repository = UmarxonAiRepository(database)

    private val sharedPrefs = application.getSharedPreferences("umarxon_ai_prefs", Context.MODE_PRIVATE)

    // TTS
    private var tts: TextToSpeech? = null
    private var isTtsInitialized = false

    // StateFlows
    private val _chatState = MutableStateFlow(ChatState())
    val chatState: StateFlow<ChatState> = _chatState.asStateFlow()

    private val _imageState = MutableStateFlow(ImageStudioState())
    val imageState: StateFlow<ImageStudioState> = _imageState.asStateFlow()

    private val _videoState = MutableStateFlow(VideoStudioState())
    val videoState: StateFlow<VideoStudioState> = _videoState.asStateFlow()

    private val _settingsState = MutableStateFlow(
        AppSettingsState(
            customApiKey = sharedPrefs.getString("custom_api_key", "") ?: "",
            speechRate = sharedPrefs.getFloat("speech_rate", 1.0f),
            speechPitch = sharedPrefs.getFloat("speech_pitch", 1.0f),
            cacheSize = FileUtils.getAppCacheSizeFormatted(application)
        )
    )
    val settingsState: StateFlow<AppSettingsState> = _settingsState.asStateFlow()

    // DB Flows
    val allSessions: StateFlow<List<ChatSessionEntity>> = repository.allSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allGeneratedImages: StateFlow<List<GeneratedImageEntity>> = repository.allGeneratedImages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allVideoProjects: StateFlow<List<VideoProjectEntity>> = repository.allVideoProjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var videoPlaybackJob: Job? = null
    private var messagesObserverJob: Job? = null

    val personas: List<AiPersona> get() = repository.personas

    val imagenEngines = listOf(
        "Imagen 3.0 Ultra (HD)",
        "Imagen 3.0 Fast"
    )

    val sampleImagePrompts = listOf(
        "Toshkent City 2050-yil, baland futuristik osmono'par binolar, neon chiroqlar va uchuvchi taksilar",
        "Registon maydoni Samarqand, oltin quyosh botishi, nozik sharqona naqshlar, 8k fotorealistik",
        "Amir Temur otliq lashkari bilan moviy gumbazlar fonida, kinematik yorug'lik va tarixiy drama",
        "Cyberpunk uslubidagi O'zbek milliy libosidagi qiz, yorqin kiber neon va hologramlar",
        "Sehrli qadimiy kutubxona, havoda uchib yurgan yorug' kitoblar va yulduzlar galaktikasi",
        "Cute 3D Pixar uslubida quvnoq qor qoploni bolasi qorli Tyan-Shan tog'larida"
    )

    val stylePresets = listOf(
        "Fotorealistik 8K",
        "Anime & Manga",
        "Kiberpank & Neon",
        "O'zbek Milliy San'at",
        "3D Pixar Uslubi",
        "Moybo'yoq & Akvarel",
        "Kinematik Portret",
        "Fantaziya & Kosmos"
    )

    val genrePresets = listOf(
        "Kinematik Blokbaster",
        "Hujjatli & Ilmiy",
        "Tijorat & Reklama",
        "Animatsion Hikoya",
        "Musiqiy Vizual Klip",
        "Tarixiy Drama"
    )

    init {
        initTts(application)
        initFirstSession()
    }

    private fun initTts(context: Context) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isTtsInitialized = true
                val result = tts?.setLanguage(Locale("uz"))
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.language = Locale.ENGLISH
                }
            }
        }
    }

    private fun initFirstSession() {
        viewModelScope.launch {
            val sessions = database.chatDao().getAllSessions()
            repository.allSessions.collect { list ->
                if (list.isNotEmpty() && _chatState.value.currentSessionId == null) {
                    selectSession(list.first())
                } else if (list.isEmpty() && _chatState.value.currentSessionId == null) {
                    val newId = repository.createNewSession("Assalomu alaykum!", "universal")
                    repository.saveMessage(
                        sessionId = newId,
                        role = "model",
                        text = "Assalomu alaykum! Men **UmarxonAI** — sizning eng aqlli universal sun'iy intellekt yordamchingizman. 🚀\n\nMen nimalar qila olaman?\n- 🧠 Har qanday savollarga chuqur va aniq javob berish\n- 💻 Dasturlash (Kotlin, Python, JS, AI modellar)\n- 🎨 Matndan ajoyib rasmlar yaratish\n- 🎬 Video ssenariylari va sahnalarini tayyorlash\n- ✍️ She'rlar, maqolalar va professional tarjimalar\n\nSizga qanday yordam bera olaman?"
                    )
                    _chatState.update { it.copy(currentSessionId = newId, currentSessionTitle = "Assalomu alaykum!") }
                    observeSessionMessages(newId)
                }
            }
        }
    }

    fun selectSession(session: ChatSessionEntity) {
        _chatState.update {
            it.copy(
                currentSessionId = session.id,
                currentSessionTitle = session.title,
                selectedPersonaId = session.personaId,
                errorMessage = null
            )
        }
        observeSessionMessages(session.id)
    }

    fun createNewChat() {
        viewModelScope.launch {
            stopSpeaking()
            val newId = repository.createNewSession("Yangi suhbat", _chatState.value.selectedPersonaId)
            repository.saveMessage(
                sessionId = newId,
                role = "model",
                text = "Yangi suhbat boshlandi. Qanday savol yoki g'oyangiz bor? 😊"
            )
            _chatState.update {
                it.copy(
                    currentSessionId = newId,
                    currentSessionTitle = "Yangi suhbat",
                    inputText = "",
                    attachedImageBase64 = null,
                    attachedImageUri = null,
                    errorMessage = null
                )
            }
            observeSessionMessages(newId)
        }
    }

    private fun observeSessionMessages(sessionId: Long) {
        messagesObserverJob?.cancel()
        messagesObserverJob = viewModelScope.launch {
            repository.getSessionMessages(sessionId).collect { list ->
                _chatState.update { it.copy(messages = list) }
            }
        }
    }

    fun onChatInputChanged(text: String) {
        _chatState.update { it.copy(inputText = text) }
    }

    fun onPersonaSelected(personaId: String) {
        _chatState.update { it.copy(selectedPersonaId = personaId) }
    }

    fun attachImage(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                val out = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 75, out)
                val base64 = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
                _chatState.update {
                    it.copy(
                        attachedImageBase64 = base64,
                        attachedImageUri = uri,
                        attachedDocumentName = "Rasm (OCR / Tahlil)",
                        showAttachMenu = false
                    )
                }
            } catch (e: Exception) {
                _chatState.update { it.copy(errorMessage = "Rasmni yuklashda xatolik: ${e.localizedMessage}") }
            }
        }
    }

    fun attachDocument(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val docResult = FileUtils.processPickedDocument(context, uri)
                if (docResult != null) {
                    if (docResult.pageImageBase64 != null) {
                        _chatState.update {
                            it.copy(
                                attachedImageBase64 = docResult.pageImageBase64,
                                attachedImageUri = uri,
                                attachedDocumentName = "${docResult.documentName} (${docResult.pageCount} bet)",
                                attachedDocumentPageCount = docResult.pageCount,
                                showAttachMenu = false
                            )
                        }
                    } else if (!docResult.extractedText.isNullOrBlank()) {
                        val currentText = _chatState.value.inputText
                        val prefix = if (currentText.isBlank()) "Quyidagi hujjat matnini tahlil qilib, qisqacha xulosasini ber:\n\n" else "$currentText\n\n"
                        _chatState.update {
                            it.copy(
                                inputText = prefix + "--- Hujjat: ${docResult.documentName} ---\n" + docResult.extractedText,
                                attachedDocumentName = docResult.documentName,
                                showAttachMenu = false
                            )
                        }
                    }
                } else {
                    _chatState.update { it.copy(errorMessage = "Hujjatni o'qib bo'lmadi") }
                }
            } catch (e: Exception) {
                _chatState.update { it.copy(errorMessage = "Hujjat yuklashda xatolik: ${e.localizedMessage}") }
            }
        }
    }

    fun removeAttachedImage() {
        _chatState.update {
            it.copy(
                attachedImageBase64 = null,
                attachedImageUri = null,
                attachedDocumentName = null
            )
        }
    }

    fun appendSpeechText(spokenText: String) {
        if (spokenText.isBlank()) return
        _chatState.update {
            val current = it.inputText.trim()
            val newText = if (current.isEmpty()) spokenText else "$current $spokenText"
            it.copy(inputText = newText)
        }
    }

    fun togglePromptGallery(show: Boolean) {
        _chatState.update { it.copy(showPromptGallery = show) }
    }

    fun toggleAttachMenu(show: Boolean) {
        _chatState.update { it.copy(showAttachMenu = show) }
    }

    fun applyPromptTemplate(template: PromptTemplateItem) {
        when (template.type) {
            TemplateType.CHAT -> {
                _chatState.update {
                    it.copy(
                        inputText = template.prompt,
                        showPromptGallery = false
                    )
                }
            }
            TemplateType.IMAGE -> {
                _imageState.update {
                    it.copy(prompt = template.prompt)
                }
                _chatState.update { it.copy(showPromptGallery = false) }
            }
            TemplateType.VIDEO -> {
                _videoState.update {
                    it.copy(prompt = template.prompt)
                }
                _chatState.update { it.copy(showPromptGallery = false) }
            }
        }
    }

    fun sendMessage() {
        val state = _chatState.value
        val text = state.inputText.trim()
        val imageBase64 = state.attachedImageBase64
        val sessionId = state.currentSessionId ?: return

        if (text.isBlank() && imageBase64 == null) return

        viewModelScope.launch {
            _chatState.update {
                it.copy(
                    inputText = "",
                    attachedImageBase64 = null,
                    attachedImageUri = null,
                    isLoading = true,
                    errorMessage = null
                )
            }

            // Save user turn in DB
            repository.saveMessage(sessionId, "user", text, imageBase64)

            val currentHistory = state.messages
            val result = repository.sendChatMessage(
                sessionId = sessionId,
                userPrompt = text,
                imageBase64 = imageBase64,
                history = currentHistory,
                personaId = state.selectedPersonaId,
                customApiKey = _settingsState.value.customApiKey
            )

            result.onSuccess {
                _chatState.update { it.copy(isLoading = false) }
            }.onFailure { err ->
                _chatState.update { it.copy(isLoading = false, errorMessage = err.localizedMessage) }
            }
        }
    }

    // TTS methods
    fun toggleSpeakMessage(message: ChatMessageEntity) {
        if (_chatState.value.isSpeaking && _chatState.value.speakingMessageId == message.id) {
            stopSpeaking()
        } else {
            speakText(message.id, message.text)
        }
    }

    private fun speakText(messageId: Long, rawText: String) {
        if (!isTtsInitialized) return
        val cleanText = rawText.replace(Regex("[*#_`>]"), "").trim()
        tts?.stop()
        tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, "msg_$messageId")
        _chatState.update { it.copy(isSpeaking = true, speakingMessageId = messageId) }
    }

    fun stopSpeaking() {
        tts?.stop()
        _chatState.update { it.copy(isSpeaking = false, speakingMessageId = null) }
    }

    // Image Studio Actions
    fun onImagePromptChanged(text: String) {
        _imageState.update { it.copy(prompt = text) }
    }

    fun onImageStyleSelected(style: String) {
        _imageState.update { it.copy(selectedStyle = style) }
    }

    fun onImageAspectSelected(aspect: String) {
        _imageState.update { it.copy(selectedAspect = aspect) }
    }

    fun onImageEngineSelected(engine: String) {
        _imageState.update { it.copy(selectedEngine = engine) }
    }

    fun onNegativePromptChanged(text: String) {
        _imageState.update { it.copy(negativePrompt = text) }
    }

    fun toggleNegativePromptField() {
        _imageState.update { it.copy(showNegativePromptField = !it.showNegativePromptField) }
    }

    fun enhanceImagePrompt() {
        val state = _imageState.value
        if (state.prompt.isBlank()) return
        viewModelScope.launch {
            _imageState.update { it.copy(isEnhancingPrompt = true) }
            val enhanced = repository.enhanceImagePrompt(
                rawPrompt = state.prompt,
                style = state.selectedStyle,
                customApiKey = _settingsState.value.customApiKey
            )
            _imageState.update { it.copy(prompt = enhanced, isEnhancingPrompt = false) }
        }
    }

    fun generateImage() {
        val state = _imageState.value
        if (state.prompt.isBlank()) return
        viewModelScope.launch {
            _imageState.update { it.copy(isGenerating = true, errorMessage = null) }
            val result = repository.generateImage(
                prompt = state.prompt,
                style = state.selectedStyle,
                aspectRatio = state.selectedAspect,
                negativePrompt = if (state.showNegativePromptField && state.negativePrompt.isNotBlank()) state.negativePrompt else null,
                engine = state.selectedEngine,
                customApiKey = _settingsState.value.customApiKey
            )
            result.onSuccess { img ->
                _imageState.update {
                    it.copy(
                        isGenerating = false,
                        currentGeneratedImage = img,
                        previewModalImage = img
                    )
                }
            }.onFailure { err ->
                _imageState.update { it.copy(isGenerating = false, errorMessage = err.localizedMessage) }
            }
        }
    }

    fun generateImagenImageFromChat(rawPrompt: String) {
        val sessionId = _chatState.value.currentSessionId ?: return
        val cleanPrompt = rawPrompt.removePrefix("/imagine").removePrefix("/imagen").removePrefix("rasm:").trim()
        if (cleanPrompt.isBlank()) return

        viewModelScope.launch {
            _chatState.update { it.copy(isLoading = true, errorMessage = null) }
            // Save user prompt in chat
            repository.saveMessage(sessionId, "user", "🎨 [Imagen 3.0]: $cleanPrompt")

            val result = repository.generateImage(
                prompt = cleanPrompt,
                style = "Fotorealistik 8K",
                aspectRatio = "1:1",
                negativePrompt = null,
                engine = "Imagen 3.0 Ultra (HD)",
                customApiKey = _settingsState.value.customApiKey
            )

            result.onSuccess { img ->
                // Save assistant message with image in chat
                repository.saveMessage(
                    sessionId = sessionId,
                    role = "model",
                    text = "🎨 **Google Imagen 3.0** yordamida rasm muvaffaqiyatli yaratildi!\n\n**Prompt:** *${img.prompt}*\n**Uslub:** *${img.style}* (${img.aspectRatio})",
                    imageBase64 = img.imageBase64
                )
                _chatState.update { it.copy(isLoading = false) }
            }.onFailure { err ->
                _chatState.update { it.copy(isLoading = false, errorMessage = err.localizedMessage) }
            }
        }
    }

    fun generateVideoFromChat(rawPrompt: String, genre: String = "Kinematik Blokbaster") {
        val sessionId = _chatState.value.currentSessionId ?: return
        val cleanPrompt = rawPrompt.removePrefix("/video").removePrefix("/kino").removePrefix("video:").trim()
        if (cleanPrompt.isBlank()) return

        viewModelScope.launch {
            _chatState.update { it.copy(isLoading = true, errorMessage = null) }
            repository.saveMessage(sessionId, "user", "🎬 [AI Video Rejissyor]: $cleanPrompt")

            val result = repository.generateVideoProject(
                userPrompt = cleanPrompt,
                genre = genre,
                aspectRatio = "16:9",
                customApiKey = _settingsState.value.customApiKey
            )

            result.onSuccess { proj ->
                val scenesSummary = StringBuilder()
                scenesSummary.append("🎬 **UmarxonAI & Google Veo Video Rejissyor** loyihasi tayyor!\n\n")
                scenesSummary.append("**Nomi:** ${proj.title}\n")
                scenesSummary.append("**Janr:** ${proj.genre} | **Davomiyligi:** ${proj.totalDurationSeconds} soniya\n\n")
                scenesSummary.append("### 🎥 Sahnalar ketma-ketligi:\n")

                proj.scenes.forEach { scene ->
                    scenesSummary.append("--- \n")
                    scenesSummary.append("**Sahna ${scene.sceneNumber}: ${scene.title}** (${scene.durationSeconds}s)\n")
                    scenesSummary.append("• **Kamera:** ${scene.cameraAngle}\n")
                    scenesSummary.append("• **Yorug'lik:** ${scene.lighting}\n")
                    scenesSummary.append("• **Vizual:** ${scene.visualPrompt}\n")
                    if (scene.narration.isNotBlank()) {
                        scenesSummary.append("• 🎙️ **Diktor matni:** *\"${scene.narration}\"*\n")
                    }
                    scenesSummary.append("\n")
                }

                scenesSummary.append("💡 *Ushbu loyihani to'liq ekranda ko'rish va sahnalarni ijro etish uchun pastdagi 'Video' bo'limiga o'tishingiz mumkin.*")

                val firstSceneImage = proj.scenes.firstOrNull()?.imageBase64

                repository.saveMessage(
                    sessionId = sessionId,
                    role = "model",
                    text = scenesSummary.toString(),
                    imageBase64 = firstSceneImage
                )

                _videoState.update {
                    it.copy(
                        currentProject = proj,
                        activeSceneIndex = 0,
                        isPlaying = false
                    )
                }

                _chatState.update { it.copy(isLoading = false) }
            }.onFailure { err ->
                _chatState.update { it.copy(isLoading = false, errorMessage = err.localizedMessage) }
            }
        }
    }

    fun openImagePreview(image: GeneratedImageEntity?) {
        _imageState.update { it.copy(previewModalImage = image) }
    }

    fun deleteImage(id: Long) {
        viewModelScope.launch {
            repository.deleteImage(id)
            if (_imageState.value.previewModalImage?.id == id) {
                _imageState.update { it.copy(previewModalImage = null) }
            }
        }
    }

    // Video Studio Actions
    fun onVideoPromptChanged(text: String) {
        _videoState.update { it.copy(prompt = text) }
    }

    fun onVideoGenreSelected(genre: String) {
        _videoState.update { it.copy(selectedGenre = genre) }
    }

    fun onVideoAspectSelected(aspect: String) {
        _videoState.update { it.copy(selectedAspect = aspect) }
    }

    fun generateVideoDirectorProject() {
        val state = _videoState.value
        if (state.prompt.isBlank()) return
        viewModelScope.launch {
            stopVideoPlayback()
            _videoState.update { it.copy(isGenerating = true, errorMessage = null) }
            val result = repository.generateVideoProject(
                userPrompt = state.prompt,
                genre = state.selectedGenre,
                aspectRatio = state.selectedAspect,
                customApiKey = _settingsState.value.customApiKey
            )
            result.onSuccess { project ->
                _videoState.update {
                    it.copy(
                        isGenerating = false,
                        currentProject = project,
                        activeSceneIndex = 0,
                        sceneProgress = 0f
                    )
                }
                startVideoPlayback()
            }.onFailure { err ->
                _videoState.update { it.copy(isGenerating = false, errorMessage = err.localizedMessage) }
            }
        }
    }

    fun selectSavedVideoProject(projectEntity: VideoProjectEntity) {
        stopVideoPlayback()
        val scenes = repository.deserializeScenes(projectEntity.scenesJson)
        val project = VideoScriptProject(
            id = projectEntity.id,
            title = projectEntity.title,
            userPrompt = projectEntity.userPrompt,
            genre = projectEntity.genre,
            aspectRatio = projectEntity.aspectRatio,
            totalDurationSeconds = projectEntity.totalDurationSeconds,
            scenes = scenes,
            timestamp = projectEntity.timestamp
        )
        _videoState.update {
            it.copy(
                currentProject = project,
                activeSceneIndex = 0,
                sceneProgress = 0f
            )
        }
    }

    fun toggleVideoPlayback() {
        if (_videoState.value.isPlaying) {
            stopVideoPlayback()
        } else {
            startVideoPlayback()
        }
    }

    fun startVideoPlayback() {
        val project = _videoState.value.currentProject ?: return
        if (project.scenes.isEmpty()) return

        videoPlaybackJob?.cancel()
        _videoState.update { it.copy(isPlaying = true) }

        videoPlaybackJob = viewModelScope.launch {
            val scenes = project.scenes
            var currentIdx = _videoState.value.activeSceneIndex
            if (currentIdx >= scenes.size) currentIdx = 0

            while (_videoState.value.isPlaying) {
                val currentScene = scenes[currentIdx]
                _videoState.update { it.copy(activeSceneIndex = currentIdx, sceneProgress = 0f) }

                // Play scene narration via TTS if available
                if (currentScene.narration.isNotBlank() && isTtsInitialized) {
                    tts?.stop()
                    tts?.speak(currentScene.narration, TextToSpeech.QUEUE_FLUSH, null, "scene_$currentIdx")
                }

                val totalSteps = (currentScene.durationSeconds * 20).coerceAtLeast(20)
                for (step in 0..totalSteps) {
                    if (!_videoState.value.isPlaying) break
                    val progress = step.toFloat() / totalSteps.toFloat()
                    _videoState.update { it.copy(sceneProgress = progress) }
                    delay(50)
                }

                if (!_videoState.value.isPlaying) break

                currentIdx++
                if (currentIdx >= scenes.size) {
                    currentIdx = 0
                    // Loop or stop after full run
                    delay(500)
                }
            }
        }
    }

    fun stopVideoPlayback() {
        videoPlaybackJob?.cancel()
        tts?.stop()
        _videoState.update { it.copy(isPlaying = false) }
    }

    fun setVideoSceneIndex(index: Int) {
        val project = _videoState.value.currentProject ?: return
        if (index in project.scenes.indices) {
            stopVideoPlayback()
            _videoState.update { it.copy(activeSceneIndex = index, sceneProgress = 0f) }
        }
    }

    fun deleteVideoProject(id: Long) {
        viewModelScope.launch {
            repository.deleteVideoProject(id)
            if (_videoState.value.currentProject?.id == id) {
                stopVideoPlayback()
                _videoState.update { it.copy(currentProject = null) }
            }
        }
    }

    // Settings & Customization
    fun saveCustomApiKey(key: String) {
        sharedPrefs.edit().putString("custom_api_key", key).apply()
        _settingsState.update { it.copy(customApiKey = key, feedbackMessage = "API kaliti saqlandi!") }
    }

    fun setSpeechRate(rate: Float) {
        sharedPrefs.edit().putFloat("speech_rate", rate).apply()
        tts?.setSpeechRate(rate)
        _settingsState.update { it.copy(speechRate = rate) }
    }

    fun setSpeechPitch(pitch: Float) {
        sharedPrefs.edit().putFloat("speech_pitch", pitch).apply()
        tts?.setPitch(pitch)
        _settingsState.update { it.copy(speechPitch = pitch) }
    }

    fun refreshCacheSize(context: Context) {
        val formatted = FileUtils.getAppCacheSizeFormatted(context)
        _settingsState.update { it.copy(cacheSize = formatted) }
    }

    fun clearAppCache(context: Context) {
        FileUtils.clearAppCache(context)
        refreshCacheSize(context)
        _settingsState.update { it.copy(feedbackMessage = "Kesh tozalandi!") }
    }

    fun saveImageToGallery(context: Context, image: GeneratedImageEntity) {
        try {
            val bytes = Base64.decode(image.imageBase64, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            if (bitmap != null) {
                FileUtils.saveBitmapToGallery(context, bitmap, "UmarxonAI_${image.style}")
            }
        } catch (e: Exception) {
            _imageState.update { it.copy(errorMessage = "Saqlashda xatolik: ${e.localizedMessage}") }
        }
    }

    fun shareImage(context: Context, image: GeneratedImageEntity) {
        try {
            val bytes = Base64.decode(image.imageBase64, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            if (bitmap != null) {
                FileUtils.shareBitmap(context, bitmap, "🎨 UmarxonAI orqali yaratildi:\n\"${image.prompt}\"")
            }
        } catch (e: Exception) {
            _imageState.update { it.copy(errorMessage = "Ulashishda xatolik: ${e.localizedMessage}") }
        }
    }

    fun exportMessageToPdf(context: Context, message: ChatMessageEntity) {
        val title = if (message.role == "user") "Foydalanuvchi Savoli" else "UmarxonAI Javobi"
        FileUtils.exportTextToPdf(context, title, message.text)
    }

    fun shareChatMessage(context: Context, message: ChatMessageEntity) {
        val prefix = if (message.role == "user") "👤 Savol:\n" else "🤖 UmarxonAI Javobi:\n"
        FileUtils.shareText(context, "$prefix${message.text}", "UmarxonAI Muloqot")
    }

    fun shareVideoProject(context: Context, project: VideoScriptProject) {
        val formatted = buildString {
            appendLine("🎬 UmarxonAI Video Loyihasi: ${project.title}")
            appendLine("🎭 Janr: ${project.genre} | Nisbat: ${project.aspectRatio} | Davomiyligi: ${project.totalDurationSeconds}s")
            appendLine()
            project.scenes.forEachIndexed { i, scene ->
                appendLine("Sahna ${i + 1}: ${scene.title} (${scene.durationSeconds}s)")
                appendLine("📷 Vizual: ${scene.visualPrompt}")
                appendLine("🎙️ Diktor: \"${scene.narration}\"")
                appendLine()
            }
        }
        FileUtils.shareText(context, formatted, project.title)
    }

    fun showApiKeyDialog(show: Boolean) {
        _settingsState.update { it.copy(showApiKeyDialog = show) }
    }

    fun deleteChatSession(sessionId: Long) {
        viewModelScope.launch {
            repository.deleteSession(sessionId)
            if (_chatState.value.currentSessionId == sessionId) {
                val remaining = database.chatDao().getAllSessions()
                _chatState.update { it.copy(currentSessionId = null) }
            }
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAllHistory()
            createNewChat()
        }
    }

    override fun onCleared() {
        super.onCleared()
        videoPlaybackJob?.cancel()
        tts?.stop()
        tts?.shutdown()
    }
}
