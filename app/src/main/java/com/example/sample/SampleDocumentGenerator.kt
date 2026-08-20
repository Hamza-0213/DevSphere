package com.example.sample

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.example.data.local.database.DocumentEntity
import com.example.domain.model.DocumentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object SampleDocumentGenerator {

    suspend fun generateInitialSamples(context: Context): List<DocumentEntity> = withContext(Dispatchers.IO) {
        val samplesDir = File(context.filesDir, "samples").apply { mkdirs() }
        val generatedEntities = mutableListOf<DocumentEntity>()

        try {
            // 1. PDF
            val pdfFile = File(samplesDir, "Welcome to DocSphere.pdf")
            if (!pdfFile.exists() || pdfFile.length() == 0L) {
                createSamplePdf(pdfFile)
            }
            generatedEntities.add(createEntityForFile(pdfFile, DocumentType.PDF, isSample = true, pageCount = 3))

            // 2. DOCX
            val docxFile = File(samplesDir, "Project_Proposal_DocSphere.docx")
            if (!docxFile.exists() || docxFile.length() == 0L) {
                createSampleDocx(docxFile)
            }
            generatedEntities.add(createEntityForFile(docxFile, DocumentType.WORD, isSample = true, pageCount = 2))

            // 3. XLSX
            val xlsxFile = File(samplesDir, "Financial_Report_Q4.xlsx")
            if (!xlsxFile.exists() || xlsxFile.length() == 0L) {
                createSampleXlsx(xlsxFile)
            }
            generatedEntities.add(createEntityForFile(xlsxFile, DocumentType.EXCEL, isSample = true, pageCount = 2))

            // 4. PPTX
            val pptxFile = File(samplesDir, "DocSphere_Executive_Pitch.pptx")
            if (!pptxFile.exists() || pptxFile.length() == 0L) {
                createSamplePptx(pptxFile)
            }
            generatedEntities.add(createEntityForFile(pptxFile, DocumentType.POWERPOINT, isSample = true, pageCount = 3))

            // 5. Legacy PPT (.ppt)
            val pptFile = File(samplesDir, "Product_Strategy_Deck.ppt")
            if (!pptFile.exists() || pptFile.length() == 0L) {
                createSamplePpt(pptFile)
            }
            generatedEntities.add(createEntityForFile(pptFile, DocumentType.POWERPOINT, isSample = true, pageCount = 3))

            // 6. CSV
            val csvFile = File(samplesDir, "Regional_Sales_2026.csv")
            if (!csvFile.exists() || csvFile.length() == 0L) {
                createSampleCsv(csvFile)
            }
            generatedEntities.add(createEntityForFile(csvFile, DocumentType.TEXT, isSample = true, pageCount = 1))

            // 7. TXT
            val txtFile = File(samplesDir, "DocSphere_Architecture_Notes.txt")
            if (!txtFile.exists() || txtFile.length() == 0L) {
                createSampleTxt(txtFile)
            }
            generatedEntities.add(createEntityForFile(txtFile, DocumentType.TEXT, isSample = true, pageCount = 1))

            // 8. PNG Image
            val pngFile = File(samplesDir, "System_Architecture_Diagram.png")
            if (!pngFile.exists() || pngFile.length() == 0L) {
                createSamplePng(pngFile)
            }
            generatedEntities.add(createEntityForFile(pngFile, DocumentType.IMAGE, isSample = true, pageCount = 1))

        } catch (e: Exception) {
            e.printStackTrace()
        }

        generatedEntities
    }

    private fun createEntityForFile(file: File, type: DocumentType, isSample: Boolean, pageCount: Int): DocumentEntity {
        val uri = Uri.fromFile(file).toString()
        val ext = file.extension.lowercase()
        return DocumentEntity(
            uri = uri,
            displayName = file.name,
            extension = ext,
            fileType = type.name,
            sizeBytes = file.length(),
            lastModified = file.lastModified(),
            lastOpenedTimestamp = if (type == DocumentType.PDF) System.currentTimeMillis() else 0L,
            isFavourite = (type == DocumentType.PDF || type == DocumentType.EXCEL),
            lastReadingPosition = 0,
            readingProgressPercent = 0f,
            pageCount = pageCount,
            isSample = isSample
        )
    }

    private fun createSamplePdf(file: File) {
        val pdfDoc = PdfDocument()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.DKGRAY
            textSize = 14f
        }
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(37, 99, 235)
            textSize = 24f
            isFakeBoldText = true
        }
        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(30, 41, 59)
            textSize = 16f
            isFakeBoldText = true
        }

        val pageWidth = 595 // A4 standard point width
        val pageHeight = 842 // A4 standard point height

        // Page 1: Welcome & Overview
        val pageInfo1 = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page1 = pdfDoc.startPage(pageInfo1)
        val canvas1 = page1.canvas

        // Header Background Banner
        paint.color = Color.rgb(239, 246, 255)
        canvas1.drawRoundRect(RectF(40f, 40f, pageWidth - 40f, 160f), 16f, 16f, paint)

        // Accent Bar
        paint.color = Color.rgb(37, 99, 235)
        canvas1.drawRoundRect(RectF(40f, 40f, 50f, 160f), 4f, 4f, paint)

        canvas1.drawText("DocSphere Document Viewer", 65f, 85f, titlePaint)
        textPaint.color = Color.rgb(100, 116, 139)
        textPaint.textSize = 13f
        canvas1.drawText("Universal Offline Document Reader & File Manager for Android", 65f, 115f, textPaint)
        canvas1.drawText("100% On-Device Processing • Zero Network Requirement • Privacy First", 65f, 138f, textPaint)

        // Section 1
        canvas1.drawText("1. Executive Summary", 40f, 200f, subtitlePaint)
        textPaint.color = Color.rgb(51, 65, 85)
        textPaint.textSize = 13f
        val summaryLines = listOf(
            "Welcome to DocSphere, your high-performance offline document suite.",
            "DocSphere allows you to seamlessly read, search, organize, annotate,",
            "and print documents locally on your device without any internet connectivity.",
            "",
            "Core Architectural Pillars:",
            "  • Offline Integrity: No internet permissions, no external server calls.",
            "  • Format Agnostic: PDF, DOCX, XLSX, PPTX, TXT, CSV, and Images.",
            "  • Storage Access Framework: Full native access to internal and SD card files.",
            "  • Persistent State: Bookmarks, reading positions, and favorites saved locally."
        )
        var y = 230f
        for (line in summaryLines) {
            canvas1.drawText(line, 45f, y, textPaint)
            y += 22f
        }

        // Section 2: Quick Start Features
        y += 20f
        canvas1.drawText("2. Key Features At A Glance", 40f, y, subtitlePaint)
        y += 30f
        val features = listOf(
            "✓ PDF Engine: Hardware-accelerated smooth continuous vertical zoom & pinch.",
            "✓ Office Viewers: Pure local parser for Word, Excel sheets, and PowerPoint slides.",
            "✓ Local Search: Fast token indexing across all scanned local files.",
            "✓ Bookmarking: Save custom page marks with personal labels.",
            "✓ Native Sharing: Direct Android Share Sheet with zero telemetry.",
            "✓ Android Print: High-fidelity document printing with preview support."
        )
        for (f in features) {
            canvas1.drawText(f, 45f, y, textPaint)
            y += 24f
        }

        // Footer
        paint.color = Color.rgb(226, 232, 240)
        canvas1.drawLine(40f, pageHeight - 50f, pageWidth - 40f, pageHeight - 50f, paint)
        textPaint.color = Color.rgb(148, 163, 184)
        textPaint.textSize = 10f
        canvas1.drawText("DocSphere v1.0 • Offline Document Suite", 40f, pageHeight - 30f, textPaint)
        canvas1.drawText("Page 1 of 3", pageWidth - 100f, pageHeight - 30f, textPaint)
        pdfDoc.finishPage(page1)

        // Page 2: Supported Formats & Capabilities
        val pageInfo2 = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 2).create()
        val page2 = pdfDoc.startPage(pageInfo2)
        val canvas2 = page2.canvas

        canvas2.drawText("3. Supported File Formats", 40f, 70f, titlePaint)
        canvas2.drawText("Comprehensive format matrix with native offline rendering:", 40f, 100f, textPaint)

        val formatTable = listOf(
            "PDF (.pdf)" to "Vector page rasterization, zoom, bookmarks, search & print",
            "Word (.docx, .doc)" to "Heading hierarchy, styled runs, bold/italic, tables & lists",
            "Excel (.xlsx, .xls)" to "Multi-sheet workbook tabs, scrollable grid, formulas & cells",
            "PowerPoint (.pptx, .ppt)" to "Slide navigation, fullscreen presentation mode & notes",
            "Text / Data (.txt, .csv, .log)" to "Custom font size, dark/sepia reading mode, line numbers",
            "Images (.png, .jpg, .webp)" to "High-res pan, pinch zoom, rotation, dimensions metadata"
        )
        y = 140f
        for ((fmt, desc) in formatTable) {
            paint.color = Color.rgb(248, 250, 252)
            canvas2.drawRoundRect(RectF(40f, y - 18f, pageWidth - 40f, y + 26f), 8f, 8f, paint)
            paint.color = Color.rgb(226, 232, 240)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1f
            canvas2.drawRoundRect(RectF(40f, y - 18f, pageWidth - 40f, y + 26f), 8f, 8f, paint)
            paint.style = Paint.Style.FILL

            subtitlePaint.textSize = 13f
            subtitlePaint.color = Color.rgb(30, 41, 59)
            canvas2.drawText(fmt, 55f, y, subtitlePaint)
            textPaint.textSize = 11f
            textPaint.color = Color.rgb(100, 116, 139)
            canvas2.drawText(desc, 55f, y + 16f, textPaint)
            y += 54f
        }

        // Footer
        paint.color = Color.rgb(226, 232, 240)
        canvas2.drawLine(40f, pageHeight - 50f, pageWidth - 40f, pageHeight - 50f, paint)
        textPaint.color = Color.rgb(148, 163, 184)
        textPaint.textSize = 10f
        canvas2.drawText("DocSphere v1.0 • Offline Document Suite", 40f, pageHeight - 30f, textPaint)
        canvas2.drawText("Page 2 of 3", pageWidth - 100f, pageHeight - 30f, textPaint)
        pdfDoc.finishPage(page2)

        // Page 3: Privacy & Security Architecture
        val pageInfo3 = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 3).create()
        val page3 = pdfDoc.startPage(pageInfo3)
        val canvas3 = page3.canvas

        canvas3.drawText("4. Privacy & Security Architecture", 40f, 70f, titlePaint)
        y = 110f
        val privacyPoints = listOf(
            "Zero Network Access Guarantee",
            "The DocSphere manifest intentionally contains NO android.permission.INTERNET.",
            "Your files never leave your device and cannot be tracked or uploaded.",
            "",
            "Storage Access Framework (SAF)",
            "User documents are accessed through scoped storage permissions. The original files",
            "are treated as strictly read-only. DocSphere never modifies your documents.",
            "",
            "Local Metadata Persistence",
            "Bookmarks, recent viewing timestamps, and favorites are stored in an encrypted local",
            "Room SQLite database within your private app sandbox.",
            "",
            "How to Test DocSphere:",
            "1. Try bookmarking this page using the bookmark icon in the top toolbar.",
            "2. Rotate your screen to verify responsive landscape viewing.",
            "3. Use the page jump dialog to navigate quickly across pages.",
            "4. Tap the Print button to test Android Print Spooler integration."
        )
        for (p in privacyPoints) {
            if (p.startsWith("Zero") || p.startsWith("Storage") || p.startsWith("Local") || p.startsWith("How")) {
                subtitlePaint.textSize = 14f
                subtitlePaint.color = Color.rgb(37, 99, 235)
                canvas3.drawText(p, 40f, y, subtitlePaint)
                y += 24f
            } else {
                textPaint.textSize = 12f
                textPaint.color = Color.rgb(51, 65, 85)
                canvas3.drawText(p, 40f, y, textPaint)
                y += 20f
            }
        }

        // Footer
        paint.color = Color.rgb(226, 232, 240)
        canvas3.drawLine(40f, pageHeight - 50f, pageWidth - 40f, pageHeight - 50f, paint)
        textPaint.color = Color.rgb(148, 163, 184)
        textPaint.textSize = 10f
        canvas3.drawText("DocSphere v1.0 • Offline Document Suite", 40f, pageHeight - 30f, textPaint)
        canvas3.drawText("Page 3 of 3", pageWidth - 100f, pageHeight - 30f, textPaint)
        pdfDoc.finishPage(page3)

        FileOutputStream(file).use { out ->
            pdfDoc.writeTo(out)
        }
        pdfDoc.close()
    }

    private fun createSampleDocx(file: File) {
        val documentXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
  <w:body>
    <w:p>
      <w:pPr><w:pStyle w:val="Title"/><w:jc w:val="center"/></w:pPr>
      <w:r><w:rPr><w:b/><w:sz w:val="48"/><w:color w:val="2563EB"/></w:rPr><w:t>DocSphere Project Proposal</w:t></w:r>
    </w:p>
    <w:p>
      <w:pPr><w:jc w:val="center"/></w:pPr>
      <w:r><w:rPr><w:i/><w:color w:val="64748B"/><w:sz w:val="24"/></w:rPr><w:t>Universal Offline Document Suite for Mobile Workspaces</w:t></w:r>
    </w:p>
    <w:p/>
    <w:p>
      <w:pPr><w:pStyle w:val="Heading1"/></w:pPr>
      <w:r><w:rPr><w:b/><w:sz w:val="32"/><w:color w:val="1E293B"/></w:rPr><w:t>1. Project Objectives</w:t></w:r>
    </w:p>
    <w:p>
      <w:r><w:t>The goal of DocSphere is to deliver a rock-solid, zero-network document reader that handles enterprise and personal files directly on Android devices.</w:t></w:r>
    </w:p>
    <w:p>
      <w:pPr><w:pStyle w:val="Heading2"/></w:pPr>
      <w:r><w:rPr><w:b/><w:sz w:val="28"/><w:color w:val="334155"/></w:rPr><w:t>Key Deliverables:</w:t></w:r>
    </w:p>
    <w:p>
      <w:r><w:t>• Comprehensive DOCX formatting support including paragraphs, headings, bold, italic, tables, and lists.</w:t></w:r>
    </w:p>
    <w:p>
      <w:r><w:t>• Robust OpenXML parsing without requiring heavy external dependencies or desktop Word.</w:t></w:r>
    </w:p>
    <w:p>
      <w:r><w:t>• Fast rendering engine with zoom and customizable reader styles.</w:t></w:r>
    </w:p>
    <w:p/>
    <w:p>
      <w:pPr><w:pStyle w:val="Heading1"/></w:pPr>
      <w:r><w:rPr><w:b/><w:sz w:val="32"/><w:color w:val="1E293B"/></w:rPr><w:t>2. Milestones &amp; Timelines</w:t></w:r>
    </w:p>
    <w:tbl>
      <w:tr>
        <w:tc><w:p><w:r><w:rPr><w:b/></w:rPr><w:t>Milestone Phase</w:t></w:r></w:p></w:tc>
        <w:tc><w:p><w:r><w:rPr><w:b/></w:rPr><w:t>Target Date</w:t></w:r></w:p></w:tc>
        <w:tc><w:p><w:r><w:rPr><w:b/></w:rPr><w:t>Status</w:t></w:r></w:p></w:tc>
      </w:tr>
      <w:tr>
        <w:tc><w:p><w:r><w:t>Phase 1: Architecture &amp; Room DB</w:t></w:r></w:p></w:tc>
        <w:tc><w:p><w:r><w:t>August 2026</w:t></w:r></w:p></w:tc>
        <w:tc><w:p><w:r><w:t>Completed</w:t></w:r></w:p></w:tc>
      </w:tr>
      <w:tr>
        <w:tc><w:p><w:r><w:t>Phase 2: PDF &amp; Office Parsers</w:t></w:r></w:p></w:tc>
        <w:tc><w:p><w:r><w:t>September 2026</w:t></w:r></w:p></w:tc>
        <w:tc><w:p><w:r><w:t>Completed</w:t></w:r></w:p></w:tc>
      </w:tr>
      <w:tr>
        <w:tc><w:p><w:r><w:t>Phase 3: QA &amp; Production Release</w:t></w:r></w:p></w:tc>
        <w:tc><w:p><w:r><w:t>October 2026</w:t></w:r></w:p></w:tc>
        <w:tc><w:p><w:r><w:t>Ready</w:t></w:r></w:p></w:tc>
      </w:tr>
    </w:tbl>
  </w:body>
</w:document>"""

        val contentTypes = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
</Types>"""

        val rels = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
</Relationships>"""

        ZipOutputStream(FileOutputStream(file)).use { zip ->
            addZipEntry(zip, "[Content_Types].xml", contentTypes)
            addZipEntry(zip, "_rels/.rels", rels)
            addZipEntry(zip, "word/document.xml", documentXml)
        }
    }

    private fun createSampleXlsx(file: File) {
        val workbookXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets>
    <sheet name="Financial Overview" sheetId="1" r:id="rId1"/>
    <sheet name="Department Breakdown" sheetId="2" r:id="rId2"/>
  </sheets>
</workbook>"""

        val sharedStringsXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<sst xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" count="15" uniqueCount="15">
  <si><t>Quarter</t></si>
  <si><t>Gross Revenue ($)</t></si>
  <si><t>Operating Expenses ($)</t></si>
  <si><t>Net Profit ($)</t></si>
  <si><t>Margin (%)</t></si>
  <si><t>Q1 2026</t></si>
  <si><t>Q2 2026</t></si>
  <si><t>Q3 2026</t></si>
  <si><t>Q4 2026</t></si>
  <si><t>Department</t></si>
  <si><t>Engineering</t></si>
  <si><t>Design &amp; UX</t></si>
  <si><t>Operations</t></si>
  <si><t>Marketing</t></si>
  <si><t>Total Budget ($)</t></si>
</sst>"""

        val sheet1Xml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <sheetData>
    <row r="1">
      <c r="A1" t="s"><v>0</v></c>
      <c r="B1" t="s"><v>1</v></c>
      <c r="C1" t="s"><v>2</v></c>
      <c r="D1" t="s"><v>3</v></c>
      <c r="E1" t="s"><v>4</v></c>
    </row>
    <row r="2">
      <c r="A2" t="s"><v>5</v></c>
      <c r="B2"><v>1250000</v></c>
      <c r="C2"><v>750000</v></c>
      <c r="D2"><v>500000</v></c>
      <c r="E2"><v>40.0</v></c>
    </row>
    <row r="3">
      <c r="A3" t="s"><v>6</v></c>
      <c r="B3"><v>1450000</v></c>
      <c r="C3"><v>820000</v></c>
      <c r="D3"><v>630000</v></c>
      <c r="E3"><v>43.4</v></c>
    </row>
    <row r="4">
      <c r="A4" t="s"><v>7</v></c>
      <c r="B4"><v>1680000</v></c>
      <c r="C4"><v>910000</v></c>
      <c r="D4"><v>770000</v></c>
      <c r="E4"><v>45.8</v></c>
    </row>
    <row r="5">
      <c r="A5" t="s"><v>8</v></c>
      <c r="B5"><v>1950000</v></c>
      <c r="C5"><v>1020000</v></c>
      <c r="D5"><v>930000</v></c>
      <c r="E5"><v>47.7</v></c>
    </row>
  </sheetData>
</worksheet>"""

        val sheet2Xml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <sheetData>
    <row r="1">
      <c r="A1" t="s"><v>9</v></c>
      <c r="B1" t="s"><v>14</v></c>
    </row>
    <row r="2">
      <c r="A2" t="s"><v>10</v></c>
      <c r="B2"><v>1200000</v></c>
    </row>
    <row r="3">
      <c r="A3" t="s"><v>11</v></c>
      <c r="B3"><v>450000</v></c>
    </row>
    <row r="4">
      <c r="A4" t="s"><v>12</v></c>
      <c r="B4"><v>600000</v></c>
    </row>
    <row r="5">
      <c r="A5" t="s"><v>13</v></c>
      <c r="B5"><v>850000</v></c>
    </row>
  </sheetData>
</worksheet>"""

        val contentTypes = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/worksheets/sheet2.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/sharedStrings.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sharedStrings+xml"/>
</Types>"""

        val rels = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""

        val wbRels = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet2.xml"/>
  <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/sharedStrings" Target="sharedStrings.xml"/>
</Relationships>"""

        ZipOutputStream(FileOutputStream(file)).use { zip ->
            addZipEntry(zip, "[Content_Types].xml", contentTypes)
            addZipEntry(zip, "_rels/.rels", rels)
            addZipEntry(zip, "xl/workbook.xml", workbookXml)
            addZipEntry(zip, "xl/_rels/workbook.xml.rels", wbRels)
            addZipEntry(zip, "xl/sharedStrings.xml", sharedStringsXml)
            addZipEntry(zip, "xl/worksheets/sheet1.xml", sheet1Xml)
            addZipEntry(zip, "xl/worksheets/sheet2.xml", sheet2Xml)
        }
    }

    private fun createSamplePptx(file: File) {
        val presentationXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<p:presentation xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <p:sldIdLst>
    <p:sldId id="256" r:id="rId1"/>
    <p:sldId id="257" r:id="rId2"/>
    <p:sldId id="258" r:id="rId3"/>
  </p:sldIdLst>
</p:presentation>"""

        val slide1Xml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<p:sld xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main" xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main">
  <p:cSld>
    <p:spTree>
      <p:sp>
        <p:txBody>
          <a:p><a:r><a:t>DocSphere — Executive Presentation</a:t></a:r></p>
        </p:txBody>
      </p:sp>
      <p:sp>
        <p:txBody>
          <a:p><a:r><a:t>Next-Generation Offline Android Document Architecture</a:t></a:r></p>
          <a:p><a:r><a:t>Presented by: DocSphere Core Engineering Team</a:t></a:r></p>
        </p:txBody>
      </p:sp>
    </p:spTree>
  </p:cSld>
</p:sld>"""

        val slide2Xml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<p:sld xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main" xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main">
  <p:cSld>
    <p:spTree>
      <p:sp>
        <p:txBody>
          <a:p><a:r><a:t>Market Challenges &amp; Solution</a:t></a:r></p>
        </p:txBody>
      </p:sp>
      <p:sp>
        <p:txBody>
          <a:p><a:r><a:t>• Cloud Dependency: Most modern viewers fail without steady Wi-Fi or data connection.</a:t></a:r></p>
          <a:p><a:r><a:t>• Privacy Risks: Unnecessary document uploads compromise sensitive corporate data.</a:t></a:r></p>
          <a:p><a:r><a:t>• Bloated Viewers: Many apps require multiple separate reader apps for Word, Excel, and PDF.</a:t></a:r></p>
          <a:p><a:r><a:t>• Solution: DocSphere provides an all-in-one unified reader with 100% offline isolation.</a:t></a:r></p>
        </p:txBody>
      </p:sp>
    </p:spTree>
  </p:cSld>
</p:sld>"""

        val slide3Xml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<p:sld xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main" xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main">
  <p:cSld>
    <p:spTree>
      <p:sp>
        <p:txBody>
          <a:p><a:r><a:t>Performance &amp; Scale Metrics</a:t></a:r></p>
        </p:txBody>
      </p:sp>
      <p:sp>
        <p:txBody>
          <a:p><a:r><a:t>• Launch Time: Sub-second instantaneous startup.</a:t></a:r></p>
          <a:p><a:r><a:t>• Memory Efficiency: Lazy viewport loading avoids high memory allocations.</a:t></a:r></p>
          <a:p><a:r><a:t>• Supported Devices: Smartphones, Foldables, Tablets (Adaptive Jetpack Compose).</a:t></a:r></p>
          <a:p><a:r><a:t>• Print &amp; Share: Fully compatible with Android Print Spooler and Share Sheets.</a:t></a:r></p>
        </p:txBody>
      </p:sp>
    </p:spTree>
  </p:cSld>
</p:sld>"""

        val contentTypes = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/ppt/presentation.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.presentation.main+xml"/>
  <Override PartName="/ppt/slides/slide1.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.slide+xml"/>
  <Override PartName="/ppt/slides/slide2.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.slide+xml"/>
  <Override PartName="/ppt/slides/slide3.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.slide+xml"/>
</Types>"""

        val rels = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="ppt/presentation.xml"/>
</Relationships>"""

        val presRels = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/slide" Target="slides/slide1.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/slide" Target="slides/slide2.xml"/>
  <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/slide" Target="slides/slide3.xml"/>
</Relationships>"""

        ZipOutputStream(FileOutputStream(file)).use { zip ->
            addZipEntry(zip, "[Content_Types].xml", contentTypes)
            addZipEntry(zip, "_rels/.rels", rels)
            addZipEntry(zip, "ppt/presentation.xml", presentationXml)
            addZipEntry(zip, "ppt/_rels/presentation.xml.rels", presRels)
            addZipEntry(zip, "ppt/slides/slide1.xml", slide1Xml)
            addZipEntry(zip, "ppt/slides/slide2.xml", slide2Xml)
            addZipEntry(zip, "ppt/slides/slide3.xml", slide3Xml)
        }
    }

    private fun createSampleCsv(file: File) {
        val content = """Region,Product Category,Quarter,Units Sold,Gross Revenue ($),Operating Margin (%),Fulfillment Status
North America,Enterprise Software,Q1 2026,14500,2900000,48.5,Fulfilled
North America,Cloud Hardware,Q1 2026,3200,1600000,32.0,Fulfilled
Europe (EMEA),Enterprise Software,Q1 2026,9800,1960000,46.2,Fulfilled
Europe (EMEA),Consulting & Support,Q1 2026,1200,600000,55.0,Fulfilled
Asia Pacific (APAC),Enterprise Software,Q1 2026,11200,2240000,51.0,Fulfilled
Asia Pacific (APAC),Consumer Devices,Q1 2026,8500,1275000,28.4,Fulfilled
Latin America (LATAM),Enterprise Software,Q1 2026,4300,860000,42.1,Fulfilled
Latin America (LATAM),Mobile Subscriptions,Q1 2026,18000,540000,60.5,Fulfilled
"""
        file.writeText(content, StandardCharsets.UTF_8)
    }

    private fun createSampleTxt(file: File) {
        val content = """================================================================================
                    DOCSPHERE OFFLINE DOCUMENT SUITE
================================================================================
Application: DocSphere Android Universal Document Reader
Version:     1.0.0 (Build 2026.08)
Environment: Android 8.0+ (API 26+)
Licensing:   Pure Offline Standalone Edition

1. OVERVIEW
--------------------------------------------------------------------------------
DocSphere provides an isolated, highly performant document reading environment.
All file parsing, indexing, searching, and caching occurs exclusively in local memory
and the application's protected sandbox.

2. SUPPORTED EXTENSIONS
--------------------------------------------------------------------------------
- Adobe PDF:        .pdf
- Microsoft Word:   .docx, .doc, .dotx, .rtf
- Microsoft Excel:  .xlsx, .xls, .xltx, .csv
- PowerPoint:       .pptx, .ppt, .ppsx
- Plain & Code:     .txt, .log, .json, .xml, .md, .kt, .java, .py, .html, .css
- Images:           .png, .jpg, .jpeg, .webp, .bmp, .gif

3. PRIVACY GUARANTEE
--------------------------------------------------------------------------------
- NO Internet Permissions requested.
- NO Network sockets opened.
- NO Telemetry, analytics, or background tracking.
- Scoped Android Storage Access Framework (SAF) utilized for storage access.

4. TIPS & GESTURES
--------------------------------------------------------------------------------
- Pinch with two fingers to zoom in on any PDF, Word document, or image.
- Tap the bookmark ribbon on the top toolbar to save reading points.
- Use the quick search bar on the Home screen to filter documents instantly.
- Toggle between Light, Dark, and System theme in Settings.
================================================================================
"""
        file.writeText(content, StandardCharsets.UTF_8)
    }

    private fun createSamplePng(file: File) {
        val width = 800
        val height = 600
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Background
        val bgPaint = Paint().apply { color = Color.rgb(15, 23, 42) }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(30, 41, 59) }
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(59, 130, 246)
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 20f
            isFakeBoldText = true
        }
        val subTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(148, 163, 184)
            textSize = 14f
        }

        // Title
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(96, 165, 250)
            textSize = 28f
            isFakeBoldText = true
        }
        canvas.drawText("DocSphere Offline Architecture", 60f, 70f, titlePaint)
        canvas.drawText("Zero-Cloud Privacy & Local Persistence Model", 60f, 105f, subTextPaint)

        // Draw 3 Architecture Layers
        val layers = listOf(
            Triple("UI & Viewers Layer", "Jetpack Compose, PdfRenderer, Canvas, M3 Components", 150f),
            Triple("Document Parsing Engine", "Pure Offline OpenXML (DOCX/XLSX/PPTX) & Plaintext Charset", 280f),
            Triple("Local Persistence & SAF", "Room SQLite Database, Android Storage Access Framework", 410f)
        )

        for ((title, desc, topY) in layers) {
            val rect = RectF(60f, topY, width - 60f, topY + 90f)
            canvas.drawRoundRect(rect, 16f, 16f, cardPaint)
            canvas.drawRoundRect(rect, 16f, 16f, strokePaint)

            canvas.drawText(title, 90f, topY + 40f, textPaint)
            canvas.drawText(desc, 90f, topY + 68f, subTextPaint)
        }

        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
    }

    private fun createSamplePpt(file: File) {
        val out = java.io.ByteArrayOutputStream()

        fun writeRecord(type: Int, data: ByteArray) {
            val header = java.nio.ByteBuffer.allocate(8).order(java.nio.ByteOrder.LITTLE_ENDIAN)
            header.putShort(0.toShort()) // ver & inst
            header.putShort(type.toShort()) // type
            header.putInt(data.size) // len
            out.write(header.array())
            out.write(data)
        }

        fun writeTextAtom(text: String) {
            val bytes = text.toByteArray(StandardCharsets.UTF_16LE)
            writeRecord(4008, bytes) // RT_TEXT_CHARS_ATOM
        }

        // Slide 1
        writeRecord(1006, ByteArray(0)) // Slide container
        writeTextAtom("DocSphere Legacy PPT Presentation")
        writeTextAtom("Backward-compatible PowerPoint 97-2003 reader engine")
        writeTextAtom("• Full support for binary .ppt presentations\n• Automatic slide segmentation and text parsing\n• Presenter notes and deck controls")

        // Slide 2
        writeRecord(1006, ByteArray(0))
        writeTextAtom("Offline Document Engine Features")
        writeTextAtom("• Zero network requirements\n• Hardware accelerated on-device rendering\n• Multi-tab format filtering and search")

        // Slide 3
        writeRecord(1006, ByteArray(0))
        writeTextAtom("Universal File Compatibility")
        writeTextAtom("• PDF, DOCX, DOC, XLSX, XLS, PPTX, PPT, CSV, TXT, and Images\n• Safe on-device document inspection")

        file.writeBytes(out.toByteArray())
    }

    private fun addZipEntry(zip: ZipOutputStream, entryName: String, content: String) {
        val entry = ZipEntry(entryName)
        zip.putNextEntry(entry)
        zip.write(content.toByteArray(StandardCharsets.UTF_8))
        zip.closeEntry()
    }
}
