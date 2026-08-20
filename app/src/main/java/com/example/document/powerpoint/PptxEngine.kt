package com.example.document.powerpoint

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

data class PptxSlide(
    val slideNumber: Int,
    val title: String,
    val textBlocks: List<String> = emptyList(),
    val bulletPoints: List<String> = emptyList(),
    val notes: String? = null,
    val subtitle: String? = null
)

data class PptxPresentation(
    val title: String,
    val slides: List<PptxSlide>,
    val totalSlides: Int,
    val format: String = "PowerPoint Presentation"
)

class PptxEngine(private val context: Context) {

    suspend fun parsePptx(uri: Uri): Result<PptxPresentation> = withContext(Dispatchers.IO) {
        try {
            val bytes = readBytesFromUri(uri)
            if (bytes == null || bytes.isEmpty()) {
                return@withContext Result.failure(Exception("Cannot open presentation: File is empty or inaccessible"))
            }

            val defaultTitle = uri.lastPathSegment?.substringAfterLast("/")?.substringBeforeLast(".")
                ?: "PowerPoint Presentation"

            // Check if file is OpenXML ZIP (.pptx / .ppsx / .potx)
            if (isZipArchive(bytes)) {
                try {
                    val pres = parseOpenXmlPresentation(bytes, defaultTitle)
                    if (pres.slides.isNotEmpty()) {
                        return@withContext Result.success(pres)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    // If ZIP parsing encountered an issue, fallback to binary parser
                }
            }

            // Parse legacy binary PPT (.ppt) or compound binary file
            val binaryPres = PptBinaryParser.parsePptBytes(bytes, defaultTitle)
            Result.success(binaryPres)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(Exception("Failed to view PowerPoint presentation: ${e.localizedMessage}"))
        }
    }

    private fun isZipArchive(bytes: ByteArray): Boolean {
        if (bytes.size < 4) return false
        // ZIP magic header 'PK\x03\x04' or empty archive 'PK\x05\x06'
        return bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte() &&
            (bytes[2] == 0x03.toByte() || bytes[2] == 0x05.toByte())
    }

    private fun readBytesFromUri(uri: Uri): ByteArray? {
        return try {
            if (uri.scheme == "file") {
                val f = File(uri.path ?: "")
                if (f.exists() && f.canRead()) f.readBytes() else null
            } else {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun parseOpenXmlPresentation(bytes: ByteArray, defaultTitle: String): PptxPresentation {
        val zipEntries = mutableMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            var entry: ZipEntry? = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    zipEntries[entry.name] = zip.readBytes()
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }

        // Find all slide*.xml in ppt/slides/
        val slideEntries = zipEntries.filter { (key, _) ->
            key.startsWith("ppt/slides/slide") && key.endsWith(".xml") && !key.contains("_rels")
        }.toSortedMap(compareBy { path ->
            // Sort by slide number: slide1.xml, slide2.xml, slide10.xml
            val numberPart = path.substringAfterLast("slide").substringBefore(".xml")
            numberPart.toIntOrNull() ?: 0
        })

        // Find all note slides in ppt/notesSlides/notesSlide*.xml
        val noteEntries = zipEntries.filter { (key, _) ->
            key.startsWith("ppt/notesSlides/notesSlide") && key.endsWith(".xml")
        }

        val slides = mutableListOf<PptxSlide>()
        var slideNum = 1

        for ((slidePath, data) in slideEntries) {
            // Find corresponding note text if any
            val slideIdx = slidePath.substringAfterLast("slide").substringBefore(".xml")
            val noteKey = "ppt/notesSlides/notesSlide$slideIdx.xml"
            val noteBytes = noteEntries[noteKey]
            val noteText = noteBytes?.let { extractNoteText(it) }

            val slide = parseSlideXml(data, slideNum, noteText)
            slides.add(slide)
            slideNum++
        }

        if (slides.isEmpty()) {
            return PptBinaryParser.parsePptBytes(bytes, defaultTitle)
        }

        val presTitle = slides.firstOrNull()?.title?.takeIf { it.isNotBlank() } ?: defaultTitle

        return PptxPresentation(
            title = presTitle,
            slides = slides,
            totalSlides = slides.size,
            format = "PowerPoint Presentation (.pptx)"
        )
    }

    private fun parseSlideXml(bytes: ByteArray, slideNumber: Int, speakerNote: String?): PptxSlide {
        val factory = XmlPullParserFactory.newInstance()
        val parser = factory.newPullParser()
        parser.setInput(ByteArrayInputStream(bytes), "UTF-8")

        var title = ""
        var subtitle = ""
        val textBlocks = mutableListOf<String>()
        val bulletPoints = mutableListOf<String>()

        var currentParagraphText = java.lang.StringBuilder()
        var isTitleShape = false
        var isSubTitleShape = false
        var isBulletParagraph = false

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                val tag = parser.name
                when {
                    tag == "p" || tag.endsWith(":p") -> {
                        currentParagraphText.clear()
                        isBulletParagraph = false
                    }
                    tag == "ph" || tag.endsWith(":ph") -> {
                        val phType = parser.getAttributeValue(null, "type")
                        if (phType == "title" || phType == "ctrTitle") {
                            isTitleShape = true
                        } else if (phType == "subTitle") {
                            isSubTitleShape = true
                        }
                    }
                    tag == "buChar" || tag.endsWith(":buChar") || tag == "buAutoNum" || tag.endsWith(":buAutoNum") -> {
                        isBulletParagraph = true
                    }
                    tag == "t" || tag.endsWith(":t") -> {
                        val text = parser.nextText()
                        currentParagraphText.append(text)
                    }
                }
            } else if (eventType == XmlPullParser.END_TAG) {
                val tag = parser.name
                if (tag == "p" || tag.endsWith(":p")) {
                    val paraStr = currentParagraphText.toString().trim()
                    if (paraStr.isNotBlank()) {
                        when {
                            isTitleShape && title.isBlank() -> {
                                title = paraStr
                            }
                            isSubTitleShape && subtitle.isBlank() -> {
                                subtitle = paraStr
                            }
                            isBulletParagraph || paraStr.startsWith("•") || paraStr.startsWith("-") || paraStr.startsWith("*") -> {
                                bulletPoints.add(paraStr.removePrefix("•").removePrefix("-").removePrefix("*").trim())
                            }
                            else -> {
                                textBlocks.add(paraStr)
                            }
                        }
                    }
                    currentParagraphText.clear()
                } else if (tag == "sp" || tag.endsWith(":sp")) {
                    isTitleShape = false
                    isSubTitleShape = false
                }
            }
            eventType = parser.next()
        }

        if (title.isBlank() && textBlocks.isNotEmpty()) {
            title = textBlocks.first()
            textBlocks.removeAt(0)
        }

        return PptxSlide(
            slideNumber = slideNumber,
            title = title.ifBlank { "Slide $slideNumber" },
            textBlocks = textBlocks,
            bulletPoints = bulletPoints,
            notes = speakerNote,
            subtitle = subtitle.takeIf { it.isNotBlank() }
        )
    }

    private fun extractNoteText(bytes: ByteArray): String? {
        return try {
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(ByteArrayInputStream(bytes), "UTF-8")

            val noteBuilder = java.lang.StringBuilder()
            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    val tag = parser.name
                    if (tag == "t" || tag.endsWith(":t")) {
                        val text = parser.nextText()
                        noteBuilder.append(text).append(" ")
                    }
                }
                eventType = parser.next()
            }
            noteBuilder.toString().trim().takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            null
        }
    }
}
