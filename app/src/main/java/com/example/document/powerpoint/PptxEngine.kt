package com.example.document.powerpoint

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.ByteArrayInputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

enum class PptElementType {
    TEXT_BOX,
    IMAGE,
    SHAPE,
    TABLE
}

data class PptTextRun(
    val text: String,
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val isUnderline: Boolean = false,
    val fontSizeSp: Float = 14f,
    val fontColorHex: Long? = null,
    val isBullet: Boolean = false,
    val bulletChar: String? = null
)

data class PptSlideElement(
    val type: PptElementType,
    val xRatio: Float = 0.05f,
    val yRatio: Float = 0.05f,
    val widthRatio: Float = 0.90f,
    val heightRatio: Float = 0.30f,
    val title: String? = null,
    val textRuns: List<PptTextRun> = emptyList(),
    val plainText: String = "",
    val imageBytes: ByteArray? = null,
    val imageMimeType: String? = null,
    val backgroundColorHex: Long? = null,
    val borderColorHex: Long? = null,
    val borderWidthDp: Float = 0f,
    val cornerRadiusDp: Float = 0f,
    val shapeType: String? = null,
    val tableRows: List<List<String>> = emptyList(),
    val alignment: String = "left" // left, center, right
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as PptSlideElement
        return type == other.type &&
            xRatio == other.xRatio &&
            yRatio == other.yRatio &&
            widthRatio == other.widthRatio &&
            heightRatio == other.heightRatio &&
            title == other.title &&
            textRuns == other.textRuns &&
            plainText == other.plainText &&
            imageBytes.contentEquals(other.imageBytes) &&
            backgroundColorHex == other.backgroundColorHex
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + xRatio.hashCode()
        result = 31 * result + yRatio.hashCode()
        result = 31 * result + (title?.hashCode() ?: 0)
        result = 31 * result + (imageBytes?.contentHashCode() ?: 0)
        return result
    }
}

data class PptSlide(
    val slideNumber: Int,
    val title: String,
    val subtitle: String? = null,
    val textBlocks: List<String> = emptyList(),
    val bulletPoints: List<String> = emptyList(),
    val elements: List<PptSlideElement> = emptyList(),
    val images: List<ByteArray> = emptyList(),
    val backgroundColorHex: Long? = null,
    val notes: String? = null,
    val themeVariant: Int = (slideNumber % 6)
)

data class PptxPresentation(
    val title: String,
    val slides: List<PptSlide>,
    val totalSlides: Int,
    val format: String = "PowerPoint Presentation",
    val slideWidthRatio: Float = 16f,
    val slideHeightRatio: Float = 9f
)

// Backwards compatibility alias
typealias PptxSlide = PptSlide

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

        // 1. Parse presentation.xml for slide dimensions & slide order
        var slideWidthEmu = 12192000L // default 16:9 (13.33 in)
        var slideHeightEmu = 6858000L // (7.5 in)
        val orderedSlideRIds = mutableListOf<String>()

        val presXmlBytes = zipEntries["ppt/presentation.xml"]
        if (presXmlBytes != null) {
            try {
                val factory = XmlPullParserFactory.newInstance()
                val parser = factory.newPullParser()
                parser.setInput(ByteArrayInputStream(presXmlBytes), "UTF-8")
                var event = parser.eventType
                while (event != XmlPullParser.END_DOCUMENT) {
                    if (event == XmlPullParser.START_TAG) {
                        val name = parser.name
                        if (name == "sldSz" || name.endsWith(":sldSz")) {
                            val cx = parser.getAttributeValue(null, "cx")?.toLongOrNull()
                            val cy = parser.getAttributeValue(null, "cy")?.toLongOrNull()
                            if (cx != null && cx > 0) slideWidthEmu = cx
                            if (cy != null && cy > 0) slideHeightEmu = cy
                        } else if (name == "sldId" || name.endsWith(":sldId")) {
                            val rId = parser.getAttributeValue(null, "id")
                                ?: parser.getAttributeValue("http://schemas.openxmlformats.org/officeDocument/2006/relationships", "id")
                                ?: parser.getAttributeValue(null, "r:id")
                            if (!rId.isNullOrBlank()) {
                                orderedSlideRIds.add(rId)
                            }
                        }
                    }
                    event = parser.next()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 2. Parse presentation.xml.rels for rId -> slide path mapping
        val presRelsMap = mutableMapOf<String, String>()
        val presRelsBytes = zipEntries["ppt/_rels/presentation.xml.rels"]
        if (presRelsBytes != null) {
            presRelsMap.putAll(parseRelationships(presRelsBytes, "ppt/"))
        }

        // Find all slide*.xml in ppt/slides/
        val slidePathList = mutableListOf<String>()
        if (orderedSlideRIds.isNotEmpty() && presRelsMap.isNotEmpty()) {
            for (rId in orderedSlideRIds) {
                val target = presRelsMap[rId]
                if (target != null && zipEntries.containsKey(target)) {
                    slidePathList.add(target)
                }
            }
        }

        // Fallback sort if rels didn't yield all slides
        if (slidePathList.isEmpty()) {
            val sortedEntries = zipEntries.filter { (key, _) ->
                key.startsWith("ppt/slides/slide") && key.endsWith(".xml") && !key.contains("_rels")
            }.keys.sortedBy { path ->
                val numberPart = path.substringAfterLast("slide").substringBefore(".xml")
                numberPart.toIntOrNull() ?: 0
            }
            slidePathList.addAll(sortedEntries)
        }

        // Find all note slides in ppt/notesSlides/notesSlide*.xml
        val noteEntries = zipEntries.filter { (key, _) ->
            key.startsWith("ppt/notesSlides/notesSlide") && key.endsWith(".xml")
        }

        val slides = mutableListOf<PptSlide>()
        var slideNum = 1

        for (slidePath in slidePathList) {
            val slideXmlData = zipEntries[slidePath] ?: continue

            // Parse slide relationships (images, media, etc.)
            val slideFileName = slidePath.substringAfterLast("/")
            val slideRelsKey = "ppt/slides/_rels/$slideFileName.rels"
            val slideRelsBytes = zipEntries[slideRelsKey]
            val slideMediaMap = if (slideRelsBytes != null) {
                parseRelationships(slideRelsBytes, "ppt/slides/")
            } else {
                emptyMap()
            }

            // Find corresponding note text if any
            val slideIdx = slidePath.substringAfterLast("slide").substringBefore(".xml")
            val noteKey = "ppt/notesSlides/notesSlide$slideIdx.xml"
            val noteBytes = noteEntries[noteKey]
            val noteText = noteBytes?.let { extractNoteText(it) }

            val slide = parseRichSlideXml(
                slideXmlData = slideXmlData,
                slideNumber = slideNum,
                speakerNote = noteText,
                mediaRels = slideMediaMap,
                zipEntries = zipEntries,
                slideWidthEmu = slideWidthEmu,
                slideHeightEmu = slideHeightEmu
            )
            slides.add(slide)
            slideNum++
        }

        if (slides.isEmpty()) {
            return PptBinaryParser.parsePptBytes(bytes, defaultTitle)
        }

        val presTitle = slides.firstOrNull()?.title?.takeIf { it.isNotBlank() } ?: defaultTitle
        val widthRatio = if (slideWidthEmu > 0) slideWidthEmu.toFloat() / 914400f else 16f
        val heightRatio = if (slideHeightEmu > 0) slideHeightEmu.toFloat() / 914400f else 9f

        return PptxPresentation(
            title = presTitle,
            slides = slides,
            totalSlides = slides.size,
            format = "PowerPoint Presentation (.pptx)",
            slideWidthRatio = widthRatio,
            slideHeightRatio = heightRatio
        )
    }

    private fun parseRelationships(relsBytes: ByteArray, basePath: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        try {
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(ByteArrayInputStream(relsBytes), "UTF-8")
            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG && (parser.name == "Relationship" || parser.name.endsWith(":Relationship"))) {
                    val id = parser.getAttributeValue(null, "Id")
                    val target = parser.getAttributeValue(null, "Target")
                    if (!id.isNullOrBlank() && !target.isNullOrBlank()) {
                        // Normalize target path relative to ppt/
                        val normalized = normalizeZipPath(basePath, target)
                        result[id] = normalized
                    }
                }
                event = parser.next()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }

    private fun normalizeZipPath(basePath: String, target: String): String {
        if (target.startsWith("/")) return target.removePrefix("/")
        val parts = (basePath.split("/") + target.split("/")).filter { it.isNotBlank() && it != "." }
        val resolved = mutableListOf<String>()
        for (p in parts) {
            if (p == "..") {
                if (resolved.isNotEmpty()) resolved.removeAt(resolved.size - 1)
            } else {
                resolved.add(p)
            }
        }
        return resolved.joinToString("/")
    }

    private fun parseRichSlideXml(
        slideXmlData: ByteArray,
        slideNumber: Int,
        speakerNote: String?,
        mediaRels: Map<String, String>,
        zipEntries: Map<String, ByteArray>,
        slideWidthEmu: Long,
        slideHeightEmu: Long
    ): PptSlide {
        val elements = mutableListOf<PptSlideElement>()
        val slideImages = mutableListOf<ByteArray>()
        var slideBgColor: Long? = null

        val factory = XmlPullParserFactory.newInstance()
        val parser = factory.newPullParser()
        parser.setInput(ByteArrayInputStream(slideXmlData), "UTF-8")

        var title = ""
        var subtitle = ""
        val textBlocks = mutableListOf<String>()
        val bulletPoints = mutableListOf<String>()

        // Parsing state variables
        var currentTag = ""
        var currentParagraphText = StringBuilder()
        var currentParagraphRuns = mutableListOf<PptTextRun>()
        var isTitleShape = false
        var isSubTitleShape = false
        var isBulletParagraph = false
        var currentBulletChar = "•"
        var currentAlignment = "left"

        // Element bounding box state
        var currentX = 0L
        var currentY = 0L
        var currentCx = 0L
        var currentCy = 0L
        var currentShapeBgColor: Long? = null
        var currentShapeBorderColor: Long? = null
        var currentShapeType: String? = null

        // Run formatting state
        var currentRunBold = false
        var currentRunItalic = false
        var currentRunUnderline = false
        var currentRunFontSize = 14f
        var currentRunColor: Long? = null

        // Table state
        var isInTable = false
        var currentTableRows = mutableListOf<MutableList<String>>()
        var currentTableRow = mutableListOf<String>()
        var currentTableCellText = StringBuilder()

        // Picture / Image state
        var currentPictureEmbedId: String? = null

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                val tag = parser.name.substringAfter(":")
                currentTag = tag

                when (tag) {
                    "bg" -> {
                        // Background definition
                    }
                    "srgbClr" -> {
                        val hexVal = parser.getAttributeValue(null, "val")
                        if (!hexVal.isNullOrBlank()) {
                            try {
                                val parsedColor = 0xFF000000L or hexVal.toLong(16)
                                if (currentTag == "bg" || slideBgColor == null) {
                                    slideBgColor = parsedColor
                                } else if (currentRunColor == null) {
                                    currentRunColor = parsedColor
                                }
                            } catch (e: Exception) {
                                // ignore
                            }
                        }
                    }
                    "off" -> {
                        val xStr = parser.getAttributeValue(null, "x")?.toLongOrNull()
                        val yStr = parser.getAttributeValue(null, "y")?.toLongOrNull()
                        if (xStr != null) currentX = xStr
                        if (yStr != null) currentY = yStr
                    }
                    "ext" -> {
                        val cxStr = parser.getAttributeValue(null, "cx")?.toLongOrNull()
                        val cyStr = parser.getAttributeValue(null, "cy")?.toLongOrNull()
                        if (cxStr != null) currentCx = cxStr
                        if (cyStr != null) currentCy = cyStr
                    }
                    "prstGeom" -> {
                        currentShapeType = parser.getAttributeValue(null, "prst")
                    }
                    "solidFill" -> {
                        // Will encounter srgbClr or schemeClr inside
                    }
                    "schemeClr" -> {
                        val schemeVal = parser.getAttributeValue(null, "val")
                        val mappedColor = mapSchemeColor(schemeVal)
                        if (mappedColor != null && currentRunColor == null) {
                            currentRunColor = mappedColor
                        }
                    }
                    "ph" -> {
                        val phType = parser.getAttributeValue(null, "type")
                        if (phType == "title" || phType == "ctrTitle") {
                            isTitleShape = true
                        } else if (phType == "subTitle") {
                            isSubTitleShape = true
                        }
                    }
                    "p" -> {
                        currentParagraphText.clear()
                        currentParagraphRuns.clear()
                        isBulletParagraph = false
                        currentBulletChar = "•"
                        currentAlignment = "left"
                    }
                    "pPr" -> {
                        val algn = parser.getAttributeValue(null, "algn")
                        if (algn != null) {
                            currentAlignment = when (algn) {
                                "ctr" -> "center"
                                "r" -> "right"
                                "just" -> "justify"
                                else -> "left"
                            }
                        }
                    }
                    "buChar" -> {
                        isBulletParagraph = true
                        val ch = parser.getAttributeValue(null, "char")
                        if (!ch.isNullOrBlank()) currentBulletChar = ch
                    }
                    "buAutoNum" -> {
                        isBulletParagraph = true
                        currentBulletChar = "1."
                    }
                    "r" -> {
                        currentRunBold = false
                        currentRunItalic = false
                        currentRunUnderline = false
                        currentRunFontSize = 14f
                        currentRunColor = null
                    }
                    "rPr" -> {
                        val szStr = parser.getAttributeValue(null, "sz")?.toFloatOrNull()
                        if (szStr != null && szStr > 0) {
                            currentRunFontSize = szStr / 100f // 2400 sz = 24pt
                        }
                        val bStr = parser.getAttributeValue(null, "b")
                        if (bStr == "1" || bStr == "true") currentRunBold = true

                        val iStr = parser.getAttributeValue(null, "i")
                        if (iStr == "1" || iStr == "true") currentRunItalic = true

                        val uStr = parser.getAttributeValue(null, "u")
                        if (uStr != null && uStr != "none") currentRunUnderline = true
                    }
                    "t" -> {
                        val text = parser.nextText()
                        currentParagraphText.append(text)
                        if (isInTable) {
                            currentTableCellText.append(text)
                        } else {
                            currentParagraphRuns.add(
                                PptTextRun(
                                    text = text,
                                    isBold = currentRunBold || isTitleShape,
                                    isItalic = currentRunItalic,
                                    isUnderline = currentRunUnderline,
                                    fontSizeSp = if (isTitleShape && currentRunFontSize < 18f) 22f else currentRunFontSize,
                                    fontColorHex = currentRunColor,
                                    isBullet = isBulletParagraph,
                                    bulletChar = if (isBulletParagraph) currentBulletChar else null
                                )
                            )
                        }
                    }
                    "blip" -> {
                        val embedId = parser.getAttributeValue("http://schemas.openxmlformats.org/officeDocument/2006/relationships", "embed")
                            ?: parser.getAttributeValue(null, "r:embed")
                        if (!embedId.isNullOrBlank()) {
                            currentPictureEmbedId = embedId
                        }
                    }
                    "tbl" -> {
                        isInTable = true
                        currentTableRows.clear()
                    }
                    "tr" -> {
                        currentTableRow = mutableListOf()
                    }
                    "tc" -> {
                        currentTableCellText.clear()
                    }
                }
            } else if (eventType == XmlPullParser.END_TAG) {
                val tag = parser.name.substringAfter(":")

                when (tag) {
                    "p" -> {
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
                    }
                    "tc" -> {
                        if (isInTable) {
                            currentTableRow.add(currentTableCellText.toString().trim())
                            currentTableCellText.clear()
                        }
                    }
                    "tr" -> {
                        if (isInTable && currentTableRow.isNotEmpty()) {
                            currentTableRows.add(currentTableRow.toMutableList())
                        }
                    }
                    "tbl" -> {
                        if (isInTable && currentTableRows.isNotEmpty()) {
                            val xR = if (slideWidthEmu > 0 && currentCx > 0) (currentX.toFloat() / slideWidthEmu).coerceIn(0.02f, 0.95f) else 0.05f
                            val yR = if (slideHeightEmu > 0 && currentCy > 0) (currentY.toFloat() / slideHeightEmu).coerceIn(0.02f, 0.95f) else 0.40f
                            val wR = if (slideWidthEmu > 0 && currentCx > 0) (currentCx.toFloat() / slideWidthEmu).coerceIn(0.1f, 0.95f) else 0.90f
                            val hR = if (slideHeightEmu > 0 && currentCy > 0) (currentCy.toFloat() / slideHeightEmu).coerceIn(0.05f, 0.85f) else 0.40f

                            elements.add(
                                PptSlideElement(
                                    type = PptElementType.TABLE,
                                    xRatio = xR,
                                    yRatio = yR,
                                    widthRatio = wR,
                                    heightRatio = hR,
                                    tableRows = currentTableRows.map { it.toList() }
                                )
                            )
                        }
                        isInTable = false
                    }
                    "pic" -> {
                        // Extract Picture element
                        if (currentPictureEmbedId != null) {
                            val mediaPath = mediaRels[currentPictureEmbedId]
                            val imageBytes = mediaPath?.let { zipEntries[it] }

                            if (imageBytes != null && imageBytes.isNotEmpty()) {
                                slideImages.add(imageBytes)

                                val xR = if (slideWidthEmu > 0 && currentCx > 0) (currentX.toFloat() / slideWidthEmu).coerceIn(0.01f, 0.98f) else 0.55f
                                val yR = if (slideHeightEmu > 0 && currentCy > 0) (currentY.toFloat() / slideHeightEmu).coerceIn(0.01f, 0.98f) else 0.25f
                                val wR = if (slideWidthEmu > 0 && currentCx > 0) (currentCx.toFloat() / slideWidthEmu).coerceIn(0.05f, 0.95f) else 0.40f
                                val hR = if (slideHeightEmu > 0 && currentCy > 0) (currentCy.toFloat() / slideHeightEmu).coerceIn(0.05f, 0.95f) else 0.55f

                                elements.add(
                                    PptSlideElement(
                                        type = PptElementType.IMAGE,
                                        xRatio = xR,
                                        yRatio = yR,
                                        widthRatio = wR,
                                        heightRatio = hR,
                                        imageBytes = imageBytes,
                                        cornerRadiusDp = 12f
                                    )
                                )
                            }
                        }
                        // Reset picture bounds
                        currentPictureEmbedId = null
                        currentX = 0L
                        currentY = 0L
                        currentCx = 0L
                        currentCy = 0L
                    }
                    "sp" -> {
                        // Shape or Text Box Element
                        val elementRuns = currentParagraphRuns.toList()
                        val elementText = currentParagraphText.toString().trim()

                        if (elementRuns.isNotEmpty() || elementText.isNotBlank() || currentShapeBgColor != null) {
                            val xR = if (slideWidthEmu > 0 && currentCx > 0) (currentX.toFloat() / slideWidthEmu).coerceIn(0.01f, 0.98f) else 0.05f
                            val yR = if (slideHeightEmu > 0 && currentCy > 0) (currentY.toFloat() / slideHeightEmu).coerceIn(0.01f, 0.98f) else (if (isTitleShape) 0.08f else 0.30f)
                            val wR = if (slideWidthEmu > 0 && currentCx > 0) (currentCx.toFloat() / slideWidthEmu).coerceIn(0.05f, 0.98f) else 0.90f
                            val hR = if (slideHeightEmu > 0 && currentCy > 0) (currentCy.toFloat() / slideHeightEmu).coerceIn(0.03f, 0.90f) else (if (isTitleShape) 0.15f else 0.55f)

                            elements.add(
                                PptSlideElement(
                                    type = if (elementRuns.isNotEmpty() || elementText.isNotBlank()) PptElementType.TEXT_BOX else PptElementType.SHAPE,
                                    xRatio = xR,
                                    yRatio = yR,
                                    widthRatio = wR,
                                    heightRatio = hR,
                                    title = if (isTitleShape) elementText else null,
                                    textRuns = elementRuns,
                                    plainText = elementText,
                                    backgroundColorHex = currentShapeBgColor,
                                    borderColorHex = currentShapeBorderColor,
                                    shapeType = currentShapeType,
                                    alignment = currentAlignment
                                )
                            )
                        }

                        // Reset shape state
                        isTitleShape = false
                        isSubTitleShape = false
                        currentX = 0L
                        currentY = 0L
                        currentCx = 0L
                        currentCy = 0L
                        currentShapeBgColor = null
                        currentShapeBorderColor = null
                        currentShapeType = null
                    }
                }
            }
            eventType = parser.next()
        }

        if (title.isBlank() && textBlocks.isNotEmpty()) {
            title = textBlocks.first()
            textBlocks.removeAt(0)
        }

        return PptSlide(
            slideNumber = slideNumber,
            title = title.ifBlank { "Slide $slideNumber" },
            subtitle = subtitle.takeIf { it.isNotBlank() },
            textBlocks = textBlocks,
            bulletPoints = bulletPoints,
            elements = elements,
            images = slideImages,
            backgroundColorHex = slideBgColor,
            notes = speakerNote,
            themeVariant = slideNumber % 6
        )
    }

    private fun mapSchemeColor(schemeName: String?): Long? {
        if (schemeName == null) return null
        return when (schemeName.lowercase()) {
            "accent1" -> 0xFF2563EBL // Blue
            "accent2" -> 0xFF059669L // Emerald
            "accent3" -> 0xFF7C3AEDL // Violet
            "accent4" -> 0xFFEA580CL // Amber Orange
            "accent5" -> 0xFF0284C7L // Sky
            "accent6" -> 0xFFDB2777L // Rose
            "dk1", "tx1" -> 0xFF0F172AL // Slate 900
            "lt1", "bg1" -> 0xFFFFFFFFL // White
            "dk2", "tx2" -> 0xFF334155L // Slate 700
            "lt2", "bg2" -> 0xFFF8FAFCL // Slate 50
            else -> null
        }
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

