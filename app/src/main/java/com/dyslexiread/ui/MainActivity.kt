package com.dyslexiread.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.dyslexiread.R
import com.dyslexiread.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHost = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHost.navController

        // Gérer l'ouverture de fichiers depuis d'autres apps
        intent?.data?.let { uri ->
            val bundle = Bundle().apply { putParcelable("external_uri", uri) }
            navController.navigate(R.id.readerFragment, bundle)
        }
    }
}
