package com.facebook.inserting_ads

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.media.session.MediaButtonReceiver
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
    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var mediaSessionCallbackHandler: MediaSessionCallbackHandler

    // Add this constant at the top of your file
    private companion object {
        private const val REQUEST_NOTIFICATION_PERMISSION = 123
    }
    private fun createAndShowNotification() {
        // Set up notification
        val notification = buildNotification()

        // Notify only if the permission is granted
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(this).notify(1, notification)
        } else {
            // Request the notification permission
           requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQUEST_NOTIFICATION_PERMISSION)
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // Hide the status bar
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)

        exoPlayerView = binding.exoplayer


        // Initialize MediaSession
        mediaSession = MediaSessionCompat(this, "YourMediaSessionTag")

        // Set the media session's metadata
        mediaSession.setMetadata(
            MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, "Video Title")
                .build()
        )

        // Initialize notification channel (required for Android O and above)
        createNotificationChannel()

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

        // Initialize MediaSessionCallbackHandler
        mediaSessionCallbackHandler = MediaSessionCallbackHandler(exoPlayer,mediaSession)
        mediaSession.setCallback(mediaSessionCallbackHandler)

        // Set the initial playback state
        val playbackState = PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY
                        or PlaybackStateCompat.ACTION_PAUSE
                        or PlaybackStateCompat.ACTION_PLAY_PAUSE
            )
            .setState(
                PlaybackStateCompat.STATE_PLAYING,  // Set the initial state here
                0,  // Set the current playback position if applicable
                1.0f  // Set the playback speed
            )
            .build()
        mediaSession.setPlaybackState(playbackState)


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

        // Create and show the notification
        createAndShowNotification()
    }

    // Handle the result of the permission request
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_NOTIFICATION_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed with creating and showing the notification
                createAndShowNotification()
            } else {
                // Permission denied, handle accordingly (e.g., show a message to the user)
            }
        }
    }



    // Build MediaSourceFactory with AdsLoaderProvider and AdViewProvider
    private fun buildMediaSourceFactory(): DefaultMediaSourceFactory {
        return DefaultMediaSourceFactory(this)
            .setAdsLoaderProvider { adsLoader }
            .setAdViewProvider(exoPlayerView)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "YourChannelId",
                "YourChannelName",
                NotificationManager.IMPORTANCE_LOW
            )
            channel.description = "YourChannelDescription"
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }



    private fun buildNotification(): Notification {
        val mediaButtonReceiverPendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, "YourChannelId")
            .setSmallIcon(R.drawable.baseline_notifications_none_24)
            .setContentTitle("Video is playing")
            .setContentText("Your video title")
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0)  // Index of the pause/play action
            )
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(mediaButtonReceiverPendingIntent)
            .addAction(
                R.drawable.baseline_pause_circle_outline_24,
                "Pause",
                MediaButtonReceiver.buildMediaButtonPendingIntent(
                    this,
                    PlaybackStateCompat.ACTION_PLAY_PAUSE
                )
            )
            .setAutoCancel(false)
            .setOngoing(true)
            .build()

        return notification
    }

    


    override fun onDestroy() {
        super.onDestroy()
        // Release resources when the activity is destroyed
        exoPlayer.release()
        adsLoader.release()
        mediaSession.release()
    }
}
