package com.samyak.urlplayerbeta.screen

import android.annotation.SuppressLint
import android.app.AppOpsManager
import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.samyak.urltvalpha.R

import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import java.util.Locale
import android.media.AudioManager
import android.content.res.Resources
import android.view.GestureDetector
import androidx.core.view.GestureDetectorCompat
import com.github.vkay94.dtpv.youtube.YouTubeOverlay
import kotlin.math.abs
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import android.media.audiofx.LoudnessEnhancer
import androidx.mediarouter.app.MediaRouteButton
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaLoadRequestData
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.framework.*
import com.google.android.gms.cast.framework.media.RemoteMediaClient
import com.google.android.gms.common.images.WebImage
import android.view.Gravity
import android.util.TypedValue
import android.widget.FrameLayout
import com.google.android.exoplayer2.C


import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.CaptionStyleCompat
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.util.Util
import com.google.android.gms.cast.CastStatusCodes
import com.samyak.urlplayerbeta.AdManage.Helper
import com.samyak.urltvalpha.databinding.ActivityPlayerBinding
import com.samyak.urltvalpha.databinding.BoosterBinding
import com.samyak.urltvalpha.databinding.MoreFeaturesBinding

class PlayerActivity : AppCompatActivity(), GestureDetector.OnGestureListener {
    private lateinit var binding: ActivityPlayerBinding
    private lateinit var player: ExoPlayer
    private lateinit var playerView: PlayerView
    private lateinit var progressBar: ProgressBar
    private lateinit var errorTextView: TextView
    private lateinit var linearLayoutControlUp: LinearLayout
    private lateinit var linearLayoutControlBottom: LinearLayout

    // Custom controller views
    private lateinit var backButton: ImageButton
    private lateinit var videoTitle: TextView
    private lateinit var moreFeaturesButton: ImageButton
    private lateinit var playPauseButton: ImageButton
    private lateinit var repeatButton: ImageButton
    private lateinit var prevButton: ImageButton
    private lateinit var nextButton: ImageButton
    private lateinit var fullScreenButton: ImageButton

    private var playbackPosition = 0L
    private var isPlayerReady = false
    private var isFullscreen: Boolean = false
    private var url: String? = null
    private var userAgent: String? = null
    private lateinit var trackSelector: DefaultTrackSelector
    private var currentQuality = "Auto"
    
    private data class VideoQuality(
        val height: Int,
        val width: Int,
        val bitrate: Int,
        val label: String,
        val description: String
    )

    private val availableQualities = listOf(
        VideoQuality(1080, 1920, 8_000_000, "1080p", "Full HD - Best quality"),
        VideoQuality(720, 1280, 5_000_000, "720p", "HD - High quality"),
        VideoQuality(480, 854, 2_500_000, "480p", "SD - Good quality"),
        VideoQuality(360, 640, 1_500_000, "360p", "SD - Normal quality"),
        VideoQuality(240, 426, 800_000, "240p", "Low - Basic quality"),
        VideoQuality(144, 256, 500_000, "144p", "Very Low - Minimal quality")
    )

    private var isManualQualityControl = false

    private lateinit var gestureDetectorCompat: GestureDetectorCompat
    private var minSwipeY: Float = 0f
    private var brightness: Int = 0
    private var volume: Int = 0
    private var audioManager: AudioManager? = null

    private var isLocked = false

    // Update supported formats with comprehensive streaming formats
    private val supportedFormats = mapOf(
        // Common video formats
        "mp4" to "video/mp4",
        "mkv" to "video/x-matroska",
        "webm" to "video/webm",
        "3gp" to "video/3gpp",
        "avi" to "video/x-msvideo",
        "mov" to "video/quicktime",
        "wmv" to "video/x-ms-wmv",
        "flv" to "video/x-flv",
        
        // Streaming formats
        "m3u8" to "application/vnd.apple.mpegurl",  // Updated MIME type
        "m3u" to "application/vnd.apple.mpegurl",   // Updated MIME type
        "ts" to "video/mp2t",
        "mpd" to "application/dash+xml",
        "ism" to "application/vnd.ms-sstr+xml",
        
        // Transport stream formats
        "mts" to "video/mp2t",
        "m2ts" to "video/mp2t",
        
        // Legacy formats
        "mp2" to "video/mpeg",
        "mpg" to "video/mpeg",
        "mpeg" to "video/mpeg",
        
        // Additional streaming formats
        "hls" to "application/vnd.apple.mpegurl",  // Updated MIME type
        "dash" to "application/dash+xml",
        "smooth" to "application/vnd.ms-sstr+xml",
        
        // Playlist formats
        "pls" to "audio/x-scpls",
        "asx" to "video/x-ms-asf",
        "xspf" to "application/xspf+xml"
    )

    private var isPlaying = false

    // Add these properties
    private var position: Int = -1
    private var playerList: ArrayList<String> = ArrayList()

    // Add these properties if not already present
    private lateinit var loudnessEnhancer: LoudnessEnhancer
    private var boostLevel: Int = 0
    private var isBoostEnabled: Boolean = false

    private val maxBoostLevel = 15 // Maximum boost level (1500%)

    // Add these properties for casting
    private lateinit var castContext: CastContext
    private lateinit var sessionManager: SessionManager
    private var castSession: CastSession? = null
    private lateinit var mediaRouteButton: MediaRouteButton

    // Add after other properties
    private var screenHeight: Int = 0
    private var screenWidth: Int = 0

    // Add at the top with other properties
    private lateinit var adHelper: Helper

    private val castSessionManagerListener = object : SessionManagerListener<CastSession> {
        override fun onSessionStarting(session: CastSession) {}
        
        override fun onSessionStarted(session: CastSession, sessionId: String) {
            castSession = session
            // Save current playback position
            val position = player.currentPosition
            // Start casting
            loadRemoteMedia(position)
            // Pause local playback
            player.pause()
        }
        
        override fun onSessionStartFailed(session: CastSession, error: Int) {
            Toast.makeText(this@PlayerActivity, "Failed to start casting", Toast.LENGTH_SHORT).show()
        }
        
        override fun onSessionEnding(session: CastSession) {
            // Return to local playback
            val position = session.remoteMediaClient?.approximateStreamPosition ?: 0
            player.seekTo(position)
            player.playWhenReady = true
        }
        
        override fun onSessionEnded(session: CastSession, error: Int) {
            castSession = null
        }
        
        override fun onSessionResuming(session: CastSession, sessionId: String) {}
        override fun onSessionResumed(session: CastSession, wasSuspended: Boolean) {
            castSession = session
        }
        override fun onSessionResumeFailed(session: CastSession, error: Int) {}
        override fun onSessionSuspended(session: CastSession, reason: Int) {}
    }

    companion object {
        private const val INCREMENT_MILLIS = 5000L
        var pipStatus: Int = 0
    }

    // Add these properties at the top of the class
    private var playbackState = PlaybackState.IDLE
    private var wasPlayingBeforePause = false

    private enum class PlaybackState {
        IDLE, PLAYING, PAUSED, BUFFERING, ENDED
    }

    // Add this enum at the top of the class
    private enum class ScreenMode {
        FIT, FILL, ZOOM
    }

    // Add this property to track current screen mode
    private var currentScreenMode = ScreenMode.FIT

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize AdHelper
        adHelper = Helper(this, binding)

        // Force landscape orientation
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

        // Hide system bars and make fullscreen
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, binding.root).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = 
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        url = intent.getStringExtra("URL")
        userAgent = intent.getStringExtra("USER_AGENT")

        if (url == null) {
            finish()
            return
        }

        // Initialize views first
        initializeViews()

        // Initialize gesture and audio controls
        gestureDetectorCompat = GestureDetectorCompat(this, this)
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        volume = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0

        setupPlayer()
        setupGestureControls()

        // Restore saved boost level
        boostLevel = getSharedPreferences("audio_settings", Context.MODE_PRIVATE)
            .getInt("boost_level", 0)
        isBoostEnabled = getSharedPreferences("audio_settings", Context.MODE_PRIVATE)
            .getBoolean("boost_enabled", false)

        // Initialize cast context
        try {
            castContext = CastContext.getSharedInstance(this)
            sessionManager = castContext.sessionManager
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun initializeViews() {
        // Initialize main views from binding
        playerView = binding.playerView
        progressBar = binding.progressBar
        errorTextView = binding.errorTextView
        linearLayoutControlUp = binding.linearLayoutControlUp
        linearLayoutControlBottom = binding.linearLayoutControlBottom

        // Setup player first
        setupPlayer()

        // Then initialize custom controller views and actions
        setupCustomControllerViews()
        setupCustomControllerActions()
    }

    private fun setupCustomControllerViews() {
        try {
            // Find all controller views from playerView
            backButton = playerView.findViewById(R.id.backBtn)
            videoTitle = playerView.findViewById(R.id.videoTitle)
            moreFeaturesButton = playerView.findViewById(R.id.moreFeaturesBtn)
            playPauseButton = playerView.findViewById(R.id.playPauseBtn)
            repeatButton = playerView.findViewById(R.id.repeatBtn)
            prevButton = playerView.findViewById(R.id.prevBtn)
            nextButton = playerView.findViewById(R.id.nextBtn)
            fullScreenButton = playerView.findViewById(R.id.fullScreenBtn)

            // Set initial title
            val channelName = intent.getStringExtra("CHANNEL_NAME") ?: getString(R.string.video_name)
            videoTitle.text = channelName
            videoTitle.isSelected = true

            // Add cast button setup
            mediaRouteButton = playerView.findViewById(R.id.mediaRouteButton)
            CastButtonFactory.setUpMediaRouteButton(this, mediaRouteButton)

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error setting up controller views", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupCustomControllerActions() {
        // Back button
        backButton.setOnClickListener {
            onBackPressed()
        }

        // Play/Pause button
        playPauseButton.setOnClickListener {
            when (playbackState) {
                PlaybackState.PLAYING -> pauseVideo()
                PlaybackState.PAUSED, PlaybackState.ENDED -> playVideo()
                PlaybackState.BUFFERING -> {
                    wasPlayingBeforePause = !wasPlayingBeforePause
                    updatePlayPauseButton(wasPlayingBeforePause)
                }
                else -> {
                    // Try to start playback for other states
                    playVideo()
                }
            }
        }

        // Previous/Next buttons (10 seconds skip)
        prevButton.setOnClickListener {
            player.seekTo(maxOf(0, player.currentPosition - 10000))
        }

        nextButton.setOnClickListener {
            player.seekTo(minOf(player.duration, player.currentPosition + 10000))
        }

        // Repeat button
        repeatButton.setOnClickListener {
            when (player.repeatMode) {
                Player.REPEAT_MODE_OFF -> {
                    player.repeatMode = Player.REPEAT_MODE_ONE
                    repeatButton.setImageResource(R.drawable.repeat_one_icon)
                }
                Player.REPEAT_MODE_ONE -> {
                    player.repeatMode = Player.REPEAT_MODE_ALL
                    repeatButton.setImageResource(R.drawable.repeat_all_icon)
                }
                else -> {
                    player.repeatMode = Player.REPEAT_MODE_OFF
                    repeatButton.setImageResource(R.drawable.repeat_off_icon)
                }
            }
        }

        // Fullscreen button
        fullScreenButton.setOnClickListener {
            if (!isFullscreen) {
                isFullscreen = true
                playInFullscreen(enable = true)
            } else {
                // Cycle through modes when already fullscreen
                playInFullscreen(enable = true)
            }
        }

        // More Features button
        moreFeaturesButton.setOnClickListener {
            pauseVideo()
            showMoreFeaturesDialog()
        }

        // Lock button
        binding.lockButton.setOnClickListener {
            isLocked = !isLocked
            lockScreen(isLocked)
            binding.lockButton.setImageResource(
                if (isLocked) R.drawable.close_lock_icon 
                else R.drawable.lock_open_icon
            )
        }
    }

    private fun showMoreFeaturesDialog() {
        val customDialog = LayoutInflater.from(this)
            .inflate(R.layout.more_features, binding.root, false)
        val bindingMF = MoreFeaturesBinding.bind(customDialog)
        
        val dialog = MaterialAlertDialogBuilder(this)
            .setView(customDialog)
            .setOnCancelListener { playVideo() }
            .setBackground(ColorDrawable(0x803700B3.toInt()))
            .create()
        
        dialog.show()

        // Handle audio booster click
        bindingMF.audioBooster.setOnClickListener {
            dialog.dismiss()
            showAudioBoosterDialog()
        }

        // Add subtitle button click listener
        bindingMF.subtitlesBtn.setOnClickListener {
            dialog.dismiss()
            playVideo()
            val subtitles = ArrayList<String>()
            val subtitlesList = ArrayList<String>()
            var hasSubtitles = false
            
            // Get available subtitle tracks
            try {
                for (group in player.currentTracksInfo.trackGroupInfos) {
                    if (group.trackType == C.TRACK_TYPE_TEXT) {
                        hasSubtitles = true
                        val groupInfo = group.trackGroup
                        for (i in 0 until groupInfo.length) {
                            val format = groupInfo.getFormat(i)
                            val language = format.language ?: "unknown"
                            val label = format.label ?: Locale(language).displayLanguage
                            
                            subtitles.add(language)
                            subtitlesList.add(
                                "${subtitlesList.size + 1}. $label" + 
                                if (language != "unknown") " (${Locale(language).displayLanguage})" else ""
                            )
                        }
                    }
                }

                if (!hasSubtitles) {
                    Toast.makeText(this, "No subtitles available for this video", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val tempTracks = subtitlesList.toArray(arrayOfNulls<CharSequence>(subtitlesList.size))
                
                MaterialAlertDialogBuilder(this, R.style.SubtitleDialogStyle)
                    .setTitle("Select Subtitles")
                    .setOnCancelListener { playVideo() }
                    .setPositiveButton("Off Subtitles") { self, _ ->
                        trackSelector.setParameters(
                            trackSelector.buildUponParameters()
                                .setRendererDisabled(C.TRACK_TYPE_TEXT, true)
                        )
                        self.dismiss()
                        playVideo()
                        Snackbar.make(playerView, "Subtitles disabled", 3000).show()
                    }
                    .setItems(tempTracks) { _, position ->
                        try {
                            trackSelector.setParameters(
                                trackSelector.buildUponParameters()
                                    .setRendererDisabled(C.TRACK_TYPE_TEXT, false)
                                    .setPreferredTextLanguage(subtitles[position])
                            )
                            Snackbar.make(
                                playerView,
                                "Selected: ${subtitlesList[position]}", 
                                3000
                            ).show()
                        } catch (e: Exception) {
                            Toast.makeText(this, "Error selecting subtitles", Toast.LENGTH_SHORT).show()
                        }
                        playVideo()
                    }
                    .setBackground(ColorDrawable(0x803700B3.toInt()))
                    .create()
                    .apply {
                        show()
                        getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(Color.WHITE)
                    }
            } catch (e: Exception) {
                Toast.makeText(this, "Error loading subtitles", Toast.LENGTH_SHORT).show()
            }
        }

        // Video Quality button in more features dialog
        bindingMF.videoQuality.setOnClickListener {
            dialog.dismiss()
            showQualityDialog()
        }

        // Add PiP button click handler
        bindingMF.pipModeBtn.setOnClickListener {
            val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val status = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_PICTURE_IN_PICTURE,
                    android.os.Process.myUid(),
                    packageName
                ) == AppOpsManager.MODE_ALLOWED
            } else {
                false
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (status) {
                    // Enter PiP mode
                    enterPictureInPictureMode(PictureInPictureParams.Builder().build())
                    dialog.dismiss()
                    binding.playerView.hideController()
                    playVideo()
                    pipStatus = 0
                } else {
                    // Open PiP settings if not enabled
                    val intent = Intent(
                        "android.settings.PICTURE_IN_PICTURE_SETTINGS",
                        Uri.parse("package:$packageName")
                    )
                    startActivity(intent)
                }
            } else {
                Toast.makeText(this, "Feature Not Supported!!", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                playVideo()
            }
        }
    }

    // Update the playVideo() method
    private fun playVideo() {
        if (!isPlayerReady) return
        
        try {
            when (playbackState) {
                PlaybackState.PAUSED, PlaybackState.ENDED -> {
                    // Show ad only if video was paused for more than 5 seconds
                    val currentTime = System.currentTimeMillis()
                    val lastPauseTime = getSharedPreferences("player_prefs", Context.MODE_PRIVATE)
                        .getLong("last_pause_time", 0)
                    val lastAdTime = getSharedPreferences("ad_prefs", Context.MODE_PRIVATE)
                        .getLong("last_ad_time", 0)
                    
                    // Show ad if enough time passed since last pause AND last ad
                    if (currentTime - lastPauseTime > 5000 && currentTime - lastAdTime > 30000) {
                        adHelper.showCounterInterstitialAd(
                            threshold = 3,
                            onAdShown = {
                                // Save ad time
                                getSharedPreferences("ad_prefs", Context.MODE_PRIVATE)
                                    .edit()
                                    .putLong("last_ad_time", currentTime)
                                    .apply()
                                
                                // Resume playback after ad is shown
                                player.play()
                                playbackState = PlaybackState.PLAYING
                                isPlaying = true
                                updatePlayPauseButton(true)
                            },
                            onAdNotShown = {
                                // Play immediately if ad isn't shown
                                player.play()
                                playbackState = PlaybackState.PLAYING
                                isPlaying = true
                                updatePlayPauseButton(true)
                            }
                        )
                    } else {
                        // Resume immediately if conditions not met
                        player.play()
                        playbackState = PlaybackState.PLAYING
                        isPlaying = true
                        updatePlayPauseButton(true)
                    }
                }
                PlaybackState.BUFFERING -> {
                    wasPlayingBeforePause = true
                }
                else -> {
                    // Do nothing for other states
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error playing video: ${e.message}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    // Update the pauseVideo() method
    private fun pauseVideo() {
        try {
            when (playbackState) {
                PlaybackState.PLAYING, PlaybackState.BUFFERING -> {
                    // Save pause time
                    val currentTime = System.currentTimeMillis()
                    getSharedPreferences("player_prefs", Context.MODE_PRIVATE)
                        .edit()
                        .putLong("last_pause_time", currentTime)
                        .apply()

                    // Get last ad time
                    val lastAdTime = getSharedPreferences("ad_prefs", Context.MODE_PRIVATE)
                        .getLong("last_ad_time", 0)

                    // Show ad if enough time passed since last ad
                    if (currentTime - lastAdTime > 30000) { // 30 seconds between ads
                        adHelper.showCounterInterstitialAd(
                            threshold = 3,
                            onAdShown = {
                                // Save ad time
                                getSharedPreferences("ad_prefs", Context.MODE_PRIVATE)
                                    .edit()
                                    .putLong("last_ad_time", currentTime)
                                    .apply()
                                
                                // Pause after ad
                                player.pause()
                                playbackState = PlaybackState.PAUSED
                                isPlaying = false
                                updatePlayPauseButton(false)
                                wasPlayingBeforePause = true
                            },
                            onAdNotShown = {
                                // Just pause if ad not shown
                                player.pause()
                                playbackState = PlaybackState.PAUSED
                                isPlaying = false
                                updatePlayPauseButton(false)
                                wasPlayingBeforePause = true
                            }
                        )
                    } else {
                        // Pause immediately if too soon for another ad
                        player.pause()
                        playbackState = PlaybackState.PAUSED
                        isPlaying = false
                        updatePlayPauseButton(false)
                        wasPlayingBeforePause = true
                    }
                }
                else -> {
                    // Do nothing for other states
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error pausing video: ${e.message}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    // Update the playInFullscreen function
    private fun playInFullscreen(enable: Boolean) {
        if (enable) {
            when (currentScreenMode) {
                ScreenMode.FIT -> {
                    // Default fit mode
                    binding.playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    player.videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT
                    fullScreenButton.setImageResource(R.drawable.fullscreen_exit_icon)
                    currentScreenMode = ScreenMode.FILL
                }
                ScreenMode.FILL -> {
                    // Stretch to fill
                    binding.playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
                    player.videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT
                    fullScreenButton.setImageResource(R.drawable.fullscreen_exit_icon)
                    currentScreenMode = ScreenMode.ZOOM
                }
                ScreenMode.ZOOM -> {
                    // Zoom and crop
                    binding.playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    player.videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
                    fullScreenButton.setImageResource(R.drawable.fullscreen_exit_icon)
                    currentScreenMode = ScreenMode.FIT
                }
            }
            
            // Show toast with current mode
//            val modeText = when (currentScreenMode) {
//                ScreenMode.FIT -> "Fit to Screen"
//                ScreenMode.FILL -> "Fill Screen"
//                ScreenMode.ZOOM -> "Zoom"
//            }
//            Toast.makeText(this, modeText, Toast.LENGTH_SHORT).show()
            
        } else {
            // Reset to default fit mode
            binding.playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
            player.videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT
            fullScreenButton.setImageResource(R.drawable.fullscreen_icon)
            currentScreenMode = ScreenMode.FIT
        }
    }

    private fun showSpeedDialog() {
        val dialogView = layoutInflater.inflate(R.layout.speed_dialog, null)
        val dialog = AlertDialog.Builder(this, R.style.AlertDialogCustom)
            .setView(dialogView)
            .create()

        var currentSpeed = player.playbackParameters.speed
        val speedText = dialogView.findViewById<TextView>(R.id.speedText)
        speedText.text = String.format("%.1fx", currentSpeed)

        dialogView.findViewById<ImageButton>(R.id.minusBtn).setOnClickListener {
            if (currentSpeed > 0.25f) {
                currentSpeed -= 0.25f
                speedText.text = String.format("%.1fx", currentSpeed)
                player.setPlaybackSpeed(currentSpeed)
            }
        }

        dialogView.findViewById<ImageButton>(R.id.plusBtn).setOnClickListener {
            if (currentSpeed < 3.0f) {
                currentSpeed += 0.25f
                speedText.text = String.format("%.1fx", currentSpeed)
                player.setPlaybackSpeed(currentSpeed)
            }
        }

        dialog.show()
    }

    private fun showQualityDialog() {
        val qualities = getAvailableQualities()
        val qualityItems = buildQualityItems(qualities)
        val currentIndex = (qualityItems.indexOfFirst { it.contains(currentQuality) }).coerceAtLeast(0)

        MaterialAlertDialogBuilder(this, R.style.QualityDialogStyle)
            .setTitle(getString(R.string.select_quality))
            .setSingleChoiceItems(qualityItems.toTypedArray(), currentIndex) { dialog, which ->
                val selectedQuality = if (which == 0) "Auto" else qualities[which - 1].label
                isManualQualityControl = selectedQuality != "Auto"
                applyQuality(selectedQuality, qualities)
                dialog.dismiss()

                Toast.makeText(
                    this,
                    if (selectedQuality == "Auto") {
                        getString(R.string.auto_quality_enabled)
                    } else {
                        getString(R.string.quality_changed, selectedQuality)
                    },
                    Toast.LENGTH_SHORT
                ).show()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .create()
            .show()
    }

    private fun buildQualityItems(qualities: List<VideoQuality>): List<String> {
        val items = mutableListOf("Auto (Recommended)")
        
        qualities.forEach { quality ->
            val currentFormat = player.videoFormat
            val isCurrent = when {
                currentFormat == null -> false
                !isManualQualityControl -> currentFormat.height == quality.height
                else -> currentQuality == quality.label
            }
            
            val qualityText = buildString {
                append(quality.label)
                append(" - ")
                append(quality.description)
                if (isCurrent) append(" ✓")
            }
            items.add(qualityText)
        }
        
        return items
    }

    private fun getAvailableQualities(): List<VideoQuality> {
        val tracks = mutableListOf<VideoQuality>()
        
        try {
            player.currentTrackGroups.let { trackGroups ->
                for (groupIndex in 0 until trackGroups.length) {
                    val group = trackGroups[groupIndex]
                    
                    for (trackIndex in 0 until group.length) {
                        val format = group.getFormat(trackIndex)
                        
                        if (format.height > 0 && format.width > 0) {
                            availableQualities.find { 
                                it.height == format.height 
                            }?.let { tracks.add(it) }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return tracks.distinct().sortedByDescending { it.height }
    }

    private fun applyQuality(quality: String, availableTracks: List<VideoQuality>) {
        val parameters = trackSelector.buildUponParameters()

        when (quality) {
            "Auto" -> {
                parameters.clearVideoSizeConstraints()
                    .setForceHighestSupportedBitrate(false)
                    .setMaxVideoBitrate(Int.MAX_VALUE)
                    .setAllowVideoMixedMimeTypeAdaptiveness(true)
                    .setAllowVideoNonSeamlessAdaptiveness(true)
            }
            else -> {
                availableTracks.find { it.label == quality }?.let { track ->
                    parameters.setMaxVideoSize(track.width, track.height)
                        .setMinVideoSize(track.width/2, track.height/2)
                        .setMaxVideoBitrate(track.bitrate)
                        .setMinVideoBitrate(track.bitrate/2)
                        .setForceHighestSupportedBitrate(true)
                        .setAllowVideoMixedMimeTypeAdaptiveness(false)
                }
            }
        }

        try {
            val position = player.currentPosition
            val wasPlaying = player.isPlaying

            trackSelector.setParameters(parameters)
            currentQuality = quality

            // Save preferences
            getSharedPreferences("player_settings", Context.MODE_PRIVATE).edit().apply {
                putString("preferred_quality", quality)
                putBoolean("manual_quality_control", isManualQualityControl)
                apply()
            }

            // Restore playback state
            player.seekTo(position)
            player.playWhenReady = wasPlaying

        } catch (e: Exception) {
            Toast.makeText(
                this,
                getString(R.string.quality_change_failed, e.message),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun initializeQuality() {
        val prefs = getSharedPreferences("player_settings", Context.MODE_PRIVATE)
        val savedQuality = prefs.getString("preferred_quality", "Auto") ?: "Auto"
        isManualQualityControl = prefs.getBoolean("manual_quality_control", false)

        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    getAvailableQualities().let { tracks ->
                        if (tracks.isNotEmpty()) {
                            // If manual control is off, use Auto
                            val qualityToApply = if (isManualQualityControl) savedQuality else "Auto"
                            applyQuality(qualityToApply, tracks)
                            player.removeListener(this)
                        }
                    }
                }
            }
        })
    }

    private fun getCurrentQualityInfo(): String {
        val currentTrack = player.videoFormat
        return when {
            currentTrack == null -> "Unknown"
            !isManualQualityControl -> "Auto (${currentTrack.height}p)"
            else -> currentQuality
        }
    }

    private fun setupPlayer() {
        try {
            if (::player.isInitialized) {
                player.release()
            }

            trackSelector = DefaultTrackSelector(this).apply {
                setParameters(buildUponParameters().setMaxVideoSizeSd())
            }

            player = ExoPlayer.Builder(this)
                .setTrackSelector(trackSelector)
                .build()

            playerView.player = player

            // Create data source factory
            val dataSourceFactory = DefaultHttpDataSource.Factory()
                .setUserAgent(userAgent ?: Util.getUserAgent(this, "URLPlayerBeta"))
                .setAllowCrossProtocolRedirects(true)

            // Create media source based on URL type
            val mediaItem = MediaItem.fromUri(url ?: return)
            val mediaSource = when {
                // HLS streams
                url?.endsWith(".m3u8", ignoreCase = true) == true ||
                url?.endsWith(".m3u", ignoreCase = true) == true ||
                url?.endsWith(".hls", ignoreCase = true) == true -> {
                    HlsMediaSource.Factory(dataSourceFactory)
                        .createMediaSource(mediaItem)
                }
                
                // Progressive streams
                else -> {
                    val extension = url?.substringAfterLast('.', "")?.lowercase() ?: ""
                    val mimeType = supportedFormats[extension]
                    
                    val finalMediaItem = if (mimeType != null) {
                        MediaItem.Builder()
                            .setUri(Uri.parse(url))
                            .setMimeType(mimeType)
                            .build()
                    } else {
                        mediaItem
                    }

                    ProgressiveMediaSource.Factory(dataSourceFactory)
                        .createMediaSource(finalMediaItem)
                }
            }

            player.setMediaSource(mediaSource)
            player.seekTo(playbackPosition)
            player.playWhenReady = true
            player.prepare()

            // Add player listener
            player.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    when (state) {
                        Player.STATE_BUFFERING -> {
                            playbackState = PlaybackState.BUFFERING
                            progressBar.visibility = View.VISIBLE
                            updatePlayPauseButton(wasPlayingBeforePause)
                        }
                        Player.STATE_READY -> {
                            progressBar.visibility = View.GONE
                            isPlayerReady = true
                            if (wasPlayingBeforePause) {
                                playbackState = PlaybackState.PLAYING
                                player.play()
                            } else {
                                playbackState = PlaybackState.PAUSED
                            }
                            updatePlayPauseButton(wasPlayingBeforePause)
                        }
                        Player.STATE_ENDED -> {
                            playbackState = PlaybackState.ENDED
                            updatePlayPauseButton(false)
                            handlePlaybackEnded()
                        }
                        Player.STATE_IDLE -> {
                            playbackState = PlaybackState.IDLE
                            updatePlayPauseButton(false)
                        }
                    }
                }

                override fun onIsPlayingChanged(playing: Boolean) {
                    isPlaying = playing
                    if (playing) {
                        playbackState = PlaybackState.PLAYING
                    } else if (playbackState != PlaybackState.BUFFERING && 
                               playbackState != PlaybackState.ENDED) {
                        playbackState = PlaybackState.PAUSED
                    }
                    updatePlayPauseButton(playing)
                }

                override fun onPlayerError(error: PlaybackException) {
                    // Show error message
                    errorTextView.visibility = View.VISIBLE
                    errorTextView.text = "Error: ${error.message}"
                    
                    // Log the error
                    error.printStackTrace()
                }
            })

            // Initialize audio booster
            setupAudioBooster()

            // Apply subtitle styling after player is created
            applySubtitleStyle()
            
            // Add configuration change listener for subtitle resizing
            player.addListener(object : Player.Listener {
                override fun onSurfaceSizeChanged(width: Int, height: Int) {
                    // Recalculate subtitle size when surface size changes
                    applySubtitleStyle()
                }
            })

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error initializing player: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun updatePlayPauseButton(isPlaying: Boolean) {
        playPauseButton.setImageResource(
            if (isPlaying) R.drawable.pause_icon
            else R.drawable.play_icon
        )
    }

    private fun handlePlaybackEnded() {
        when (player.repeatMode) {
            Player.REPEAT_MODE_ONE -> {
                // Just replay current video
                player.seekTo(0)
                playVideo()
            }
            Player.REPEAT_MODE_ALL -> {
                // For single video, treat same as REPEAT_MODE_ONE
                player.seekTo(0)
                playVideo()
            }
            else -> {
                // Just stop at the end
                pauseVideo()
                // Optionally show replay button or end screen
                showPlaybackEndedUI()
            }
        }
    }

    private fun showPlaybackEndedUI() {
        try {
            // Show replay button with fallback to play icon
            playPauseButton.setImageResource(
                try {
                    R.drawable.replay_icon
                } catch (e: Exception) {
                    R.drawable.play_icon // Fallback to play icon
                }
            )
            
            playPauseButton.setOnClickListener {
                player.seekTo(0)
                playVideo()
                // Restore normal play/pause listener
                setupCustomControllerActions()
            }
        } catch (e: Exception) {
            // If anything fails, just show play icon
            playPauseButton.setImageResource(R.drawable.play_icon)
        }
    }

    private fun updateQualityInfo() {
        videoTitle.text = getCurrentQualityInfo()
    }

    private fun lockScreen(lock: Boolean) {
        linearLayoutControlUp.visibility = if (lock) View.INVISIBLE else View.VISIBLE
        linearLayoutControlBottom.visibility = if (lock) View.INVISIBLE else View.VISIBLE
        playerView.useController = !lock
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        
        // Always force landscape mode
        if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }

        // Keep fullscreen in both orientations
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, binding.root).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = 
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        // Reset screen dimensions and recalculate subtitle size
        screenWidth = 0
        screenHeight = 0
        applySubtitleStyle()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        playbackPosition = player.currentPosition
        outState.putLong("playbackPosition", playbackPosition)
        outState.putString("URL", url)
        outState.putString("USER_AGENT", userAgent)
    }

    override fun onStart() {
        super.onStart()
        if (Util.SDK_INT > 23) {
            player.playWhenReady = true
        }
    }

    override fun onResume() {
        super.onResume()
        if (Util.SDK_INT <= 23 || !isPlayerReady) {
            player.playWhenReady = true
        }
        if (audioManager == null) {
            audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        }
        audioManager?.requestAudioFocus(
            null,
            AudioManager.STREAM_MUSIC,
            AudioManager.AUDIOFOCUS_GAIN
        )
        if (brightness != 0) setScreenBrightness(brightness)
        if (isPlaying) {
            playVideo()
        }
        if (::sessionManager.isInitialized) {
            sessionManager.addSessionManagerListener(castSessionManagerListener, CastSession::class.java)
        }
    }

    override fun onPause() {
        super.onPause()
        if (Util.SDK_INT <= 23) {
            playbackPosition = player.currentPosition
            player.playWhenReady = false
        }
        if (isPlaying) {
            pauseVideo()
        }
        if (::sessionManager.isInitialized) {
            sessionManager.removeSessionManagerListener(castSessionManagerListener, CastSession::class.java)
        }
    }

    override fun onStop() {
        super.onStop()
        if (Util.SDK_INT > 23) {
            playbackPosition = player.currentPosition
            player.playWhenReady = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        player.release()
        audioManager?.abandonAudioFocus(null)
        try {
            if (::loudnessEnhancer.isInitialized) {
                loudnessEnhancer.release()
            }
            // Clean up ads
            if (::adHelper.isInitialized) {
                adHelper.destroy()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
        // Just finish the activity when back is pressed
        finish()
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode)
        
        if (pipStatus != 0) {
            finish()
            val intent = Intent(this, PlayerActivity::class.java)
            when (pipStatus) {
                1 -> intent.putExtra("class", "FolderActivity")
                2 -> intent.putExtra("class", "SearchedVideos")
                3 -> intent.putExtra("class", "AllVideos")
            }
            startActivity(intent)
        }
        
        if (!isInPictureInPictureMode) {
            pauseVideo() // Pause video when exiting PiP mode
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!isInPictureInPictureMode) {
                // Auto enter PiP when user leaves activity
                try {
                    enterPictureInPictureMode(PictureInPictureParams.Builder().build())
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupGestureControls() {
        binding.playerView.player = player
        
        // Setup YouTube style overlay
        binding.ytOverlay.performListener(object : YouTubeOverlay.PerformListener {
            override fun onAnimationEnd() {
                binding.ytOverlay.visibility = View.GONE
            }

            override fun onAnimationStart() {
                binding.ytOverlay.visibility = View.VISIBLE
            }
        })
        binding.ytOverlay.player(player)

        // Handle touch events
        binding.playerView.setOnTouchListener { _, motionEvent ->
            if (!isLocked) {
                gestureDetectorCompat.onTouchEvent(motionEvent)
                
                if (motionEvent.action == MotionEvent.ACTION_UP) {
                    binding.brightnessIcon.visibility = View.GONE
                    binding.volumeIcon.visibility = View.GONE
                    
                    // For immersive mode
                    WindowCompat.setDecorFitsSystemWindows(window, false)
                    WindowInsetsControllerCompat(window, binding.root).let { controller ->
                        controller.hide(WindowInsetsCompat.Type.systemBars())
                        controller.systemBarsBehavior = 
                            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    }
                }
            }
            false
        }
    }

    override fun onScroll(
        e1: MotionEvent?,
        event: MotionEvent,
        distanceX: Float,
        distanceY: Float
    ): Boolean {
        if (isLocked) return false
        
        minSwipeY += distanceY

        val sWidth = Resources.getSystem().displayMetrics.widthPixels
        val sHeight = Resources.getSystem().displayMetrics.heightPixels

        val border = 100 * Resources.getSystem().displayMetrics.density.toInt()
        if (event.x < border || event.y < border || 
            event.x > sWidth - border || event.y > sHeight - border)
            return false

        if (abs(distanceX) < abs(distanceY) && abs(minSwipeY) > 50) {
            if (event.x < sWidth / 2) {
                // Brightness control
                binding.brightnessIcon.visibility = View.VISIBLE
                binding.volumeIcon.visibility = View.GONE
                val increase = distanceY > 0
                val newValue = if (increase) brightness + 1 else brightness - 1
                if (newValue in 0..30) brightness = newValue
                binding.brightnessIcon.text = brightness.toString()
                setScreenBrightness(brightness)
            } else {
                // Volume control
                binding.brightnessIcon.visibility = View.GONE
                binding.volumeIcon.visibility = View.VISIBLE
                val maxVolume = audioManager!!.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                val increase = distanceY > 0
                val newValue = if (increase) volume + 1 else volume - 1
                if (newValue in 0..maxVolume) volume = newValue
                binding.volumeIcon.text = volume.toString()
                audioManager!!.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0)
            }
            minSwipeY = 0f
        }
        return true
    }

    private fun setScreenBrightness(value: Int) {
        val d = 1.0f / 30
        val lp = window.attributes
        lp.screenBrightness = d * value
        window.attributes = lp
    }

    // Add other required GestureDetector.OnGestureListener methods
    override fun onDown(e: MotionEvent) = false
    override fun onShowPress(e: MotionEvent) = Unit
    override fun onSingleTapUp(e: MotionEvent) = false
    override fun onLongPress(e: MotionEvent) = Unit
    override fun onFling(e1: MotionEvent?, e2: MotionEvent, vX: Float, vY: Float) = false

    private fun setupAudioBooster() {
        try {
            // Create new LoudnessEnhancer with player's audio session
            loudnessEnhancer = LoudnessEnhancer(player.audioSessionId)
            
            // Restore saved settings
            val prefs = getSharedPreferences("audio_settings", Context.MODE_PRIVATE)
            boostLevel = prefs.getInt("boost_level", 0)
            isBoostEnabled = prefs.getBoolean("boost_enabled", false)
            
            // Apply saved settings
            loudnessEnhancer.enabled = isBoostEnabled
            if (isBoostEnabled && boostLevel > 0) {
                loudnessEnhancer.setTargetGain(boostLevel * 100) // Convert to millibels
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error initializing audio booster", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    private fun showAudioBoosterDialog() {
        val customDialogB = LayoutInflater.from(this)
            .inflate(R.layout.booster, binding.root, false)
        val bindingB = BoosterBinding.bind(customDialogB)
        
        // Set initial values
        bindingB.verticalBar.apply {
            progress = boostLevel
            // The max value should be set in XML via app:vsb_max_value="15"
        }
        
        val dialogB = MaterialAlertDialogBuilder(this)
            .setView(customDialogB)
            .setTitle("Audio Boost")
            .setOnCancelListener { playVideo() }
            .setPositiveButton("Apply") { self, _ ->
                try {
                    // Update boost level
                    boostLevel = bindingB.verticalBar.progress
                    isBoostEnabled = boostLevel > 0
                    
                    // Apply settings
                    loudnessEnhancer.enabled = isBoostEnabled
                    loudnessEnhancer.setTargetGain(boostLevel * 100)
                    
                    // Save settings
                    getSharedPreferences("audio_settings", Context.MODE_PRIVATE)
                        .edit()
                        .putInt("boost_level", boostLevel)
                        .putBoolean("boost_enabled", isBoostEnabled)
                        .apply()

                    // Show feedback
                    val message = if (isBoostEnabled) 
                        "Audio boost set to ${boostLevel * 10}%" 
                    else 
                        "Audio boost disabled"
                    Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
                    
                } catch (e: Exception) {
                    Toast.makeText(this, "Error setting audio boost", Toast.LENGTH_SHORT).show()
                    e.printStackTrace()
                }
                playVideo()
                self.dismiss()
            }
            .setNegativeButton("Reset") { _, _ ->
                try {
                    // Reset all settings
                    boostLevel = 0
                    isBoostEnabled = false
                    bindingB.verticalBar.progress = 0
                    loudnessEnhancer.enabled = false
                    loudnessEnhancer.setTargetGain(0)
                    
                    // Save reset state
                    getSharedPreferences("audio_settings", Context.MODE_PRIVATE)
                        .edit()
                        .putInt("boost_level", 0)
                        .putBoolean("boost_enabled", false)
                        .apply()
                        
                    Snackbar.make(binding.root, "Audio boost reset", Snackbar.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this, "Error resetting audio boost", Toast.LENGTH_SHORT).show()
                    e.printStackTrace()
                }
            }
            .create()

        // Update progress text function
        fun updateProgressText(progress: Int) {
            val percentage = progress * 10
            bindingB.progressText.text = if (progress > 0) {
                "Audio Boost\n\n+${percentage}%"
            } else {
                "Audio Boost\n\nOff"
            }
        }
        
        updateProgressText(boostLevel)

        // Update progress text while sliding
        bindingB.verticalBar.setOnProgressChangeListener { progress ->
            updateProgressText(progress)
        }

        dialogB.show()
    }

    private fun loadRemoteMedia(position: Long = 0) {
        val castSession = castSession ?: return
        val remoteMediaClient = castSession.remoteMediaClient ?: return
        
        try {
            // Create media metadata
            val videoMetadata = MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE)
            val title = intent.getStringExtra("CHANNEL_NAME") ?: getString(R.string.video_name)
            videoMetadata.putString(MediaMetadata.KEY_TITLE, title)
            
            // Get correct MIME type and stream type
            val mimeType = getMimeType(url)
            val streamType = when {
                // HLS streams
                url?.contains(".m3u8", ignoreCase = true) == true || 
                mimeType == "application/vnd.apple.mpegurl" -> 
                    MediaInfo.STREAM_TYPE_LIVE
                
                // DASH streams
                url?.contains("dash", ignoreCase = true) == true ||
                mimeType == "application/dash+xml" ->
                    MediaInfo.STREAM_TYPE_BUFFERED
                
                // Progressive streams (MP4, WebM etc)
                mimeType.startsWith("video/") -> 
                    MediaInfo.STREAM_TYPE_BUFFERED
                
                // Default to buffered
                else -> MediaInfo.STREAM_TYPE_BUFFERED
            }

            // Create media info with proper content type and stream type
            val mediaInfo = MediaInfo.Builder(url ?: return)
                .setStreamType(streamType)
                .setContentType(mimeType)
                .setMetadata(videoMetadata)
                .apply {
                    // Only set duration for buffered streams
                    if (streamType == MediaInfo.STREAM_TYPE_BUFFERED) {
                        setStreamDuration(player.duration)
                    }
                }
                .build()
            
            // Load media with options
            val loadRequestData = MediaLoadRequestData.Builder()
                .setMediaInfo(mediaInfo)
                .setAutoplay(true)
                .apply {
                    // Only set position for buffered streams
                    if (streamType == MediaInfo.STREAM_TYPE_BUFFERED) {
                        setCurrentTime(position)
                    }
                }
                .build()
            
            // Add result listener with enhanced error handling
            remoteMediaClient.load(loadRequestData)
                .addStatusListener { result ->
                    when {
                        result.isSuccess -> {
                            Toast.makeText(this, "Casting started", Toast.LENGTH_SHORT).show()
                            getSharedPreferences("cast_prefs", Context.MODE_PRIVATE)
                                .edit()
                                .putBoolean("is_casting", true)
                                .apply()
                        }
                        result.isInterrupted -> {
                            handleCastError("Casting interrupted")
                        }
                        else -> {
                            val errorMsg = when (result.statusCode) {
                                CastStatusCodes.FAILED -> "Format not supported"
                                CastStatusCodes.INVALID_REQUEST -> "Invalid stream URL"
                                CastStatusCodes.NETWORK_ERROR -> "Network error"
                                CastStatusCodes.APPLICATION_NOT_RUNNING -> "Cast app not running"
                                else -> "Cast error: ${result.statusCode}"
                            }
                            handleCastError(errorMsg)
                        }
                    }
                }
        } catch (e: Exception) {
            e.printStackTrace()
            handleCastError("Cast error: ${e.message}")
        }
    }

    private fun handleCastError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        // Fallback to local playback
        castSession?.remoteMediaClient?.stop()
        player.playWhenReady = true
        getSharedPreferences("cast_prefs", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("is_casting", false)
            .apply()
    }

    private fun getMimeType(url: String?): String {
        if (url == null) return "video/mp4"
        
        return try {
            // First check for HLS streams
            if (url.contains(".m3u8", ignoreCase = true) || 
                url.contains("playlist", ignoreCase = true)) {
                return "application/vnd.apple.mpegurl"
            }
            
            // Then check file extension
            val extension = url.substringAfterLast('.', "").lowercase()
            supportedFormats[extension] ?: when {
                // Fallback checks for streaming URLs
                url.contains("dash", ignoreCase = true) -> "application/dash+xml"
                url.contains("hls", ignoreCase = true) -> "application/vnd.apple.mpegurl"
                url.contains("smooth", ignoreCase = true) -> "application/vnd.ms-sstr+xml"
                // Default to MP4 for unknown types
                else -> "video/mp4"
            }
        } catch (e: Exception) {
            "video/mp4"  // Default fallback
        }
    }

    // Add this function to calculate optimal subtitle size
    private fun calculateSubtitleSize(): Float {
        // Get screen dimensions if not already set
        if (screenHeight == 0 || screenWidth == 0) {
            val metrics = resources.displayMetrics
            screenHeight = metrics.heightPixels
            screenWidth = metrics.widthPixels
        }

        // Base size calculation on screen width
        // For 1080p width, default size would be 20sp
        val baseSize = 20f
        val baseWidth = 1080f
        
        // Calculate scaled size based on screen width
        val scaledSize = (screenWidth / baseWidth) * baseSize
        
        // Clamp the size between min and max values
        return scaledSize.coerceIn(16f, 26f)
    }

    // Add this function to apply subtitle styling
    private fun applySubtitleStyle() {
        try {
            val subtitleSize = calculateSubtitleSize()
            
            // Create subtitle style
            val style = CaptionStyleCompat(
                Color.WHITE,                      // Text color
                Color.TRANSPARENT,                // Background color
                Color.TRANSPARENT,                // Window color
                CaptionStyleCompat.EDGE_TYPE_OUTLINE, // Edge type
                Color.BLACK,                      // Edge color
                null                             // Default typeface
            )

            // Apply style to player view
            playerView.subtitleView?.setStyle(style)
            
            // Set text size
            playerView.subtitleView?.setFixedTextSize(TypedValue.COMPLEX_UNIT_SP, subtitleSize)
            
            // Center align subtitles and position them slightly above bottom
            playerView.subtitleView?.let { subtitleView ->
                subtitleView.setApplyEmbeddedStyles(true)
                subtitleView.setApplyEmbeddedFontSizes(false)
                
                // Position subtitles slightly above bottom (90% from top)
                val params = subtitleView.layoutParams as FrameLayout.LayoutParams
                params.gravity = Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM
                params.bottomMargin = (screenHeight * 0.1).toInt() // 10% from bottom
                subtitleView.layoutParams = params
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}