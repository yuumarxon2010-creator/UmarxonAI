package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.util.Base64
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FileUtils {

    /**
     * Rasmni telefon galereyasiga (Pictures/UmarxonAI) saqlash
     */
    fun saveBitmapToGallery(context: Context, bitmap: Bitmap, title: String = "UmarxonAI_Image"): Boolean {
        return try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val filename = "${title}_$timeStamp.jpg"

            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/UmarxonAI")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }

            val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    context.contentResolver.update(uri, contentValues, null, null)
                }
                Toast.makeText(context, "✅ Rasm Galereyaga saqlandi!", Toast.LENGTH_SHORT).show()
                true
            } else {
                Toast.makeText(context, "❌ Rasmni saqlab bo'lmadi", Toast.LENGTH_SHORT).show()
                false
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Xatolik: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            false
        }
    }

    /**
     * Rasmni Telegram, Instagram, WhatsApp va boshqa ilovalarga ulashish
     */
    fun shareBitmap(context: Context, bitmap: Bitmap, caption: String = "UmarxonAI orqali yaratildi") {
        try {
            val cachePath = File(context.cacheDir, "shared_images")
            cachePath.mkdirs()
            val file = File(cachePath, "umarxon_ai_${System.currentTimeMillis()}.jpg")
            val stream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)
            stream.close()

            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_TEXT, caption)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Rasmni ulashish"))
        } catch (e: Exception) {
            // Fallback to text share if FileProvider fails
            shareText(context, caption, "UmarxonAI")
        }
    }

    /**
     * Matnni Telegram yoki boshqa ilovalarga ulashish
     */
    fun shareText(context: Context, text: String, title: String = "UmarxonAI javobi") {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, title)
                putExtra(Intent.EXTRA_TEXT, text)
            }
            context.startActivity(Intent.createChooser(intent, "Ulashish"))
        } catch (e: Exception) {
            Toast.makeText(context, "Xatolik: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Matnli javobni chiroyli PDF hujjatga aylantirib saqlash va ulashish
     */
    fun exportTextToPdf(context: Context, title: String, textContent: String): File? {
        return try {
            val pdfDoc = PdfDocument()
            val pageWidth = 595 // A4 standard width
            val pageHeight = 842 // A4 standard height
            val margin = 40f
            val contentWidth = pageWidth - 2 * margin

            val textPaint = Paint().apply {
                color = Color.DKGRAY
                textSize = 11f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                isAntiAlias = true
            }

            val titlePaint = Paint().apply {
                color = Color.rgb(30, 41, 59)
                textSize = 18f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }

            val headerPaint = Paint().apply {
                color = Color.rgb(79, 70, 229)
                textSize = 12f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }

            val footerPaint = Paint().apply {
                color = Color.GRAY
                textSize = 9f
                isAntiAlias = true
            }

            // Split into lines that fit the width
            val cleanText = textContent.replace("\r", "")
            val rawLines = cleanText.split("\n")
            val wrappedLines = mutableListOf<String>()

            for (rawLine in rawLines) {
                if (rawLine.isBlank()) {
                    wrappedLines.add("")
                    continue
                }
                var currentLine = ""
                val words = rawLine.split(" ")
                for (word in words) {
                    val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
                    if (textPaint.measureText(testLine) <= contentWidth) {
                        currentLine = testLine
                    } else {
                        if (currentLine.isNotEmpty()) wrappedLines.add(currentLine)
                        currentLine = word
                    }
                }
                if (currentLine.isNotEmpty()) wrappedLines.add(currentLine)
            }

            val lineHeight = 16f
            val startY = 100f
            val maxLinesPerPage = ((pageHeight - startY - 60f) / lineHeight).toInt()

            var currentLineIndex = 0
            var pageNumber = 1

            while (currentLineIndex < wrappedLines.size || pageNumber == 1) {
                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                val page = pdfDoc.startPage(pageInfo)
                val canvas: Canvas = page.canvas

                // Header
                canvas.drawText("UmarxonAI Universal Intellekt — Hujjat Tahlili", margin, 40f, headerPaint)
                canvas.drawLine(margin, 48f, pageWidth - margin, 48f, headerPaint)

                // Title on first page
                var y = startY
                if (pageNumber == 1) {
                    canvas.drawText(title.take(45), margin, 75f, titlePaint)
                }

                // Lines
                var linesOnThisPage = 0
                while (currentLineIndex < wrappedLines.size && linesOnThisPage < maxLinesPerPage) {
                    val line = wrappedLines[currentLineIndex]
                    canvas.drawText(line, margin, y, textPaint)
                    y += lineHeight
                    linesOnThisPage++
                    currentLineIndex++
                }

                // Footer
                val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
                canvas.drawText("Yaratildi: $dateStr | Sahifa: $pageNumber", margin, pageHeight - 30f, footerPaint)
                pdfDoc.finishPage(page)

                pageNumber++
                if (currentLineIndex >= wrappedLines.size) break
            }

            val cachePath = File(context.cacheDir, "documents")
            cachePath.mkdirs()
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val file = File(cachePath, "UmarxonAI_Hujjat_$timeStamp.pdf")
            val outputStream = FileOutputStream(file)
            pdfDoc.writeTo(outputStream)
            outputStream.close()
            pdfDoc.close()

            // Open or share PDF
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, title)
                putExtra(Intent.EXTRA_TEXT, "UmarxonAI orqali tayyorlangan PDF hujjat")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "PDF ni ulashish"))
            Toast.makeText(context, "📄 PDF yaratildi va ochildi!", Toast.LENGTH_SHORT).show()
            file
        } catch (e: Exception) {
            Toast.makeText(context, "PDF yaratishda xatolik: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            null
        }
    }

    /**
     * PDF yoki hujjat faylini tahlil qilish uchun o'qish (PDF -> Rasm sahifasi yoki matn)
     */
    data class DocumentAnalysisResult(
        val extractedText: String?,
        val pageImageBase64: String?,
        val documentName: String,
        val pageCount: Int = 1
    )

    fun processPickedDocument(context: Context, uri: Uri): DocumentAnalysisResult? {
        return try {
            val contentResolver = context.contentResolver
            val mimeType = contentResolver.getType(uri) ?: ""
            var fileName = "Hujjat"

            // Get file name
            val cursor = contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        fileName = it.getString(nameIndex) ?: "Hujjat"
                    }
                }
            }

            if (mimeType.contains("pdf") || fileName.endsWith(".pdf", ignoreCase = true)) {
                // Render first page of PDF as Bitmap for Gemini Vision
                val pfd: ParcelFileDescriptor? = contentResolver.openFileDescriptor(uri, "r")
                if (pfd != null) {
                    val pdfRenderer = PdfRenderer(pfd)
                    val pageCount = pdfRenderer.pageCount
                    if (pageCount > 0) {
                        val page = pdfRenderer.openPage(0)
                        val bitmap = Bitmap.createBitmap(
                            (page.width * 1.5).toInt(),
                            (page.height * 1.5).toInt(),
                            Bitmap.Config.ARGB_8888
                        )
                        canvasWhite(bitmap)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        page.close()
                        pdfRenderer.close()
                        pfd.close()

                        val out = ByteArrayOutputStream()
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
                        val base64 = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)

                        return DocumentAnalysisResult(
                            extractedText = null,
                            pageImageBase64 = base64,
                            documentName = fileName,
                            pageCount = pageCount
                        )
                    }
                }
            }

            // If text / code / csv / markdown
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            val text = inputStream?.bufferedReader()?.use { it.readText() }
            if (!text.isNullOrBlank()) {
                return DocumentAnalysisResult(
                    extractedText = text.take(15000),
                    pageImageBase64 = null,
                    documentName = fileName,
                    pageCount = 1
                )
            }

            null
        } catch (e: Exception) {
            null
        }
    }

    private fun canvasWhite(bitmap: Bitmap) {
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
    }

    /**
     * Kesh hajmini hisoblash va tozalash
     */
    fun getAppCacheSizeFormatted(context: Context): String {
        return try {
            var size = getFolderSize(context.cacheDir)
            context.externalCacheDir?.let { size += getFolderSize(it) }
            val mb = size / (1024.0 * 1024.0)
            String.format(Locale.getDefault(), "%.1f MB", mb)
        } catch (e: Exception) {
            "0.0 MB"
        }
    }

    private fun getFolderSize(dir: File?): Long {
        if (dir == null || !dir.exists()) return 0
        var result: Long = 0
        val fileList = dir.listFiles() ?: return 0
        for (file in fileList) {
            result += if (file.isDirectory) {
                getFolderSize(file)
            } else {
                file.length()
            }
        }
        return result
    }

    fun clearAppCache(context: Context): Boolean {
        return try {
            deleteDir(context.cacheDir)
            context.externalCacheDir?.let { deleteDir(it) }
            Toast.makeText(context, "🧹 Kesh muvaffaqiyatli tozalandi!", Toast.LENGTH_SHORT).show()
            true
        } catch (e: Exception) {
            Toast.makeText(context, "Keshni tozalashda xatolik: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            false
        }
    }

    private fun deleteDir(dir: File?): Boolean {
        if (dir != null && dir.isDirectory) {
            val children = dir.list() ?: return true
            for (child in children) {
                val success = deleteDir(File(dir, child))
                if (!success) return false
            }
            return dir.delete()
        } else if (dir != null && dir.isFile) {
            return dir.delete()
        }
        return false
    }
}
