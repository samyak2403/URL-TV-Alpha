package com.samyak.urltvalpha

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.onesignal.OneSignal
import com.onesignal.debug.LogLevel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class ApplicationClass : Application() {

    companion object {
        const val CHANNEL_ID = "url_tv_channel"
        const val CHANNEL_NAME = "URL TV Notifications"
        const val CHANNEL_DESCRIPTION = "Notifications for URL TV app"
        
        // NOTE: Replace the below with your own ONESIGNAL_APP_ID
        const val ONESIGNAL_APP_ID = "ba1cab7b-e6a6-4e54-907b-c6f6ab2eb906"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        
        // Initialize OneSignal
        setupOneSignal()
    }

    private fun createNotificationChannel() {
        // Create notification channel only on API 26+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = CHANNEL_DESCRIPTION
            }

            // Register the channel with the system
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun setupOneSignal() {
        // Verbose Logging set to help debug issues, remove before releasing your app.
        OneSignal.Debug.logLevel = LogLevel.VERBOSE

        // OneSignal Initialization
        OneSignal.initWithContext(this, ONESIGNAL_APP_ID)

        // requestPermission will show the native Android notification permission prompt.
        // NOTE: It's recommended to use a OneSignal In-App Message to prompt instead.
        CoroutineScope(Dispatchers.IO).launch {
            OneSignal.Notifications.requestPermission(false)
        }
    }
}