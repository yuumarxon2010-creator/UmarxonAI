package com.example.ui.screens.video

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VolumeUp
import com.example.ui.components.copyToClipboard
import com.example.util.FileUtils
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.VideoProjectEntity
import com.example.data.model.VideoScene
import com.example.data.model.VideoScriptProject
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.VioletSecondary
import com.example.viewmodel.VideoStudioState

@Composable
fun VideoStudioScreen(
    state: VideoStudioState,
    savedProjects: List<VideoProjectEntity>,
    genrePresets: List<String>,
    onPromptChanged: (String) -> Unit,
    onGenreSelected: (String) -> Unit,
    onAspectSelected: (String) -> Unit,
    onGenerateVideo: () -> Unit,
    onTogglePlay: () -> Unit,
    onSceneSelected: (Int) -> Unit,
    onSelectSavedProject: (VideoProjectEntity) -> Unit,
    onDeleteProject: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val aspectRatios = listOf("16:9", "9:16", "1:1")

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))

            // Studio Header Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(VioletSecondary, AmberAccent))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "AI Video Studiyasi & Rejissyor",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Veo 3.1 & Gemini quvvatida professional video tayyorlash",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Active Interactive Video Player (if project is available)
        if (state.currentProject != null && state.currentProject.scenes.isNotEmpty()) {
            item {
                InteractiveVideoPlayer(
                    project = state.currentProject,
                    activeSceneIndex = state.activeSceneIndex,
                    sceneProgress = state.sceneProgress,
                    isPlaying = state.isPlaying,
                    onTogglePlay = onTogglePlay,
                    onSceneSelected = onSceneSelected
                )
            }
        }

        // Prompt Input Section
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Video g'oyasi yoki ssenariy mavzusi",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                OutlinedTextField(
                    value = state.prompt,
                    onValueChange = onPromptChanged,
                    placeholder = {
                        Text("Masalan: Toshkent 2050-yilda: Osmono'par binolar, uchar mashinalar va robot texnologiyalari haqida dinamik film...")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("video_prompt_input"),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = VioletSecondary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    minLines = 3,
                    maxLines = 5
                )
            }
        }

        // Genre Selector
        item {
            Column {
                Text(
                    text = "Video janri va rejissura uslubi",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                val scrollState = rememberScrollState()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(scrollState),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    genrePresets.forEach { genre ->
                        val isSelected = genre == state.selectedGenre
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) VioletSecondary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onGenreSelected(genre) }
                                .testTag("genre_$genre")
                        ) {
                            Text(
                                text = genre,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        // Aspect Ratio
        item {
            Column {
                Text(
                    text = "Video formati",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    aspectRatios.forEach { ratio ->
                        val isSelected = ratio == state.selectedAspect
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) IndigoPrimary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onAspectSelected(ratio) }
                                .testTag("video_aspect_$ratio")
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = ratio,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = when (ratio) {
                                        "16:9" -> "YouTube / TV"
                                        "9:16" -> "Shorts / Reels"
                                        else -> "Instagram Post"
                                    },
                                    fontSize = 10.sp,
                                    color = if (isSelected) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Generate Button
        item {
            Button(
                onClick = onGenerateVideo,
                enabled = state.prompt.isNotBlank() && !state.isGenerating,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = VioletSecondary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("generate_video_button")
            ) {
                if (state.isGenerating) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("AI video ssenariy va sahnalarini yaratmoqda...", fontWeight = FontWeight.Bold)
                } else {
                    Icon(imageVector = Icons.Default.Movie, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Video Tayyorlash & Rejissura", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Error message
        if (state.errorMessage != null) {
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Xatolik: ${state.errorMessage}",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        // Screenplay Details (if current project exists)
        if (state.currentProject != null) {
            item {
                Text(
                    text = "Ssenariy va Sahnalar Tafsiloti",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            items(state.currentProject.scenes, key = { it.sceneNumber }) { scene ->
                SceneDetailCard(
                    scene = scene,
                    isActive = scene.sceneNumber - 1 == state.activeSceneIndex,
                    onClick = { onSceneSelected(scene.sceneNumber - 1) }
                )
            }
        }

        // Saved Projects Section
        item {
            Text(
                text = "Saqlangan Video Loyihalar (${savedProjects.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 12.dp)
            )
        }

        if (savedProjects.isEmpty()) {
            item {
                Text(
                    text = "Hozircha saqlangan video loyihalar mavjud emas. Yuqorida video g'oyangizni kiritib birinchi loyihangizni yarating!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }
        } else {
            items(savedProjects, key = { it.id }) { proj ->
                SavedVideoProjectCard(
                    project = proj,
                    onSelect = { onSelectSavedProject(proj) },
                    onDelete = { onDeleteProject(proj.id) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp).navigationBarsPadding())
        }
    }
}

@Composable
fun InteractiveVideoPlayer(
    project: VideoScriptProject,
    activeSceneIndex: Int,
    sceneProgress: Float,
    isPlaying: Boolean,
    onTogglePlay: () -> Unit,
    onSceneSelected: (Int) -> Unit
) {
    val currentScene = project.scenes.getOrNull(activeSceneIndex) ?: return

    val animatedScale by animateFloatAsState(
        targetValue = if (isPlaying) 1.06f else 1.0f,
        animationSpec = tween(durationMillis = 2000, easing = LinearEasing),
        label = "kenBurnsScale"
    )

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0E1A)),
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, Brush.horizontalGradient(listOf(VioletSecondary, CyanAccent)), RoundedCornerShape(20.dp))
    ) {
        Column {
            // Video Screen Viewport
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                // Keyframe visual with Ken-Burns animation
                if (!currentScene.imageBase64.isNullOrBlank()) {
                    val bitmap = remember(currentScene.imageBase64) {
                        try {
                            val bytes = Base64.decode(currentScene.imageBase64, Base64.DEFAULT)
                            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = currentScene.title,
                            modifier = Modifier
                                .fillMaxSize()
                                .scale(animatedScale),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                // Dark vignette overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Black.copy(alpha = 0.4f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.8f)
                                )
                            )
                        )
                )

                // Top Badge: Scene Number & Title
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isPlaying) Color.Red else CyanAccent)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "SAHNA ${activeSceneIndex + 1}/${project.scenes.size}: ${currentScene.title}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // Center Play Button if paused
                if (!isPlaying) {
                    IconButton(
                        onClick = onTogglePlay,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(VioletSecondary.copy(alpha = 0.85f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "O'ynatish",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                // Bottom Subtitle Narration Bar
                if (currentScene.narration.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(10.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.75f))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = currentScene.narration,
                            fontSize = 12.sp,
                            color = Color(0xFFF1F5F9),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Progress Bar
            LinearProgressIndicator(
                progress = { sceneProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                color = CyanAccent,
                trackColor = Color(0xFF1E293B)
            )

            // Controls & Scene Selectors
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = project.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.White
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val context = LocalContext.current

                        IconButton(
                            onClick = {
                                val scriptText = buildString {
                                    appendLine("🎬 ${project.title} (${project.genre})")
                                    appendLine("⏱️ Umumiy davomiyligi: ${project.totalDurationSeconds} soniya | Nisbat: ${project.aspectRatio}")
                                    appendLine()
                                    project.scenes.forEach { scene ->
                                        appendLine("📍 Sahna ${scene.sceneNumber}: ${scene.title} (${scene.durationSeconds}s)")
                                        appendLine("🎥 Kamera: ${scene.cameraAngle} | Yorug'lik: ${scene.lighting}")
                                        appendLine("🎙️ Ovoz: ${scene.narration}")
                                        appendLine()
                                    }
                                }
                                FileUtils.shareText(context, scriptText, "🎬 ${project.title}")
                            }
                        ) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = "Ulashish", tint = Color.White)
                        }

                        IconButton(
                            onClick = {
                                if (activeSceneIndex > 0) onSceneSelected(activeSceneIndex - 1)
                            }
                        ) {
                            Icon(imageVector = Icons.Default.SkipPrevious, contentDescription = "Oldingi", tint = Color.White)
                        }

                        IconButton(
                            onClick = onTogglePlay,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(VioletSecondary)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pauza" else "Ijro",
                                tint = Color.White
                            )
                        }

                        IconButton(
                            onClick = {
                                if (activeSceneIndex < project.scenes.size - 1) onSceneSelected(activeSceneIndex + 1)
                            }
                        ) {
                            Icon(imageVector = Icons.Default.SkipNext, contentDescription = "Keyingi", tint = Color.White)
                        }
                    }
                }

                // Scene timeline chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    project.scenes.forEachIndexed { idx, s ->
                        val isSelected = idx == activeSceneIndex
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) CyanAccent else Color(0xFF1E293B),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onSceneSelected(idx) }
                        ) {
                            Text(
                                text = "Sahna ${idx + 1}",
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.Black else Color.White,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SceneDetailCard(
    scene: VideoScene,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(
                width = if (isActive) 1.5.dp else 0.dp,
                color = if (isActive) CyanAccent else Color.Transparent,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Sahna ${scene.sceneNumber}: ${scene.title}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${scene.durationSeconds} sek",
                    fontSize = 12.sp,
                    color = CyanAccent,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(14.dp), tint = VioletSecondary)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Kamera: ${scene.cameraAngle}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                Icon(imageVector = Icons.Default.Lightbulb, contentDescription = null, modifier = Modifier.size(14.dp), tint = AmberAccent)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Yorug'lik: ${scene.lighting}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (scene.narration.isNotBlank()) {
                Row(modifier = Modifier.padding(top = 6.dp), verticalAlignment = Alignment.Top) {
                    Icon(imageVector = Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(14.dp), tint = CyanAccent)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "\"${scene.narration}\"",
                        fontSize = 12.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun SavedVideoProjectCard(
    project: VideoProjectEntity,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onSelect() }
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = project.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Janr: ${project.genre} • ${project.aspectRatio} • ${project.totalDurationSeconds} sek",
                    fontSize = 12.sp,
                    color = CyanAccent,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Text(
                    text = "\"${project.userPrompt}\"",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                val context = LocalContext.current
                IconButton(
                    onClick = {
                        val shareText = "🎬 ${project.title} (${project.genre})\n⏱️ Davomiyligi: ${project.totalDurationSeconds} sek\n\n📌 G'oya: ${project.userPrompt}\n\nUmarxonAI Video Rejissyor orqali yaratildi."
                        FileUtils.shareText(context, shareText, "🎬 ${project.title}")
                    }
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = "Ulashish", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                IconButton(onClick = onDelete) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "O'chirish", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
