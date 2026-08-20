package com.example.ui.navigation

import android.net.Uri
import androidx.navigation.NavController
import com.example.data.local.database.DocumentEntity
import com.example.domain.model.DocumentType

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Main : Screen("main")
    object Search : Screen("search")
    object CategoryDetail : Screen("category/{type}") {
        fun createRoute(type: DocumentType) = "category/${type.name}"
    }

    object PdfViewer : Screen("viewer_pdf/{uri}") {
        fun createRoute(uri: String) = "viewer_pdf/${Uri.encode(uri)}"
    }

    object WordViewer : Screen("viewer_word/{uri}") {
        fun createRoute(uri: String) = "viewer_word/${Uri.encode(uri)}"
    }

    object ExcelViewer : Screen("viewer_excel/{uri}") {
        fun createRoute(uri: String) = "viewer_excel/${Uri.encode(uri)}"
    }

    object PowerPointViewer : Screen("viewer_ppt/{uri}") {
        fun createRoute(uri: String) = "viewer_ppt/${Uri.encode(uri)}"
    }

    object TextViewer : Screen("viewer_text/{uri}") {
        fun createRoute(uri: String) = "viewer_text/${Uri.encode(uri)}"
    }

    object ImageViewer : Screen("viewer_image/{uri}") {
        fun createRoute(uri: String) = "viewer_image/${Uri.encode(uri)}"
    }
}

fun NavController.navigateToViewer(document: DocumentEntity) {
    val route = when (document.toDocumentType()) {
        DocumentType.PDF -> Screen.PdfViewer.createRoute(document.uri)
        DocumentType.WORD -> Screen.WordViewer.createRoute(document.uri)
        DocumentType.EXCEL -> Screen.ExcelViewer.createRoute(document.uri)
        DocumentType.POWERPOINT -> Screen.PowerPointViewer.createRoute(document.uri)
        DocumentType.TEXT -> Screen.TextViewer.createRoute(document.uri)
        DocumentType.IMAGE -> Screen.ImageViewer.createRoute(document.uri)
        DocumentType.UNKNOWN -> Screen.TextViewer.createRoute(document.uri)
    }
    navigate(route)
}

fun NavController.navigateToViewerByUri(uri: String, fileName: String) {
    val type = DocumentType.fromFileName(fileName)
    val route = when (type) {
        DocumentType.PDF -> Screen.PdfViewer.createRoute(uri)
        DocumentType.WORD -> Screen.WordViewer.createRoute(uri)
        DocumentType.EXCEL -> Screen.ExcelViewer.createRoute(uri)
        DocumentType.POWERPOINT -> Screen.PowerPointViewer.createRoute(uri)
        DocumentType.TEXT -> Screen.TextViewer.createRoute(uri)
        DocumentType.IMAGE -> Screen.ImageViewer.createRoute(uri)
        DocumentType.UNKNOWN -> Screen.TextViewer.createRoute(uri)
    }
    navigate(route)
}
