package com.dyslexiread.ui.reader

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.dyslexiread.R
import com.dyslexiread.databinding.FragmentReaderBinding
import com.dyslexiread.services.TtsService
import com.dyslexiread.ui.ReaderViewModel
import com.dyslexiread.ui.settings.SettingsBottomSheet
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class ReaderFragment : Fragment() {

    private var _binding: FragmentReaderBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ReaderViewModel by activityViewModels()

    private lateinit var openDyslexicTypeface: Typeface

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentReaderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Charger la police OpenDyslexic depuis les assets
        openDyslexicTypeface = Typeface.createFromAsset(
            requireContext().assets,
            "fonts/OpenDyslexic-Regular.ttf"
        )

        setupMenu()
        setupTtsControls()
        observeViewModel()

        // Si ouverture depuis une Intent externe (fichier partagé)
        arguments?.getParcelable<android.net.Uri>("external_uri")?.let { uri ->
            viewModel.loadFromDocumentUri(uri)
        }
    }

    // ── Menu toolbar (réglages + export) ────────────────────────────────────

    private fun setupMenu() {
        (requireActivity() as MenuHost).addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, inflater: MenuInflater) {
                inflater.inflate(R.menu.reader_menu, menu)
            }
            override fun onMenuItemSelected(item: MenuItem): Boolean {
                return when (item.itemId) {
                    R.id.action_settings -> {
                        SettingsBottomSheet().show(childFragmentManager, "settings")
                        true
                    }
                    R.id.action_export -> {
                        exportPdf()
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    // ── Contrôles TTS ───────────────────────────────────────────────────────

    private fun setupTtsControls() {
        binding.btnPlay.setOnClickListener {
            if (viewModel.ttsState.value == TtsService.State.PLAYING) {
                viewModel.pauseTts()
            } else {
                viewModel.speakCurrentDocument()
            }
        }
        binding.btnStop.setOnClickListener { viewModel.stopTts() }

        binding.chipFr.setOnClickListener { viewModel.updateTtsLanguage("fr-FR") }
        binding.chipEn.setOnClickListener { viewModel.updateTtsLanguage("en-US") }
        binding.chipIt.setOnClickListener { viewModel.updateTtsLanguage("it-IT") }

        binding.sliderSpeed.addOnChangeListener { _, value, _ -> viewModel.updateTtsSpeed(value) }
        binding.sliderPitch.addOnChangeListener { _, value, _ -> viewModel.updateTtsPitch(value) }
    }

    // ── Observers ───────────────────────────────────────────────────────────

    private fun observeViewModel() {
        viewModel.document.observe(viewLifecycleOwner) { doc ->
            if (doc != null) {
                applyTextToView(doc.text)
                requireActivity().title = doc.label
            } else {
                binding.tvContent.text = getString(R.string.no_text_loaded)
            }
        }

        viewModel.settings.observe(viewLifecycleOwner) { s ->
            binding.tvContent.apply {
                textSize = s.fontSize
                typeface = openDyslexicTypeface
                setLineSpacing(0f, s.lineHeight)
                letterSpacing = s.letterSpacing
            }
            binding.scrollView.setBackgroundColor(s.backgroundColor)
            val textColor = if (Color.luminance(s.backgroundColor) > 0.5f)
                Color.parseColor("#1A1A1A") else Color.WHITE
            binding.tvContent.setTextColor(textColor)

            // Langue TTS chips
            binding.chipFr.isChecked = s.ttsLanguage == "fr-FR"
            binding.chipEn.isChecked = s.ttsLanguage == "en-US"
            binding.chipIt.isChecked = s.ttsLanguage == "it-IT"

            binding.sliderSpeed.value = s.ttsSpeed
            binding.sliderPitch.value = s.ttsPitch
        }

        viewModel.loading.observe(viewLifecycleOwner) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        }

        viewModel.error.observe(viewLifecycleOwner) { err ->
            err?.let { Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show() }
        }

        // TTS state (StateFlow → coroutine)
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.ttsState.collect { state ->
                    binding.btnPlay.setIconResource(
                        if (state == TtsService.State.PLAYING) R.drawable.ic_pause
                        else R.drawable.ic_play
                    )
                    binding.tvTtsState.text = when (state) {
                        TtsService.State.PLAYING -> "🔊 Lecture…"
                        TtsService.State.PAUSED  -> "⏸ Pause"
                        TtsService.State.ERROR   -> "⚠ Voix indisponible"
                        else                     -> "🔇 Arrêt"
                    }
                }
            }
        }
    }

    private fun applyTextToView(text: String) {
        binding.tvContent.apply {
            typeface = openDyslexicTypeface
            this.text = text
        }
    }

    // ── Export PDF ──────────────────────────────────────────────────────────

    private fun exportPdf() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Exporter en PDF")
            .setItems(arrayOf("📂 Ouvrir sur l'appareil", "📤 Partager")) { _, which ->
                viewLifecycleOwner.lifecycleScope.launch {
                    val bytes = viewModel.buildExportPdf() ?: return@launch
                    val label = viewModel.document.value?.label ?: "document"
                    val file = viewModel.pdfExport.saveToFile(bytes, label)
                    if (which == 0) viewModel.pdfExport.openFile(file)
                    else viewModel.pdfExport.shareFile(file)
                }
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
