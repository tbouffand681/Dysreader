package com.dyslexiread.services

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.content.Context
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.InputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Service OCR hors-ligne via ML Kit (script Latin → FR, EN, IT).
 * Retourne le texte structuré bloc par bloc.
 */
class OcrService(private val context: Context) {

    // Instance unique du recognizer (à fermer quand plus utilisé)
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    /**
     * Extrait le texte depuis un URI (photo caméra ou galerie).
     */
    suspend fun extractTextFromUri(uri: Uri): String {
        val inputImage = InputImage.fromFilePath(context, uri)
        return processImage(inputImage)
    }

    /**
     * Extrait le texte depuis un Bitmap (frame caméra live, etc.)
     */
    suspend fun extractTextFromBitmap(bitmap: Bitmap): String {
        val inputImage = InputImage.fromBitmap(bitmap, 0)
        return processImage(inputImage)
    }

    /**
     * Extrait le texte depuis un InputStream (image intégrée dans un fichier).
     */
    suspend fun extractTextFromStream(stream: InputStream): String {
        val bitmap = BitmapFactory.decodeStream(stream)
            ?: throw IllegalArgumentException("Impossible de décoder l'image")
        return extractTextFromBitmap(bitmap)
    }

    private suspend fun processImage(image: InputImage): String =
        suspendCancellableCoroutine { cont ->
            recognizer.process(image)
                .addOnSuccessListener { result ->
                    val sb = StringBuilder()
                    for (block in result.textBlocks) {
                        for (line in block.lines) {
                            sb.appendLine(line.text)
                        }
                        sb.appendLine() // saut entre blocs = paragraphe
                    }
                    cont.resume(sb.toString().trim())
                }
                .addOnFailureListener { e ->
                    cont.resumeWithException(e)
                }
        }

    fun close() {
        recognizer.close()
    }
}
