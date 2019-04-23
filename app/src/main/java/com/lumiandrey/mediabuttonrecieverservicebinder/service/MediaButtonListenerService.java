package com.lumiandrey.mediabuttonrecieverservicebinder.service;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.graphics.BitmapFactory;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.ext.okhttp.OkHttpDataSourceFactory;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.extractor.ogg.OggExtractor;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.LoopingMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.FileDataSourceFactory;
import com.google.android.exoplayer2.upstream.RawResourceDataSource;
import com.google.android.exoplayer2.upstream.cache.Cache;
import com.google.android.exoplayer2.upstream.cache.CacheDataSource;
import com.google.android.exoplayer2.upstream.cache.CacheDataSourceFactory;
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor;
import com.google.android.exoplayer2.upstream.cache.SimpleCache;
import com.google.android.exoplayer2.util.Util;
import com.lumiandrey.mediabuttonrecieverservicebinder.MainActivity;
import com.lumiandrey.mediabuttonrecieverservicebinder.MediaButtonHelper;
import com.lumiandrey.mediabuttonrecieverservicebinder.R;
import com.lumiandrey.mediabuttonrecieverservicebinder.style.MediaStyleHelper;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.UUID;

import okhttp3.OkHttpClient;

public class MediaButtonListenerService extends Service {

    public static final String TAG = MediaButtonListenerService.class.getName();
    public static final String NAME_COMMAND = TAG + "Command";

    private static final int ID_FOREGROUND_NOTIFICATION = (TAG.hashCode() > 0 ? -1*TAG.hashCode() : TAG.hashCode());

    private int countBindingUser = 0;

    private boolean isRunning = false;

    private MediaSessionCompat _mediaSession;
    private MediaSessionCompat.Token _mediaSessionToken;

    private IntentFilter filter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
    @Nullable
    private BroadcastReceiver mBecomingNoisyReceiver = null;

    // ...метаданных трека
    final MediaMetadataCompat.Builder metadataBuilder = new MediaMetadataCompat.Builder();

    private SimpleExoPlayer mExoPlayer;
    private DataSource.Factory dataSourceFactory;
    private ExtractorsFactory extractorsFactory;


    // ...состояния плеера
    // Здесь мы указываем действия, которые собираемся обрабатывать в коллбэках.
    // Например, если мы не укажем ACTION_PAUSE,
    // то нажатие на паузу не вызовет onPause.
    // ACTION_PLAY_PAUSE обязателен, иначе не будет работать
    // управление с Android Wear!
    private final PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder().setActions(
            PlaybackStateCompat.ACTION_PLAY
                    | PlaybackStateCompat.ACTION_STOP
                    | PlaybackStateCompat.ACTION_PAUSE
                    | PlaybackStateCompat.ACTION_PLAY_PAUSE
                    | PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                    | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
    );

    private AudioManager audioManager;
    private AudioFocusRequest audioFocusRequest;
    private boolean audioFocusRequested = false;

    int currentState = PlaybackStateCompat.STATE_STOPPED;

    private MediaSessionCompat.Callback mediaSessionCallback = new MediaSessionCompat.Callback() {

        private Uri currentUri;
        @Override
        public boolean onMediaButtonEvent(Intent mediaButtonEvent) {

            Log.d(TAG, "onMediaButtonEvent: true main service" + mediaButtonEvent);
            return super.onMediaButtonEvent(mediaButtonEvent);
        }

        @Override
        public void onPlay() {

            Log.d(TAG, "onPlay: ");
            if (!mExoPlayer.getPlayWhenReady() || !isRunning) {
                prepareToPlay(
                       RawResourceDataSource.buildRawResourceUri(R.raw.sound)/*
                        Uri.parse("assets:///sound.mp3")*/
                );

                updateMetadataFromTrack();

                startService(new Intent(getApplicationContext(), MediaButtonListenerService.class));

                if (!audioFocusRequested) {
                    audioFocusRequested = true;

                    int audioFocusResult;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        audioFocusResult = audioManager.requestAudioFocus(audioFocusRequest);
                    } else {
                        audioFocusResult = audioManager.requestAudioFocus(audioFocusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
                    }
                    if (audioFocusResult != AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
                        return;
                }

                Log.d(TAG, "onPlay: granted audio focus");
                _mediaSession.setActive(true); // Сразу после получения фокуса

                mExoPlayer.setPlayWhenReady(true);
                if (mBecomingNoisyReceiver == null) {

                    mBecomingNoisyReceiver = new BroadcastReceiver() {
                        @Override
                        public void onReceive(Context context, Intent intent) {

                            // Disconnecting headphones - stop playback
                            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
                                mediaSessionCallback.onPause();
                                Log.d(TAG, "onReceive: ACTION_AUDIO_BECOMING_NOISY");
                            } else {
                                Log.d(TAG, "onReceive: ACTION_AUDIO_BECOMING_NOISY no");
                            }
                        }
                    };

                    registerReceiver(mBecomingNoisyReceiver, filter);
                }

                isRunning = true;
            }

            _mediaSession.setPlaybackState(stateBuilder.setState(PlaybackStateCompat.STATE_PLAYING, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 1).build());
            currentState = PlaybackStateCompat.STATE_PLAYING;

            refreshNotificationAndForegroundStatus(currentState);
        }

        @Override
        public void onPause() {

            Log.d(TAG, "onPause: ");

            if (mExoPlayer.getPlayWhenReady()) {
                mExoPlayer.setPlayWhenReady(false);

                if(mBecomingNoisyReceiver != null){
                    unregisterReceiver(mBecomingNoisyReceiver);
                    mBecomingNoisyReceiver = null;
                }
            }

            isRunning = false;
            _mediaSession.setPlaybackState(stateBuilder.setState(PlaybackStateCompat.STATE_PAUSED, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 1).build());
            currentState = PlaybackStateCompat.STATE_PAUSED;

            refreshNotificationAndForegroundStatus(currentState);
        }

        @Override
        public void onStop() {

            if (mExoPlayer.getPlayWhenReady()) {
                mExoPlayer.setPlayWhenReady(false);

                if(mBecomingNoisyReceiver != null){
                    unregisterReceiver(mBecomingNoisyReceiver);
                    mBecomingNoisyReceiver = null;
                }
            }

            if (audioFocusRequested) {
                audioFocusRequested = false;

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    audioManager.abandonAudioFocusRequest(audioFocusRequest);
                } else {
                    audioManager.abandonAudioFocus(audioFocusChangeListener);
                }
            }

            isRunning = false;

            _mediaSession.setActive(false);

            _mediaSession.setPlaybackState(stateBuilder.setState(PlaybackStateCompat.STATE_STOPPED, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 1).build());
            currentState = PlaybackStateCompat.STATE_STOPPED;

            refreshNotificationAndForegroundStatus(currentState);
            stopSelf();
        }

        private void updateMetadataFromTrack() {
            // Заполняем данные о треке
            MediaMetadataCompat metadata = metadataBuilder
                    .putBitmap(MediaMetadataCompat.METADATA_KEY_ART,
                            BitmapFactory.decodeResource(getResources(), R.drawable.ic_dashboard_black_24dp))
                    .putString(MediaMetadataCompat.METADATA_KEY_TITLE, "Handling")
                    .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, "Handling")
                    .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, "Handling")
                    .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, 20)
                    .build();

            _mediaSession.setMetadata(metadata);
        }

        private void prepareToPlay(Uri uri) {

            if (!uri.equals(currentUri)) {
                currentUri = uri;

                DataSpec dataSpec = new DataSpec(uri);
                final RawResourceDataSource rawResourceDataSource = new RawResourceDataSource(MediaButtonListenerService.this);
                try {
                    rawResourceDataSource.open(dataSpec);
                } catch (RawResourceDataSource.RawResourceDataSourceException e) {
                    e.printStackTrace();
                }

                DataSource.Factory factory = new DataSource.Factory() {
                    @Override
                    public DataSource createDataSource() {
                        return rawResourceDataSource;
                    }
                };

                MediaSource audioSource = new ExtractorMediaSource(rawResourceDataSource.getUri(),
                        factory, new DefaultExtractorsFactory(), null, null);

                mExoPlayer.prepare(audioSource);

                /*ExtractorMediaSource mediaSource = new ExtractorMediaSource(uri, dataSourceFactory, extractorsFactory, null, null);
                mExoPlayer.prepare(mediaSource);*/
            }
        }
    };

    private AudioManager.OnAudioFocusChangeListener audioFocusChangeListener = focusChange -> {
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
                mediaSessionCallback.onPlay();
                Log.d(TAG, "AUDIOFOCUS_GAIN: приложение дает понять, что оно собирается долго воспроизводить свой звук, и текущее воспроизведение должно приостановиться на это время");
                break;
            //воспроизведение будет коротким, и текущее воспроизведение должно приостановиться на это время
            case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT:{
                Log.d(TAG, "AUDIOFOCUS_GAIN_TRANSIENT воспроизведение будет коротким, и текущее воспроизведение должно приостановиться на это время: ");
            } break;
            //фокус потерян в результате того, что другое приложение запросило фокус
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                Log.d(TAG, "AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK: ");
                mediaSessionCallback.onPause();
                break;

            case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK:{ // воспроизведение будет коротким, но текущее воспроизведение может просто на это время убавить звук и продолжать играть
                Log.d(TAG, " воспроизведение будет коротким, но текущее воспроизведение может просто на это время убавить звук и продолжать играть: ");
            } break;
            //фокус потерян в результате того, что другое приложение запросило фокус AUDIOFOCUS_GAIN.
            // Т.е. нам дают понять, что другое приложение собирается воспроизводить что-то долгое и просит нас пока приостановить наше воспроизведение.
            case AudioManager.AUDIOFOCUS_LOSS:{
                mediaSessionCallback.onPause();
                Log.d(TAG, ": AUDIOFOCUS_LOSS");

            } break;
            default:
                mediaSessionCallback.onPause();
                Log.d(TAG, "AUDIOFOCUS_NONE: ");
                break;
        }
    };

    @NonNull
    private MediaButtonListenerServiceBinder mButtonListenerServiceBinder = new MediaButtonListenerServiceBinder(this);

    @Override
    public void onCreate() {
        super.onCreate();

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            @SuppressLint("WrongConstant") NotificationChannel notificationChannel = new NotificationChannel(TAG, getString(R.string.app_name), NotificationManagerCompat.IMPORTANCE_DEFAULT);
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(notificationChannel);

            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build();
            audioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setOnAudioFocusChangeListener(audioFocusChangeListener)
                    .setAcceptsDelayedFocusGain(false)
                    .setWillPauseWhenDucked(true)
                    .setAudioAttributes(audioAttributes)
                    .build();
        }

        _mediaSession = new MediaSessionCompat(getApplicationContext(), MainActivity.class.getName() + ".__");

        if (_mediaSession == null) {
            Log.e(TAG, "initMediaSession: _mediaSession = null");
            return;
        }

        // FLAG_HANDLES_MEDIA_BUTTONS - хотим получать события от аппаратных кнопок
        // (например, гарнитуры)
        // FLAG_HANDLES_TRANSPORT_CONTROLS - хотим получать события от кнопок
        // на окне блокировки
        _mediaSession.setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                        MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        _mediaSessionToken = _mediaSession.getSessionToken();
        Log.d(TAG, "onCreate: " + _mediaSessionToken);

        _mediaSession.setCallback(mediaSessionCallback);

        // Отдаем наши коллбэки
        Context appContext = getApplicationContext();

        Intent activityIntent = new Intent(appContext, MainActivity.class);
        _mediaSession.setSessionActivity(PendingIntent.getActivity(appContext, 0, activityIntent, 0));

        Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON, null, appContext, MediaButtonReceiver.class);
        _mediaSession.setMediaButtonReceiver(PendingIntent.getBroadcast(appContext, 0, mediaButtonIntent, 0));

        mExoPlayer = ExoPlayerFactory.newSimpleInstance(new DefaultRenderersFactory(this), new DefaultTrackSelector(), new DefaultLoadControl());
        mExoPlayer.addListener(exoPlayerListener);
        DataSource.Factory httpDataSourceFactory = new OkHttpDataSourceFactory(new OkHttpClient(), Util.getUserAgent(this, getString(R.string.app_name)), null);
        Cache cache = new SimpleCache(new File(this.getCacheDir().getAbsolutePath() + "/exoplayer"), new LeastRecentlyUsedCacheEvictor(1024 * 1024 * 100)); // 100 Mb max
        //this.dataSourceFactory = new CacheDataSourceFactory(cache, httpDataSourceFactory, CacheDataSource.FLAG_BLOCK_ON_CACHE | CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR);

        this.dataSourceFactory = new FileDataSourceFactory();
        this.extractorsFactory = new DefaultExtractorsFactory();

        Log.d(TAG, "onCreate" + String.format(" Count binder component %d",  countBindingUser));
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {

        if(countBindingUser < 0){
            countBindingUser = 0;
        }

        countBindingUser++;

        Log.d(TAG, "onBind" + String.format(" Count binder component %d",  countBindingUser));

        return mButtonListenerServiceBinder;
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);

        if(countBindingUser < 0){
            countBindingUser = 0;
        }

        countBindingUser++;

        Log.d(TAG, "onBind" + String.format(" Count binder component %d",  countBindingUser));

        Log.d(TAG, "onRebind" + String.format(" Count binder component %d",  countBindingUser));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        MediaButtonReceiver.handleIntent(_mediaSession, intent);

        Log.d(TAG, "onStartCommand: " + MediaButtonHelper.getKeyName(intent));

        Log.d(TAG, "onStartCommand: " + intent);

        refreshNotificationAndForegroundStatus(currentState);

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {

        Log.d(TAG, "onDestroy" + String.format(" Count binder component %d",  countBindingUser));

        _mediaSession.release();
        super.onDestroy();
    }

    @Override
    public boolean onUnbind(Intent intent) {

        countBindingUser--;

        Log.d(TAG, "onUnbind " + String.format(" Count binder component %d",  countBindingUser));

        if(countBindingUser < 0){
            countBindingUser = 0;
        }

        //startService(checkForeground(this));

        return true;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        Log.d(TAG, "onConfigurationChanged");
    }

    @Nullable
    public MediaSessionCompat.Token getSessionToken() {

        return (_mediaSession == null? null: _mediaSession.getSessionToken());
    }

    private void refreshNotificationAndForegroundStatus(int playbackState) {
        switch (playbackState) {
            case PlaybackStateCompat.STATE_PLAYING: {

                Log.d(TAG, "refreshNotificationAndForegroundStatus: STATE_PLAYING");
                startForeground(ID_FOREGROUND_NOTIFICATION, getNotification(playbackState));
                break;
            }
            case PlaybackStateCompat.STATE_PAUSED: {
                Log.d(TAG, "refreshNotificationAndForegroundStatus: STATE_PAUSED ");
                NotificationManagerCompat.from(MediaButtonListenerService.this)
                        .notify(ID_FOREGROUND_NOTIFICATION, getNotification(playbackState));

                stopForeground(false);
                break;
            }
            default: {

                Log.d(TAG, "refreshNotificationAndForegroundStatus: default");

                stopForeground(true);
                break;
            }
        }
    }

    private Notification getNotification(int playbackState) {
        NotificationCompat.Builder builder = MediaStyleHelper.from(this, _mediaSession, TAG);

        if (playbackState == PlaybackStateCompat.STATE_PLAYING)
            builder.addAction(new NotificationCompat.Action(android.R.drawable.ic_media_pause, getString(R.string.app_name), MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_PLAY_PAUSE)));
        else
            builder.addAction(new NotificationCompat.Action(android.R.drawable.ic_media_play, getString(R.string.app_name), MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_PLAY_PAUSE)));

        builder.setStyle(new android.support.v4.media.app.NotificationCompat.MediaStyle()
                .setShowActionsInCompactView(0)
                .setShowCancelButton(true)
                .setCancelButtonIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_STOP))
                .setMediaSession(_mediaSession.getSessionToken())); // setMediaSession требуется для Android Wear
        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setColor(ContextCompat.getColor(this, R.color.colorPrimaryDark)); // The whole background (in MediaStyle), not just icon background
        builder.setShowWhen(false);
        builder.setPriority(NotificationCompat.PRIORITY_HIGH);
        builder.setOnlyAlertOnce(true);
        builder.setChannelId(TAG);

        return builder.build();
    }

    private ExoPlayer.EventListener exoPlayerListener = new ExoPlayer.EventListener() {
        @Override
        public void onTimelineChanged(Timeline timeline, Object manifest) {
        }

        @Override
        public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
        }

        @Override
        public void onLoadingChanged(boolean isLoading) {
        }

        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
            if (playWhenReady && playbackState == ExoPlayer.STATE_ENDED) {
                mediaSessionCallback.onSkipToNext();
            }
        }

        @Override
        public void onPlayerError(ExoPlaybackException error) {
        }

        @Override
        public void onPositionDiscontinuity() {
        }

        @Override
        public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {
        }
    };

}
