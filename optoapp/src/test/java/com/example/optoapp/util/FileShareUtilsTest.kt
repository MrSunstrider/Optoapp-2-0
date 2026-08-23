package com.example.optoapp.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class FileShareUtilsTest {

    private lateinit var context: Context
    private val testFile = File("/data/test/file.pdf")
    private val mockUri: Uri = Uri.parse("content://com.example.optoapp.fileprovider/external/file.pdf")

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        mockkStatic(FileProvider::class)
        every { FileProvider.getUriForFile(any(), any(), any()) } returns mockUri
        every { context.packageName } returns "com.example.optoapp"
    }

    @After
    fun tearDown() {
        // unmockkStatic is not called to avoid issues with shared state in Robolectric
    }

    @Test
    fun `getUri returns provider URI`() {
        val uri = FileShareUtils.getUri(context, testFile)
        org.junit.Assert.assertEquals(mockUri, uri)
    }

    @Test
    fun `sendWhatsAppMessage blank phone shows toast and returns`() {
        FileShareUtils.sendWhatsAppMessage(context, "", "Hola")
        verify(exactly = 0) { context.startActivity(any<Intent>()) }
    }

    @Test
    fun `sendWhatsAppMessage blank phone with custom message`() {
        FileShareUtils.sendWhatsAppMessage(
            context,
            "",
            "Hola",
            emptyNumberMessage = "No hay teléfono",
        )
        verify(exactly = 0) { context.startActivity(any<Intent>()) }
    }

    @Test
    fun `sendWhatsAppMessage strips spaces from phone`() {
        FileShareUtils.sendWhatsAppMessage(context, "999 888 777", "Hola")
        verify(exactly = 1) { context.startActivity(any<Intent>()) }
    }

    @Test
    fun `sendWhatsAppMessage handles ActivityNotFoundException`() {
        every { context.startActivity(any<Intent>()) } throws ActivityNotFoundException()
        // Should not crash
        FileShareUtils.sendWhatsAppMessage(context, "999888777", "Hola")
        verify(exactly = 1) { context.startActivity(any<Intent>()) }
    }

    @Test
    fun `sendWhatsAppMessage handles generic Exception`() {
        every { context.startActivity(any<Intent>()) } throws RuntimeException("fail")
        // Should not crash
        FileShareUtils.sendWhatsAppMessage(context, "999888777", "Hola")
        verify(exactly = 1) { context.startActivity(any<Intent>()) }
    }

    @Test
    fun `sendWhatsAppMessage cleans plus and dash from phone`() {
        FileShareUtils.sendWhatsAppMessage(context, "+51-999-888-777", "Hola")
        verify(exactly = 1) { context.startActivity(any<Intent>()) }
    }

    @Test
    fun `sharePdf delegates to shareFile`() {
        FileShareUtils.sharePdf(context, testFile, "Compartir")
        verify(exactly = 1) { context.startActivity(any<Intent>()) }
    }

    @Test
    fun `shareCsv delegates to shareFile`() {
        val csvFile = File("/data/test/file.csv")
        FileShareUtils.shareCsv(context, csvFile, "Compartir CSV")
        verify(exactly = 1) { context.startActivity(any<Intent>()) }
    }

    @Test
    fun `shareFile handles ActivityNotFoundException`() {
        every { context.startActivity(any<Intent>()) } throws ActivityNotFoundException()
        // Should not crash
        FileShareUtils.shareFile(context, testFile, "application/pdf", "Compartir")
        verify(exactly = 1) { context.startActivity(any<Intent>()) }
    }

    @Test
    fun `openPdf starts activity with chooser`() {
        FileShareUtils.openPdf(context, testFile, "Abrir")
        verify(exactly = 1) { context.startActivity(any<Intent>()) }
    }

    @Test
    fun `openPdf handles ActivityNotFoundException`() {
        every { context.startActivity(any<Intent>()) } throws ActivityNotFoundException()
        // Should not crash
        FileShareUtils.openPdf(context, testFile, "Abrir")
        verify(exactly = 1) { context.startActivity(any<Intent>()) }
    }
}
