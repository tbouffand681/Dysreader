package com.dyslexiread.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.dyslexiread.models.LoadedDocument
import com.dyslexiread.models.ReaderSettings
import com.dyslexiread.models.SourceType
import com.dyslexiread.services.DocumentService
import com.dyslexiread.services.OcrService
import com.dyslexiread.services.PdfExportService
import com.dyslexiread.services.TtsService
import kotlinx.coroutines.launch

class ReaderViewModel(application: Application) : AndroidViewModel(application) {

    val tts = TtsService(application)
    private val ocr = OcrService(application)
    private val docs = DocumentService(application)
    val pdfExport = PdfExportService(application)

    private val _document = MutableLiveData<LoadedDocument?>()
    val document: LiveData<LoadedDocument?> = _document

    private val _settings = MutableLiveData(ReaderSettings())
    val settings: LiveData<ReaderSettings> = _settings

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    val ttsState = tts.state

    fun loadFromCameraUri(uri: Uri) = launchSafe {
        val text = ocr.extractTextFromUri(uri)
        _document.postValue(LoadedDocument(text, "Photo", SourceType.CAMERA))
    }

    fun loadFromGalleryUri(uri: Uri) = launchSafe {
        val text = ocr.extractTextFromUri(uri)
        _document.postValue(LoadedDocument(text, "Galerie", SourceType.GALLERY))
    }

    fun loadFromDocumentUri(uri: Uri) = launchSafe {
        val text = docs.extractText(uri)
        val name = docs.getFileName(uri)
        val type = when {
            name.endsWith(".pdf", ignoreCase = true)  -> SourceType.PDF
            name.endsWith(".docx", ignoreCase = true) ||
            name.endsWith(".doc", ignoreCase = true)  -> SourceType.DOCX
            else                                       -> SourceType.TXT
        }
        _document.postValue(LoadedDocument(text, name, type))
    }

    fun loadManualText(text: String) {
        _document.value = LoadedDocument(text, "Texte libre", SourceType.MANUAL)
    }

    fun updateFontSize(sp: Float)       = updateSettings { copy(fontSize = sp.coerceIn(14f, 40f)) }
    fun updateLineHeight(v: Float)      = updateSettings { copy(lineHeight = v.coerceIn(1.2f, 3f)) }
    fun updateLetterSpacing(v: Float)   = updateSettings { copy(letterSpacing = v.coerceIn(0f, 0.3f)) }
    fun updateBgColor(color: Int)       = updateSettings { copy(backgroundColor = color) }
    fun toggleDarkMode()                = updateSettings { copy(darkMode = !darkMode) }
    fun updateTtsLanguage(code: String) = updateSettings { copy(ttsLanguage = code) }
    fun updateTtsSpeed(v: Float)        = updateSettings { copy(ttsSpeed = v) }
    fun updateTtsPitch(v: Float)        = updateSettings { copy(ttsPitch = v) }

    private fun updateSettings(transform: ReaderSettings.() -> ReaderSettings) {
        _settings.value = _settings.value?.transform()
    }

    fun speakCurrentDocument() {
        val doc = _document.value ?: return
        val s   = _settings.value  ?: return
        tts.speak(doc.text, s.ttsLanguage, s.ttsSpeed, s.ttsPitch)
    }

    fun pauseTts() = tts.pause()
    fun stopTts()  = tts.stop()

    suspend fun buildExportPdf(): ByteArray? {
        val doc = _document.value ?: return null
        val s   = _settings.value  ?: return null
        return pdfExport.buildPdf(
            text          = doc.text,
            title         = doc.label,
            fontSize      = s.fontSize,
            lineHeight    = s.lineHeight,
            letterSpacing = s.letterSpacing * 20f
        )
    }

    private fun launchSafe(block: suspend () -> Unit) {
        viewModelScope.launch {
            _loading.value = true
            _error.value   = null
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
