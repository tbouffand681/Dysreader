package com.dyslexiread.ui.home

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.dyslexiread.R
import com.dyslexiread.databinding.FragmentHomeBinding
import com.dyslexiread.ui.ReaderViewModel
import com.google.android.material.snackbar.Snackbar
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ReaderViewModel by activityViewModels()
    private var cameraImageUri: Uri? = null
    private var navigated = false

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            cameraImageUri?.let { viewModel.loadFromCameraUri(it) }
        }
    }

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { viewModel.loadFromGalleryUri(it) } }

    private val fileLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { viewModel.loadFromDocumentUri(it) } }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navigated = false

        binding.btnCamera.setOnClickListener { launchCamera() }
        binding.btnGallery.setOnClickListener { galleryLauncher.launch("image/*") }
        binding.btnFile.setOnClickListener { fileLauncher.launch("*/*") }
        binding.btnManual.setOnClickListener { showManualTextDialog() }

        viewModel.loading.observe(viewLifecycleOwner) { loading ->
            binding.progressBar.visibility   = if (loading) View.VISIBLE else View.GONE
            binding.loadingOverlay.visibility = if (loading) View.VISIBLE else View.GONE
        }

        viewModel.error.observe(viewLifecycleOwner) { err ->
            err?.let { Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show() }
        }

        // Navigation dès qu'un document est chargé
        viewModel.document.observe(viewLifecycleOwner) { doc ->
            if (doc != null && !navigated) {
                navigated = true
                findNavController().navigate(R.id.action_home_to_reader)
            }
        }
    }

    private fun launchCamera() {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val photoFile = File(
            requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES),
            "DyslexiRead_$timestamp.jpg"
        )
        cameraImageUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.provider",
            photoFile
        )
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri)
        }
        cameraLauncher.launch(intent)
    }

    private fun showManualTextDialog() {
        val editText = EditText(requireContext()).apply {
            hint = "Collez ou tapez votre texte ici…"
            minLines = 5
            maxLines = 12
            setPadding(48, 32, 48, 32)
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Saisir du texte")
            .setView(editText)
            .setPositiveButton("Lire") { _, _ ->
                val text = editText.text.toString()
                if (text.isNotBlank()) viewModel.loadManualText(text)
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
