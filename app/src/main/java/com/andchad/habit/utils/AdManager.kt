package com.andchad.habit.utils

import android.app.Activity
import android.content.Context
import android.util.Log
import android.widget.Toast
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "AdManager"

@Singleton
class AdManager @Inject constructor(private val context: Context) {
    private var interstitialAd: InterstitialAd? = null
    private val isAdLoading = AtomicBoolean(false)
    private var isInitialized = false

    // Using Google's test ad unit ID
    private val adUnitId = "ca-app-pub-3940256099942544/1033173712" // Test interstitial ad unit ID

    fun initialize() {
        if (isInitialized) return

        try {
            Log.d(TAG, "Starting MobileAds initialization")
            MobileAds.initialize(context) { initStatus ->
                Log.d(TAG, "MobileAds initialization complete with status: $initStatus")
                isInitialized = true

                // Immediately load an ad after initialization
                loadInterstitialAd()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing AdMob: ${e.message}", e)
        }
    }

    private fun loadInterstitialAd() {
        // Don't try to load if already loading or loaded
        if (interstitialAd != null) {
            Log.d(TAG, "Ad already loaded, skipping loading")
            return
        }

        if (isAdLoading.get()) {
            Log.d(TAG, "Ad is currently loading, skipping duplicate load")
            return
        }

        isAdLoading.set(true)
        Log.d(TAG, "Starting to load interstitial ad with adUnitId: $adUnitId")

        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(
            context,
            adUnitId,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.e(TAG, "Ad failed to load! Error code: ${adError.code}, " +
                            "message: ${adError.message}, domain: ${adError.domain}, " +
                            "cause: ${adError.cause}")

                    interstitialAd = null
                    isAdLoading.set(false)

                    // Retry loading after a delay
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        Log.d(TAG, "Retrying ad load after failure")
                        loadInterstitialAd()
                    }, 30000) // retry after 30 seconds
                }

                override fun onAdLoaded(ad: InterstitialAd) {
                    Log.d(TAG, "Ad loaded successfully! 🎉")
                    interstitialAd = ad
                    isAdLoading.set(false)

                    interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            Log.d(TAG, "Ad was dismissed")
                            interstitialAd = null

                            // Load the next ad immediately
                            loadInterstitialAd()
                        }

                        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                            Log.e(TAG, "Ad failed to show: ${adError.message}")
                            interstitialAd = null
                            loadInterstitialAd()
                        }

                        override fun onAdShowedFullScreenContent() {
                            Log.d(TAG, "Ad showed fullscreen content successfully")
                        }
                    }
                }
            }
        )
    }

    fun showInterstitialAd(activity: Activity) {
        try {
            if (interstitialAd != null) {
                Log.d(TAG, "Showing interstitial ad")
                interstitialAd?.show(activity)
            } else {
                Log.d(TAG, "Ad not ready yet, attempting to load")

                // Debug info
                if (!isInitialized) {
                    Log.e(TAG, "MobileAds not initialized yet!")
                    Toast.makeText(context, "Ad SDK not initialized", Toast.LENGTH_SHORT).show()
                    initialize()
                } else if (isAdLoading.get()) {
                    Log.d(TAG, "Ad is still loading, please wait")
                    Toast.makeText(context, "Ad is still loading...", Toast.LENGTH_SHORT).show()
                } else {
                    Log.d(TAG, "No ad available and not currently loading, starting new load")
                    Toast.makeText(context, "Loading new ad...", Toast.LENGTH_SHORT).show()
                    loadInterstitialAd()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error showing ad: ${e.message}", e)
            Toast.makeText(context, "Error with ad: ${e.message}", Toast.LENGTH_SHORT).show()

            // Reset and try to reload
            interstitialAd = null
            isAdLoading.set(false)
            loadInterstitialAd()
        }
    }

    // Call this in MainActivity's onResume to ensure we always have an ad ready
    fun ensureAdLoaded() {
        if (interstitialAd == null && !isAdLoading.get()) {
            Log.d(TAG, "Preloading ad in onResume")
            loadInterstitialAd()
        }
    }
}