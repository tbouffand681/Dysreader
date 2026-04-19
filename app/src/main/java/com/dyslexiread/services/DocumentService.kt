package com.dyslexiread.services

import android.content.Context
import android.net.Uri
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.xwpf.usermodel.XWPFDocument
import java.io.InputStream

/**
 * Extraction de texte depuis PDF, DOCX et TXT.
 * Toutes les opérations sont exécutées sur Dispatchers.IO.
 */
class DocumentService(private val context: Context) {

    /**
     * Point d'entrée unique : détecte le type MIME et extrait le texte.
     */
    suspend fun extractText(uri: Uri): String = withContext(Dispatchers.IO) {
        val mime = context.contentResolver.getType(uri) ?: inferMimeFromUri(uri)
        val stream = context.contentResolver.openInputStream(uri)
            ?: error("Impossible d'ouvrir le fichier")
        stream.use {
            when {
                mime.contains("pdf")  -> extractFromPdf(it)
                mime.contains("wordprocessingml") ||
                mime.contains("docx") ||
                mime.contains("msword") -> extractFromDocx(it)
                else                  -> it.bufferedReader().readText()
            }
        }
    }

    // ── PDF via iText7 ────────────────────────────────────────────────────────

    private fun extractFromPdf(stream: InputStream): String {
        val reader = PdfReader(stream)
        val pdfDoc = PdfDocument(reader)
        val sb = StringBuilder()
        for (i in 1..pdfDoc.numberOfPages) {
            val page = pdfDoc.getPage(i)
            val text = PdfTextExtractor.getTextFromPage(page)
            if (text.isNotBlank()) {
                sb.append(text)
                sb.appendLine()
                sb.appendLine() // séparateur de pages
            }
        }
        pdfDoc.close()
        return sb.toString().trim()
    }

    // ── DOCX via Apache POI ───────────────────────────────────────────────────

    private fun extractFromDocx(stream: InputStream): String {
        val doc = XWPFDocument(stream)
        val sb = StringBuilder()
        for (para in doc.paragraphs) {
            val text = para.text
            if (text.isNotBlank()) sb.appendLine(text)
            else sb.appendLine() // paragraphe vide = saut de ligne
        }
        // Tables
        for (table in doc.tables) {
            for (row in table.rows) {
                val cells = row.tableCells.joinToString(" | ") { it.text }
                sb.appendLine(cells)
            }
        }
        doc.close()
        return sb.toString().trim()
    }

    private fun inferMimeFromUri(uri: Uri): String {
        return when (uri.toString().substringAfterLast('.').lowercase()) {
            "pdf"  -> "application/pdf"
            "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            "doc"  -> "application/msword"
            else   -> "text/plain"
        }
    }

    /**
     * Retourne le nom du fichier depuis l'URI.
     */
    fun getFileName(uri: Uri): String {
        var name = "Document"
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && idx >= 0) name = cursor.getString(idx)
        }
        return name
    }
}
