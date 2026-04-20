package com.dyslexiread.services

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class OcrService(private val context: Context) {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun extractTextFromUri(uri: Uri): String {
        Log.d("OcrService", "extractTextFromUri: $uri")

        // ── Méthode 1 : InputImage.fromFilePath (recommandée) ─────────────────
        return try {
            val inputImage = InputImage.fromFilePath(context, uri)
            Log.d("OcrService", "InputImage créé depuis filePath OK")
            processImage(inputImage)
        } catch (e1: Exception) {
            Log.w("OcrService", "fromFilePath échoué (${e1.message}), tentative via InputStream…")

            // ── Méthode 2 : fallback via InputStream + BitmapFactory ──────────
            try {
                val stream = context.contentResolver.openInputStream(uri)
                    ?: throw IllegalStateException("ContentResolver ne peut pas ouvrir l'URI : $uri")

                val bitmap = stream.use { BitmapFactory.decodeStream(it) }
                    ?: throw IllegalStateException("BitmapFactory n'a pas pu décoder l'image")

                Log.d("OcrService", "Bitmap décodé OK (${bitmap.width}×${bitmap.height})")
                val inputImage = InputImage.fromBitmap(bitmap, 0)
                processImage(inputImage)
            } catch (e2: Exception) {
                Log.e("OcrService", "Les deux méthodes ont échoué", e2)
                throw Exception("Impossible de lire l'image : ${e2.localizedMessage}", e2)
            }
        }
    }

    private suspend fun processImage(image: InputImage): String =
        suspendCancellableCoroutine { cont ->
            recognizer.process(image)
                .addOnSuccessListener { result ->
                    Log.d("OcrService", "OCR OK — ${result.textBlocks.size} blocs détectés")
                    val sb = StringBuilder()
                    for (block in result.textBlocks) {
                        for (line in block.lines) {
                            sb.appendLine(line.text)
                        }
                        sb.appendLine()
                    }
                    val text = sb.toString().trim()
                    cont.resume(if (text.isEmpty()) "(Aucun texte détecté)" else text)
                }
                .addOnFailureListener { e ->
                    Log.e("OcrService", "OCR échoué", e)
                    cont.resumeWithException(e)
                }
        }

    fun close() {
        recognizer.close()
    }
}
