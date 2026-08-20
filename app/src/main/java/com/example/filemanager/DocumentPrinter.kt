package com.example.filemanager

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.widget.Toast
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream

object DocumentPrinter {

    fun printDocument(context: Context, documentUri: Uri, documentTitle: String) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
        if (printManager == null) {
            Toast.makeText(context, "Printing service unavailable on this device", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val printAdapter = object : PrintDocumentAdapter() {
                override fun onLayout(
                    oldAttributes: PrintAttributes?,
                    newAttributes: PrintAttributes?,
                    cancellationSignal: CancellationSignal?,
                    callback: LayoutResultCallback?,
                    extras: Bundle?
                ) {
                    if (cancellationSignal?.isCanceled == true) {
                        callback?.onLayoutCancelled()
                        return
                    }

                    val info = PrintDocumentInfo.Builder("$documentTitle.pdf")
                        .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                        .build()

                    callback?.onLayoutFinished(info, true)
                }

                override fun onWrite(
                    pages: Array<out PageRange>?,
                    destination: ParcelFileDescriptor?,
                    cancellationSignal: CancellationSignal?,
                    callback: WriteResultCallback?
                ) {
                    var input: InputStream? = null
                    var output: FileOutputStream? = null

                    try {
                        input = context.contentResolver.openInputStream(documentUri)
                        if (input == null && documentUri.scheme == "file") {
                            input = FileInputStream(documentUri.path)
                        }

                        if (destination != null) {
                            output = FileOutputStream(destination.fileDescriptor)
                            val buffer = ByteArray(16384)
                            var bytesRead = 0
                            while (input != null && input.read(buffer).also { bytesRead = it } >= 0) {
                                if (cancellationSignal?.isCanceled == true) {
                                    callback?.onWriteCancelled()
                                    return
                                }
                                output.write(buffer, 0, bytesRead)
                            }
                            callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                        }
                    } catch (e: Exception) {
                        callback?.onWriteFailed(e.localizedMessage)
                    } finally {
                        try {
                            input?.close()
                            output?.close()
                        } catch (e: Exception) {
                            // Ignored
                        }
                    }
                }
            }

            val jobName = "DocSphere Print - $documentTitle"
            val printAttributes = PrintAttributes.Builder()
                .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                .build()

            printManager.print(jobName, printAdapter, printAttributes)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Print error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }
}
