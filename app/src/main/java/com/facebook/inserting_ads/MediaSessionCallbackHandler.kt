package com.facebook.inserting_ads

// MediaSessionCallbackHandler.kt

import android.os.Handler
import android.os.Looper
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer

// Inside MediaSessionCallbackHandler
class MediaSessionCallbackHandler(private val exoPlayer: ExoPlayer,private  val mediaSession: MediaSessionCompat) : MediaSessionCompat.Callback() {

    private val handler = Handler(Looper.getMainLooper())
    private val updateInterval = 1000L // Update interval in milliseconds

    // Declare updateRunnable as a property of the class
    private val updateRunnable = object : Runnable {
        override fun run() {
            updateNotification()
            handler.postDelayed(this, updateInterval)
        }
    }

    init {
        // Schedule periodic updates
        handler.postDelayed(updateRunnable, updateInterval)
    }

    override fun onPlay() {
        when {
            exoPlayer.playbackState == ExoPlayer.STATE_IDLE -> {
                // Handle play action when the player is not prepared
                exoPlayer.prepare()
                exoPlayer.play()
            }
            exoPlayer.playbackState == ExoPlayer.STATE_ENDED -> {
                // Handle play action when playback has ended
                exoPlayer.seekTo(0)
                exoPlayer.play()
            }
            else -> {
                // Handle play action when the player is prepared or buffering
                exoPlayer.play()
            }
        }
        updateNotification()
    }

    override fun onSkipToNext() {
        // Handle next action
        exoPlayer.next()
        updateNotification()
    }

    override fun onSkipToPrevious() {
        // Handle previous action
        exoPlayer.previous()
        updateNotification()
    }

    override fun onSeekTo(pos: Long) {
        // Handle seek action
        exoPlayer.seekTo(pos)
        updateNotification()
    }


    // Add this method to update the notification
    private fun updateNotification() {
        val playbackState = exoPlayer.playbackState
        val isPlaying = playbackState != ExoPlayer.STATE_IDLE &&
                playbackState != ExoPlayer.STATE_ENDED &&
                exoPlayer.playWhenReady

        val actions = when {
            isPlaying -> (PlaybackStateCompat.ACTION_PLAY_PAUSE or
                    PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                    PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)
            else -> PlaybackStateCompat.ACTION_PLAY_PAUSE
        }

        val currentPosition = exoPlayer.currentPosition
        val duration = exoPlayer.duration

        mediaSession.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setActions(actions)
                .setState(
                    if (isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED,
                    currentPosition,
                    1.0f  // Set a dummy playback speed; you can adjust this accordingly
                )
                .build()
        )

        // Update the MediaMetadata for seeking progress in the notification
        val mediaMetadata = MediaMetadataCompat.Builder()
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration)
            .build()

        mediaSession.setMetadata(mediaMetadata)
    }







    override fun onPause() {
        if (exoPlayer.playbackState == ExoPlayer.STATE_BUFFERING || exoPlayer.playbackState == ExoPlayer.STATE_READY) {
            // Handle pause action
            exoPlayer.pause()
            updateNotification()
        }
    }


}

