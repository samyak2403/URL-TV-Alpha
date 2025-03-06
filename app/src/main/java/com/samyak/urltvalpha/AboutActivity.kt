package com.samyak.urltvalpha

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.animation.OvershootInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.doOnLayout
import com.google.android.material.card.MaterialCardView
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.snackbar.Snackbar
import android.widget.TextView

class AboutActivity : AppCompatActivity() {

    // View references
    private lateinit var appIcon: ShapeableImageView
    private lateinit var versionTextView: TextView
    private lateinit var developerCard: MaterialCardView
    private lateinit var appNameText: TextView
    private lateinit var descriptionText: TextView
    
    // Animation constants
    private companion object {
        const val ANIMATION_DURATION_ICON = 500L
        const val ANIMATION_DURATION_TEXT = 300L
        const val ANIMATION_DURATION_CARD = 400L
        const val ANIMATION_OFFSET_Y = 50f
        const val ANIMATION_SCALE_SMALL = 0.8f
        const val ANIMATION_SCALE_NORMAL = 1f
        const val ANIMATION_SCALE_CLICK = 0.9f
        const val ANIMATION_ROTATION = 360f
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)

        // Set status bar color
        window.statusBarColor = ContextCompat.getColor(this, R.color.Red)

        // Initialize views and setup UI
        initViews()
        setupUI()
    }

    private fun initViews() {
        // Initialize all views
        appIcon = findViewById(R.id.appIcon)
        versionTextView = findViewById(R.id.versionTextView)
        developerCard = findViewById(R.id.developer_card)
        appNameText = findViewById(R.id.appNameText)
        descriptionText = findViewById(R.id.descriptionText)
    }

    private fun setupUI() {
        // Setup toolbar
        setupToolbar()

        // Load and set version with fade animation
        setAppVersion()

        // Add animations with proper choreography
        // Wait for layout to be ready before starting animations
        appIcon.doOnLayout {
            animateViews()
        }

        // Setup click listeners with haptic feedback
        setupClickListeners()
    }

    private fun setupToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = getString(R.string.about)
        }
    }

    private fun setAppVersion() {
        try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            ViewCompat.setTransitionName(versionTextView, "version_text")
            versionTextView.alpha = 0f
            versionTextView.text = getString(R.string.version_format, packageInfo.versionName)
            versionTextView.animate()
                .alpha(1f)
                .setDuration(ANIMATION_DURATION_TEXT)
                .start()
        } catch (e: Exception) {
            versionTextView.visibility = View.GONE
        }
    }

    private fun animateViews() {
        // Reset initial states
        appIcon.scaleX = ANIMATION_SCALE_SMALL
        appIcon.scaleY = ANIMATION_SCALE_SMALL
        appIcon.alpha = 0f
        appNameText.alpha = 0f
        descriptionText.alpha = 0f
        developerCard.alpha = 0f
        developerCard.translationY = ANIMATION_OFFSET_Y

        // Animate app icon with bounce
        appIcon.animate()
            .scaleX(ANIMATION_SCALE_NORMAL)
            .scaleY(ANIMATION_SCALE_NORMAL)
            .alpha(1f)
            .setDuration(ANIMATION_DURATION_ICON)
            .setInterpolator(OvershootInterpolator())
            .withEndAction {
                // Animate app name after icon animation
                appNameText.animate()
                    .alpha(1f)
                    .setDuration(ANIMATION_DURATION_TEXT)
                    .withEndAction {
                        // Animate description
                        descriptionText.animate()
                            .alpha(1f)
                            .setDuration(ANIMATION_DURATION_TEXT)
                            .start()
                        
                        // Animate developer card
                        developerCard.animate()
                            .alpha(1f)
                            .translationY(0f)
                            .setDuration(ANIMATION_DURATION_CARD)
                            .setInterpolator(OvershootInterpolator())
                            .start()
                    }
                    .start()
            }
            .start()
    }

    private fun setupClickListeners() {
        // App icon click animation
        appIcon.setOnClickListener { view ->
            view.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
            ViewCompat.animate(view)
                .scaleX(ANIMATION_SCALE_CLICK)
                .scaleY(ANIMATION_SCALE_CLICK)
                .setDuration(100)
                .withEndAction {
                    ViewCompat.animate(view)
                        .scaleX(ANIMATION_SCALE_NORMAL)
                        .scaleY(ANIMATION_SCALE_NORMAL)
                        .rotationBy(ANIMATION_ROTATION)
                        .setDuration(ANIMATION_DURATION_TEXT)
                        .setInterpolator(OvershootInterpolator())
                        .start()
                }
                .start()
            
            // Show feedback message
            showFeedbackMessage(view)
        }

        // Developer card click with ripple and haptic feedback
        developerCard.setOnClickListener { view ->
            view.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
            openDeveloperEmail(view)
        }
    }
    
    private fun showFeedbackMessage(view: View) {
        Snackbar.make(view, getString(R.string.app_feedback_message), Snackbar.LENGTH_SHORT)
            .setAnchorView(view)
            .show()
    }
    
    private fun openDeveloperEmail(view: View) {
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:developer@example.com")
                putExtra(Intent.EXTRA_SUBJECT, getString(R.string.email_subject))
            }
            startActivity(intent)
        } catch (e: Exception) {
            Snackbar.make(view, getString(R.string.email_error_message), Snackbar.LENGTH_SHORT)
                .setAnchorView(view)
                .show()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
    
    override fun onDestroy() {
        // Clear any pending animations to prevent memory leaks
        appIcon.clearAnimation()
        versionTextView.clearAnimation()
        appNameText.clearAnimation()
        descriptionText.clearAnimation()
        developerCard.clearAnimation()
        super.onDestroy()
    }
} 