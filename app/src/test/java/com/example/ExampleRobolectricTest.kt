package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.document.text.TextEngine
import com.example.domain.model.DocumentType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("DocSphere", appName)
  }

  @Test
  fun `document type detection test`() {
    assertEquals(DocumentType.PDF, DocumentType.fromExtension("pdf"))
    assertEquals(DocumentType.WORD, DocumentType.fromExtension("docx"))
    assertEquals(DocumentType.WORD, DocumentType.fromExtension("doc"))
    assertEquals(DocumentType.EXCEL, DocumentType.fromExtension("xlsx"))
    assertEquals(DocumentType.EXCEL, DocumentType.fromExtension("xls"))
    assertEquals(DocumentType.POWERPOINT, DocumentType.fromExtension("pptx"))
    assertEquals(DocumentType.POWERPOINT, DocumentType.fromExtension("ppt"))
    assertEquals(DocumentType.TEXT, DocumentType.fromExtension("txt"))
    assertEquals(DocumentType.TEXT, DocumentType.fromExtension("json"))
    assertEquals(DocumentType.IMAGE, DocumentType.fromExtension("png"))
    assertEquals(DocumentType.IMAGE, DocumentType.fromExtension("jpg"))
  }

  @Test
  fun `legacy ppt engine parse test`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val tempFile = java.io.File(context.cacheDir, "sample_test.ppt")

    val out = java.io.ByteArrayOutputStream()
    fun writeRecord(type: Int, data: ByteArray) {
      val header = java.nio.ByteBuffer.allocate(8).order(java.nio.ByteOrder.LITTLE_ENDIAN)
      header.putShort(0.toShort())
      header.putShort(type.toShort())
      header.putInt(data.size)
      out.write(header.array())
      out.write(data)
    }

    fun writeTextAtom(text: String) {
      val bytes = text.toByteArray(java.nio.charset.StandardCharsets.UTF_16LE)
      writeRecord(4008, bytes)
    }

    writeRecord(1006, ByteArray(0))
    writeTextAtom("Quarterly Business Review")
    writeTextAtom("• Revenue grew by 45%\n• Global expansion on track\n• New mobile architecture launched")

    tempFile.writeBytes(out.toByteArray())

    val engine = com.example.document.powerpoint.PptxEngine(context)
    val result = engine.parsePptx(android.net.Uri.fromFile(tempFile))

    assertTrue(result.isSuccess)
    val pres = result.getOrNull()
    assertNotNull(pres)
    assertEquals(1, pres?.totalSlides)
    assertEquals("Quarterly Business Review", pres?.slides?.firstOrNull()?.title)
    assertEquals(3, pres?.slides?.firstOrNull()?.bulletPoints?.size)
  }

  @Test
  fun `text engine parse test`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val tempFile = java.io.File(context.cacheDir, "sample_test.txt")
    tempFile.writeText("Line 1: Hello DocSphere\nLine 2: Offline Document Viewer\nLine 3: Fast and Secure")

    val engine = TextEngine(context)
    val result = engine.parseText(android.net.Uri.fromFile(tempFile))

    assertTrue(result.isSuccess)
    val doc = result.getOrNull()
    assertNotNull(doc)
    assertEquals(3, doc?.lineCount)
    assertTrue((doc?.characterCount ?: 0) > 0)
    assertTrue((doc?.wordCount ?: 0) >= 8)
  }
}
