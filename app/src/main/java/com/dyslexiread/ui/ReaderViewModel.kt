package com.dyslexiread.ui

import android.app.Application
import android.graphics.Color
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.dyslexiread.models.LoadedDocument
import com.dyslexiread.models.ReaderSettings
import com.dyslexiread.models.SourceType
import com.dyslexiread.models.TtsLanguage
import com.dyslexiread.services.DocumentService
import com.dyslexiread.services.OcrService
import com.dyslexiread.services.PdfExportService
import com.dyslexiread.services.TtsService
import kotlinx.coroutines.launch

class ReaderViewModel(application: Application) : AndroidViewModel(application) {

    // ── Services ───────────────────────────────────────────────────────────────
    val tts = TtsService(application)
    private val ocr = OcrService(application)
    private val docs = DocumentService(application)
    val pdfExport = PdfExportService(application)

    // ── Document courant ───────────────────────────────────────────────────────
    private val _document = MutableLiveData<LoadedDocument?>()
    val document: LiveData<LoadedDocument?> = _document

    // ── Paramètres ─────────────────────────────────────────────────────────────
    private val _settings = MutableLiveData(ReaderSettings())
    val settings: LiveData<ReaderSettings> = _settings

    // ── Chargement ─────────────────────────────────────────────────────────────
    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    // ── TTS state (relay depuis TtsService) ────────────────────────────────────
    val ttsState = tts.state

    // ────────────────────────────────────────────────────────────────────────────
    // Sources d'entrée
    // ────────────────────────────────────────────────────────────────────────────

    fun loadFromCameraUri(uri: Uri) = launch("Analyse de l'image…") {
        val text = ocr.extractTextFromUri(uri)
        _document.postValue(LoadedDocument(text, "Photo", SourceType.CAMERA))
    }

    fun loadFromGalleryUri(uri: Uri) = launch("Lecture de l'image…") {
        val text = ocr.extractTextFromUri(uri)
        _document.postValue(LoadedDocument(text, "Galerie", SourceType.GALLERY))
    }

    fun loadFromDocumentUri(uri: Uri) = launch("Extraction du document…") {
        val text = docs.extractText(uri)
        val name = docs.getFileName(uri)
        val type = if (name.endsWith(".pdf")) SourceType.PDF
                   else if (name.endsWith(".docx") || name.endsWith(".doc")) SourceType.DOCX
                   else SourceType.TXT
        _document.postValue(LoadedDocument(text, name, type))
    }

    fun loadManualText(text: String) {
        _document.value = LoadedDocument(text, "Texte libre", SourceType.MANUAL)
    }

    // ────────────────────────────────────────────────────────────────────────────
    // Paramètres d'affichage
    // ────────────────────────────────────────────────────────────────────────────

    fun updateFontSize(sp: Float) = updateSettings { copy(fontSize = sp.coerceIn(14f, 40f)) }
    fun updateLineHeight(v: Float) = updateSettings { copy(lineHeight = v.coerceIn(1.2f, 3f)) }
    fun updateLetterSpacing(v: Float) = updateSettings { copy(letterSpacing = v.coerceIn(0f, 0.3f)) }
    fun updateBgColor(color: Int) = updateSettings { copy(backgroundColor = color) }
    fun toggleDarkMode() = updateSettings { copy(darkMode = !darkMode) }
    fun updateTtsLanguage(code: String) = updateSettings { copy(ttsLanguage = code) }
    fun updateTtsSpeed(v: Float) = updateSettings { copy(ttsSpeed = v) }
    fun updateTtsPitch(v: Float) = updateSettings { copy(ttsPitch = v) }

    private fun updateSettings(transform: ReaderSettings.() -> ReaderSettings) {
        _settings.value = _settings.value?.transform()
    }

    // ────────────────────────────────────────────────────────────────────────────
    // Audio
    // ────────────────────────────────────────────────────────────────────────────

    fun speakCurrentDocument() {
        val doc = _document.value ?: return
        val s = _settings.value ?: return
        tts.speak(doc.text, s.ttsLanguage, s.ttsSpeed, s.ttsPitch)
    }

    fun pauseTts() = tts.pause()
    fun stopTts() = tts.stop()

    // ────────────────────────────────────────────────────────────────────────────
    // Export PDF
    // ────────────────────────────────────────────────────────────────────────────

    suspend fun buildExportPdf(): ByteArray? {
        val doc = _document.value ?: return null
        val s = _settings.value ?: return null
        return pdfExport.buildPdf(
            text = doc.text,
            title = doc.label,
            fontSize = s.fontSize,
            lineHeight = s.lineHeight,
            letterSpacing = s.letterSpacing * 20 // em → pt approximatif
        )
    }

    // ────────────────────────────────────────────────────────────────────────────
    // Helpers
    // ────────────────────────────────────────────────────────────────────────────

    private fun launch(loadingMsg: String = "", block: suspend () -> Unit) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                block()
            } catch (e: Exception) {
                _error.postValue(e.localizedMessage ?: "Erreur inconnue")
            } finally {
                _loading.postValue(false)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        tts.shutdown()
        ocr.close()
    }
}
