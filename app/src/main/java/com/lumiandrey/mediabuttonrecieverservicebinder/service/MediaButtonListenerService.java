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
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.lumiandrey.mediabuttonrecieverservicebinder.MainActivity;
import com.lumiandrey.mediabuttonrecieverservicebinder.MediaButtonHelper;
import com.lumiandrey.mediabuttonrecieverservicebinder.R;

import java.io.Serializable;
import java.util.UUID;

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

    private MediaSessionCompat.Callback mediaSessionCallback = new MediaSessionCompat.Callback() {

        private Uri currentUri;
        int currentState = PlaybackStateCompat.STATE_STOPPED;

        @Override
        public boolean onMediaButtonEvent(Intent mediaButtonEvent) {

            Log.d(TAG, "onMediaButtonEvent: true main service" + mediaButtonEvent);
            return super.onMediaButtonEvent(mediaButtonEvent);
        }

        @Override
        public void onPlay() {

            Log.d(TAG, "onPlay: ");
            if (!isRunning) {

                // Заполняем данные о треке
                MediaMetadataCompat metadata = metadataBuilder
                        .putBitmap(MediaMetadataCompat.METADATA_KEY_ART,
                                BitmapFactory.decodeResource(getResources(), R.drawable.ic_dashboard_black_24dp))
                        .putString(MediaMetadataCompat.METADATA_KEY_TITLE, "Handling")
                        .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, "Handling")
                        .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, "Handling")
                        .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, 200)
                        .build();

                _mediaSession.setMetadata(metadata);
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

                _mediaSession.setActive(true); // Сразу после получения фокуса

                if (mBecomingNoisyReceiver == null) {

                    mBecomingNoisyReceiver = new BroadcastReceiver() {
                        @Override
                        public void onReceive(Context context, Intent intent) {

                            Log.d(TAG, "onReceive: " + intent);
                            mediaSessionCallback.onPause();
                        }
                    };

                    registerReceiver(mBecomingNoisyReceiver, filter);
                }

                isRunning = true;
            }

            _mediaSession.setPlaybackState(stateBuilder.setState(PlaybackStateCompat.STATE_PLAYING, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 1).build());
            currentState = PlaybackStateCompat.STATE_PLAYING;

        }

        @Override
        public void onPause() {

            Log.d(TAG, "onPause: ");

            isRunning = false;
            _mediaSession.setPlaybackState(stateBuilder.setState(PlaybackStateCompat.STATE_PAUSED, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 1).build());
            currentState = PlaybackStateCompat.STATE_PAUSED;

        }

        @Override
        public void onStop() {

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

            stopSelf();
        }

        private void updateMetadataFromTrack(MusicRepository.Track track) {
            metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ART, BitmapFactory.decodeResource(getResources(), track.getBitmapResId()));
            metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_TITLE, track.getTitle());
            metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_ALBUM, track.getArtist());
            metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, track.getArtist());
            metadataBuilder.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, track.getDuration());
            _mediaSession.setMetadata(metadataBuilder.build());
        }
    };

    private AudioManager.OnAudioFocusChangeListener audioFocusChangeListener = focusChange -> {
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
                // Фокус предоставлен.
                // Например, был входящий звонок и фокус у нас отняли.
                // Звонок закончился, фокус выдали опять
                // и мы продолжили воспроизведение.
                Log.d(TAG, "AUDIOFOCUS_GAIN:  Фокус предоставлен");
                mediaSessionCallback.onPlay();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                // Фокус отняли, потому что какому-то приложению надо
                // коротко "крякнуть".
                // Например, проиграть звук уведомления или навигатору сказать
                // "Через 50 метров поворот направо".
                // В этой ситуации нам разрешено не останавливать вопроизведение,
                // но надо снизить громкость.
                // Приложение не обязано именно снижать громкость,
                // можно встать на паузу, что мы здесь и делаем.
                mediaSessionCallback.onPause();
                Log.d(TAG, "AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK: кому-то коротко \"крякнуть\"" );
                break;

            default:
                audioFocusRequested = false;
                mediaSessionCallback.onPause();
                Log.d(TAG, " // Фокус совсем отняли.: ");
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

        startService(checkForeground(this));

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

        startService(checkForeground(this));

        Log.d(TAG, "onRebind" + String.format(" Count binder component %d",  countBindingUser));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        MediaButtonReceiver.handleIntent(_mediaSession, intent);

        Log.d(TAG, "onStartCommand " + String.format(" Count binder component %d",  countBindingUser));
        Log.d(TAG, "onStartCommand: " + MediaButtonHelper.getKeyName(intent));

        startForeground(ID_FOREGROUND_NOTIFICATION, buildNotification());
        stopForeground(true);

      if(intent.getExtras() != null) {
            Command command = (Command) intent.getExtras().getSerializable(NAME_COMMAND);

            switch (command != null ? command : Command.DEFAULT){
                case CHECK_FOREGROUND:{

                    if(isForegroundWork()){
                        startForeground(ID_FOREGROUND_NOTIFICATION, buildNotification());
                    } else {
                        stopForeground(true);
                    }

                    mediaSessionCallback.onPlay();
                } break;
                case START:{

                } break;
                case STOP: {


                    Log.d(TAG, "onStartCommand: stop listener");

                    if(mBecomingNoisyReceiver != null) {
                        unregisterReceiver(mBecomingNoisyReceiver);
                        mBecomingNoisyReceiver = null;
                    }

                    if (audioFocusRequested) {
                        audioFocusRequested = false;

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            audioManager.abandonAudioFocusRequest(audioFocusRequest);
                        } else {
                            audioManager.abandonAudioFocus(audioFocusChangeListener);
                        }
                    }

                    stopForeground(true);
                    stopSelf();
                } break;
            }
        }

        return super.onStartCommand(intent, flags, startId);
    }


    private Notification buildNotification() {

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationChannel channel = new NotificationChannel(TAG, TAG,
                    NotificationManager.IMPORTANCE_DEFAULT);

            channel.setDescription(TAG);

            channel.enableLights(false);
            channel.enableVibration(false);


            manager.createNotificationChannel(channel);
        }

        PendingIntent deletePendingIntent;

        int requestCode = UUID.randomUUID().hashCode();
        deletePendingIntent = PendingIntent.getService(this,
                (requestCode > 0 ? -1*requestCode : requestCode),
                MediaButtonListenerService.createStop(this),
                0);

        PendingIntent contentIntent = PendingIntent.getActivity(
                this,
                0,
                new Intent(this, MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, TAG);

        builder.setContentTitle(getString(R.string.app_name));

        builder.setContentText("MediaButtonListen");

        builder.setSmallIcon(R.drawable.ic_launcher_foreground);

        builder.setWhen(System.currentTimeMillis());

        builder = builder.addAction(android.R.drawable.ic_delete,
                "stop service", deletePendingIntent);

        builder.setContentIntent(contentIntent);

        return builder.build();
    }

    @Override
    public void onDestroy() {

        Log.d(TAG, "onDestroy" + String.format(" Count binder component %d",  countBindingUser));

        _mediaSession.setActive(false);
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

        startService(checkForeground(this));

        return true;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        Log.d(TAG, "onConfigurationChanged");
    }

    private boolean isForegroundWork(){

        return countBindingUser <= 0;
    }

    public static void bindingMediaButtonListenerService (
            @NonNull final Context context,
            @NonNull final ServiceConnection serviceConnection){

        context.bindService(
                new Intent(context, MediaButtonListenerService.class),
                serviceConnection,
                Context.BIND_AUTO_CREATE
        );
    }

    public static void unbindingMediaButtonListenerService(
            @NonNull final Context context,
            @NonNull final ServiceConnection serviceConnection
    ) {

        context.unbindService(serviceConnection);
    }

    public static Intent createStop(@NonNull final Context context){

        return createCommand(context, Command.STOP);
    }

    public static Intent createStart(@NonNull final Context context){

        return createCommand(context, Command.START);
    }

    private static Intent checkForeground(@NonNull final Context context){

        return createCommand(context, Command.CHECK_FOREGROUND);
    }

    public static Intent createCommand(@NonNull final Context context, @NonNull Command command){

        Intent intent = new Intent(context, MediaButtonListenerService.class);

        intent.putExtra(NAME_COMMAND, command);

        return intent;
    }

    @Nullable
    public MediaSessionCompat.Token getSessionToken() {

        return null;//(mediaSession == null? null: mediaSession.getSessionToken());
    }

    private enum Command implements Serializable {

        CHECK_FOREGROUND,
        START,
        STOP,
        DEFAULT
    }
}
