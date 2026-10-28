package com.brijby77

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.brijby77.databinding.ActivitySplashBinding
import com.brijby77.ui.auth.AuthActivity
import com.brijby77.ui.auth.AuthRepository

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private var isNavigated = false
    private lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestWindowFeature(Window.FEATURE_NO_TITLE)
        @Suppress("DEPRECATION")
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        authRepository = AuthRepository(this)

        try {
            val videoUri = Uri.parse("android.resource://$packageName/${R.raw.splash_video}")
            binding.videoViewSplash.setVideoURI(videoUri)

            binding.videoViewSplash.setOnCompletionListener {
                navigateToNextScreen()
            }

            binding.videoViewSplash.setOnErrorListener { _, _, _ ->
                navigateToNextScreen()
                true
            }

            binding.videoViewSplash.start()
        } catch (e: Exception) {
            e.printStackTrace()
            navigateToNextScreen()
        }

        binding.btnSkipSplash.setOnClickListener {
            navigateToNextScreen()
        }

        binding.rootSplashLayout.setOnClickListener {
            navigateToNextScreen()
        }
    }

    private fun navigateToNextScreen() {
        if (isNavigated) return
        isNavigated = true

        val destinationIntent = if (authRepository.isLoggedIn()) {
            Intent(this, MainActivity::class.java)
        } else {
            Intent(this, AuthActivity::class.java)
        }

        startActivity(destinationIntent)
        finish()
    }
}
