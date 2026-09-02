package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.database.DocumentEntity
import com.example.domain.model.DocumentType

@Composable
fun StorageOverviewCard(
    documents: List<DocumentEntity>,
    modifier: Modifier = Modifier
) {
    val totalSize = documents.sumOf { it.sizeBytes }
    val totalDocs = documents.size

    val pdfCount = documents.count { it.toDocumentType() == DocumentType.PDF }
    val wordCount = documents.count { it.toDocumentType() == DocumentType.WORD }
    val excelCount = documents.count { it.toDocumentType() == DocumentType.EXCEL }
    val pptCount = documents.count { it.toDocumentType() == DocumentType.POWERPOINT }
    val textCount = documents.count { it.toDocumentType() == DocumentType.TEXT }
    val imageCount = documents.count { it.toDocumentType() == DocumentType.IMAGE }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Storage,
                            contentDescription = "Storage overview",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Library Overview",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.5.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (totalDocs == 1) "1 document indexed" else "$totalDocs documents indexed",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                ) {
                    Text(
                        text = formatFileSize(totalSize),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            if (totalDocs > 0) {
                Spacer(modifier = Modifier.height(12.dp))

                // Segmented Thin Progress Track
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                ) {
                    if (pdfCount > 0) {
                        Box(
                            modifier = Modifier
                                .weight(pdfCount.toFloat())
                                .height(5.dp)
                                .background(DocumentType.PDF.primaryColor)
                        )
                    }
                    if (wordCount > 0) {
                        Box(
                            modifier = Modifier
                                .weight(wordCount.toFloat())
                                .height(5.dp)
                                .background(DocumentType.WORD.primaryColor)
                        )
                    }
                    if (excelCount > 0) {
                        Box(
                            modifier = Modifier
                                .weight(excelCount.toFloat())
                                .height(5.dp)
                                .background(DocumentType.EXCEL.primaryColor)
                        )
                    }
                    if (pptCount > 0) {
                        Box(
                            modifier = Modifier
                                .weight(pptCount.toFloat())
                                .height(5.dp)
                                .background(DocumentType.POWERPOINT.primaryColor)
                        )
                    }
                    if (textCount > 0) {
                        Box(
                            modifier = Modifier
                                .weight(textCount.toFloat())
                                .height(5.dp)
                                .background(DocumentType.TEXT.primaryColor)
                        )
                    }
                    if (imageCount > 0) {
                        Box(
                            modifier = Modifier
                                .weight(imageCount.toFloat())
                                .height(5.dp)
                                .background(DocumentType.IMAGE.primaryColor)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Minimalist Legend
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LegendItem("PDF", pdfCount, DocumentType.PDF.primaryColor)
                    LegendItem("Word", wordCount, DocumentType.WORD.primaryColor)
                    LegendItem("Excel", excelCount, DocumentType.EXCEL.primaryColor)
                    LegendItem("PPT", pptCount, DocumentType.POWERPOINT.primaryColor)
                    LegendItem("Text", textCount, DocumentType.TEXT.primaryColor)
                }
            }
        }
    }
}

@Composable
private fun LegendItem(label: String, count: Int, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "$label ($count)",
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}


