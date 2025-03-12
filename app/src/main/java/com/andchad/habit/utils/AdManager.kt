package com.andchad.habit.utils

import android.app.Activity
import android.content.Context
import android.widget.Toast
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

// This shared preference key will store the timestamp of the last ad shown
private const val PREFS_NAME = "ad_prefs"
private const val LAST_AD_SHOWN_KEY = "last_ad_shown"

// Ad should be shown 3 times a day = every 8 hours
private const val AD_INTERVAL_HOURS = 8L

@Singleton
class AdManager @Inject constructor(private val context: Context) {
    private var interstitialAd: InterstitialAd? = null
    private val isAdLoading = AtomicBoolean(false)

    // Ad Unit ID - Using test ID
    private val adUnitId = "ca-app-pub-2939318428995466~9181390448"

    fun initialize() {
        try {
            MobileAds.initialize(context) { initStatus ->
                loadInterstitialAd()
            }

            scheduleAdDisplayJob(context)
        } catch (e: Exception) {
            // Error handling without logging
        }
    }

    private fun loadInterstitialAd() {
        if (interstitialAd != null || isAdLoading.get()) {
            return
        }

        isAdLoading.set(true)
        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(
            context,
            adUnitId,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    interstitialAd = null
                    isAdLoading.set(false)
                }

                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isAdLoading.set(false)

                    // Set the full screen content callback
                    interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            interstitialAd = null
                            loadInterstitialAd() // Reload ad for next time
                        }

                        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                            interstitialAd = null
                        }
                    }
                }
            }
        )
    }

    fun showInterstitialAd(activity: Activity) {
        try {
            if (shouldShowAd()) {
                // Show ad if available
                if (interstitialAd != null) {
                    interstitialAd?.show(activity)
                    updateLastAdShownTime()
                } else {
                    // If ad is not loaded, try to load it for next time
                    loadInterstitialAd()
                }
            }
        } catch (e: Exception) {
            // Error handling without logging
        }
    }

    // Ensure an ad is loaded when app resumes
    fun ensureAdLoaded() {
        if (interstitialAd == null && !isAdLoading.get()) {
            loadInterstitialAd()
        }
    }

    private fun shouldShowAd(): Boolean {
        //TODO chose to show ads or not
        return false;
//        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
//        val lastAdShown = prefs.getLong(LAST_AD_SHOWN_KEY, 0)
//        val currentTime = System.currentTimeMillis()
//
//        // Check if AD_INTERVAL_HOURS have passed since the last ad
//        return currentTime - lastAdShown >= TimeUnit.HOURS.toMillis(AD_INTERVAL_HOURS)
    }

    private fun updateLastAdShownTime() {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong(LAST_AD_SHOWN_KEY, System.currentTimeMillis()).apply()
    }

    // Schedule periodic work to check and potentially show ads
    private fun scheduleAdDisplayJob(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        val adWorkRequest = PeriodicWorkRequestBuilder<AdDisplayWorker>(
            AD_INTERVAL_HOURS, TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .build()

        androidx.work.WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "ad_display_work",
            ExistingPeriodicWorkPolicy.KEEP,
            adWorkRequest
        )
    }
}

class AdDisplayWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    override fun doWork(): Result {
        // This just notifies the app that it's time to consider showing an ad
        // The actual ad display will happen in the app when the user interacts with it
        return Result.success()
    }
}