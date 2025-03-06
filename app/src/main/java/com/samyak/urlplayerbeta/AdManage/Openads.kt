package com.samyak.urlplayerbeta.AdManage

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import java.util.Date

class Openads(private val myApplication: Application) : LifecycleObserver,
    Application.ActivityLifecycleCallbacks {

    private var appOpenAd: AppOpenAd? = null
    private var currentActivity: Activity? = null
    private var loadTime: Long = 0
    private var isShowingAd = false
    private var isAdLoadInProgress = false
    private var maxRetryAttempts = 3
    private var currentRetryAttempt = 0

    private val TAG = "AppOpenAdManager"
    private val AD_UNIT_ID = "ca-app-pub-6211293117600297/6848932650" // Replace with your actual Ad Unit ID
    private val TIMEOUT_DURATION: Long = 4 * 3600000 // 4 hours in milliseconds

    init {
        myApplication.registerActivityLifecycleCallbacks(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    /** Shows the ad if one isn't already showing. */
    fun showAdIfAvailable() {
        if (!isShowingAd && isAdAvailable()) {
            val fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    appOpenAd = null
                    isShowingAd = false
                    Log.d(TAG, "Ad dismissed fullscreen content")
                    fetchAd()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    isShowingAd = false
                    Log.e(TAG, "Failed to show fullscreen content: ${adError.message}")
                    fetchAd()
                }

                override fun onAdShowedFullScreenContent() {
                    isShowingAd = true
                    Log.d(TAG, "Ad showed fullscreen content")
                }

                override fun onAdImpression() {
                    Log.d(TAG, "Ad recorded an impression")
                }

                override fun onAdClicked() {
                    Log.d(TAG, "Ad recorded a click")
                }
            }

            appOpenAd?.fullScreenContentCallback = fullScreenContentCallback
            currentActivity?.let { appOpenAd?.show(it) }
        } else {
            fetchAd()
        }
    }

    /** Request an ad with retry logic */
    fun fetchAd() {
        if (isAdAvailable() || isAdLoadInProgress) {
            return
        }

        isAdLoadInProgress = true
        val request = AdRequest.Builder().build()
        AppOpenAd.load(
            myApplication,
            AD_UNIT_ID,
            request,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenAd = ad
                    loadTime = Date().time
                    isAdLoadInProgress = false
                    currentRetryAttempt = 0
                    Log.d(TAG, "App Open Ad loaded successfully")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    isAdLoadInProgress = false
                    Log.e(TAG, "App Open Ad failed to load: ${loadAdError.message}")
                    
                    // Implement retry logic
                    if (currentRetryAttempt < maxRetryAttempts) {
                        currentRetryAttempt++
                        Log.d(TAG, "Retrying ad load attempt $currentRetryAttempt")
                        fetchAd()
                    }
                }
            }
        )
    }

    /** Check if ad exists and can be shown. */
    private fun isAdAvailable(): Boolean {
        return appOpenAd != null && wasLoadTimeLessThanNHoursAgo()
    }

    /** Checks if ad was loaded more than n hours ago. */
    private fun wasLoadTimeLessThanNHoursAgo(): Boolean {
        val dateDifference: Long = Date().time - loadTime
        return dateDifference < TIMEOUT_DURATION
    }

    /** ActivityLifecycleCallback methods */
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

    override fun onActivityStarted(activity: Activity) {
        currentActivity = activity
    }

    override fun onActivityResumed(activity: Activity) {
        currentActivity = activity
    }

    override fun onActivityPaused(activity: Activity) {}

    override fun onActivityStopped(activity: Activity) {}

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {
        currentActivity = null
    }

    /** LifecycleObserver method */
    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onStart() {
        showAdIfAvailable()
    }
}