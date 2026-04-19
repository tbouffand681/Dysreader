package com.dyslexiread.services
 
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.itextpdf.io.font.FontProgram
import com.itextpdf.io.font.FontProgramFactory
import com.itextpdf.io.font.PdfEncodings
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.font.PdfFontFactory.EmbeddingStrategy
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
 
class PdfExportService(private val context: Context) {
 
    suspend fun buildPdf(
        text: String,
        title: String,
        fontSize: Float,
        lineHeight: Float,
        letterSpacing: Float
    ): ByteArray = withContext(Dispatchers.IO) {
 
        val out = ByteArrayOutputStream()
 
        val fontBytes = context.assets.open("fonts/OpenDyslexic-Regular.ttf").readBytes()
        val boldBytes = context.assets.open("fonts/OpenDyslexic-Regular.ttf").readBytes() // fallback Regular si Bold absent
 
        val regularProgram: FontProgram = FontProgramFactory.createFont(fontBytes)
        val boldProgram: FontProgram    = FontProgramFactory.createFont(boldBytes)
 
        val writer  = PdfWriter(out)
        val pdfDoc  = PdfDocument(writer)
        val document = Document(pdfDoc, PageSize.A4)
        document.setMargins(60f, 50f, 60f, 50f)
 
        // ── Correction : utiliser EmbeddingStrategy.PREFER_EMBEDDED ──────────
        val regularFont = PdfFontFactory.createFont(
            regularProgram,
            PdfEncodings.IDENTITY_H,
            EmbeddingStrategy.PREFER_EMBEDDED
        )
        val boldFont = PdfFontFactory.createFont(
            boldProgram,
            PdfEncodings.IDENTITY_H,
            EmbeddingStrategy.PREFER_EMBEDDED
        )
 
        val blue = DeviceRgb(0x2D, 0x7D, 0xD2)
        val grey = DeviceRgb(0x88, 0x88, 0x88)
 
        // Titre
        document.add(
            Paragraph(title.ifBlank { "DyslexiRead" })
                .setFont(boldFont)
                .setFontSize(fontSize + 4)
                .setFontColor(blue)
                .setCharacterSpacing(letterSpacing)
                .setMarginBottom(16f)
        )
 
        // Séparateur
        document.add(
            Paragraph("─".repeat(55))
                .setFontColor(blue)
                .setFontSize(8f)
                .setMarginBottom(20f)
        )
 
        // Corps du texte
        val paragraphs = text.split("\n").filter { it.isNotBlank() }
        for (para in paragraphs) {
            document.add(
                Paragraph(para)
                    .setFont(regularFont)
                    .setFontSize(fontSize)
                    .setCharacterSpacing(letterSpacing)
                    .setFixedLeading(fontSize * lineHeight)
                    .setMarginBottom(fontSize * 0.6f)
            )
        }
 
        // Pied de page
        document.add(
            Paragraph("\n\nGénéré par DyslexiRead • Police OpenDyslexic")
                .setFont(regularFont)
                .setFontSize(8f)
                .setFontColor(grey)
        )
 
        document.close()
        out.toByteArray()
    }
 
    suspend fun saveToFile(bytes: ByteArray, filename: String): File =
        withContext(Dispatchers.IO) {
            val dir  = File(context.filesDir, "exports").also { it.mkdirs() }
            val safe = filename.replace(Regex("[^\\w\\s\\-.]"), "_")
            val file = File(dir, "${safe}_dyslexiread.pdf")
            file.writeBytes(bytes)
            file
        }
 
    fun shareFile(file: File) {
        val uri = FileProvider.getUriForFile(
            context, "${context.packageName}.provider", file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Document DyslexiRead")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            Intent.createChooser(intent, "Partager via")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
 
    fun openFile(file: File) {
        val uri = FileProvider.getUriForFile(
            context, "${context.packageName}.provider", file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
