package me.nidhinradh.kidu_player.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.PictureInPictureParams;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Rational;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector;
import com.google.android.exoplayer2.source.ConcatenatingMediaSource;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.google.android.gms.vision.face.LargestFaceFocusingProcessor;

import java.io.IOException;
import java.util.ArrayList;

import me.nidhinradh.kidu_player.R;

public class PlayerActivity extends AppCompatActivity {

    private static final String KEY_WINDOW = "window";
    private static final String KEY_POSITION = "position";
    private static final String KEY_AUTO_PLAY = "auto_play";
    private static final String KEY_FACE_DETECTION = "face_detection";
    private static final int PERMISSIONS_REQUEST_CAMERA = 100;
    private static boolean IS_FACE_DETECTION_MODE = false;
    private static boolean HAVE_CAMERA_PERMISSION = false;
    private static boolean PLAYBACK_STOPPED = false;
    private static boolean VOLUME_REDUCED = false;
    private static float PREV_VOLUME;
    @SuppressLint("InlinedApi")
    private static int uiOptions = View.SYSTEM_UI_FLAG_IMMERSIVE
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_FULLSCREEN;
    private static int width, height;
    private static int curBrightnessValue;
    private static int maxVolume;
    private Context context;
    private DefaultTrackSelector trackSelector;
    private SimpleExoPlayer player;
    private PlayerView playerView;
    private MediaSource mediaSource;
    private boolean startAutoPlay;
    private int startWindow;
    private long startPosition;
    private GestureDetector gestureDetector;
    View.OnTouchListener onTouchListener = new View.OnTouchListener() {
        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            return gestureDetector.onTouchEvent(event);
        }
    };
    private CameraSource cameraSource;
    private ImageButton faceButton;
    private MediaSessionCompat mediaSession;
    private MediaSessionConnector mediaSessionConnector;
    private AudioManager.OnAudioFocusChangeListener afChangeListener;
    private AudioManager audioManager;
    private LinearLayout infoLayout;
    private ImageView infoImage;
    private TextView infoText;
    private ProgressBar progressBar;
    private HideInfo hideInfo;


    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View decorView = getWindow().getDecorView();
        decorView.setOnSystemUiVisibilityChangeListener
                (new View.OnSystemUiVisibilityChangeListener() {
                    @Override
                    public void onSystemUiVisibilityChange(final int visibility) {
                        if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                            hideSystemControls();
                        }
                    }
                });
        decorView.setSystemUiVisibility(uiOptions);
        setContentView(R.layout.activity_player);

        infoLayout = findViewById(R.id.info_layout);
        infoImage = findViewById(R.id.info_icon);
        infoText = findViewById(R.id.info_text);

        hideInfo = new HideInfo();

        progressBar = findViewById(R.id.progress_bar);

        context = getApplicationContext();
        playerView = findViewById(R.id.player_view);

        ImageButton aspectRatioButton = playerView.findViewById(R.id.exo_aspect);

        aspectRatioButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (player != null) {
                    if (playerView.getResizeMode() == AspectRatioFrameLayout.RESIZE_MODE_FIT) {
                        playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FILL);
                    } else if (playerView.getResizeMode() == AspectRatioFrameLayout.RESIZE_MODE_FILL) {
                        playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_ZOOM);
                    } else if (playerView.getResizeMode() == AspectRatioFrameLayout.RESIZE_MODE_ZOOM) {
                        playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT);
                    }
                }
            }
        });

        gestureDetector = new GestureDetector(this, new SwipeGestureListener());
        playerView.setOnTouchListener(onTouchListener);

        if (ContextCompat.checkSelfPermission(PlayerActivity.this,
                Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            HAVE_CAMERA_PERMISSION = true;
        }


        faceButton = playerView.findViewById(R.id.exo_face);
        faceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (cameraSource == null) {
                    prepareCameraSource();
                }
                if (HAVE_CAMERA_PERMISSION) {
                    manageFaceTracking();
                } else {
                    requestCameraPermission();
                }
            }
        });

        if (savedInstanceState != null) {
            startAutoPlay = savedInstanceState.getBoolean(KEY_AUTO_PLAY);
            startWindow = savedInstanceState.getInt(KEY_WINDOW);
            startPosition = savedInstanceState.getLong(KEY_POSITION);
            IS_FACE_DETECTION_MODE = savedInstanceState.getBoolean(KEY_FACE_DETECTION);
            if (IS_FACE_DETECTION_MODE)
                faceButton.callOnClick();
        } else {
            clearStartPosition();
        }

        initMediaSession();
        afChangeListener =
                new AudioManager.OnAudioFocusChangeListener() {
                    public void onAudioFocusChange(int focusChange) {
                        if (focusChange == AudioManager.AUDIOFOCUS_LOSS || focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                            if (player != null && player.getPlayWhenReady()) {
                                PLAYBACK_STOPPED = true;
                                player.setPlayWhenReady(false);
                            }
                        } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
                            float volume = player.getVolume();
                            if (player != null && volume != 0 && player.getPlayWhenReady()) {
                                PREV_VOLUME = volume;
                                VOLUME_REDUCED = true;
                                player.setVolume(volume / 10);
                            }
                        } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
                            if (player != null && PLAYBACK_STOPPED) {
                                player.setPlayWhenReady(true);
                                PLAYBACK_STOPPED = false;
                            }
                            if (player != null && VOLUME_REDUCED) {
                                player.setVolume(PREV_VOLUME);
                                VOLUME_REDUCED = false;
                            }
                        }
                    }
                };

        calculateWidthandHeight();

        maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

        try {
            curBrightnessValue = android.provider.Settings.System.getInt(getContentResolver(), android.provider.Settings.System.SCREEN_BRIGHTNESS);
        } catch (Settings.SettingNotFoundException e) {
            Log.e(getResources().getString(R.string.app_name), getResources().getString(R.string.settings_not_found_exception));
        }

        SharedPreferences sharedPreferences = getSharedPreferences("BRIGHTNESS", MODE_PRIVATE);
        curBrightnessValue = sharedPreferences.getInt("BRIGHTNESS", 0);

        if(curBrightnessValue != 0){
            WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
            layoutParams.screenBrightness = curBrightnessValue / 100.0f;
            getWindow().setAttributes(layoutParams);
        } else{
            curBrightnessValue = 50;
            WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
            layoutParams.screenBrightness = curBrightnessValue / 100.0f;
            getWindow().setAttributes(layoutParams);
        }

    }

    private void calculateWidthandHeight() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        width = displayMetrics.widthPixels;
        height = displayMetrics.heightPixels;
    }

    private void initMediaSession() {
        mediaSession = new MediaSessionCompat(this, getPackageName());
        mediaSessionConnector = new MediaSessionConnector(mediaSession);
        audioManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);

    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(PlayerActivity.this,
                new String[]{Manifest.permission.CAMERA},
                PERMISSIONS_REQUEST_CAMERA);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_CAMERA: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    HAVE_CAMERA_PERMISSION = true;
                    manageFaceTracking();
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void prepareCameraSource() {
        FaceDetector detector = new FaceDetector.Builder(context)
                .setProminentFaceOnly(true)
                .setTrackingEnabled(true)
                .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
                .setMode(FaceDetector.FAST_MODE)
                .build();
        detector.setProcessor(new LargestFaceFocusingProcessor(detector, new FaceTracker()));
        cameraSource = new CameraSource.Builder(context, detector)
                .setFacing(CameraSource.CAMERA_FACING_FRONT)
                .setRequestedPreviewSize(320, 240)
                .build();
    }

    private void manageFaceTracking() {
        IS_FACE_DETECTION_MODE = !IS_FACE_DETECTION_MODE;
        try {
            if (IS_FACE_DETECTION_MODE) {
                cameraSource.start();
                faceButton.setColorFilter(getResources().getColor(R.color.colorPrimary));
            } else {
                cameraSource.stop();
                faceButton.setColorFilter(getResources().getColor(android.R.color.black));
            }
        } catch (IOException e) {
            Log.e(getResources().getString(R.string.app_name), getResources().getString(R.string.io_exception));
        } catch (SecurityException e) {
            Log.e(getResources().getString(R.string.app_name), getResources().getString(R.string.security_exception));
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        updateStartPosition();
        outState.putBoolean(KEY_AUTO_PLAY, startAutoPlay);
        outState.putInt(KEY_WINDOW, startWindow);
        outState.putLong(KEY_POSITION, startPosition);
        outState.putBoolean(KEY_FACE_DETECTION, IS_FACE_DETECTION_MODE);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (Util.SDK_INT > 23) {
            initializePlayer();
            mediaSessionConnector.setPlayer(player, null, null);
            mediaSession.setActive(true);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (Util.SDK_INT <= 23 || player == null) {
            initializePlayer();
            mediaSessionConnector.setPlayer(player, null, null);
            mediaSession.setActive(true);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (Util.SDK_INT <= 23) {
            mediaSessionConnector.setPlayer(null, null, null);
            mediaSession.setActive(false);
            releasePlayer();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (Util.SDK_INT > 23) {
            mediaSessionConnector.setPlayer(null, null, null);
            mediaSession.setActive(false);
            releasePlayer();
        }
    }

    @Override
    public void onDestroy() {
        if (cameraSource != null) {
            cameraSource.release();
        }
        super.onDestroy();
    }

    private void releasePlayer() {
        abandonAudioFocus();
        if (player != null) {
            updateStartPosition();
            player.release();
            player = null;
            mediaSource = null;
            trackSelector = null;
        }
    }

    private void updateStartPosition() {
        if (player != null) {
            startAutoPlay = player.getPlayWhenReady();
            startWindow = player.getCurrentWindowIndex();
            startPosition = Math.max(0, player.getContentPosition());
        }
    }

    private void clearStartPosition() {
        startAutoPlay = true;
        startWindow = C.INDEX_UNSET;
        startPosition = C.TIME_UNSET;
    }

    private void initializePlayer() {
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        ArrayList<Uri> uriArrayList = new ArrayList<>();

        if (Intent.ACTION_VIEW.equals(action) && intent.getData() != null) {
            Uri videoUri = Uri.parse(Uri.encode(Uri.decode(intent.getDataString()), ".-_~/()&!$*+,;='@:"));
            Log.e("URI", String.valueOf(videoUri));
            if (videoUri != null) {
                uriArrayList.add(videoUri);
            }
        }

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if (type.startsWith("video/")) {
                Uri videoUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
                if (videoUri != null) {
                    uriArrayList.add(videoUri);
                }
            }
        } else if (Intent.ACTION_SEND_MULTIPLE.equals(action) && type != null) {
            if (type.startsWith("video/")) {
                ArrayList<Uri> videoUris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
                if (videoUris != null) {
                    uriArrayList = videoUris;
                }
            }
        }

        if (getIntent().getIntExtra("INDEX", -1) != -1) {
            startWindow = getIntent().getIntExtra("INDEX", 0);
            getIntent().removeExtra("INDEX");
        }

        MediaSource[] mediaSources = new MediaSource[uriArrayList.size()];
        DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
        DataSource.Factory mediaDataSourceFactory = new DefaultDataSourceFactory(context, Util.getUserAgent(context, getResources().getString(R.string.app_name)), bandwidthMeter);
        TrackSelection.Factory videoTrackSelectionFactory = new AdaptiveTrackSelection.Factory(bandwidthMeter);
        trackSelector = new DefaultTrackSelector(videoTrackSelectionFactory);

        for (int i = 0; i < uriArrayList.size(); i++) {
            mediaSources[i] = new ExtractorMediaSource.Factory(mediaDataSourceFactory).createMediaSource(uriArrayList.get(i));
        }
        player = ExoPlayerFactory.newSimpleInstance(context, trackSelector);

        playerView.setPlayer(player);

        mediaSource = mediaSources.length == 1 ? mediaSources[0]
                : new ConcatenatingMediaSource(mediaSources);

        boolean haveStartPosition = startWindow != C.INDEX_UNSET;
        if (haveStartPosition) {
            player.seekTo(startWindow, startPosition);
        }
        player.prepare(mediaSource, !haveStartPosition, false);

        requestAudioFocus();

        player.setPlayWhenReady(startAutoPlay);

        player.addListener(new Player.DefaultEventListener() {
            @Override
            public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
                if (playbackState == Player.STATE_BUFFERING && progressBar.getVisibility() == View.GONE) {
                    progressBar.setVisibility(View.VISIBLE);
                } else if (playbackState == Player.STATE_READY && progressBar.getVisibility() == View.VISIBLE) {
                    progressBar.setVisibility(View.GONE);
                }
                if (playWhenReady)
                    requestAudioFocus();
                else
                    abandonAudioFocus();
                super.onPlayerStateChanged(playWhenReady, playbackState);
            }
        });
    }

    private void hideSystemControls() {
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                View decorView = getWindow().getDecorView();
                decorView.setSystemUiVisibility(uiOptions);
            }
        }, 2000);
    }

    @Override
    public void onUserLeaveHint() {
        super.onUserLeaveHint();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            enterPictureInPictureMode(
                    new PictureInPictureParams.Builder()
                            .setAspectRatio(new Rational(playerView.getVideoSurfaceView().getWidth(), playerView.getVideoSurfaceView().getHeight()))
                            .build());
        }
    }

    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode) {
        playerView.setUseController(!isInPictureInPictureMode);
        super.onPictureInPictureModeChanged(isInPictureInPictureMode);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        if (cameraSource != null)
            cameraSource.release();
        prepareCameraSource();
        if (IS_FACE_DETECTION_MODE) {
            try {
                cameraSource.start();
            } catch (IOException e) {
                Log.e(getResources().getString(R.string.app_name), getResources().getString(R.string.io_exception));
            } catch (SecurityException e) {
                Log.e(getResources().getString(R.string.app_name), getResources().getString(R.string.security_exception));
            }
        }
        calculateWidthandHeight();
        super.onConfigurationChanged(newConfig);
    }

    private void requestAudioFocus() {
        audioManager.requestAudioFocus(afChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
    }

    private void abandonAudioFocus() {
        audioManager.abandonAudioFocus(afChangeListener);
    }

    private class FaceTracker extends Tracker<Face> {

        @Override
        public void onNewItem(int i, Face face) {
            if (player != null && !player.getPlayWhenReady()) {
                requestAudioFocus();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        player.setPlayWhenReady(true);
                    }
                });
            }
            super.onNewItem(i, face);
        }

        @Override
        public void onMissing(Detector.Detections<Face> detections) {
            if (detections.getDetectedItems().size() == 0 && player != null && player.getPlayWhenReady()) {
                abandonAudioFocus();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        player.setPlayWhenReady(false);
                    }
                });
            }
            super.onMissing(detections);
        }

    }

    class SwipeGestureListener extends GestureDetector.SimpleOnGestureListener {

        int volume;

        @Override
        public boolean onDown(MotionEvent event) {
            if (playerView.isControllerVisible())
                playerView.hideController();
            else
                playerView.showController();
            return true;
        }

        @Override
        public boolean onDoubleTap(MotionEvent event) {
            if (infoLayout.getVisibility() == View.GONE && progressBar.getVisibility() == View.GONE)
                infoLayout.setVisibility(View.VISIBLE);
            infoLayout.removeCallbacks(hideInfo);
            infoText.setText("");
            if (event.getX() < width / 3) {
                if (player != null)
                    if (player.getCurrentAdGroupIndex() - 15000 < player.getDuration())
                        player.seekTo(player.getCurrentPosition() - 15000);
                    else
                        player.seekTo(0);
                infoImage.setImageDrawable(getResources().getDrawable(R.drawable.ic_rewind));
            }
            if (event.getX() > width / 3 && event.getX() < width / 1.5) {
                if (player != null)
                    player.setPlayWhenReady(!player.getPlayWhenReady());
                if (player.getPlayWhenReady())
                    infoImage.setImageDrawable(getResources().getDrawable(R.drawable.ic_play));
                else
                    infoImage.setImageDrawable(getResources().getDrawable(R.drawable.ic_pause));
            }
            if (event.getX() > width / 1.5) {
                if (player != null)
                    if (player.getCurrentAdGroupIndex() + 15000 < player.getDuration())
                        player.seekTo(player.getCurrentPosition() + 15000);
                    else
                        player.seekTo(player.getDuration());
                infoImage.setImageDrawable(getResources().getDrawable(R.drawable.ic_forward));
            }
            infoLayout.postDelayed(hideInfo, 2000);
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            if(e1.getY()> (height/100)*5 && e1.getY() < height-((height/100)*5)){

                if (infoLayout.getVisibility() == View.GONE && progressBar.getVisibility() == View.GONE)
                    infoLayout.setVisibility(View.VISIBLE);
                infoLayout.removeCallbacks(hideInfo);

                if (e1.getX() > width / 2) {
                    volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                    infoImage.setImageDrawable(getResources().getDrawable(R.drawable.ic_volume));
                    if (e1.getY() < e2.getY()) {
                        if (volume > 0)
                            volume--;
                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0);
                    }
                    if (e1.getY() > e2.getY()) {
                        if (volume < maxVolume)
                            volume++;
                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0);
                    }
                    infoText.setText(String.valueOf(volume));
                } else {
                    infoImage.setImageDrawable(getResources().getDrawable(R.drawable.ic_brightness));
                    if (e1.getY() < e2.getY()) {
                        if (curBrightnessValue > 5)
                            curBrightnessValue -= 1;
                        WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
                        layoutParams.screenBrightness = curBrightnessValue / 100.0f;
                        getWindow().setAttributes(layoutParams);
                    }
                    if (e1.getY() > e2.getY()) {
                        if (curBrightnessValue < 100)
                            curBrightnessValue += 1;
                        WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
                        layoutParams.screenBrightness = curBrightnessValue / 100.0f;
                        getWindow().setAttributes(layoutParams);
                    }
                    SharedPreferences sharedPreferences = getSharedPreferences("BRIGHTNESS", MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putInt("BRIGHTNESS", curBrightnessValue);
                    editor.apply();
                    editor.commit();
                    infoText.setText(String.valueOf(curBrightnessValue));
                }
                infoLayout.postDelayed(hideInfo, 2000);

            }
            return true;
        }

    }

    private class HideInfo implements Runnable {
        @Override
        public void run() {
            if (infoLayout.getVisibility() == View.VISIBLE)
                infoLayout.setVisibility(View.GONE);
        }
    }

}
