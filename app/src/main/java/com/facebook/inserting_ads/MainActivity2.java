package com.facebook.inserting_ads;


import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.MediaController;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.ads.interactivemedia.v3.api.AdDisplayContainer;
import com.google.ads.interactivemedia.v3.api.AdErrorEvent;
import com.google.ads.interactivemedia.v3.api.AdEvent;
import com.google.ads.interactivemedia.v3.api.AdsLoader;
import com.google.ads.interactivemedia.v3.api.AdsManager;
import com.google.ads.interactivemedia.v3.api.AdsManagerLoadedEvent;
import com.google.ads.interactivemedia.v3.api.AdsRenderingSettings;
import com.google.ads.interactivemedia.v3.api.AdsRequest;
import com.google.ads.interactivemedia.v3.api.ImaSdkFactory;
import com.google.ads.interactivemedia.v3.api.ImaSdkSettings;
import com.google.ads.interactivemedia.v3.api.player.VideoProgressUpdate;
import java.util.Arrays;



public class MainActivity2 extends AppCompatActivity {

    private static final String LOGTAG = "IMABasicSample";
    private static final String SAMPLE_VIDEO_URL =
            "https://storage.googleapis.com/gvabox/media/samples/stock.mp4";

    /**
     * IMA sample tag for a single skippable inline video ad. See more IMA sample tags at
     * https://developers.google.com/interactive-media-ads/docs/sdks/html5/client-side/tags
     */

    /**
     * List of ad tag URLs for a playlist of skippable inline video ads.
     */
    private static final String[] SAMPLE_VAST_TAG_URL= {
            "https://pubads.g.doubleclick.net/gampad/ads?iu=/21775744923/external/"
                    + "single_preroll_skippable&sz=640x480&ciu_szs=300x250%2C728x90&gdfp_req=1&output=vast"
                    + "&unviewed_position_start=1&env=vp&impl=s&correlator=",

            "https://pubads.g.doubleclick.net/gampad/ads?iu=/21775744923/external/" +
                    "single_ad_samples&sz=640x480&cust_params=sample_ct%3Dlinear&ciu_szs=300x250%2C728x90&gdfp_req=1&" +
                    "output=vast&unviewed_position_start=1&env=vp&impl=s&correlator=",

            // Add more ad tag URLs as needed
    };
    private int currentAdIndex = 0;
    // Factory class for creating SDK objects.
    private ImaSdkFactory sdkFactory;

    // The AdsLoader instance exposes the requestAds method.
    private AdsLoader adsLoader;

    // AdsManager exposes methods to control ad playback and listen to ad events.
    private AdsManager adsManager;

    // The saved content position, used to resumed content following an ad break.
    private int savedPosition = 0;

    // This sample uses a VideoView for content and ad playback. For production
    // apps, Android's Exoplayer offers a more fully featured player compared to
    // the VideoView.
    private VideoView videoPlayer;
    private MediaController mediaController;
    private View playButton;
    private VideoAdPlayerAdapter videoAdPlayerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        // Create the UI for controlling the video view.
        mediaController = new MediaController(this);
        videoPlayer = findViewById(R.id.videoView);
        mediaController.setAnchorView(videoPlayer);
        videoPlayer.setMediaController(mediaController);

        // Create an ad display container that uses a ViewGroup to listen to taps.
        ViewGroup videoPlayerContainer = findViewById(R.id.videoPlayerContainer);
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        videoAdPlayerAdapter = new VideoAdPlayerAdapter(videoPlayer, audioManager);

        sdkFactory = ImaSdkFactory.getInstance();

        AdDisplayContainer adDisplayContainer =
                ImaSdkFactory.createAdDisplayContainer(videoPlayerContainer, videoAdPlayerAdapter);

        // Create an AdsLoader.
        ImaSdkSettings settings = sdkFactory.createImaSdkSettings();
        adsLoader = sdkFactory.createAdsLoader(this, settings, adDisplayContainer);

        // When the play button is clicked, request ads and hide the button.
        playButton = findViewById(R.id.playButton);
        playButton.setOnClickListener(
                view -> {
                    videoPlayer.setVideoPath(SAMPLE_VIDEO_URL);
                    // Request the first ad in the playlist
                    requestAds(SAMPLE_VAST_TAG_URL[currentAdIndex]);
                    view.setVisibility(View.GONE);
                    // Introduce a delay of 40 seconds before the main video starts
                    Handler delayHandler = new Handler();
                    delayHandler.postDelayed(() -> {
                        watchOnProgress();
                    }, 40000); // 40 seconds delay

                });

        // Add listeners for when ads are loaded and for errors.
        adsLoader.addAdErrorListener(
                new AdErrorEvent.AdErrorListener() {
                    /** An event raised when there is an error loading or playing ads. */
                    @Override
                    public void onAdError(AdErrorEvent adErrorEvent) {
                        Log.i(LOGTAG, "Ad Error: " + adErrorEvent.getError().getMessage());
                        resumeContent();
                    }
                });
        adsLoader.addAdsLoadedListener(
                new AdsLoader.AdsLoadedListener() {
                    @Override
                    public void onAdsManagerLoaded(AdsManagerLoadedEvent adsManagerLoadedEvent) {
                        // Ads were successfully loaded, so get the AdsManager instance. AdsManager has
                        // events for ad playback and errors.
                        adsManager = adsManagerLoadedEvent.getAdsManager();

                        // Attach event and error event listeners.
                        adsManager.addAdErrorListener(
                                new AdErrorEvent.AdErrorListener() {
                                    /** An event raised when there is an error loading or playing ads. */
                                    @Override
                                    public void onAdError(AdErrorEvent adErrorEvent) {
                                        Log.e(LOGTAG, "Ad Error: " + adErrorEvent.getError().getMessage());
                                        String universalAdIds =
                                                Arrays.toString(adsManager.getCurrentAd().getUniversalAdIds());
                                        Log.i(
                                                LOGTAG,
                                                "Discarding the current ad break with universal "
                                                        + "ad Ids: "
                                                        + universalAdIds);
                                        adsManager.discardAdBreak();
                                    }
                                });
                        adsManager.addAdEventListener(new AdEvent.AdEventListener() {
                            @Override
                            public void onAdEvent(AdEvent adEvent) {
                                if (adEvent.getType() != AdEvent.AdEventType.AD_PROGRESS) {
                                    Log.i(LOGTAG, "Event: " + adEvent.getType());
                                }

                                switch (adEvent.getType()) {
                                    case LOADED:
                                        adsManager.start();
                                        break;
                                    case CONTENT_PAUSE_REQUESTED:
                                        pauseContentForAds();
                                        break;
                                    case CONTENT_RESUME_REQUESTED:
                                        resumeContent();
                                        break;
                                    case ALL_ADS_COMPLETED:
                                        adsManager.destroy();
                                        adsManager = null;
                                        break;
                                    case CLICKED:
                                        // Handle the click event.
                                        break;
                                    default:
                                        // Handle other ad events as needed.
                                        break;
                                }
                            }
                        });

                        AdsRenderingSettings adsRenderingSettings =
                                ImaSdkFactory.getInstance().createAdsRenderingSettings();
                        adsManager.init(adsRenderingSettings);
                    }
                });

    }

    // Add the watchOnProgress method
    private void watchOnProgress() {
            // Play the next ad in the playlist
            currentAdIndex = (currentAdIndex + 1) % SAMPLE_VAST_TAG_URL.length;
            requestAds(SAMPLE_VAST_TAG_URL[currentAdIndex]);
    }

    private void pauseContentForAds() {
        Log.i(LOGTAG, "pauseContentForAds");
        savedPosition = videoPlayer.getCurrentPosition();
        videoPlayer.stopPlayback();
        // Hide the buttons and seek bar controlling the video view.
        videoPlayer.setMediaController(null);
    }

    private void resumeContent() {
        Log.i(LOGTAG, "resumeContent");

        // Show the buttons and seek bar controlling the video view.
        videoPlayer.setVideoPath(SAMPLE_VIDEO_URL);
        videoPlayer.setMediaController(mediaController);
        videoPlayer.setOnPreparedListener(
                mediaPlayer -> {
                    if (savedPosition > 0) {
                        mediaPlayer.seekTo(savedPosition);
                    }
                    mediaPlayer.start();
                });
        videoPlayer.setOnCompletionListener(
                mediaPlayer -> videoAdPlayerAdapter.notifyImaOnContentCompleted());
    }

    private void requestAds(String adTagUrl) {
        // Create the ads request.
        AdsRequest request = sdkFactory.createAdsRequest();
        request.setAdTagUrl(adTagUrl);
        request.setContentProgressProvider(
                () -> {
                    if (videoPlayer.getDuration() <= 0) {
                        return VideoProgressUpdate.VIDEO_TIME_NOT_READY;
                    }
                    return new VideoProgressUpdate(
                            videoPlayer.getCurrentPosition(), videoPlayer.getDuration());
                });

        // Request the ad. After the ad is loaded, onAdsManagerLoaded() will be called.
        adsLoader.requestAds(request);
    }

}