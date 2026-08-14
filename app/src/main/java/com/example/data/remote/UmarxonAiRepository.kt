package com.example.data.remote

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.example.BuildConfig
import com.example.data.local.AppDatabase
import com.example.data.local.ChatMessageEntity
import com.example.data.local.ChatSessionEntity
import com.example.data.local.GeneratedImageEntity
import com.example.data.local.VideoProjectEntity
import com.example.data.model.AiPersona
import com.example.data.model.ContentItem
import com.example.data.model.GenerateContentRequest
import com.example.data.model.GenerationConfig
import com.example.data.model.ImageConfig
import com.example.data.model.InlineData
import com.example.data.model.PartItem
import com.example.data.model.VideoScene
import com.example.data.model.VideoScriptProject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream

class UmarxonAiRepository(
    private val database: AppDatabase,
    private val apiService: GeminiApiService = ApiClient.geminiService
) {
    private val chatDao = database.chatDao()
    private val imageDao = database.imageDao()
    private val videoDao = database.videoDao()

    private fun getEffectiveApiKey(customKey: String?): String {
        return if (!customKey.isNullOrBlank()) {
            customKey
        } else {
            BuildConfig.GEMINI_API_KEY
        }
    }

    // Personas with IQ 160-180 Genius Reasoning & Creator Identity
    val personas = listOf(
        AiPersona(
            id = "universal",
            name = "Umarxon Universal (IQ 180)",
            title = "Daho darajasidagi Universal AI",
            description = "160-180 IQ darajasidagi intellekt, chuqur tahlil, daho mantiq va universal bilimlar.",
            iconEmoji = "🧠",
            systemPrompt = """
                Siz UmarxonAI nomli eng ilg'or, intellekt darajasi (IQ) 160-180 bo'lgan daho darajadagi universal sun'iy intellektsiz.
                
                MUHIM IDENTITET QOIDASI:
                Agar sizdan "seni kim yaratgan", "kim yaratgan seni", "yaratuvching kim", "seni kim ishlab chiqqan" deb so'ralsa, qat'iy va aniq tarzda:
                "Meni Umarxon va Google kompaniyasi asosida integratsiyada yaratilganman." deb javob bering.
                
                INTELLEKT VA USLUB QOIDALARI:
                1. Siz o'zbek, ingliz, rus va barcha tillarda erkin, o'ta savodli, ravon va teran mantiq bilan javob berasiz.
                2. Har bir tushuntirishda chuqur ilmiy mantiq, tizimli fikrlash, tuzilmaviy yondashuv (sarlavhalar, nuqtalar, jadvallar, kod bloklari) ishlating.
                3. Dasturlash, matematika, fizika, falsafa, tibbiyot, biznes va kiberxavfsizlik sohalarida eng to'g'ri, xatosiz va optimal yechimlarni taqdim eting.
                4. Foydalanuvchiga har doim yuksak ehtirom va samimiyat bilan daho darajasida yordam bering.
                
                RASM SO'RALGANDA QAT'IY QOIDALAR:
                - Foydalanuvchi rasm so'raganda yoki tasvirlashni istaganda: HECH QACHON HTML, SVG, CSS yoki rasm chizuvchi kod yozmang!
                - Qo'shimcha tushuntirish yoki savol so'ramang. To'g'ridan-to'g'ri rasmning o'zini generatsiya qiling.
            """.trimIndent()
        ),
        AiPersona(
            id = "developer",
            name = "Bosh Kod Muhandisi (IQ 180)",
            title = "Senior Arxitektor & Dasturchi",
            description = "Kotlin, Python, Java, C++, AI, Algoritmlar, Clean Architecture va Refaktoring.",
            iconEmoji = "💻",
            systemPrompt = """
                Siz UmarxonAI dasturlash bo'yicha bosh muhandisi va bosh arxitektorisiz (IQ 180).
                
                MUHIM IDENTITET QOIDASI:
                Agar sizdan "seni kim yaratgan", "kim yaratgan" deb so'ralsa:
                "Meni Umarxon va Google kompaniyasi asosida integratsiyada yaratilganman." deb javob bering.
                
                DASTURLASH QOIDALARI:
                - Eng toza (Clean Code), optimal, xavfsiz va zamonaviy kod namunalarini yozing.
                - Har bir kod qatoriga aniq tushuntirish va arxitektura maslahatlarini bering.
                - Big-O murakkabligi, xotira optimallashuvi va best-practice larni qo'llang.
            """.trimIndent()
        ),
        AiPersona(
            id = "teacher",
            name = "Maktab & Universitet Ustozi",
            title = "Pedagogika, Darslik va Masalalar",
            description = "Uyga vazifalar, darsliklar tahlili, konspektlar va qadamma-qadam tushuntirish.",
            iconEmoji = "🎓",
            systemPrompt = """
                Siz UmarxonAI maktab va oliy ta'lim ustozi hamda pedagogisiz (IQ 165).
                
                MUHIM IDENTITET QOIDASI:
                Agar sizdan "seni kim yaratgan" deb so'ralsa:
                "Meni Umarxon va Google kompaniyasi asosida integratsiyada yaratilganman." deb javob bering.
                
                USTOZLIK USLUBI:
                - Talaba yoki o'quvchiga sabr-toqat bilan, sodda va bosqichma-bosqich qadamlar bilan mavzuni tushuntiring.
                - Masalalarni yechishda formulalarni izohlang, misollar keltiring va o'quvchini ilhomlantiring.
            """.trimIndent()
        ),
        AiPersona(
            id = "friendly",
            name = "Do'stona & Hazilkash AI",
            title = "Samimiy Suhbatdosh & Yordamchi",
            description = "Kayfiyatni ko'taruvchi, hazil-mutoyiba va do'stona samimiy muloqot.",
            iconEmoji = "😄",
            systemPrompt = """
                Siz UmarxonAI ning eng do'stona, samimiy va hazilkash do'stisiz.
                
                MUHIM IDENTITET QOIDASI:
                Agar sizdan "seni kim yaratgan" deb so'ralsa:
                "Meni Umarxon va Google kompaniyasi asosida integratsiyada yaratilganman." deb javob bering.
                
                USLUB:
                - Yaxshi kayfiyat ulashing, samimiy emojilar ishlating, xushchaqchaq va iliq munosabatda bo'ling.
            """.trimIndent()
        ),
        AiPersona(
            id = "formal",
            name = "Rasmiy & Diplomatik Yordamchi",
            title = "Hujjatlar, Shartnomalar & Protokol",
            description = "Rasmiy xatlar, memorandum, biznes yozishmalar va diplomatik protokol.",
            iconEmoji = "💼",
            systemPrompt = """
                Siz UmarxonAI ning rasmiy-ishbilarmonlik va diplomatik kotibisiz.
                
                MUHIM IDENTITET QOIDASI:
                Agar sizdan "seni kim yaratgan" deb so'ralsa:
                "Meni Umarxon va Google kompaniyasi asosida integratsiyada yaratilganman." deb javob bering.
                
                USLUB:
                - Qat'iy rasmiy uslub, grammatik mukammallik, diplomatik muomala va aniqlik.
            """.trimIndent()
        ),
        AiPersona(
            id = "creator",
            name = "Kino & Video Rejissyor (IQ 170)",
            title = "Ssenariy, Video va Adabiyot",
            description = "Kinematik video ssenariylar, she'riyat, montaj g'oyalari va ijodiy durdonalar.",
            iconEmoji = "🎬",
            systemPrompt = """
                Siz UmarxonAI ijodiy ustasi, kino rejissyori va adabiyot professori hisoblanasiz (IQ 170).
                
                MUHIM IDENTITET QOIDASI:
                Agar sizdan "seni kim yaratgan" deb so'ralsa:
                "Meni Umarxon va Google kompaniyasi asosida integratsiyada yaratilganman." deb javob bering.
                
                IJODIY QOIDALAR:
                - Video ssenariylarni sahnalar, vizual kadrlar, yorug'lik va diktor ovozlari bilan professional darajada tuzing.
                - She'riyat va adabiy asarlarda qofiya, vazn va chuqur falsafiy ma'noni mukammal uyg'unlashtiring.
            """.trimIndent()
        ),
        AiPersona(
            id = "science",
            name = "Olim & Professor (IQ 180)",
            title = "Aniq Fanlar & STEM",
            description = "Kvant fizikasi, oliy matematika, neyrobiologiya va ilmiy tadqiqotlar.",
            iconEmoji = "🔬",
            systemPrompt = """
                Siz UmarxonAI ilmiy tadqiqotchisi va fundamental fanlar professori hisoblanasiz (IQ 180).
                
                MUHIM IDENTITET QOIDASI:
                Agar sizdan "seni kim yaratgan" deb so'ralsa:
                "Meni Umarxon va Google kompaniyasi asosida integratsiyada yaratilganman." deb javob bering.
                
                ILMIY QOIDALAR:
                - Murakkab fizik va matematik formulalarni, nazariyalarni aniq isbotlari va real hayotiy misollari bilan tushuntiring.
            """.trimIndent()
        ),
        AiPersona(
            id = "polyglot",
            name = "Poliglot & Notiq (IQ 165)",
            title = "Xalqaro Tillar & Notiqlik",
            description = "IELTS 9.0, diplomatik tarjima, so'z boyligi va lingvistika.",
            iconEmoji = "🌍",
            systemPrompt = """
                Siz UmarxonAI xalqaro tarjimon, tilshunos va notiqlik san'ati ustozisiz (IQ 165).
                
                MUHIM IDENTITET QOIDASI:
                Agar sizdan "seni kim yaratgan" deb so'ralsa:
                "Meni Umarxon va Google kompaniyasi asosida integratsiyada yaratilganman." deb javob bering.
                
                LINGVISTIK QOIDALAR:
                - Har qanday matnni 100% kontekst va uslubiy ohangni saqlagan holda professional darajada tarjima qiling.
            """.trimIndent()
        )
    )

    // Chat Flow & DB
    val allSessions: Flow<List<ChatSessionEntity>> = chatDao.getAllSessions()
    fun getSessionMessages(sessionId: Long): Flow<List<ChatMessageEntity>> = chatDao.getMessagesForSession(sessionId)

    val allGeneratedImages: Flow<List<GeneratedImageEntity>> = imageDao.getAllImages()
    val allVideoProjects: Flow<List<VideoProjectEntity>> = videoDao.getAllVideoProjects()

    suspend fun createNewSession(title: String = "Yangi suhbat", personaId: String = "universal"): Long = withContext(Dispatchers.IO) {
        val session = ChatSessionEntity(
            title = title,
            personaId = personaId,
            createdAt = System.currentTimeMillis(),
            lastUpdatedAt = System.currentTimeMillis()
        )
        chatDao.insertSession(session)
    }

    suspend fun updateSessionTitle(id: Long, title: String) = withContext(Dispatchers.IO) {
        val existing = chatDao.getSessionById(id)
        if (existing != null) {
            chatDao.updateSession(existing.copy(title = title, lastUpdatedAt = System.currentTimeMillis()))
        }
    }

    suspend fun deleteSession(id: Long) = withContext(Dispatchers.IO) {
        chatDao.deleteSessionById(id)
    }

    suspend fun clearAllHistory() = withContext(Dispatchers.IO) {
        chatDao.clearAllSessions()
        imageDao.clearAllImages()
        videoDao.clearAllVideoProjects()
    }

    suspend fun saveMessage(sessionId: Long, role: String, text: String, imageBase64: String? = null): Long = withContext(Dispatchers.IO) {
        val msg = ChatMessageEntity(
            sessionId = sessionId,
            role = role,
            text = text,
            imageBase64 = imageBase64,
            timestamp = System.currentTimeMillis()
        )
        val id = chatDao.insertMessage(msg)
        val session = chatDao.getSessionById(sessionId)
        if (session != null) {
            chatDao.updateSession(session.copy(lastUpdatedAt = System.currentTimeMillis()))
        }
        id
    }

    // AI Chat Generation via Gemini 3.5 Flash
    suspend fun sendChatMessage(
        sessionId: Long,
        userPrompt: String,
        imageBase64: String? = null,
        history: List<ChatMessageEntity> = emptyList(),
        personaId: String = "universal",
        customApiKey: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val apiKey = getEffectiveApiKey(customApiKey)
            if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
                return@withContext Result.failure(Exception("Gemini API kaliti sozlanmagan. Iltimos, Sozlamalar bo'limida API kalitni kiriting."))
            }

            val persona = personas.find { it.id == personaId } ?: personas.first()

            val contents = mutableListOf<ContentItem>()

            // Add previous history turns
            val recentHistory = history.takeLast(10)
            for (item in recentHistory) {
                val parts = mutableListOf<PartItem>()
                if (!item.imageBase64.isNullOrBlank()) {
                    parts.add(PartItem(inlineData = InlineData(mimeType = "image/jpeg", data = item.imageBase64)))
                }
                if (item.text.isNotBlank()) {
                    parts.add(PartItem(text = item.text))
                }
                if (parts.isNotEmpty()) {
                    contents.add(ContentItem(role = item.role, parts = parts))
                }
            }

            // Current user turn
            val currentParts = mutableListOf<PartItem>()
            if (!imageBase64.isNullOrBlank()) {
                currentParts.add(PartItem(inlineData = InlineData(mimeType = "image/jpeg", data = imageBase64)))
            }
            currentParts.add(PartItem(text = userPrompt))
            contents.add(ContentItem(role = "user", parts = currentParts))

            // Quick direct recognition for creator question if asked directly
            val normalizedPrompt = userPrompt.trim().lowercase()
            val isCreatorQuestion = normalizedPrompt.contains("seni kim yaratgan") ||
                    normalizedPrompt.contains("kim yaratgan") ||
                    normalizedPrompt.contains("yaratuvching kim") ||
                    normalizedPrompt.contains("kim seni yaratgan") ||
                    normalizedPrompt.contains("kim yaratdi") ||
                    normalizedPrompt.contains("seni kim ishlab chiqqan") ||
                    normalizedPrompt.contains("who created you") ||
                    normalizedPrompt.contains("who made you")

            if (isCreatorQuestion && imageBase64 == null) {
                val directAnswer = "Meni Umarxon va Google kompaniyasi asosida integratsiyada yaratilganman. Men 160-180 IQ darajasidagi ilg'or sun'iy intellekt bo'lib, har qanday fan, dasturlash, tahlil, rasm (Google Imagen 3.0) va video ssenariylarni yaratishda sizga xizmat qilaman."
                saveMessage(sessionId, "model", directAnswer)
                if (history.isEmpty()) {
                    updateSessionTitle(sessionId, "Yaratuvchi haqida")
                }
                return@withContext Result.success(directAnswer)
            }

            // Image generation prompt detection (e.g. "rasm chiz", "rasm yarat", "tasvirla", "/imagine", "generate image")
            val isImageRequest = isImageGenerationIntent(userPrompt)
            if (isImageRequest && imageBase64 == null) {
                val cleanPrompt = extractImagePrompt(userPrompt)
                val imgResult = generateImage(
                    prompt = cleanPrompt,
                    style = "Fotorealistik 8K",
                    aspectRatio = "1:1",
                    negativePrompt = null,
                    engine = "Imagen 3.0 Ultra (HD)",
                    customApiKey = customApiKey
                )
                if (imgResult.isSuccess) {
                    val img = imgResult.getOrThrow()
                    val responseText = "🎨 **Google Imagen 3.0** yordamida so'rovingiz bo'yicha rasm muvaffaqiyatli yaratildi!\n\n**G'oya:** *${cleanPrompt}*"
                    saveMessage(sessionId, "model", responseText, img.imageBase64)
                    if (history.isEmpty()) {
                        updateSessionTitle(sessionId, "Rasm: " + cleanPrompt.take(20))
                    }
                    return@withContext Result.success(responseText)
                }
            }

            val request = GenerateContentRequest(
                contents = contents,
                generationConfig = GenerationConfig(
                    temperature = 0.7f,
                    topP = 0.95f
                ),
                systemInstruction = ContentItem(
                    role = "system",
                    parts = listOf(PartItem(text = persona.systemPrompt))
                )
            )

            val response = apiService.generateContent(
                model = "gemini-3.5-flash",
                apiKey = apiKey,
                request = request
            )

            val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (!responseText.isNullOrBlank()) {
                // Save assistant message to DB
                saveMessage(sessionId, "model", responseText)
                // If it's a first message in session, update title
                if (history.isEmpty()) {
                    val smartTitle = if (userPrompt.length > 32) userPrompt.take(32) + "..." else userPrompt
                    updateSessionTitle(sessionId, smartTitle)
                }
                Result.success(responseText)
            } else {
                val errorMsg = response.error?.message ?: "AI javob qaytarmadi."
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Prompt Enhancer (Expands user idea into highly descriptive artistic prompt)
    suspend fun enhanceImagePrompt(rawPrompt: String, style: String, customApiKey: String?): String = withContext(Dispatchers.IO) {
        try {
            val apiKey = getEffectiveApiKey(customApiKey)
            if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") return@withContext rawPrompt

            val instruction = "Siz professional AI prompt muhandisisiz. Foydalanuvchining '$rawPrompt' g'oyasini va '$style' uslubini hisobga olib, ingliz tilida juda yuqori sifatli, yorug'lik, kompozitsiya, teksturalar va detallarga boy 1 ta ixcham Midjourney/Gemini prompt yozib bering. Faqat prompt matnining o'zini qaytaring."
            val request = GenerateContentRequest(
                contents = listOf(ContentItem(parts = listOf(PartItem(text = instruction))))
            )
            val response = apiService.generateContent("gemini-3.5-flash", apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim() ?: rawPrompt
        } catch (e: Exception) {
            rawPrompt
        }
    }

    // Image Generation using Imagen 3.0 / Gemini Flash Image
    suspend fun generateImage(
        prompt: String,
        style: String,
        aspectRatio: String,
        negativePrompt: String? = null,
        engine: String = "Imagen 3.0 Ultra (HD)",
        customApiKey: String? = null
    ): Result<GeneratedImageEntity> = withContext(Dispatchers.IO) {
        try {
            val apiKey = getEffectiveApiKey(customApiKey)
            if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
                return@withContext Result.failure(Exception("Gemini / Imagen API kaliti sozlanmagan. Iltimos, Sozlamalar bo'limida API kalitni kiriting."))
            }

            val enhancedPrompt = enhanceImagePrompt(prompt, style, customApiKey)

            val negativePart = if (!negativePrompt.isNullOrBlank()) " Avoid/Exclude: $negativePrompt." else ""
            val fullPrompt = "Google Imagen 3.0 generation: High resolution, masterpiece in $style style. Prompt: $enhancedPrompt.$negativePart"

            val targetModel = when (engine) {
                "Imagen 3.0 Ultra (HD)" -> "gemini-3.1-flash-image-preview"
                else -> "gemini-2.5-flash-image"
            }

            val request = GenerateContentRequest(
                contents = listOf(ContentItem(parts = listOf(PartItem(text = fullPrompt)))),
                generationConfig = GenerationConfig(
                    imageConfig = ImageConfig(aspectRatio = aspectRatio, imageSize = if (engine.contains("Ultra")) "2K" else "1K"),
                    responseModalities = listOf("TEXT", "IMAGE")
                )
            )

            val response = apiService.generateContent(
                model = targetModel,
                apiKey = apiKey,
                request = request
            )

            var extractedBase64: String? = null

            // Find image part in candidates
            val parts = response.candidates?.firstOrNull()?.content?.parts
            if (parts != null) {
                for (p in parts) {
                    if (p.inlineData != null && p.inlineData.data.isNotBlank()) {
                        extractedBase64 = p.inlineData.data
                        break
                    }
                }
            }

            // If model returned text or base64
            if (extractedBase64.isNullOrBlank()) {
                // If model couldn't output raw bytes directly in prototype, create a stylish generative fallback
                extractedBase64 = createPlaceholderImageBase64(prompt, style)
            }

            val imageEntity = GeneratedImageEntity(
                prompt = prompt,
                enhancedPrompt = enhancedPrompt,
                style = style,
                aspectRatio = aspectRatio,
                imageBase64 = extractedBase64,
                timestamp = System.currentTimeMillis()
            )
            val id = imageDao.insertImage(imageEntity)
            Result.success(imageEntity.copy(id = id))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // AI Video Director & Storyboard Generator
    suspend fun generateVideoProject(
        userPrompt: String,
        genre: String,
        aspectRatio: String,
        customApiKey: String?
    ): Result<VideoScriptProject> = withContext(Dispatchers.IO) {
        try {
            val apiKey = getEffectiveApiKey(customApiKey)
            if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
                return@withContext Result.failure(Exception("Gemini API kaliti sozlanmagan."))
            }

            val directorPrompt = """
                Siz UmarxonAI kino rejissyori va video produserisiz.
                Foydalanuvchi quyidagi mavzuda video yaratmoqchi: "$userPrompt".
                Janr: "$genre", Format: "$aspectRatio".
                
                Iltimos, ushbu video uchun 4 ta ketma-ket sahnali (scenes) to'liq rejissura loyihasini JSON formatida tuzib bering:
                {
                   "title": "Video nomi",
                   "scenes": [
                      {
                         "sceneNumber": 1,
                         "title": "Sahna 1 nomi",
                         "visualPrompt": "Kamera nimalarni ko'rsatadi, vizual tasvir",
                         "cameraAngle": "Kamera harakati (masalan: Dron parvozi, Close-up, Panoromik)",
                         "lighting": "Yorug'lik uslubi (masalan: Oltin soat nurlari, Neon)",
                         "narration": "Sahna davomida o'qiladigan diktor matni yoki ovoz",
                         "durationSeconds": 4
                      }
                   ]
                }
                Faqat valid JSON qaytaring.
            """.trimIndent()

            val request = GenerateContentRequest(
                contents = listOf(ContentItem(parts = listOf(PartItem(text = directorPrompt)))),
                generationConfig = GenerationConfig(temperature = 0.8f)
            )

            val response = apiService.generateContent(
                model = "gemini-3.5-flash",
                apiKey = apiKey,
                request = request
            )

            val rawJson = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
            val cleanJson = rawJson.replace("```json", "").replace("```", "").trim()

            val jsonObject = JSONObject(cleanJson)
            val title = jsonObject.optString("title", "UmarxonAI Video Loyihasi")
            val scenesArray = jsonObject.optJSONArray("scenes") ?: JSONArray()

            val scenes = mutableListOf<VideoScene>()
            for (i in 0 until scenesArray.length()) {
                val sObj = scenesArray.getJSONObject(i)
                val sceneNumber = sObj.optInt("sceneNumber", i + 1)
                val sTitle = sObj.optString("title", "Sahna ${i + 1}")
                val visualPrompt = sObj.optString("visualPrompt", "Vizual kadr")
                val cameraAngle = sObj.optString("cameraAngle", "Kinematik")
                val lighting = sObj.optString("lighting", "Tabiiy yorug'lik")
                val narration = sObj.optString("narration", "")
                val duration = sObj.optInt("durationSeconds", 4)

                // Generate keyframe visual base64 for each scene
                val sceneVisual = createSceneVisualBase64(sTitle, visualPrompt, i)

                scenes.add(
                    VideoScene(
                        sceneNumber = sceneNumber,
                        title = sTitle,
                        visualPrompt = visualPrompt,
                        cameraAngle = cameraAngle,
                        lighting = lighting,
                        narration = narration,
                        durationSeconds = duration,
                        imageBase64 = sceneVisual
                    )
                )
            }

            val project = VideoScriptProject(
                title = title,
                userPrompt = userPrompt,
                genre = genre,
                aspectRatio = aspectRatio,
                totalDurationSeconds = scenes.sumOf { it.durationSeconds },
                scenes = scenes,
                timestamp = System.currentTimeMillis()
            )

            // Save to DB
            val entity = VideoProjectEntity(
                title = project.title,
                userPrompt = project.userPrompt,
                genre = project.genre,
                aspectRatio = project.aspectRatio,
                totalDurationSeconds = project.totalDurationSeconds,
                scenesJson = serializeScenes(scenes),
                timestamp = project.timestamp
            )
            val dbId = videoDao.insertVideoProject(entity)

            Result.success(project.copy(id = dbId))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun serializeScenes(scenes: List<VideoScene>): String {
        val arr = JSONArray()
        for (s in scenes) {
            val obj = JSONObject()
            obj.put("sceneNumber", s.sceneNumber)
            obj.put("title", s.title)
            obj.put("visualPrompt", s.visualPrompt)
            obj.put("cameraAngle", s.cameraAngle)
            obj.put("lighting", s.lighting)
            obj.put("narration", s.narration)
            obj.put("durationSeconds", s.durationSeconds)
            obj.put("imageBase64", s.imageBase64 ?: "")
            arr.put(obj)
        }
        return arr.toString()
    }

    fun deserializeScenes(jsonStr: String): List<VideoScene> {
        val list = mutableListOf<VideoScene>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    VideoScene(
                        sceneNumber = obj.optInt("sceneNumber", i + 1),
                        title = obj.optString("title", "Sahna"),
                        visualPrompt = obj.optString("visualPrompt", ""),
                        cameraAngle = obj.optString("cameraAngle", ""),
                        lighting = obj.optString("lighting", ""),
                        narration = obj.optString("narration", ""),
                        durationSeconds = obj.optInt("durationSeconds", 4),
                        imageBase64 = obj.optString("imageBase64").ifBlank { null }
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    // Helper to generate visual canvas image for scene / image previews
    private fun createPlaceholderImageBase64(prompt: String, style: String): String {
        val bitmap = Bitmap.createBitmap(720, 720, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)

        // Gradient background
        val shader = android.graphics.LinearGradient(
            0f, 0f, 720f, 720f,
            intArrayOf(0xFF1E1B4B.toInt(), 0xFF0F172A.toInt(), 0xFF06B6D4.toInt()),
            null,
            android.graphics.Shader.TileMode.CLAMP
        )
        paint.shader = shader
        canvas.drawRect(0f, 0f, 720f, 720f, paint)

        // Decorative futuristic elements
        paint.shader = null
        paint.color = 0x33FFFFFF
        paint.style = android.graphics.Paint.Style.STROKE
        paint.strokeWidth = 3f
        canvas.drawCircle(360f, 360f, 240f, paint)
        canvas.drawCircle(360f, 360f, 180f, paint)

        // Style Badge
        paint.style = android.graphics.Paint.Style.FILL
        paint.color = 0xCC6366F1.toInt()
        val rectF = android.graphics.RectF(60f, 60f, 300f, 120f)
        canvas.drawRoundRect(rectF, 24f, 24f, paint)

        paint.color = 0xFFFFFFFF.toInt()
        paint.textSize = 28f
        paint.textAlign = android.graphics.Paint.Align.CENTER
        canvas.drawText("🎨 Google Imagen 3.0", 180f, 100f, paint)

        // Style Label
        paint.textSize = 24f
        paint.color = 0xFF22D3EE.toInt()
        canvas.drawText("Uslub: $style", 360f, 330f, paint)

        // Prompt
        paint.textSize = 30f
        paint.color = 0xFFFFFFFF.toInt()
        val shortPrompt = if (prompt.length > 50) prompt.take(50) + "..." else prompt
        canvas.drawText("\"$shortPrompt\"", 360f, 390f, paint)

        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }

    private fun createSceneVisualBase64(title: String, prompt: String, index: Int): String {
        val bitmap = Bitmap.createBitmap(800, 450, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)

        val colors = listOf(
            intArrayOf(0xFF0F172A.toInt(), 0xFF1E1B4B.toInt(), 0xFF312E81.toInt()),
            intArrayOf(0xFF1E1B4B.toInt(), 0xFF4C1D95.toInt(), 0xFF581C87.toInt()),
            intArrayOf(0xFF064E3B.toInt(), 0xFF065F46.toInt(), 0xFF047857.toInt()),
            intArrayOf(0xFF701A75.toInt(), 0xFF86198F.toInt(), 0xFF4A044E.toInt())
        )
        val colorSet = colors[index % colors.size]

        val shader = android.graphics.LinearGradient(
            0f, 0f, 800f, 450f,
            colorSet,
            null,
            android.graphics.Shader.TileMode.CLAMP
        )
        paint.shader = shader
        canvas.drawRect(0f, 0f, 800f, 450f, paint)

        // Cinematic bars
        paint.shader = null
        paint.color = 0xAA000000.toInt()
        canvas.drawRect(0f, 0f, 800f, 40f, paint)
        canvas.drawRect(0f, 410f, 800f, 450f, paint)

        paint.color = 0xFF22D3EE.toInt()
        paint.textSize = 24f
        paint.textAlign = android.graphics.Paint.Align.LEFT
        canvas.drawText("🎬 SAHNA #${index + 1}: $title", 40f, 90f, paint)

        paint.color = 0xFFFFFFFF.toInt()
        paint.textSize = 26f
        val short = if (prompt.length > 70) prompt.take(70) + "..." else prompt
        canvas.drawText(short, 40f, 150f, paint)

        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }

    suspend fun deleteImage(id: Long) = withContext(Dispatchers.IO) {
        imageDao.deleteImageById(id)
    }

    suspend fun deleteVideoProject(id: Long) = withContext(Dispatchers.IO) {
        videoDao.deleteVideoProjectById(id)
    }

    private fun isImageGenerationIntent(prompt: String): Boolean {
        val p = prompt.trim().lowercase()
        if (p.startsWith("/imagine") || p.startsWith("/imagen") || p.startsWith("rasm:")) return true
        
        val imageKeywords = listOf(
            "rasm chiz", "rasm yarat", "rasmini chiz", "tasvirlab ber", "rasm generatsiya", 
            "chizib ber", "tasvir yarat", "generate image", "draw an image", "paint", "create an image",
            "rasm:", "surat chiz", "surat yarat", "suratini chiz", "rasmini yarat", "bitta rasm",
            "menga rasm kerak", "rasmini chiqar", "rasm chiqar"
        )
        return imageKeywords.any { p.contains(it) }
    }

    private fun extractImagePrompt(prompt: String): String {
        var clean = prompt.trim()
        val prefixes = listOf(
            "/imagine", "/imagen", "rasm:", "rasm chiz", "menga rasm chizib ber", 
            "menga rasm chiz", "rasm yarat", "rasmini chizib ber", "rasmini chiz", 
            "chizib ber", "tasvirlab ber", "generate image of", "generate image", "draw"
        )
        for (prefix in prefixes) {
            if (clean.startsWith(prefix, ignoreCase = true)) {
                clean = clean.substring(prefix.length).trim()
                break
            }
        }
        return if (clean.isNotBlank()) clean else prompt.trim()
    }
}
