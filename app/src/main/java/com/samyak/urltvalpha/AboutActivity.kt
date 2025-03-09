package com.samyak.urltvalpha

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AnticipateOvershootInterpolator
import android.view.animation.OvershootInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.doOnLayout
import androidx.core.widget.NestedScrollView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.snackbar.Snackbar
import android.widget.TextView
import com.samyak.urltvalpha.utils.ToolbarUtils
import java.util.Calendar

class AboutActivity : AppCompatActivity() {

    // View references
    private lateinit var appIcon: ShapeableImageView
    private lateinit var versionTextView: TextView
    private lateinit var developerCard: MaterialCardView
    private lateinit var appNameText: TextView
    private lateinit var descriptionText: TextView
    private lateinit var developerAvatar: ShapeableImageView
    private lateinit var fabGithub: ExtendedFloatingActionButton
    private lateinit var motionLayout: MotionLayout
    private lateinit var scrollView: NestedScrollView
    private lateinit var iconBackground: View
    private lateinit var avatarBackground: View
    private lateinit var githubButton: MaterialButton
    private lateinit var copyrightText: TextView
    private lateinit var copyrightDivider: View
    private lateinit var toolbar: Toolbar
    
    // Animation constants
    private companion object {
        const val ANIMATION_DURATION_ICON = 600L
        const val ANIMATION_DURATION_TEXT = 400L
        const val ANIMATION_DURATION_CARD = 500L
        const val ANIMATION_OFFSET_Y = 50f
        const val ANIMATION_SCALE_SMALL = 0.8f
        const val ANIMATION_SCALE_NORMAL = 1f
        const val ANIMATION_SCALE_CLICK = 0.9f
        const val ANIMATION_ROTATION = 360f
        const val GITHUB_URL = "https://github.com/samyak2403"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)

        // Set status bar color
        window.statusBarColor = ContextCompat.getColor(this, R.color.col_blue_header)

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
        developerAvatar = findViewById(R.id.developerAvatar)
        fabGithub = findViewById(R.id.fabGithub)
        motionLayout = findViewById(R.id.motionLayout)
        scrollView = findViewById(R.id.scrollView)
        iconBackground = findViewById(R.id.iconBackground)
        avatarBackground = findViewById(R.id.avatarBackground)
        githubButton = findViewById(R.id.githubButton)
        copyrightText = findViewById(R.id.copyrightText)
        copyrightDivider = findViewById(R.id.copyrightDivider)
        toolbar = findViewById(R.id.toolbar)
    }

    private fun setupUI() {
        // Setup toolbar
        setupToolbar()

        // Update copyright text with current year
        updateCopyrightText()

        // Load and set version with fade animation
        setAppVersion()

        // Add animations with proper choreography
        // Wait for layout to be ready before starting animations
        appIcon.doOnLayout {
            animateViews()
        }

        // Setup click listeners with haptic feedback
        setupClickListeners()
        
        // Setup motion layout and FAB
        setupMotionAndFab()
    }

    private fun updateCopyrightText() {
        // Get current year for copyright
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val copyrightString = getString(R.string.copyright_text).replace("2023", currentYear.toString())
        copyrightText.text = copyrightString
    }

    private fun setupToolbar() {
        // Use the utility to setup centered toolbar
        ToolbarUtils.setupCenteredToolbar(this, toolbar, getString(R.string.about), true)
    }
    
    private fun setupMotionAndFab() {
        // Setup FAB click listener
        fabGithub.setOnClickListener { view ->
            view.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
            
            // Animate FAB when clicked
            ViewCompat.animate(view)
                .scaleX(ANIMATION_SCALE_CLICK)
                .scaleY(ANIMATION_SCALE_CLICK)
                .setDuration(100)
                .withEndAction {
                    ViewCompat.animate(view)
                        .scaleX(ANIMATION_SCALE_NORMAL)
                        .scaleY(ANIMATION_SCALE_NORMAL)
                        .setDuration(100)
                        .start()
                    
                    // Open GitHub profile
                    openGitHubProfile(view)
                }
                .start()
        }
        
        // Setup motion layout transition listener
        motionLayout.setTransitionListener(object : MotionLayout.TransitionListener {
            override fun onTransitionStarted(layout: MotionLayout?, startId: Int, endId: Int) {
                // When transition starts, ensure FAB is visible but transparent
                if (startId == R.id.start && endId == R.id.end) {
                    fabGithub.visibility = View.VISIBLE
                }
            }
            
            override fun onTransitionChange(layout: MotionLayout?, startId: Int, endId: Int, progress: Float) {
                // Dynamically shrink/expand the FAB text based on progress
                if (progress > 0.5 && fabGithub.isExtended) {
                    fabGithub.shrink()
                } else if (progress <= 0.5 && !fabGithub.isExtended) {
                    fabGithub.extend()
                }
            }
            
            override fun onTransitionCompleted(layout: MotionLayout?, currentId: Int) {
                // When transition completes, update FAB visibility based on current state
                if (currentId == R.id.end) {
                    fabGithub.show()
                    fabGithub.shrink() // Ensure it's in icon-only mode
                } else {
                    fabGithub.hide()
                    fabGithub.extend() // Reset to extended for next time
                }
            }
            
            override fun onTransitionTrigger(layout: MotionLayout?, triggerId: Int, positive: Boolean, progress: Float) {}
        })
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
        fabGithub.alpha = 0f
        fabGithub.scaleX = 0f
        fabGithub.scaleY = 0f
        iconBackground.alpha = 0f
        iconBackground.scaleX = 1.5f
        iconBackground.scaleY = 1.5f
        
        // Reset developer card elements
        developerAvatar.alpha = 0f
        developerAvatar.scaleX = 0.5f
        developerAvatar.scaleY = 0.5f
        avatarBackground.alpha = 0f
        avatarBackground.scaleX = 1.2f
        avatarBackground.scaleY = 1.2f
        githubButton.alpha = 0f
        githubButton.translationX = -50f
        copyrightText.alpha = 0f
        copyrightDivider.alpha = 0f

        // Animate app icon background with pulsating effect
        iconBackground.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(ANIMATION_DURATION_ICON)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                // Start subtle pulsating effect
                startIconBackgroundPulsating()
            }
            .start()

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
                            .withEndAction {
                                animateDeveloperCardElements()
                            }
                            .start()
                    }
                    .start()
            }
            .start()
    }
    
    private fun startIconBackgroundPulsating() {
        // Subtle pulsating effect for the icon background
        iconBackground.animate()
            .scaleX(1.05f)
            .scaleY(1.05f)
            .setDuration(2000)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                iconBackground.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(2000)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .withEndAction {
                        // Only continue pulsing if activity is still active
                        if (!isFinishing) {
                            startIconBackgroundPulsating()
                        }
                    }
                    .start()
            }
            .start()
    }
    
    private fun animateDeveloperCardElements() {
        // Animate avatar background first with a scale effect
        avatarBackground.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(ANIMATION_DURATION_TEXT)
            .setInterpolator(OvershootInterpolator())
            .start()
            
        // Animate developer avatar with a slight delay
        developerAvatar.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setStartDelay(100)
            .setDuration(ANIMATION_DURATION_TEXT)
            .setInterpolator(AnticipateOvershootInterpolator())
            .withEndAction {
                // Start subtle rotation animation for avatar
                startAvatarRotation()
                
                // Animate GitHub button
                githubButton.animate()
                    .alpha(1f)
                    .translationX(0f)
                    .setDuration(ANIMATION_DURATION_TEXT)
                    .setInterpolator(OvershootInterpolator())
                    .withEndAction {
                        // Animate copyright section
                        animateCopyrightSection()
                    }
                    .start()
            }
            .start()
    }
    
    private fun animateCopyrightSection() {
        // Animate copyright divider first
        copyrightDivider.animate()
            .alpha(0.1f)
            .setDuration(ANIMATION_DURATION_TEXT)
            .withEndAction {
                // Then animate copyright text
                copyrightText.animate()
                    .alpha(1f)
                    .setDuration(ANIMATION_DURATION_TEXT)
                    .start()
            }
            .start()
    }
    
    private fun startAvatarRotation() {
        // Very subtle rotation animation for the avatar
        developerAvatar.animate()
            .rotationBy(5f)
            .setDuration(3000)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                developerAvatar.animate()
                    .rotationBy(-5f)
                    .setDuration(3000)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .withEndAction {
                        // Only continue rotating if activity is still active
                        if (!isFinishing) {
                            startAvatarRotation()
                        }
                    }
                    .start()
            }
            .start()
    }

    private fun setupClickListeners() {
        // App icon click animation
        appIcon.setOnClickListener { view ->
            view.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
            
            // Animate background with a more pronounced effect
            iconBackground.animate()
                .scaleX(1.2f)
                .scaleY(1.2f)
                .setDuration(300)
                .setInterpolator(OvershootInterpolator())
                .withEndAction {
                    iconBackground.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(500)
                        .setInterpolator(AccelerateDecelerateInterpolator())
                        .start()
                }
                .start()
            
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
            
            // Animate avatar background
            avatarBackground.animate()
                .scaleX(1.2f)
                .scaleY(1.2f)
                .setDuration(200)
                .setInterpolator(OvershootInterpolator())
                .withEndAction {
                    avatarBackground.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(300)
                        .start()
                }
                .start()
            
            // Animate developer avatar on click
            developerAvatar.animate()
                .scaleX(1.2f)
                .scaleY(1.2f)
                .rotationBy(20f)
                .setDuration(200)
                .withEndAction {
                    developerAvatar.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .rotationBy(-20f)
                        .setDuration(200)
                        .start()
                }
                .start()
                
            openGitHubProfile(view)
        }
        
        // Also make the avatar clickable
        developerAvatar.setOnClickListener {
            it.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
            developerCard.performClick()
        }
        
        // Make GitHub button clickable
        githubButton.setOnClickListener {
            it.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
            
            // Animate button
            ViewCompat.animate(it)
                .scaleX(ANIMATION_SCALE_CLICK)
                .scaleY(ANIMATION_SCALE_CLICK)
                .setDuration(100)
                .withEndAction {
                    ViewCompat.animate(it)
                        .scaleX(ANIMATION_SCALE_NORMAL)
                        .scaleY(ANIMATION_SCALE_NORMAL)
                        .setDuration(100)
                        .start()
                }
                .start()
                
            openGitHubProfile(it)
        }
        
        // Make copyright text clickable to show app info
        copyrightText.setOnClickListener { view ->
            view.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
            Snackbar.make(view, getString(R.string.app_feedback_message), Snackbar.LENGTH_SHORT)
                .setAnchorView(if (fabGithub.visibility == View.VISIBLE) fabGithub else view)
                .setBackgroundTint(ContextCompat.getColor(this, R.color.col_blue_2))
                .setTextColor(ContextCompat.getColor(this, android.R.color.white))
                .show()
        }
    }
    
    private fun showFeedbackMessage(view: View) {
        Snackbar.make(view, getString(R.string.app_feedback_message), Snackbar.LENGTH_SHORT)
            .setAnchorView(if (fabGithub.visibility == View.VISIBLE) fabGithub else view)
            .setBackgroundTint(ContextCompat.getColor(this, R.color.col_blue_2))
            .setTextColor(ContextCompat.getColor(this, android.R.color.white))
            .show()
    }
    
    private fun openGitHubProfile(view: View) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB_URL))
            startActivity(intent)
            
            // Add a subtle transition animation
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        } catch (e: Exception) {
            Snackbar.make(view, getString(R.string.github_error_message), Snackbar.LENGTH_SHORT)
                .setAnchorView(if (fabGithub.visibility == View.VISIBLE) fabGithub else view)
                .setBackgroundTint(ContextCompat.getColor(this, R.color.col_blue_2))
                .setTextColor(ContextCompat.getColor(this, android.R.color.white))
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
        developerAvatar.clearAnimation()
        fabGithub.clearAnimation()
        iconBackground.clearAnimation()
        avatarBackground.clearAnimation()
        githubButton.clearAnimation()
        copyrightText.clearAnimation()
        copyrightDivider.clearAnimation()
        super.onDestroy()
    }
} 