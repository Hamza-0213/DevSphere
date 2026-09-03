package com.example.domain.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.ui.theme.ColorExcel
import com.example.ui.theme.ColorImage
import com.example.ui.theme.ColorOther
import com.example.ui.theme.ColorPdf
import com.example.ui.theme.ColorPowerPoint
import com.example.ui.theme.ColorText
import com.example.ui.theme.ColorWord

enum class DocumentType(
    val displayName: String,
    val primaryColor: Color,
    val icon: ImageVector,
    val mimeType: String,
    val supportedExtensions: Set<String>
) {
    PDF(
        displayName = "PDF Document",
        primaryColor = ColorPdf,
        icon = Icons.Filled.PictureAsPdf,
        mimeType = "application/pdf",
        supportedExtensions = setOf("pdf")
    ),
    WORD(
        displayName = "Word Document",
        primaryColor = ColorWord,
        icon = Icons.Filled.Description,
        mimeType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        supportedExtensions = setOf("docx", "doc", "dotx", "dot", "rtf")
    ),
    EXCEL(
        displayName = "Excel Spreadsheet",
        primaryColor = ColorExcel,
        icon = Icons.Filled.GridOn,
        mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        supportedExtensions = setOf("xlsx", "xls", "xltx", "xlt", "csv")
    ),
    POWERPOINT(
        displayName = "PowerPoint Presentation",
        primaryColor = ColorPowerPoint,
        icon = Icons.Filled.Slideshow,
        mimeType = "application/vnd.openxmlformats-officedocument.presentationml.presentation",
        supportedExtensions = setOf("pptx", "ppt", "potx", "pot", "ppsx", "pps", "pptm", "potm", "odp")
    ),
    TEXT(
        displayName = "Text Document",
        primaryColor = ColorText,
        icon = Icons.AutoMirrored.Filled.Article,
        mimeType = "text/plain",
        supportedExtensions = setOf("txt", "log", "json", "xml", "md", "html", "htm", "css", "js", "ts", "kt", "java", "py", "sh", "yaml", "yml", "ini", "conf")
    ),
    IMAGE(
        displayName = "Image File",
        primaryColor = ColorImage,
        icon = Icons.Filled.Image,
        mimeType = "image/*",
        supportedExtensions = setOf("jpg", "jpeg", "png", "webp", "bmp", "gif", "svg")
    ),
    UNKNOWN(
        displayName = "Document",
        primaryColor = ColorOther,
        icon = Icons.AutoMirrored.Filled.InsertDriveFile,
        mimeType = "application/octet-stream",
        supportedExtensions = emptySet()
    );

    companion object {
        fun fromExtension(extension: String?): DocumentType {
            if (extension.isNullOrBlank()) return UNKNOWN
            val normalized = extension.lowercase().trim().removePrefix(".")
            return entries.firstOrNull { it.supportedExtensions.contains(normalized) } ?: UNKNOWN
        }

        fun fromFileName(fileName: String?): DocumentType {
            if (fileName.isNullOrBlank()) return UNKNOWN
            val ext = fileName.substringAfterLast(".", "").lowercase()
            return fromExtension(ext)
        }

        fun fromMimeType(mimeType: String?): DocumentType {
            if (mimeType.isNullOrBlank()) return UNKNOWN
            val lower = mimeType.lowercase()
            return when {
                lower.contains("pdf") -> PDF
                lower.contains("wordprocessingml") || lower.contains("msword") || lower.contains("word") -> WORD
                lower.contains("spreadsheetml") || lower.contains("ms-excel") || lower.contains("excel") || lower.contains("csv") -> EXCEL
                lower.contains("presentationml") || lower.contains("ms-powerpoint") || lower.contains("powerpoint") || lower.contains("presentation") -> POWERPOINT
                lower.startsWith("text/") || lower.contains("json") || lower.contains("xml") -> TEXT
                lower.startsWith("image/") -> IMAGE
                else -> UNKNOWN
            }
        }
    }
}
