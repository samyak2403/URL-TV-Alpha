package com.samyak.urltvalpha.utils

import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.samyak.urltvalpha.R

/**
 * Utility class for toolbar operations
 */
object ToolbarUtils {

    /**
     * Sets up a toolbar with a centered title
     *
     * @param activity The activity where the toolbar is used
     * @param toolbar The toolbar to set up
     * @param title The title to display
     * @param showBackButton Whether to show the back button
     */
    fun setupCenteredToolbar(
        activity: AppCompatActivity,
        toolbar: Toolbar,
        title: String,
        showBackButton: Boolean = true
    ) {
        // Set the toolbar as the action bar
        activity.setSupportActionBar(toolbar)
        
        // Hide the default title
        activity.supportActionBar?.setDisplayShowTitleEnabled(false)
        
        // Create a custom title TextView
        val customTitle = TextView(activity)
        customTitle.text = title
        customTitle.setTextColor(Color.WHITE)
        customTitle.textSize = 20f
        customTitle.typeface = Typeface.DEFAULT_BOLD
        
        // Center the title
        val params = Toolbar.LayoutParams(
            Toolbar.LayoutParams.WRAP_CONTENT,
            Toolbar.LayoutParams.WRAP_CONTENT
        )
        params.gravity = Gravity.CENTER
        customTitle.layoutParams = params
        
        // Add the title to the toolbar
        toolbar.addView(customTitle)
        
        // Show back button if needed
        if (showBackButton) {
            activity.supportActionBar?.setDisplayHomeAsUpEnabled(true)
            activity.supportActionBar?.setDisplayShowHomeEnabled(true)
        }
    }
} 