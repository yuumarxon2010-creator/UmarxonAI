package com.example.ui.screens.chat

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.speech.RecognizerIntent
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.ChatMessageEntity
import com.example.data.model.AiPersona
import com.example.data.model.PromptTemplateItem
import com.example.ui.components.MarkdownContent
import com.example.ui.components.PersonaSelectorRow
import com.example.ui.components.PromptGalleryBottomSheet
import com.example.ui.components.ThinkingPulseIndicator
import com.example.ui.components.copyToClipboard
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.VioletSecondary
import com.example.util.FileUtils
import com.example.viewmodel.ChatState

@Composable
fun ChatScreen(
    state: ChatState,
    personas: List<AiPersona>,
    onInputChanged: (String) -> Unit,
    onSendMessage: () -> Unit,
    onPersonaSelected: (String) -> Unit,
    onAttachImage: (Uri) -> Unit,
    onAttachDocument: (Uri) -> Unit,
    onRemoveAttachment: () -> Unit,
    onNewChat: () -> Unit,
    onToggleSpeak: (ChatMessageEntity) -> Unit,
    onQuickPromptClick: (String) -> Unit,
    onSpeechRecognized: (String) -> Unit,
    onSelectPromptTemplate: (PromptTemplateItem) -> Unit,
    onTogglePromptGallery: (Boolean) -> Unit,
    onToggleAttachMenu: (Boolean) -> Unit,
    onExportPdf: (ChatMessageEntity) -> Unit,
    onShareMessage: (ChatMessageEntity) -> Unit,
    onGenerateImagen: (String) -> Unit = {},
    onGenerateVideo: (String, String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val context = LocalContext.current

    var showImagenDialog by remember { mutableStateOf(false) }
    var imagenPromptInput by remember { mutableStateOf("") }

    var showVideoDialog by remember { mutableStateOf(false) }
    var videoPromptInput by remember { mutableStateOf("") }
    var selectedVideoGenre by remember { mutableStateOf("Kinematik Blokbaster") }

    val videoGenres = listOf(
        "Kinematik Blokbaster",
        "Kiberpank & Sci-Fi",
        "Tarixiy & Hujjatli",
        "3D Multfilm (Pixar)",
        "Tabiat va Sayohat"
    )

    // Image Picker
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { onAttachImage(it) }
    }

    // Document/PDF Picker
    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { onAttachDocument(it) }
    }

    // Speech-to-Text Recognizer Launcher
    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenMatches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!spokenMatches.isNullOrEmpty()) {
                val spokenText = spokenMatches[0]
                onSpeechRecognized(spokenText)
                Toast.makeText(context, "Ovoz qabul qilindi: $spokenText", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun launchSpeechRecognition() {
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "uz-UZ")
                putExtra(RecognizerIntent.EXTRA_PROMPT, "UmarxonAI tinglamoqda... Savolingizni gapiring")
            }
            speechRecognizerLauncher.launch(intent)
        } catch (e: Exception) {
            // Fallback to default locale
            try {
                val fallback = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                }
                speechRecognizerLauncher.launch(fallback)
            } catch (ex: Exception) {
                Toast.makeText(context, "Qurilmangizda ovozli kiritish qo'llab-quvvatlanmaydi", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(state.messages.size, state.isLoading) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // IQ 160-180 Banner & Persona Selector
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(IndigoPrimary, CyanAccent))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "UmarxonAI Core",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = EmeraldSuccess.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldSuccess)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "IQ 160-180 Daho",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldSuccess
                            )
                        }
                    }

                    // Prompt Gallery shortcut button
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = IndigoPrimary.copy(alpha = 0.12f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onTogglePromptGallery(true) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lightbulb,
                                contentDescription = null,
                                tint = IndigoPrimary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Promptlar",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = IndigoPrimary
                            )
                        }
                    }
                }
            }

            PersonaSelectorRow(
                personas = personas,
                selectedPersonaId = state.selectedPersonaId,
                onSelect = onPersonaSelected,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }

        // Chat Message List
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (state.messages.isEmpty()) {
                EmptyChatGreeting(
                    onQuickPrompt = onQuickPromptClick,
                    onOpenImagenDialog = { showImagenDialog = true },
                    onOpenVideoDialog = { showVideoDialog = true },
                    onOpenPromptGallery = { onTogglePromptGallery(true) },
                    onAttachPdf = { documentPickerLauncher.launch("application/pdf") },
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item { Spacer(modifier = Modifier.height(6.dp)) }

                    items(state.messages, key = { it.id }) { msg ->
                        ChatMessageBubble(
                            message = msg,
                            isSpeaking = state.isSpeaking && state.speakingMessageId == msg.id,
                            onToggleSpeak = { onToggleSpeak(msg) },
                            onCopy = {
                                copyToClipboard(context, msg.text)
                                Toast.makeText(context, "Nusxa olindi", Toast.LENGTH_SHORT).show()
                            },
                            onExportPdf = { onExportPdf(msg) },
                            onShare = { onShareMessage(msg) }
                        )
                    }

                    if (state.isLoading) {
                        item {
                            ThinkingPulseIndicator(
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }
                    }

                    if (state.errorMessage != null) {
                        item {
                            ErrorMessageCard(message = state.errorMessage)
                        }
                    }

                    item { Spacer(modifier = Modifier.height(12.dp)) }
                }
            }
        }

        // Document / Image Attachment Preview Strip
        AnimatedVisibility(
            visible = state.attachedImageUri != null || state.attachedImageBase64 != null || state.attachedDocumentName != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (state.attachedImageUri != null) {
                    AsyncImage(
                        model = state.attachedImageUri,
                        contentDescription = "Biriktirilgan rasm",
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(IndigoPrimary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = IndigoPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = state.attachedDocumentName ?: "Rasm biriktirildi",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    Text(
                        text = "OCR tahlil va savollarga javob berishga tayyor",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
                IconButton(onClick = onRemoveAttachment) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "O'chirish")
                }
            }
        }

        // Bottom Input Row with Speech, Camera/Gallery, Document, Prompt Hub & Send
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 8.dp)
                    .navigationBarsPadding()
                    .imePadding(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // New chat button
                IconButton(
                    onClick = onNewChat,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("new_chat_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Yangi suhbat",
                        tint = IndigoPrimary
                    )
                }

                // Attachments Menu Button (+/Clip)
                Box {
                    IconButton(
                        onClick = { onToggleAttachMenu(true) },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("attach_menu_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AttachFile,
                            contentDescription = "Fayl va Rasm biriktirish",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    DropdownMenu(
                        expanded = state.showAttachMenu,
                        onDismissRequest = { onToggleAttachMenu(false) }
                    ) {
                        DropdownMenuItem(
                            text = { Text("🖼️ Rasm yuklash (OCR / Masala)") },
                            onClick = {
                                onToggleAttachMenu(false)
                                imagePickerLauncher.launch("image/*")
                            },
                            leadingIcon = { Icon(Icons.Default.Image, contentDescription = null, tint = CyanAccent) }
                        )
                        DropdownMenuItem(
                            text = { Text("📄 PDF / Hujjat tahlili") },
                            onClick = {
                                onToggleAttachMenu(false)
                                documentPickerLauncher.launch("application/pdf")
                            },
                            leadingIcon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = AmberAccent) }
                        )
                        DropdownMenuItem(
                            text = { Text("💡 Tayyor Promptlar Galeriyasi") },
                            onClick = {
                                onToggleAttachMenu(false)
                                onTogglePromptGallery(true)
                            },
                            leadingIcon = { Icon(Icons.Default.Lightbulb, contentDescription = null, tint = IndigoPrimary) }
                        )
                        DropdownMenuItem(
                            text = { Text("🎨 Google Imagen 3.0") },
                            onClick = {
                                onToggleAttachMenu(false)
                                showImagenDialog = true
                            },
                            leadingIcon = { Icon(Icons.Default.Brush, contentDescription = null, tint = CyanAccent) }
                        )
                        DropdownMenuItem(
                            text = { Text("🎬 AI Video Rejissyor") },
                            onClick = {
                                onToggleAttachMenu(false)
                                showVideoDialog = true
                            },
                            leadingIcon = { Icon(Icons.Default.Movie, contentDescription = null, tint = AmberAccent) }
                        )
                    }
                }

                // Microphone (Speech-to-Text) Button
                IconButton(
                    onClick = { launchSpeechRecognition() },
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("speech_to_text_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Ovozli savol berish",
                        tint = if (state.inputText.isEmpty()) IndigoPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Text field
                OutlinedTextField(
                    value = state.inputText,
                    onValueChange = onInputChanged,
                    placeholder = {
                        Text(
                            text = "Ovoz bering, yozing yoki fayl biriktiring...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 2.dp)
                        .testTag("chat_input_field"),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = IndigoPrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        val input = state.inputText.trim()
                        when {
                            input.startsWith("/imagine") || input.startsWith("/imagen") -> {
                                onGenerateImagen(input)
                                onInputChanged("")
                            }
                            input.startsWith("/video") || input.startsWith("/kino") -> {
                                onGenerateVideo(input, "Kinematik Blokbaster")
                                onInputChanged("")
                            }
                            else -> {
                                onSendMessage()
                            }
                        }
                    })
                )

                // Send Button
                val isSendActive = state.inputText.isNotBlank() || state.attachedImageBase64 != null
                IconButton(
                    onClick = {
                        val input = state.inputText.trim()
                        when {
                            input.startsWith("/imagine") || input.startsWith("/imagen") -> {
                                onGenerateImagen(input)
                                onInputChanged("")
                            }
                            input.startsWith("/video") || input.startsWith("/kino") -> {
                                onGenerateVideo(input, "Kinematik Blokbaster")
                                onInputChanged("")
                            }
                            else -> {
                                onSendMessage()
                            }
                        }
                    },
                    enabled = isSendActive && !state.isLoading,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSendActive) {
                                Brush.linearGradient(listOf(IndigoPrimary, VioletSecondary))
                            } else {
                                Brush.linearGradient(listOf(Color.Gray.copy(alpha = 0.3f), Color.Gray.copy(alpha = 0.3f)))
                            }
                        )
                        .testTag("send_button")
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Yuborish",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }

    // Prompt Gallery Modal Sheet
    if (state.showPromptGallery) {
        PromptGalleryBottomSheet(
            onSelectTemplate = onSelectPromptTemplate,
            onDismiss = { onTogglePromptGallery(false) }
        )
    }

    // Video Director Quick Dialog in Chat
    if (showVideoDialog) {
        AlertDialog(
            onDismissRequest = { showVideoDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Movie, contentDescription = null, tint = AmberAccent)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("AI Video Rejissyor", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column {
                    Text(
                        text = "Video g'oyangizni yozing, UmarxonAI kinematik 4 sahnali ssenariy va vizual kadrlarni shu yerda yaratadi:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )

                    OutlinedTextField(
                        value = videoPromptInput,
                        onValueChange = { videoPromptInput = it },
                        placeholder = { Text("Masalan: Amir Temur saltanati haqida epik hujjatli film...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        minLines = 3,
                        maxLines = 4
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Janr tanlang:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    val genreScroll = rememberScrollState()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(genreScroll),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        videoGenres.forEach { genre ->
                            val isSelected = genre == selectedVideoGenre
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) AmberAccent else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { selectedVideoGenre = genre }
                            ) {
                                Text(
                                    text = genre,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (videoPromptInput.isNotBlank()) {
                            onGenerateVideo(videoPromptInput, selectedVideoGenre)
                            videoPromptInput = ""
                            showVideoDialog = false
                        }
                    },
                    enabled = videoPromptInput.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = AmberAccent)
                ) {
                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Video Rejissura Yaratish", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showVideoDialog = false }) {
                    Text("Bekor qilish")
                }
            }
        )
    }

    // Imagen Quick Prompt Dialog in Chat
    if (showImagenDialog) {
        AlertDialog(
            onDismissRequest = { showImagenDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Brush, contentDescription = null, tint = CyanAccent)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Google Imagen 3.0 Tool", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column {
                    Text(
                        text = "Tasvir tavsifini yozing, UmarxonAI uni Imagen 3.0 orqali shu chatda yaratib beradi:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    OutlinedTextField(
                        value = imagenPromptInput,
                        onValueChange = { imagenPromptInput = it },
                        placeholder = { Text("Masalan: Samarqand Registon maydoni kiberpank uslubida...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        minLines = 3,
                        maxLines = 4
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (imagenPromptInput.isNotBlank()) {
                            onGenerateImagen(imagenPromptInput)
                            imagenPromptInput = ""
                            showImagenDialog = false
                        }
                    },
                    enabled = imagenPromptInput.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
                ) {
                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Imagen Bilan Chizish")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImagenDialog = false }) {
                    Text("Bekor qilish")
                }
            }
        )
    }
}

@Composable
fun ChatMessageBubble(
    message: ChatMessageEntity,
    isSpeaking: Boolean,
    onToggleSpeak: () -> Unit,
    onCopy: () -> Unit,
    onExportPdf: () -> Unit,
    onShare: () -> Unit
) {
    val isUser = message.role == "user"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(IndigoPrimary, VioletSecondary)))
                    .padding(2.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Psychology,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Card(
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (isUser) 18.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 18.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) {
                    IndigoPrimary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            ),
            modifier = Modifier
                .widthIn(max = 330.dp)
                .border(
                    width = if (isUser) 0.dp else 1.dp,
                    color = if (isUser) Color.Transparent else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomStart = if (isUser) 18.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 18.dp
                    )
                )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Attached or Generated Image / Video Keyframe
                if (!message.imageBase64.isNullOrBlank()) {
                    val bitmap = remember(message.imageBase64) {
                        try {
                            val bytes = Base64.decode(message.imageBase64, Base64.DEFAULT)
                            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Vizual Kadr",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                if (isUser) {
                    Text(
                        text = message.text,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    MarkdownContent(text = message.text)

                    Spacer(modifier = Modifier.height(10.dp))

                    // AI Message Actions Row (TTS, PDF Export, Share, Copy)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Text-to-Speech Button
                        IconButton(
                            onClick = onToggleSpeak,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (isSpeaking) Icons.Default.Stop else Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = if (isSpeaking) "To'xtatish" else "Ovozli o'qish",
                                tint = if (isSpeaking) VioletSecondary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Export to PDF Button
                        IconButton(
                            onClick = onExportPdf,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = "PDF yuklab olish",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(17.dp)
                            )
                        }

                        // Share to Telegram/Instagram Button
                        IconButton(
                            onClick = onShare,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Ulashish",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Copy to clipboard
                        IconButton(
                            onClick = onCopy,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Nusxa olish",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyChatGreeting(
    onQuickPrompt: (String) -> Unit,
    onOpenImagenDialog: () -> Unit,
    onOpenVideoDialog: () -> Unit,
    onOpenPromptGallery: () -> Unit,
    onAttachPdf: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(68.dp)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(CyanAccent.copy(alpha = 0.3f), Color.Transparent)))
                .border(2.dp, IndigoPrimary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Psychology,
                contentDescription = null,
                tint = IndigoPrimary,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "UmarxonAI Universal Intellekt",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = "IQ 160-180 daho mantiq, Ovozli muloqot, PDF/OCR tahlil va Imagen 3.0",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp, bottom = 12.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        // 4 Action Hub Cards (Promptlar, PDF Tahlil, Imagen, Video)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = IndigoPrimary.copy(alpha = 0.15f),
                border = androidx.compose.foundation.BorderStroke(1.dp, IndigoPrimary.copy(alpha = 0.4f)),
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onOpenPromptGallery() }
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Icon(imageVector = Icons.Default.Lightbulb, contentDescription = null, tint = IndigoPrimary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "💡 Promptlar", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = IndigoPrimary)
                    Text(text = "Tayyor shablon", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = CyanAccent.copy(alpha = 0.15f),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyanAccent.copy(alpha = 0.4f)),
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onOpenImagenDialog() }
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Icon(imageVector = Icons.Default.Brush, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "🎨 Imagen 3.0", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = CyanAccent)
                    Text(text = "Rasm chizish", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = AmberAccent.copy(alpha = 0.15f),
                border = androidx.compose.foundation.BorderStroke(1.dp, AmberAccent.copy(alpha = 0.4f)),
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onAttachPdf() }
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null, tint = AmberAccent, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "📄 PDF Tahlil", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = AmberAccent)
                    Text(text = "Hujjat o'qish", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Suggested Prompt Questions
        val prompts = listOf(
            "🌟 Seni kim yaratgan?",
            "🧠 Kvant kompyuterlari klassik kompyuterlardan qanday ustun? (IQ 180 tahlil)",
            "💻 Kotlin Coroutines va Flow qanday ishlaydi? To'liq kodli arxitektura namunasi",
            "🎬 /video Kosmosga birinchi parvoz va yulduzlararo sayohat haqida ssenariy",
            "🎨 /imagine Registon maydoni ustida yorqin yulduzlar va kiberpank chiroqlar"
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            prompts.forEach { prompt ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onQuickPrompt(prompt.removePrefix("🌟 ").removePrefix("🧠 ").removePrefix("💻 ").removePrefix("🎬 ").removePrefix("🎨 ").trim()) }
                ) {
                    Text(
                        text = prompt,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ErrorMessageCard(message: String) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Text(
            text = "Xatolik: $message",
            color = MaterialTheme.colorScheme.onErrorContainer,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(12.dp)
        )
    }
}
