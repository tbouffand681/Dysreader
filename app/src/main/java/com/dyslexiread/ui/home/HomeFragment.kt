package com.dyslexiread.ui.home

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
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

    private var pendingPhotoUri: Uri? = null
    private var navigated = false

    // ── Demande de permission caméra ─────────────────────────────────────────
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            openCamera()
        } else {
            Snackbar.make(
                binding.root,
                "Permission caméra refusée. Autorisez-la dans Paramètres > Applications > DyslexiRead.",
                Snackbar.LENGTH_LONG
            ).show()
        }
    }

    // ── Capture photo ────────────────────────────────────────────────────────
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            val uri = pendingPhotoUri ?: return@registerForActivityResult
            showLoading(true, "Analyse de l'image…")
            viewModel.loadFromCameraUri(uri)
        } else {
            Snackbar.make(binding.root, "Photo annulée.", Snackbar.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navigated = false

        // Seul bouton actif pour l'instant
        binding.btnCamera.setOnClickListener { checkCameraPermissionAndOpen() }

        // Observer chargement
        viewModel.loading.observe(viewLifecycleOwner) { loading ->
            showLoading(loading, "Analyse en cours…")
        }

        // Observer erreurs
        viewModel.error.observe(viewLifecycleOwner) { err ->
            err?.let {
                showLoading(false)
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
            }
        }

        // Navigation vers le lecteur dès qu'un document est prêt
        viewModel.document.observe(viewLifecycleOwner) { doc ->
            if (doc != null && !navigated) {
                navigated = true
                showLoading(false)
                findNavController().navigate(R.id.action_home_to_reader)
            }
        }
    }

    // ── Gestion permission ───────────────────────────────────────────────────

    private fun checkCameraPermissionAndOpen() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                openCamera()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                Snackbar.make(
                    binding.root,
                    "La caméra est nécessaire pour photographier un texte.",
                    Snackbar.LENGTH_LONG
                ).setAction("Autoriser") {
                    requestPermissionLauncher.launch(Manifest.permission.CAMERA)
                }.show()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    // ── Ouvrir caméra ────────────────────────────────────────────────────────

    private fun openCamera() {
        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                ?: requireContext().filesDir  // fallback si stockage externe indisponible

            val photoFile = File.createTempFile(
                "DyslexiRead_${timestamp}_", ".jpg", storageDir
            )

            val uri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.provider",
                photoFile
            )

            pendingPhotoUri = uri
            cameraLauncher.launch(uri)

        } catch (e: Exception) {
            Snackbar.make(
                binding.root,
                "Impossible d'ouvrir la caméra : ${e.localizedMessage}",
                Snackbar.LENGTH_LONG
            ).show()
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun showLoading(show: Boolean, message: String = "") {
        binding.progressBar.visibility    = if (show) View.VISIBLE else View.GONE
        binding.loadingOverlay.visibility = if (show) View.VISIBLE else View.GONE
        if (message.isNotEmpty()) binding.tvLoadingMessage.text = message
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
