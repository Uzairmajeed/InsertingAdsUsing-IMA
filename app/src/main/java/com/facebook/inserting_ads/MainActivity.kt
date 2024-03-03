package com.facebook.inserting_ads

import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.facebook.inserting_ads.databinding.ActivityMainBinding
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.ext.ima.ImaAdsLoader
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.ui.StyledPlayerView

class MainActivity : AppCompatActivity() {
    private lateinit var exoPlayerView: StyledPlayerView
    private lateinit var exoPlayer: ExoPlayer
    private lateinit var binding: ActivityMainBinding
    private lateinit var adsLoader: ImaAdsLoader

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // Hide the status bar
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        exoPlayerView = binding.exoplayer

        // Initialize ExoPlayer
        exoPlayer = ExoPlayer.Builder(this)
            .setMediaSourceFactory(buildMediaSourceFactory())
            .build()

        // Initialize IMA AdsLoader
        adsLoader = ImaAdsLoader.Builder(this).build()

        // Set player on adsLoader before preparing the player
        adsLoader.setPlayer(exoPlayer)

        // Set ExoPlayer to PlayerView
        exoPlayerView.player = exoPlayer

        // Ad tag URI
        val adTagUri = "https://pubads.g.doubleclick.net/gampad/ads?iu=/21775744923/external/single_ad_samples&sz=640x480&cust_params=sample_ct%3Dlinear&ciu_szs=300x250%2C728x90&gdfp_req=1&output=vast&unviewed_position_start=1&env=vp&impl=s&correlator="

        // Content URI
        val contentUri = "https://storage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"

        // Build MediaItem with AdsConfiguration
        val mediaItem = MediaItem.Builder()
            .setUri(contentUri)
            .setAdsConfiguration(MediaItem.AdsConfiguration.Builder(Uri.parse(adTagUri)).build())
            .build()

        // Set media item to ExoPlayer
        exoPlayer.setMediaItem(mediaItem)

        // Prepare and play the player
        exoPlayer.prepare()
        exoPlayer.play()
    }

    // Build MediaSourceFactory with AdsLoaderProvider and AdViewProvider
    private fun buildMediaSourceFactory(): DefaultMediaSourceFactory {
        return DefaultMediaSourceFactory(this)
            .setAdsLoaderProvider { adsLoader }
            .setAdViewProvider(exoPlayerView)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Release resources when the activity is destroyed
        exoPlayer.release()
        adsLoader.release()
    }
}
