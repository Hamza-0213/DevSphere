package com.example.document.powerpoint

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets

/**
 * Parser for Microsoft PowerPoint 97-2003 (.ppt) binary record structures.
 * Implements [MS-PPT] specification record atom traversal & fallback text extraction.
 */
object PptBinaryParser {

    // Record types according to MS-PPT specification
    private const val RT_DOCUMENT = 1000
    private const val RT_SLIDE = 1006
    private const val RT_NOTES = 1008
    private const val RT_SLIDE_LIST_WITH_TEXT = 1016
    private const val RT_SLIDE_PERSIST_ATOM = 3998
    private const val RT_TEXT_HEADER_ATOM = 4000
    private const val RT_TEXT_CHARS_ATOM = 4008
    private const val RT_TEXT_BYTES_ATOM = 4007
    private const val RT_C_STRING = 4006
    private const val RT_NOTES_ATOM = 4057

    data class RawSlideData(
        var slideId: Int = 0,
        val textRuns: MutableList<String> = mutableListOf(),
        var notes: String? = null
    )

    /**
     * Parse PowerPoint binary document stream or raw file bytes into slides.
     */
    fun parsePptBytes(bytes: ByteArray, defaultTitle: String): PptxPresentation {
        // First try to extract "PowerPoint Document" stream if it's an OLE2 container
        val pptDocumentBytes = if (Ole2Reader.isOle2File(bytes)) {
            val streams = Ole2Reader.extractStreams(bytes)
            streams["PowerPoint Document"] ?: bytes
        } else {
            bytes
        }

        val parsedSlides = parseRecords(pptDocumentBytes)
        if (parsedSlides.isNotEmpty() && parsedSlides.any { it.title.isNotBlank() || it.textBlocks.isNotEmpty() }) {
            val presTitle = parsedSlides.firstOrNull()?.title?.takeIf { it.isNotBlank() } ?: defaultTitle
            return PptxPresentation(
                title = presTitle,
                slides = parsedSlides,
                totalSlides = parsedSlides.size,
                format = "PowerPoint 97-2003 (.ppt)"
            )
        }

        // Resilient heuristic text & slide extraction if record table is damaged or sparse
        return parseHeuristicSlides(pptDocumentBytes, defaultTitle)
    }

    private fun parseRecords(bytes: ByteArray): List<PptxSlide> {
        val slides = mutableListOf<PptxSlide>()
        if (bytes.size < 8) return slides

        try {
            val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            var offset = 0

            var currentSlideIndex = 0
            val slideMap = mutableMapOf<Int, MutableList<String>>()
            var activeSlideNumber = 1
            var isInsideSlideContainer = false

            while (offset + 8 <= bytes.size) {
                val recVerAndInst = buffer.getShort(offset).toInt() and 0xFFFF
                val recVer = recVerAndInst and 0x0F
                val isContainer = (recVer == 0x0F)
                val recType = buffer.getShort(offset + 2).toInt() and 0xFFFF
                val recLen = buffer.getInt(offset + 4)

                if (recLen < 0 || offset + 8 + recLen > bytes.size) {
                    offset += 8
                    continue
                }

                val payloadOffset = offset + 8

                when (recType) {
                    RT_SLIDE, RT_SLIDE_PERSIST_ATOM -> {
                        currentSlideIndex++
                        activeSlideNumber = currentSlideIndex
                        slideMap.getOrPut(activeSlideNumber) { mutableListOf() }
                    }

                    RT_TEXT_CHARS_ATOM -> {
                        // UTF-16LE text string
                        if (recLen >= 2) {
                            val text = String(bytes, payloadOffset, recLen, StandardCharsets.UTF_16LE)
                                .replace("\u000B", "\n")
                                .replace("\r", "\n")
                                .trim()
                            if (text.isNotBlank() && isValidSlideText(text)) {
                                slideMap.getOrPut(activeSlideNumber) { mutableListOf() }.add(text)
                            }
                        }
                    }

                    RT_TEXT_BYTES_ATOM -> {
                        // 8-bit text string (ISO-8859-1 / ASCII)
                        if (recLen > 0) {
                            val text = String(bytes, payloadOffset, recLen, StandardCharsets.ISO_8859_1)
                                .replace("\u000B", "\n")
                                .replace("\r", "\n")
                                .trim()
                            if (text.isNotBlank() && isValidSlideText(text)) {
                                slideMap.getOrPut(activeSlideNumber) { mutableListOf() }.add(text)
                            }
                        }
                    }

                    RT_C_STRING -> {
                        if (recLen >= 2) {
                            val text = String(bytes, payloadOffset, recLen, StandardCharsets.UTF_16LE)
                                .trim().trimEnd('\u0000')
                            if (text.isNotBlank() && isValidSlideText(text)) {
                                slideMap.getOrPut(activeSlideNumber) { mutableListOf() }.add(text)
                            }
                        }
                    }
                }

                offset += if (isContainer) 8 else 8 + recLen
            }

            // Convert gathered slide text entries to PptxSlide models
            var num = 1
            for ((_, textList) in slideMap) {
                if (textList.isEmpty()) continue

                val flatLines = textList.flatMap { it.split("\n") }.map { it.trim() }.filter { it.isNotBlank() }
                if (flatLines.isEmpty()) continue

                val title = flatLines.firstOrNull() ?: "Slide $num"
                val remaining = flatLines.drop(1)

                val bullets = mutableListOf<String>()
                val blocks = mutableListOf<String>()

                for (line in remaining) {
                    if (line.startsWith("•") || line.startsWith("-") || line.startsWith("*") ||
                        (line.length > 2 && line[0].isDigit() && (line[1] == '.' || line[1] == ')'))
                    ) {
                        bullets.add(line.removePrefix("•").removePrefix("-").removePrefix("*").trim())
                    } else {
                        blocks.add(line)
                    }
                }

                slides.add(
                    PptxSlide(
                        slideNumber = num++,
                        title = title,
                        textBlocks = blocks,
                        bulletPoints = bullets
                    )
                )
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }

        return slides
    }

    /**
     * Resilient scanner that extracts both UTF-16LE and ASCII string runs from binary PPT data.
     */
    private fun parseHeuristicSlides(bytes: ByteArray, defaultTitle: String): PptxPresentation {
        val extractedStrings = mutableListOf<String>()

        // 1. Scan for UTF-16LE text sequences (at least 3 characters)
        var i = 0
        while (i + 1 < bytes.size) {
            val charLen = getUtf16RunLength(bytes, i)
            if (charLen >= 3) {
                val str = String(bytes, i, charLen * 2, StandardCharsets.UTF_16LE).trim()
                if (isValidSlideText(str)) {
                    extractedStrings.add(str)
                }
                i += charLen * 2
            } else {
                i += 2
            }
        }

        // 2. Scan for ASCII text sequences (at least 4 characters)
        var j = 0
        while (j < bytes.size) {
            val asciiLen = getAsciiRunLength(bytes, j)
            if (asciiLen >= 4) {
                val str = String(bytes, j, asciiLen, StandardCharsets.ISO_8859_1).trim()
                if (isValidSlideText(str) && !extractedStrings.contains(str)) {
                    extractedStrings.add(str)
                }
                j += asciiLen
            } else {
                j++
            }
        }

        // Deduplicate and filter out formatting keywords
        val cleanStrings = extractedStrings
            .flatMap { it.split("\n", "\r") }
            .map { it.trim() }
            .filter { line ->
                line.length >= 3 &&
                    !line.startsWith("Microsoft") &&
                    !line.startsWith("Arial") &&
                    !line.startsWith("Calibri") &&
                    !line.startsWith("Tahoma") &&
                    !line.startsWith("Times New Roman") &&
                    !line.contains("Default Design") &&
                    !line.contains("PowerPoint Document")
            }
            .distinct()

        val slides = mutableListOf<PptxSlide>()
        if (cleanStrings.isEmpty()) {
            slides.add(
                PptxSlide(
                    slideNumber = 1,
                    title = defaultTitle,
                    textBlocks = listOf("PowerPoint presentation loaded. No editable text blocks found in this slide deck."),
                    bulletPoints = emptyList()
                )
            )
        } else {
            // Group text chunks into slides (approx 4-6 items per slide)
            val chunked = cleanStrings.chunked(5)
            chunked.forEachIndexed { index, chunk ->
                val slideTitle = chunk.firstOrNull() ?: "Slide ${index + 1}"
                val bullets = mutableListOf<String>()
                val blocks = mutableListOf<String>()

                for (line in chunk.drop(1)) {
                    if (line.startsWith("•") || line.startsWith("-") || line.startsWith("*")) {
                        bullets.add(line.removePrefix("•").removePrefix("-").removePrefix("*").trim())
                    } else {
                        blocks.add(line)
                    }
                }

                slides.add(
                    PptxSlide(
                        slideNumber = index + 1,
                        title = slideTitle,
                        textBlocks = blocks,
                        bulletPoints = bullets
                    )
                )
            }
        }

        return PptxPresentation(
            title = slides.firstOrNull()?.title?.takeIf { it.isNotBlank() } ?: defaultTitle,
            slides = slides,
            totalSlides = slides.size,
            format = "PowerPoint Presentation (.ppt)"
        )
    }

    private fun getUtf16RunLength(bytes: ByteArray, startOffset: Int): Int {
        var len = 0
        var ptr = startOffset
        while (ptr + 1 < bytes.size) {
            val b0 = bytes[ptr].toInt() and 0xFF
            val b1 = bytes[ptr + 1].toInt() and 0xFF
            // Standard Latin / ASCII in UTF-16LE has high byte 0x00 and low byte in printable range
            if (b1 == 0 && (b0 in 32..126 || b0 in 160..255 || b0 == 10 || b0 == 13 || b0 == 9)) {
                len++
                ptr += 2
            } else {
                break
            }
        }
        return len
    }

    private fun getAsciiRunLength(bytes: ByteArray, startOffset: Int): Int {
        var len = 0
        var ptr = startOffset
        while (ptr < bytes.size) {
            val b = bytes[ptr].toInt() and 0xFF
            if (b in 32..126 || b in 160..255 || b == 10 || b == 13 || b == 9) {
                len++
                ptr++
            } else {
                break
            }
        }
        return len
    }

    private fun isValidSlideText(text: String): Boolean {
        if (text.length < 2) return false
        // Skip control character sequences or internal OLE markers
        val printable = text.count { it.isLetterOrDigit() || it.isWhitespace() || it in ".,;:!?-–—()[]\"'/$%&*+#@<>" }
        return (printable.toFloat() / text.length.toFloat()) >= 0.7f
    }
}
