package com.dyslexiread.models

import android.graphics.Color

/**
 * Paramètres d'affichage du lecteur.
 * Sérialisable pour SharedPreferences.
 */
data class ReaderSettings(
    val fontSize: Float = 20f,          // en sp
    val lineHeight: Float = 1.8f,       // multiplicateur
    val letterSpacing: Float = 0.12f,   // em (TextView utilise em)
    val backgroundColor: Int = Color.parseColor("#FFF8F0"),
    val darkMode: Boolean = false,
    val ttsLanguage: String = "fr-FR",
    val ttsSpeed: Float = 0.8f,         // 0.0 – 1.0 (Android TTS)
    val ttsPitch: Float = 1.0f
)

/**
 * Document chargé en mémoire.
 */
data class LoadedDocument(
    val text: String,
    val label: String,
    val sourceType: SourceType
)

enum class SourceType { CAMERA, GALLERY, PDF, DOCX, TXT, MANUAL }

/**
 * Langues TTS supportées hors-ligne.
 */
enum class TtsLanguage(val code: String, val label: String, val flag: String) {
    FRENCH("fr-FR", "Français", "🇫🇷"),
    ENGLISH("en-US", "English", "🇺🇸"),
    ITALIAN("it-IT", "Italiano", "🇮🇹");

    companion object {
        fun fromCode(code: String) = values().firstOrNull { it.code == code } ?: FRENCH
    }
}
