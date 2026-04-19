package com.dyslexiread.ui.settings

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.fragment.app.activityViewModels
import com.dyslexiread.databinding.BottomSheetSettingsBinding
import com.dyslexiread.ui.ReaderViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

class SettingsBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetSettingsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ReaderViewModel by activityViewModels()

    private val bgPresets = listOf(
        Color.parseColor("#FFF8F0") to "Crème",
        Color.parseColor("#FFFDE7") to "Jaune doux",
        Color.parseColor("#E8F5E9") to "Vert doux",
        Color.parseColor("#E3F2FD") to "Bleu doux",
        Color.parseColor("#F3E5F5") to "Lavande",
        Color.WHITE                 to "Blanc",
        Color.parseColor("#1A1A2E") to "Nuit"
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val settings = viewModel.settings.value ?: return

        // Taille de police
        binding.seekbarFontSize.apply {
            min = 14; max = 40
            progress = settings.fontSize.toInt()
            setOnSeekBarChangeListener(onSeekBar { viewModel.updateFontSize(it.toFloat()) })
        }
        updateFontSizeLabel(settings.fontSize.toInt())

        // Interligne
        binding.seekbarLineHeight.apply {
            min = 0; max = 18
            progress = ((settings.lineHeight - 1.2f) * 10).toInt()
            setOnSeekBarChangeListener(onSeekBar { viewModel.updateLineHeight(it * 0.1f + 1.2f) })
        }

        // Espacement lettres
        binding.seekbarLetterSpacing.apply {
            min = 0; max = 30
            progress = (settings.letterSpacing * 100).toInt()
            setOnSeekBarChangeListener(onSeekBar { viewModel.updateLetterSpacing(it * 0.01f) })
        }

        // Mode sombre
        binding.switchDarkMode.apply {
            isChecked = settings.darkMode
            setOnCheckedChangeListener { _, _ -> viewModel.toggleDarkMode() }
        }

        setupColorChips()

        // Aperçu en temps réel
        viewModel.settings.observe(viewLifecycleOwner) { s ->
            updateFontSizeLabel(s.fontSize.toInt())
            binding.tvPreview.apply {
                textSize      = s.fontSize
                setLineSpacing(0f, s.lineHeight)
                letterSpacing = s.letterSpacing
                setBackgroundColor(s.backgroundColor)
                val tc = if (Color.luminance(s.backgroundColor) > 0.5f)
                    Color.parseColor("#1A1A1A") else Color.WHITE
                setTextColor(tc)
            }
        }
    }

    private fun setupColorChips() {
        val chipGroup: ChipGroup = binding.chipGroupColors
        chipGroup.removeAllViews()
        val currentColor = viewModel.settings.value?.backgroundColor ?: Color.WHITE

        bgPresets.forEach { (color, label) ->
            val chip = Chip(requireContext()).apply {
                text = label
                isCheckable = true
                isChecked   = (color == currentColor)
                chipBackgroundColor = android.content.res.ColorStateList.valueOf(color)
                setTextColor(
                    if (Color.luminance(color) > 0.5f) Color.BLACK else Color.WHITE
                )
                setOnClickListener { viewModel.updateBgColor(color) }
            }
            chipGroup.addView(chip)
        }
    }

    private fun updateFontSizeLabel(size: Int) {
        binding.tvFontSizeLabel.text = "$size sp"
    }

    private fun onSeekBar(block: (Int) -> Unit) = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
            if (fromUser) block(progress)
        }
        override fun onStartTrackingTouch(sb: SeekBar?) {}
        override fun onStopTrackingTouch(sb: SeekBar?) {}
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
