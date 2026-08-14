package com.example.data.model

data class PromptCategory(
    val id: String,
    val title: String,
    val iconEmoji: String,
    val description: String,
    val templates: List<PromptTemplateItem>
)

data class PromptTemplateItem(
    val id: String,
    val title: String,
    val prompt: String,
    val tag: String,
    val type: TemplateType = TemplateType.CHAT // CHAT, IMAGE, VIDEO
)

enum class TemplateType {
    CHAT,
    IMAGE,
    VIDEO
}

object PromptGalleryData {
    val categories = listOf(
        PromptCategory(
            id = "coding",
            title = "Dasturlash & IT",
            iconEmoji = "💻",
            description = "Kotlin, Python, Web, API, Algoritmlar va Arxitektura",
            templates = listOf(
                PromptTemplateItem(
                    id = "code_compose",
                    title = "Jetpack Compose Clean Architecture",
                    prompt = "Android Kotlin va Jetpack Compose yordamida zamonaviy Clean Architecture (MVVM, StateFlow, Coroutines) asosida to'liq ishlaydigan kod namunasi va arxitektura qoidalarini yozib ber.",
                    tag = "Android / Kotlin",
                    type = TemplateType.CHAT
                ),
                PromptTemplateItem(
                    id = "code_bot",
                    title = "Python Aiogram 3 Telegram Bot",
                    prompt = "Python va aiogram 3 kutubxonasida asinxron ishlaydigan, menyuli, tugmali va ma'lumotlar bazasiga ulanadigan mukammal Telegram bot skriptini yozib ber.",
                    tag = "Python / Bot",
                    type = TemplateType.CHAT
                ),
                PromptTemplateItem(
                    id = "code_refactor",
                    title = "Kod Tahlili va Refaktoring (Clean Code)",
                    prompt = "Quyidagi kodni chuqur tahlil qilib, undagi xatoliklar, xavfsizlik zaifliklari va samaradorlikni oshirish bo'yicha Clean Code qoidalariga moslab qayta yozib ber:\n```\n// kodingizni bu yerga joylang\n```",
                    tag = "Refactoring",
                    type = TemplateType.CHAT
                ),
                PromptTemplateItem(
                    id = "code_sql",
                    title = "SQL Murakkab So'rovlar & Indexing",
                    prompt = "PostgreSQL / MySQL uchun millionlab qatorli jadvaldan eng tezkor ma'lumot olish bo'yicha indekslangan, optimallashtirilgan so'rov va uning EXPLAIN tahlilini tushuntir.",
                    tag = "Database",
                    type = TemplateType.CHAT
                )
            )
        ),
        PromptCategory(
            id = "smm",
            title = "SMM & Marketing",
            iconEmoji = "📱",
            description = "Instagram Reels, TikTok, Telegram va Sotuv Kopiraytingi",
            templates = listOf(
                PromptTemplateItem(
                    id = "smm_reels",
                    title = "10 ta Virusli Reels & TikTok Ssenariysi",
                    prompt = "Mening mavzuyim bo'yicha Instagram Reels va TikTok uchun odamlarni birinchi 3 soniyada ilintirib oladigan (Hook), qiziqarli (Body) va harakatga chaqiruvchi (CTA) 10 ta virusli video ssenariysi tuzib ber.",
                    tag = "Reels / TikTok",
                    type = TemplateType.CHAT
                ),
                PromptTemplateItem(
                    id = "smm_content_plan",
                    title = "Telegram Kanal uchun 30 Kunlik Kontent Reja",
                    prompt = "Mening soham bo'yicha Telegram kanalimga obunachilarni jalb qilish, faollikni oshirish va ishonch uyg'otish uchun 30 kunlik to'liq kontent reja (kun, mavzu, format va xeshteglar) tuzib ber.",
                    tag = "Kontent Reja",
                    type = TemplateType.CHAT
                ),
                PromptTemplateItem(
                    id = "smm_copywriting",
                    title = "AIDA Formulasi asosida Sotuv Matni",
                    prompt = "Mening mahsulotim/xizmatim uchun xaridorlarni befarq qoldirmaydigan, AIDA (Attention, Interest, Desire, Action) formulasi bo'yicha professional sotuv matnini yozib ber.",
                    tag = "Kopirayting",
                    type = TemplateType.CHAT
                ),
                PromptTemplateItem(
                    id = "smm_targeting",
                    title = "Meta Ads (Target) Reklama Matnlari",
                    prompt = "Instagram va Facebook targeting reklamasi uchun 3 xil auditoriyaga (Sovuq, Iliq, Issiq) mo'ljallangan 3 xil yuqori konversiyali reklama matni va sarlavhalari yozib ber.",
                    tag = "Targeting",
                    type = TemplateType.CHAT
                )
            )
        ),
        PromptCategory(
            id = "study",
            title = "Maktab, Universitet & Insholar",
            iconEmoji = "🎓",
            description = "Insholar, IELTS 8.0, Masalalar yechish va Referatlar",
            templates = listOf(
                PromptTemplateItem(
                    id = "study_ielts",
                    title = "IELTS Writing Task 2 (Band 8.0-9.0)",
                    prompt = "IELTS Writing Task 2 uchun 'Technology and Society' mavzusida Band 8.5 darajasidagi akademik insho, undagi Advanced lug'atlar (C1/C2) va qoliplarni (Collocations) tahlil qilib ber.",
                    tag = "IELTS",
                    type = TemplateType.CHAT
                ),
                PromptTemplateItem(
                    id = "study_math",
                    title = "Matematika & Fizika Masalasini Yechish",
                    prompt = "Quyidagi masalani bosqichma-bosqich, har bir formulani aniq tushuntirib, grafik va mantiqiy xulosalar bilan yechib ber:\n[Masalangiz shartini yozing]",
                    tag = "STEM Masala",
                    type = TemplateType.CHAT
                ),
                PromptTemplateItem(
                    id = "study_essay",
                    title = "O'zbek Tilida Ilmiy Insho / Esse",
                    prompt = "Mavzu: 'Jadidchilik harakati va zamonaviy ta'lim'. Ushbu mavzuda chuqur falsafiy-tarixiy, adabiy til me'yorlariga to'la va kirish, asosiy qism, xulosa bilan boyitilgan ajoyib insho yozib ber.",
                    tag = "Insho / Esse",
                    type = TemplateType.CHAT
                ),
                PromptTemplateItem(
                    id = "study_summary",
                    title = "Kitob va Konspektning Qisqacha Mazmuni",
                    prompt = "Ushbu matn yoki kitobning eng asosiy 5 ta muhim g'oyasini, asosiy xulosalari va amaliyotga tatbiq etish qoidalarini aniq tuzilmaviy ko'rinishda umumlashtirib ber.",
                    tag = "Konspekt",
                    type = TemplateType.CHAT
                )
            )
        ),
        PromptCategory(
            id = "media",
            title = "Foto & Video Promptlar",
            iconEmoji = "🎨",
            description = "Google Imagen 3.0 va Veo uchun tayyor yuqori sifatli buyruqlar",
            templates = listOf(
                PromptTemplateItem(
                    id = "img_samarkand",
                    title = "Registon Samarqand Cyberpunk 8K",
                    prompt = "Registon maydoni Samarqand, futuristik 2077 neon cyberpunk uslubida, moviy koshinlar ustida yaltiragan gologrammalar, oltin quyosh botishi, 8k fotorealistik kinematik",
                    tag = "Imagen 3.0",
                    type = TemplateType.IMAGE
                ),
                PromptTemplateItem(
                    id = "img_tashkent",
                    title = "Futuristik Toshkent 2050",
                    prompt = "Toshkent City 2050-yil, baland yashil ekologik osmono'par binolar, shisha gumbazlar, uchuvchi transportlar va moviy osmon, fotorealistik 8k",
                    tag = "Imagen 3.0",
                    type = TemplateType.IMAGE
                ),
                PromptTemplateItem(
                    id = "img_pixar",
                    title = "3D Pixar Qahramoni",
                    prompt = "Cute 3D Pixar Disney style character of an Uzbek boy exploring science laboratory with glowing stars, ultra detailed, volumetric lighting, Octane render 8k",
                    tag = "3D / Pixar",
                    type = TemplateType.IMAGE
                ),
                PromptTemplateItem(
                    id = "vid_space",
                    title = "Kinematik Kosmik Sayohat Video",
                    prompt = "Koinot qa'riga sayohat, yulduzlar tumanligi, yangi yashil sayyoraning kashf etilishi va qadimgi sivilizatsiya sirlari haqida 4K blokbaster treyleri",
                    tag = "AI Video",
                    type = TemplateType.VIDEO
                )
            )
        ),
        PromptCategory(
            id = "business",
            title = "Biznes & Startap",
            iconEmoji = "🚀",
            description = "Biznes reja, Investitsiya Pitch Deck, SWOT tahlil",
            templates = listOf(
                PromptTemplateItem(
                    id = "biz_plan",
                    title = "Startap uchun 6 Oylik Biznes Reja",
                    prompt = "O'zbekiston bozori uchun yangi startap loyihasiga 6 oylik batafsil biznes reja (Bozor tahlili, Raqobatchilar, Moliyaviy reja, Unit-ekonomika va Xatarlar) tuzib ber.",
                    tag = "Biznes Reja",
                    type = TemplateType.CHAT
                ),
                PromptTemplateItem(
                    id = "biz_pitch",
                    title = "Investorlar uchun 10 Sahifali Pitch Deck",
                    prompt = "Venchur investorlar oldida taqdimot qilish uchun 10 ta slayddan iborat kuchli Pitch Deck tuzilishi va har bir slayd matnini tayyorlab ber.",
                    tag = "Pitch Deck",
                    type = TemplateType.CHAT
                ),
                PromptTemplateItem(
                    id = "biz_swot",
                    title = "Kompaniya SWOT Tahlili",
                    prompt = "Mening loyiham uchun kuchli tomonlar (Strengths), zaif tomonlar (Weaknesses), imkoniyatlar (Opportunities) va xatarlarni (Threats) chuqur tahlil qiluvchi jadval tuzib ber.",
                    tag = "SWOT Tahlil",
                    type = TemplateType.CHAT
                )
            )
        )
    )
}
