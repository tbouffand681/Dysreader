package com.dyslexiread.services

import android.content.Context
import android.content.Intent
import android.graphics.Color
import androidx.core.content.FileProvider
import com.itextpdf.io.font.FontProgram
import com.itextpdf.io.font.FontProgramFactory
import com.itextpdf.io.font.PdfEncodings
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Text
import com.itextpdf.layout.properties.Leading
import com.itextpdf.layout.properties.Property
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.ByteArrayOutputStream

/**
 * Génère un PDF avec la police OpenDyslexic via iText7.
 * La police doit être placée dans assets/fonts/OpenDyslexic-Regular.ttf
 */
class PdfExportService(private val context: Context) {

    /**
     * Génère les bytes du PDF.
     */
    suspend fun buildPdf(
        text: String,
        title: String,
        fontSize: Float,
        lineHeight: Float,
        letterSpacing: Float
    ): ByteArray = withContext(Dispatchers.IO) {

        val out = ByteArrayOutputStream()

        // Charger la police depuis les assets
        val fontBytes = context.assets.open("fonts/OpenDyslexic-Regular.ttf").readBytes()
        val boldBytes = context.assets.open("fonts/OpenDyslexic-Bold.ttf").readBytes()

        val regularProgram: FontProgram = FontProgramFactory.createFont(fontBytes)
        val boldProgram: FontProgram = FontProgramFactory.createFont(boldBytes)

        val writer = PdfWriter(out)
        val pdfDoc = PdfDocument(writer)
        val document = Document(pdfDoc, PageSize.A4)
        document.setMargins(60f, 50f, 60f, 50f)

        val regularFont = PdfFontFactory.createFont(regularProgram, PdfEncodings.IDENTITY_H, true)
        val boldFont    = PdfFontFactory.createFont(boldProgram, PdfEncodings.IDENTITY_H, true)

        val blue = DeviceRgb(0x2D, 0x7D, 0xD2)
        val grey = DeviceRgb(0x88, 0x88, 0x88)

        // ── Titre ─────────────────────────────────────────────────────────────
        val titlePara = Paragraph(title.ifBlank { "DyslexiRead" })
            .setFont(boldFont)
            .setFontSize(fontSize + 4)
            .setFontColor(blue)
            .setCharacterSpacing(letterSpacing)
            .setMarginBottom(16f)
        document.add(titlePara)

        // Séparateur
        val separator = Paragraph("━".repeat(60))
            .setFontColor(blue)
            .setFontSize(8f)
            .setMarginBottom(20f)
        document.add(separator)

        // ── Corps du texte ────────────────────────────────────────────────────
        val paragraphs = text.split("\n").filter { it.isNotBlank() }
        for (para in paragraphs) {
            val p = Paragraph(para)
                .setFont(regularFont)
                .setFontSize(fontSize)
                .setCharacterSpacing(letterSpacing)
                .setFixedLeading(fontSize * lineHeight)
                .setMarginBottom(fontSize * 0.6f)
            document.add(p)
        }

        // ── Pied de page ──────────────────────────────────────────────────────
        document.add(
            Paragraph("\n\nGénéré par DyslexiRead • Police OpenDyslexic")
                .setFont(regularFont)
                .setFontSize(8f)
                .setFontColor(grey)
        )

        document.close()
        out.toByteArray()
    }

    /**
     * Sauvegarde le PDF dans les fichiers internes et retourne l'URI shareable.
     */
    suspend fun saveToFile(bytes: ByteArray, filename: String): File =
        withContext(Dispatchers.IO) {
            val dir = File(context.filesDir, "exports").also { it.mkdirs() }
            val safe = filename.replace(Regex("[^\\w\\s\\-.]"), "_")
            val file = File(dir, "${safe}_dyslexiread.pdf")
            file.writeBytes(bytes)
            file
        }

    /**
     * Lance le partage natif Android (WhatsApp, Gmail, Drive, etc.)
     */
    fun shareFile(file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Document DyslexiRead")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Partager via").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    /**
     * Ouvre le PDF avec le lecteur par défaut de l'appareil.
     */
    fun openFile(file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
