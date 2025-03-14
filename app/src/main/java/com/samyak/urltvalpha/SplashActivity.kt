package com.samyak.urltvalpha

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AnticipateOvershootInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.samyak.urltvalpha.databinding.ActivitySplashBinding

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Hide system bars
        hideSystemBars()

        // Start animations
        startSplashAnimation()
    }

    private fun hideSystemBars() {
        val windowInsetsController =
            ViewCompat.getWindowInsetsController(window.decorView) ?: return
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
    }

    private fun startSplashAnimation() {
        // Initial setup - move views off screen
        binding.apply {
            logoImageView.translationY = -1000f
            appNameTextView.alpha = 0f
            appNameTextView.scaleX = 0f
            appNameTextView.scaleY = 0f
        }

        // Logo animation
        val logoDropIn = ObjectAnimator.ofFloat(binding.logoImageView, "translationY", -1000f, 0f).apply {
            duration = 1000
            interpolator = AnticipateOvershootInterpolator()
        }

        val logoRotate = ObjectAnimator.ofFloat(binding.logoImageView, "rotation", 0f, 360f).apply {
            duration = 1000
            interpolator = AccelerateDecelerateInterpolator()
        }

        // Text animations
        val textFadeIn = ObjectAnimator.ofFloat(binding.appNameTextView, "alpha", 0f, 1f).apply {
            duration = 800
        }

        val textScaleX = ObjectAnimator.ofFloat(binding.appNameTextView, "scaleX", 0f, 1f).apply {
            duration = 800
            interpolator = AnticipateOvershootInterpolator()
        }

        val textScaleY = ObjectAnimator.ofFloat(binding.appNameTextView, "scaleY", 0f, 1f).apply {
            duration = 800
            interpolator = AnticipateOvershootInterpolator()
        }

        // Combine animations
        AnimatorSet().apply {
            playTogether(logoDropIn, logoRotate)
            play(textFadeIn).after(logoDropIn)
            play(textScaleX).with(textFadeIn)
            play(textScaleY).with(textFadeIn)
            start()
        }

        // Navigate to MainActivity after animation
        binding.logoImageView.postDelayed({
            startActivity(Intent(this@SplashActivity, MainActivity::class.java))
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }, 2500) // Total animation duration
    }
} 