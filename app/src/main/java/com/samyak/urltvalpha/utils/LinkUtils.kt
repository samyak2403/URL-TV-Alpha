package com.samyak.urltvalpha.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast

/**
 * Utility class to manage all external links used in the application.
 * This centralizes all URLs in one place for easier management and updates.
 */
object LinkUtils {
    // Social Media Links
    const val FACEBOOK_URL = "https://www.facebook.com/yourpage"
    const val TWITTER_URL = "https://twitter.com/yourhandle"
    const val INSTAGRAM_URL = "https://www.instagram.com/yourprofile"
    const val YOUTUBE_URL = "https://www.youtube.com/yourchannel"
    const val TELEGRAM_URL = "https://t.me/yourchannelname"
    const val LINKEDIN_URL = "https://www.linkedin.com/in/yourprofile"
    const val MESSENGER_URL = "https://m.me/yourpage" // Messenger URL
    
    // Social Media App Package Names
    private const val FACEBOOK_PACKAGE = "com.facebook.katana"
    private const val TWITTER_PACKAGE = "com.twitter.android"
    private const val INSTAGRAM_PACKAGE = "com.instagram.android"
    private const val YOUTUBE_PACKAGE = "com.google.android.youtube"
    private const val TELEGRAM_PACKAGE = "org.telegram.messenger"
    private const val LINKEDIN_PACKAGE = "com.linkedin.android"
    private const val MESSENGER_PACKAGE = "com.facebook.orca" // Messenger package name
    
    // App Related Links
    const val PRIVACY_POLICY_URL = "https://yourprivacypolicyurl.com"
    const val PLAY_STORE_BASE_URL = "https://play.google.com/store/apps/details?id="
    const val MARKET_BASE_URL = "market://details?id="
    
    // Contact Information
    const val CONTACT_EMAIL = "your-email@example.com"
    
    /**
     * Returns the Play Store URL for this app
     * @param packageName The package name of the app
     * @return The complete Play Store URL
     */
    fun getPlayStoreUrl(packageName: String): String {
        return PLAY_STORE_BASE_URL + packageName
    }
    
    /**
     * Returns the Market URL for this app (for direct Play Store app opening)
     * @param packageName The package name of the app
     * @return The complete Market URL
     */
    fun getMarketUrl(packageName: String): String {
        return MARKET_BASE_URL + packageName
    }
    
    /**
     * Returns the mailto URL for contact
     * @return The complete mailto URL
     */
    fun getMailtoUrl(): String {
        return "mailto:$CONTACT_EMAIL"
    }
    
    /**
     * Opens a social media link, trying to use the app if installed, otherwise falls back to browser
     * @param context The context to use for starting the activity
     * @param url The URL to open
     * @param packageName The package name of the app to try opening first
     */
    fun openSocialMediaLink(context: Context, url: String, packageName: String) {
        try {
            // Check if the app is installed
            context.packageManager.getPackageInfo(packageName, 0)
            
            // If we get here, the app is installed, try to open it
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            intent.setPackage(packageName)
            context.startActivity(intent)
        } catch (e: PackageManager.NameNotFoundException) {
            // App not installed, open in browser
            openInBrowser(context, url)
        } catch (e: ActivityNotFoundException) {
            // Could not open the app, fall back to browser
            openInBrowser(context, url)
        } catch (e: Exception) {
            // Any other error, show a toast
            Toast.makeText(context, "Could not open link. Please try again later.", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Opens a URL in the browser
     * @param context The context to use for starting the activity
     * @param url The URL to open
     */
    fun openInBrowser(context: Context, url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open link. Please try again later.", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Opens Facebook with fallback to browser
     * @param context The context to use for starting the activity
     */
    fun openFacebook(context: Context) {
        openSocialMediaLink(context, FACEBOOK_URL, FACEBOOK_PACKAGE)
    }
    
    /**
     * Opens Twitter with fallback to browser
     * @param context The context to use for starting the activity
     */
    fun openTwitter(context: Context) {
        openSocialMediaLink(context, TWITTER_URL, TWITTER_PACKAGE)
    }
    
    /**
     * Opens Instagram with fallback to browser
     * @param context The context to use for starting the activity
     */
    fun openInstagram(context: Context) {
        openSocialMediaLink(context, INSTAGRAM_URL, INSTAGRAM_PACKAGE)
    }
    
    /**
     * Opens YouTube with fallback to browser
     * @param context The context to use for starting the activity
     */
    fun openYouTube(context: Context) {
        openSocialMediaLink(context, YOUTUBE_URL, YOUTUBE_PACKAGE)
    }
    
    /**
     * Opens Telegram with fallback to browser
     * @param context The context to use for starting the activity
     */
    fun openTelegram(context: Context) {
        openSocialMediaLink(context, TELEGRAM_URL, TELEGRAM_PACKAGE)
    }
    
    /**
     * Opens LinkedIn with fallback to browser
     * @param context The context to use for starting the activity
     */
    fun openLinkedIn(context: Context) {
        openSocialMediaLink(context, LINKEDIN_URL, LINKEDIN_PACKAGE)
    }
    
    /**
     * Opens Messenger with fallback to browser
     * @param context The context to use for starting the activity
     */
    fun openMessenger(context: Context) {
        openSocialMediaLink(context, MESSENGER_URL, MESSENGER_PACKAGE)
    }
} 