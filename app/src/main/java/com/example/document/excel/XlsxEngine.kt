package com.example.document.excel

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
import java.io.StringReader
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

data class XlsxCell(
    val columnRef: String,
    val columnIndex: Int,
    val rowIndex: Int,
    val value: String,
    val isHeader: Boolean = false,
    val formula: String? = null
)

data class XlsxRow(
    val rowIndex: Int,
    val cells: List<XlsxCell>
)

data class XlsxSheet(
    val name: String,
    val rows: List<XlsxRow>,
    val maxColumns: Int,
    val maxRows: Int
)

data class XlsxWorkbook(
    val title: String,
    val sheets: List<XlsxSheet>,
    val totalCells: Int
)

class XlsxEngine(private val context: Context) {

    suspend fun parseXlsx(uri: Uri): Result<XlsxWorkbook> = withContext(Dispatchers.IO) {
        var inputStream: InputStream? = null
        try {
            inputStream = if (uri.scheme == "file") {
                FileInputStream(File(uri.path ?: ""))
            } else {
                context.contentResolver.openInputStream(uri)
            }

            if (inputStream == null) {
                return@withContext Result.failure(Exception("Unable to open Excel file stream"))
            }

            val zipEntries = mutableMapOf<String, ByteArray>()
            val zip = ZipInputStream(inputStream)
            var entry: ZipEntry? = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    zipEntries[entry.name] = zip.readBytes()
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }

            if (zipEntries.isEmpty() || !zipEntries.containsKey("xl/workbook.xml")) {
                // If not OpenXML XLSX, try CSV/TSV parser fallback
                return@withContext parseCsvAsSpreadsheet(uri)
            }

            // 1. Parse Shared Strings
            val sharedStrings = mutableListOf<String>()
            val sstBytes = zipEntries["xl/sharedStrings.xml"]
            if (sstBytes != null) {
                parseSharedStrings(sstBytes, sharedStrings)
            }

            // 2. Parse Sheet Names from Workbook
            val sheetNames = mutableListOf<Pair<String, String>>() // (sheetId/rId, name)
            val wbBytes = zipEntries["xl/workbook.xml"] ?: return@withContext Result.failure(Exception("Invalid XLSX: missing workbook.xml"))
            parseWorkbookSheets(wbBytes, sheetNames)

            // 3. Parse Each Sheet
            val parsedSheets = mutableListOf<XlsxSheet>()
            var totalCellsCount = 0

            var sheetIndex = 1
            for ((_, sheetName) in sheetNames) {
                val sheetPath = "xl/worksheets/sheet$sheetIndex.xml"
                val sheetData = zipEntries[sheetPath]
                if (sheetData != null) {
                    val sheet = parseWorksheet(sheetName, sheetData, sharedStrings)
                    parsedSheets.add(sheet)
                    totalCellsCount += sheet.rows.sumOf { it.cells.size }
                }
                sheetIndex++
            }

            // If no sheets found by naming, parse any sheet*.xml in archive
            if (parsedSheets.isEmpty()) {
                zipEntries.filter { it.key.startsWith("xl/worksheets/sheet") && it.key.endsWith(".xml") }.forEach { (path, data) ->
                    val name = "Sheet " + path.substringAfter("sheet").substringBefore(".xml")
                    val sheet = parseWorksheet(name, data, sharedStrings)
                    parsedSheets.add(sheet)
                    totalCellsCount += sheet.rows.sumOf { it.cells.size }
                }
            }

            val docTitle = uri.lastPathSegment?.substringAfterLast("/") ?: "Excel Workbook"
            Result.success(
                XlsxWorkbook(
                    title = docTitle,
                    sheets = parsedSheets,
                    totalCells = totalCellsCount
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(Exception("Failed to parse spreadsheet: ${e.localizedMessage}"))
        } finally {
            try {
                inputStream?.close()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    private fun parseSharedStrings(bytes: ByteArray, outStrings: MutableList<String>) {
        val factory = XmlPullParserFactory.newInstance()
        val parser = factory.newPullParser()
        parser.setInput(ByteArrayInputStream(bytes), "UTF-8")

        var eventType = parser.eventType
        val currentText = StringBuilder()
        var insideT = false

        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                val tag = parser.name
                if (tag == "t" || tag.endsWith(":t")) {
                    insideT = true
                }
            } else if (eventType == XmlPullParser.TEXT) {
                if (insideT) {
                    currentText.append(parser.text)
                }
            } else if (eventType == XmlPullParser.END_TAG) {
                val tag = parser.name
                if (tag == "t" || tag.endsWith(":t")) {
                    insideT = false
                } else if (tag == "si" || tag.endsWith(":si")) {
                    outStrings.add(currentText.toString())
                    currentText.clear()
                }
            }
            eventType = parser.next()
        }
    }

    private fun parseWorkbookSheets(bytes: ByteArray, outSheets: MutableList<Pair<String, String>>) {
        val factory = XmlPullParserFactory.newInstance()
        val parser = factory.newPullParser()
        parser.setInput(ByteArrayInputStream(bytes), "UTF-8")

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG && (parser.name == "sheet" || parser.name.endsWith(":sheet"))) {
                val name = parser.getAttributeValue(null, "name") ?: "Sheet"
                val id = parser.getAttributeValue(null, "sheetId") ?: "1"
                outSheets.add(id to name)
            }
            eventType = parser.next()
        }
    }

    private fun parseWorksheet(
        sheetName: String,
        sheetBytes: ByteArray,
        sharedStrings: List<String>
    ): XlsxSheet {
        val rows = mutableListOf<XlsxRow>()
        val factory = XmlPullParserFactory.newInstance()
        val parser = factory.newPullParser()
        parser.setInput(ByteArrayInputStream(sheetBytes), "UTF-8")

        var currentRowIndex = 0
        var currentCells = mutableListOf<XlsxCell>()
        var maxCol = 0

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                val tag = parser.name
                if (tag == "row" || tag.endsWith(":row")) {
                    currentRowIndex = parser.getAttributeValue(null, "r")?.toIntOrNull() ?: (currentRowIndex + 1)
                    currentCells = mutableListOf()
                } else if (tag == "c" || tag.endsWith(":c")) {
                    val cellRef = parser.getAttributeValue(null, "r") ?: ""
                    val cellType = parser.getAttributeValue(null, "t") ?: "n" // "s" = shared string, "n" = number, "b" = bool

                    var cellValue = ""
                    var formula: String? = null

                    // Parse cell children (v, f)
                    var cellEventType = parser.next()
                    while (!(cellEventType == XmlPullParser.END_TAG && (parser.name == "c" || parser.name.endsWith(":c")))) {
                        if (cellEventType == XmlPullParser.START_TAG) {
                            val innerTag = parser.name
                            if (innerTag == "v" || innerTag.endsWith(":v")) {
                                val rawVal = parser.nextText()
                                cellValue = if (cellType == "s") {
                                    val sstIdx = rawVal.toIntOrNull()
                                    if (sstIdx != null && sstIdx in sharedStrings.indices) {
                                        sharedStrings[sstIdx]
                                    } else {
                                        rawVal
                                    }
                                } else {
                                    rawVal
                                }
                            } else if (innerTag == "f" || innerTag.endsWith(":f")) {
                                formula = parser.nextText()
                            }
                        }
                        cellEventType = parser.next()
                    }

                    val colLetters = cellRef.filter { it.isLetter() }
                    val colIndex = colLetterToIndex(colLetters)
                    if (colIndex + 1 > maxCol) maxCol = colIndex + 1

                    currentCells.add(
                        XlsxCell(
                            columnRef = cellRef,
                            columnIndex = colIndex,
                            rowIndex = currentRowIndex - 1,
                            value = cellValue,
                            isHeader = (currentRowIndex == 1),
                            formula = formula
                        )
                    )
                }
            } else if (eventType == XmlPullParser.END_TAG) {
                val tag = parser.name
                if (tag == "row" || tag.endsWith(":row")) {
                    if (currentCells.isNotEmpty()) {
                        rows.add(XlsxRow(rowIndex = currentRowIndex, cells = currentCells.sortedBy { it.columnIndex }))
                    }
                }
            }
            eventType = parser.next()
        }

        return XlsxSheet(
            name = sheetName,
            rows = rows.sortedBy { it.rowIndex },
            maxColumns = maxCol.coerceAtLeast(1),
            maxRows = rows.size
        )
    }

    private fun colLetterToIndex(letters: String): Int {
        var index = 0
        for (c in letters.uppercase()) {
            index = index * 26 + (c - 'A' + 1)
        }
        return (index - 1).coerceAtLeast(0)
    }

    private suspend fun parseCsvAsSpreadsheet(uri: Uri): Result<XlsxWorkbook> = withContext(Dispatchers.IO) {
        try {
            val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText()
                ?: return@withContext Result.failure(Exception("Cannot read CSV file"))

            val lines = text.split("\n").filter { it.isNotBlank() }
            val rows = mutableListOf<XlsxRow>()
            var maxCol = 0

            lines.forEachIndexed { rowIdx, line ->
                val tokens = line.split(",").map { it.trim().removeSurrounding("\"") }
                if (tokens.size > maxCol) maxCol = tokens.size

                val cells = tokens.mapIndexed { colIdx, value ->
                    XlsxCell(
                        columnRef = "${('A' + colIdx)}${rowIdx + 1}",
                        columnIndex = colIdx,
                        rowIndex = rowIdx,
                        value = value,
                        isHeader = (rowIdx == 0)
                    )
                }
                rows.add(XlsxRow(rowIndex = rowIdx + 1, cells = cells))
            }

            val sheet = XlsxSheet(
                name = "Data Sheet",
                rows = rows,
                maxColumns = maxCol.coerceAtLeast(1),
                maxRows = rows.size
            )

            Result.success(
                XlsxWorkbook(
                    title = uri.lastPathSegment ?: "Spreadsheet",
                    sheets = listOf(sheet),
                    totalCells = rows.sumOf { it.cells.size }
                )
            )
        } catch (e: Exception) {
            Result.failure(Exception("Failed to read CSV spreadsheet: ${e.localizedMessage}"))
        }
    }
}
