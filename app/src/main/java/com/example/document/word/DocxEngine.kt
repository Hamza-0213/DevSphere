package com.example.document.word

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.io.StringReader
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

data class TextRun(
    val text: String,
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val isUnderline: Boolean = false,
    val isStrike: Boolean = false,
    val colorHex: String? = null,
    val sizeSp: Float = 16f
)

sealed class DocxElement {
    data class Paragraph(
        val runs: List<TextRun>,
        val isHeading: Boolean = false,
        val headingLevel: Int = 0,
        val isTitle: Boolean = false,
        val isBullet: Boolean = false,
        val alignment: String = "left"
    ) : DocxElement()

    data class Table(
        val rows: List<TableRow>
    ) : DocxElement()
}

data class TableRow(
    val cells: List<TableCell>
)

data class TableCell(
    val paragraphs: List<DocxElement.Paragraph>,
    val isHeader: Boolean = false
)

data class DocxDocument(
    val title: String,
    val elements: List<DocxElement>,
    val wordCount: Int,
    val paragraphCount: Int,
    val tableCount: Int
)

class DocxEngine(private val context: Context) {

    suspend fun parseDocx(uri: Uri): Result<DocxDocument> = withContext(Dispatchers.IO) {
        var inputStream: InputStream? = null
        try {
            inputStream = if (uri.scheme == "file") {
                FileInputStream(File(uri.path ?: ""))
            } else {
                context.contentResolver.openInputStream(uri)
            }

            if (inputStream == null) {
                return@withContext Result.failure(Exception("Cannot open document stream"))
            }

            var documentXmlContent: String? = null
            val zip = ZipInputStream(inputStream)
            var entry: ZipEntry? = zip.nextEntry
            while (entry != null) {
                if (entry.name == "word/document.xml") {
                    documentXmlContent = zip.bufferedReader(Charsets.UTF_8).readText()
                    break
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }

            if (documentXmlContent == null) {
                // If not an OpenXML zip, try fallback text extraction for legacy doc / rtf
                return@withContext parseLegacyDocFallback(uri)
            }

            val elements = parseDocumentXml(documentXmlContent)
            var wordCount = 0
            var paraCount = 0
            var tableCount = 0

            for (el in elements) {
                when (el) {
                    is DocxElement.Paragraph -> {
                        paraCount++
                        val fullText = el.runs.joinToString("") { it.text }
                        wordCount += fullText.split("\\s+".toRegex()).count { it.isNotBlank() }
                    }
                    is DocxElement.Table -> {
                        tableCount++
                        for (row in el.rows) {
                            for (cell in row.cells) {
                                for (p in cell.paragraphs) {
                                    val cellText = p.runs.joinToString("") { it.text }
                                    wordCount += cellText.split("\\s+".toRegex()).count { it.isNotBlank() }
                                }
                            }
                        }
                    }
                }
            }

            val docTitle = (elements.firstOrNull { it is DocxElement.Paragraph && (it.isTitle || it.isHeading) } as? DocxElement.Paragraph)
                ?.runs?.joinToString("") { it.text } ?: "Word Document"

            Result.success(
                DocxDocument(
                    title = docTitle,
                    elements = elements,
                    wordCount = wordCount,
                    paragraphCount = paraCount,
                    tableCount = tableCount
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(Exception("Failed to parse Word document: ${e.localizedMessage}"))
        } finally {
            try {
                inputStream?.close()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    private fun parseDocumentXml(xml: String): List<DocxElement> {
        val elements = mutableListOf<DocxElement>()
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = true
        val parser = factory.newPullParser()
        parser.setInput(StringReader(xml))

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                val tag = parser.name
                if (tag == "p" || tag.endsWith(":p")) {
                    elements.add(parseParagraph(parser))
                } else if (tag == "tbl" || tag.endsWith(":tbl")) {
                    elements.add(parseTable(parser))
                }
            }
            eventType = parser.next()
        }
        return elements
    }

    private fun parseParagraph(parser: XmlPullParser): DocxElement.Paragraph {
        val runs = mutableListOf<TextRun>()
        var isHeading = false
        var headingLevel = 0
        var isTitle = false
        var isBullet = false
        var alignment = "left"

        var eventType = parser.next()
        while (!(eventType == XmlPullParser.END_TAG && (parser.name == "p" || parser.name.endsWith(":p")))) {
            if (eventType == XmlPullParser.START_TAG) {
                val tag = parser.name
                if (tag == "pStyle" || tag.endsWith(":pStyle")) {
                    val styleVal = parser.getAttributeValue(null, "val") ?: ""
                    if (styleVal.contains("Title", ignoreCase = true)) {
                        isTitle = true
                    } else if (styleVal.contains("Heading", ignoreCase = true)) {
                        isHeading = true
                        headingLevel = styleVal.filter { it.isDigit() }.toIntOrNull() ?: 1
                    }
                } else if (tag == "numPr" || tag.endsWith(":numPr")) {
                    isBullet = true
                } else if (tag == "jc" || tag.endsWith(":jc")) {
                    alignment = parser.getAttributeValue(null, "val") ?: "left"
                } else if (tag == "r" || tag.endsWith(":r")) {
                    runs.add(parseRun(parser))
                }
            }
            eventType = parser.next()
        }

        return DocxElement.Paragraph(
            runs = runs,
            isHeading = isHeading,
            headingLevel = headingLevel,
            isTitle = isTitle,
            isBullet = isBullet,
            alignment = alignment
        )
    }

    private fun parseRun(parser: XmlPullParser): TextRun {
        var text = ""
        var isBold = false
        var isItalic = false
        var isUnderline = false
        var isStrike = false
        var colorHex: String? = null
        var sizeSp = 16f

        var eventType = parser.next()
        while (!(eventType == XmlPullParser.END_TAG && (parser.name == "r" || parser.name.endsWith(":r")))) {
            if (eventType == XmlPullParser.START_TAG) {
                val tag = parser.name
                when {
                    tag == "b" || tag.endsWith(":b") -> isBold = true
                    tag == "i" || tag.endsWith(":i") -> isItalic = true
                    tag == "u" || tag.endsWith(":u") -> isUnderline = true
                    tag == "strike" || tag.endsWith(":strike") -> isStrike = true
                    tag == "color" || tag.endsWith(":color") -> {
                        colorHex = parser.getAttributeValue(null, "val")
                    }
                    tag == "sz" || tag.endsWith(":sz") -> {
                        val szVal = parser.getAttributeValue(null, "val")?.toFloatOrNull()
                        if (szVal != null) {
                            sizeSp = (szVal / 2f).coerceIn(10f, 36f) // Half-points to pt/sp
                        }
                    }
                    tag == "t" || tag.endsWith(":t") -> {
                        text = parser.nextText()
                    }
                }
            }
            eventType = parser.next()
        }

        return TextRun(
            text = text,
            isBold = isBold,
            isItalic = isItalic,
            isUnderline = isUnderline,
            isStrike = isStrike,
            colorHex = colorHex,
            sizeSp = sizeSp
        )
    }

    private fun parseTable(parser: XmlPullParser): DocxElement.Table {
        val rows = mutableListOf<TableRow>()
        var eventType = parser.next()
        while (!(eventType == XmlPullParser.END_TAG && (parser.name == "tbl" || parser.name.endsWith(":tbl")))) {
            if (eventType == XmlPullParser.START_TAG && (parser.name == "tr" || parser.name.endsWith(":tr"))) {
                rows.add(parseTableRow(parser, isFirstRow = rows.isEmpty()))
            }
            eventType = parser.next()
        }
        return DocxElement.Table(rows = rows)
    }

    private fun parseTableRow(parser: XmlPullParser, isFirstRow: Boolean): TableRow {
        val cells = mutableListOf<TableCell>()
        var eventType = parser.next()
        while (!(eventType == XmlPullParser.END_TAG && (parser.name == "tr" || parser.name.endsWith(":tr")))) {
            if (eventType == XmlPullParser.START_TAG && (parser.name == "tc" || parser.name.endsWith(":tc"))) {
                val paragraphs = mutableListOf<DocxElement.Paragraph>()
                var cellEventType = parser.next()
                while (!(cellEventType == XmlPullParser.END_TAG && (parser.name == "tc" || parser.name.endsWith(":tc")))) {
                    if (cellEventType == XmlPullParser.START_TAG && (parser.name == "p" || parser.name.endsWith(":p"))) {
                        paragraphs.add(parseParagraph(parser))
                    }
                    cellEventType = parser.next()
                }
                cells.add(TableCell(paragraphs = paragraphs, isHeader = isFirstRow))
            }
            eventType = parser.next()
        }
        return TableRow(cells = cells)
    }

    private suspend fun parseLegacyDocFallback(uri: Uri): Result<DocxDocument> = withContext(Dispatchers.IO) {
        try {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return@withContext Result.failure(Exception("Empty document"))

            // Extract ASCII/UTF text streams from binary file
            val textBuilder = StringBuilder()
            var currentWord = StringBuilder()
            for (b in bytes) {
                val c = b.toInt().toChar()
                if (c in ' '..'~' || c == '\n' || c == '\t') {
                    currentWord.append(c)
                } else {
                    if (currentWord.length > 3) {
                        textBuilder.append(currentWord.toString()).append(" ")
                    }
                    currentWord.clear()
                }
            }

            val extractedLines = textBuilder.toString().split("\n").filter { it.isNotBlank() }
            val elements = extractedLines.map { line ->
                DocxElement.Paragraph(
                    runs = listOf(TextRun(text = line.trim(), sizeSp = 15f))
                )
            }

            Result.success(
                DocxDocument(
                    title = "Word Document (Legacy DOC)",
                    elements = elements,
                    wordCount = extractedLines.sumOf { it.split(" ").size },
                    paragraphCount = elements.size,
                    tableCount = 0
                )
            )
        } catch (e: Exception) {
            Result.failure(Exception("Cannot parse binary doc file: ${e.localizedMessage}"))
        }
    }
}
