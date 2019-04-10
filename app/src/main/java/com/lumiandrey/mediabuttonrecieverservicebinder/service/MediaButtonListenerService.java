package com.lumiandrey.mediabuttonrecieverservicebinder.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.graphics.BitmapFactory;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ResultReceiver;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import com.lumiandrey.mediabuttonrecieverservicebinder.MainActivity;
import com.lumiandrey.mediabuttonrecieverservicebinder.R;
import com.lumiandrey.mediabuttonrecieverservicebinder.style.MediaStyleHelper;

import java.io.Serializable;
import java.util.UUID;

public class MediaButtonListenerService extends Service {

    public static final String TAG = MediaButtonListenerService.class.getName();
    public static final String NAME_COMMAND = TAG + "Command";

    private static final int ID_FOREGROUND_NOTIFICATION = (TAG.hashCode() > 0 ? -1*TAG.hashCode() : TAG.hashCode());

    private int countBindingUser = 0;

    private BroadcastReceiver mBecomingNoisyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            Log.d(TAG, "onReceive: " + intent);
            mediaSessionCallback.onPause();
        }
    };

    private IntentFilter filter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);

    // ...метаданных трека
    final MediaMetadataCompat.Builder metadataBuilder = new MediaMetadataCompat.Builder();

    // ...состояния плеера
    // Здесь мы указываем действия, которые собираемся обрабатывать в коллбэках.
    // Например, если мы не укажем ACTION_PAUSE,
    // то нажатие на паузу не вызовет onPause.
    // ACTION_PLAY_PAUSE обязателен, иначе не будет работать
    // управление с Android Wear!
    final PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder()
            .setActions(
                    PlaybackStateCompat.ACTION_PLAY
                            | PlaybackStateCompat.ACTION_STOP
                            | PlaybackStateCompat.ACTION_PAUSE
                            | PlaybackStateCompat.ACTION_PLAY_PAUSE
                            | PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                            | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS);

    private MediaSessionCompat mediaSession;

    private AudioManager mAudioManager;
    private AudioFocusRequest audioFocusRequest;

    private AudioManager.OnAudioFocusChangeListener audioFocusChangeListener =
            new AudioManager.OnAudioFocusChangeListener() {
                @Override
                public void onAudioFocusChange(int focusChange) {
                    switch (focusChange) {
                        case AudioManager.AUDIOFOCUS_GAIN:
                            // Фокус предоставлен.
                            // Например, был входящий звонок и фокус у нас отняли.
                            // Звонок закончился, фокус выдали опять
                            // и мы продолжили воспроизведение.
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
                            break;
                        default:
                            // Фокус совсем отняли.
                            mediaSessionCallback.onPause();
                            break;
                    }
                }
            };

    @NonNull
    private MediaButtonListenerServiceBinder mButtonListenerServiceBinder = new MediaButtonListenerServiceBinder(this);


    @Override
    public void onCreate() {
        super.onCreate();

        // "PlayerService" - просто tag для отладки
        mediaSession = new MediaSessionCompat(this, "PlayerService");

        // FLAG_HANDLES_MEDIA_BUTTONS - хотим получать события от аппаратных кнопок
        // (например, гарнитуры)
        // FLAG_HANDLES_TRANSPORT_CONTROLS - хотим получать события от кнопок
        // на окне блокировки
        mediaSession.setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS
                        | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        // Отдаем наши коллбэки
        mediaSession.setCallback(mediaSessionCallback);

        Context appContext = getApplicationContext();

        // Укажем activity, которую запустит система, если пользователь
        // заинтересуется подробностями данной сессии
        Intent activityIntent = new Intent(appContext, MainActivity.class);
        mediaSession.setSessionActivity(
                PendingIntent.getActivity(appContext, 0, activityIntent, 0));

        Intent mediaButtonIntent = new Intent(
                Intent.ACTION_MEDIA_BUTTON, null, appContext, MediaButtonReceiver.class);
        mediaSession.setMediaButtonReceiver(
                PendingIntent.getBroadcast(appContext, 0, mediaButtonIntent, 0));

        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

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

        MediaButtonReceiver.handleIntent(mediaSession, intent);
        Log.d(TAG, "onStartCommand" + String.format(" Count binder component %d",  countBindingUser));

        if(intent != null && intent.getExtras() != null) {
            Command command = (Command) intent.getExtras().getSerializable(NAME_COMMAND);

            switch (command != null ? command : Command.DEFAULT){
                case CHECK_FOREGROUND:{

                    if(isForegroundWork()){
                        startForeground(ID_FOREGROUND_NOTIFICATION, buildNotification());
                    } else {
                        stopForeground(true);
                    }


                } break;
                case START:{

                    startListen();

                } break;
                case STOP: {


                    Log.d(TAG, "onStartCommand: stop listener");

                    unregisterReceiver(mBecomingNoisyReceiver);

                    stopForeground(true);
                    stopSelf();
                } break;
            }
        }

        return super.onStartCommand(intent, flags, startId);
    }

    private void startListen() {

        Log.d(TAG, "startListen");

        registerReceiver(
                mBecomingNoisyReceiver,
                filter);

    }

    MediaSessionCompat.Callback mediaSessionCallback = new MediaSessionCompat.Callback() {

        @Override
        public boolean onMediaButtonEvent(Intent mediaButtonEvent) {

            Log.d(TAG, "onMediaButtonEvent: " + mediaButtonEvent);
            return super.onMediaButtonEvent(mediaButtonEvent);
        }

        @Override
        public void onPlay() {

            Log.d(TAG, "onPlay: ");

            // Заполняем данные о треке
            MediaMetadataCompat.Builder metadata = metadataBuilder;
            metadata.putString(MediaMetadataCompat.METADATA_KEY_TITLE, "title");
            metadata.putString(MediaMetadataCompat.METADATA_KEY_ALBUM, "Artist");
            metadata.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, "Album");
            metadata.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, 0);
            mediaSession.setMetadata(metadata.build());

            int audioFocusResult;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {


                AudioAttributes audioAttributes = new AudioAttributes.Builder()
                        // Собираемся воспроизводить звуковой контент
                        // (а не звук уведомления или звонок будильника)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        // ...и именно музыку (а не трек фильма или речь)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build();
                audioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                        .setOnAudioFocusChangeListener(audioFocusChangeListener)
                        // Если получить фокус не удалось, ничего не делаем
                        // Если true - нам выдадут фокус как только это будет возможно
                        // (например, закончится телефонный разговор)
                        .setAcceptsDelayedFocusGain(false)
                        // Вместо уменьшения громкости собираемся вставать на паузу
                        .setWillPauseWhenDucked(true)
                        .setAudioAttributes(audioAttributes)
                        .build();

                audioFocusResult = mAudioManager.requestAudioFocus(audioFocusRequest);
            }
            else {
                audioFocusResult = mAudioManager.requestAudioFocus(
                        audioFocusChangeListener,
                        AudioManager.STREAM_MUSIC,
                        AudioManager.AUDIOFOCUS_GAIN);
            }

            if (audioFocusResult != AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
                return;

            Log.d(TAG, "onPlay: audiofocus granted" );


            // Указываем, что наше приложение теперь активный плеер и кнопки
            // на окне блокировки должны управлять именно нами
            mediaSession.setActive(true);

            // Сообщаем новое состояние
            mediaSession.setPlaybackState(
                    stateBuilder.setState(PlaybackStateCompat.STATE_PLAYING,
                            PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 1).build());
        }

        @Override
        public void onStop() {

            Log.d(TAG, "onStop: ");
            mAudioManager.abandonAudioFocus(audioFocusChangeListener);
            // Все, больше мы не "главный" плеер, уходим со сцены
            mediaSession.setActive(false);

            // Сообщаем новое состояние
            mediaSession.setPlaybackState(
                    stateBuilder.setState(PlaybackStateCompat.STATE_STOPPED,
                            PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 1).build());
        }
    };

    Notification getNotification(int playbackState) {
        // MediaStyleHelper заполняет уведомление метаданными трека.
        // Хелпер любезно написал Ian Lake / Android Framework Developer at Google
        // и выложил здесь: https://gist.github.com/ianhanniballake/47617ec3488e0257325c
        NotificationCompat.Builder builder = MediaStyleHelper.from(this, mediaSession, TAG);

        // Добавляем кнопки

        // ...play/pause
        if (playbackState == PlaybackStateCompat.STATE_PLAYING)
            builder.addAction(
                    new NotificationCompat.Action(
                            android.R.drawable.ic_media_pause, "pause",
                            MediaButtonReceiver.buildMediaButtonPendingIntent(
                                    this,
                                    PlaybackStateCompat.ACTION_PLAY_PAUSE)));
        else
            builder.addAction(
                    new NotificationCompat.Action(
                            android.R.drawable.ic_media_play, "play",
                            MediaButtonReceiver.buildMediaButtonPendingIntent(
                                    this,
                                    PlaybackStateCompat.ACTION_PLAY_PAUSE)));

        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setColor(ContextCompat.getColor(this, R.color.colorPrimaryDark));

        // Не отображать время создания уведомления. В нашем случае это не имеет смысла
        builder.setShowWhen(false);

        // Это важно. Без этой строчки уведомления не отображаются на Android Wear
        // и криво отображаются на самом телефоне.
        builder.setPriority(NotificationCompat.PRIORITY_HIGH);

        // Не надо каждый раз вываливать уведомление на пользователя
        builder.setOnlyAlertOnce(true);

        return builder.build();
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

        // Ресурсы освобождать обязательно
        mediaSession.release();
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

        return (mediaSession == null? null: mediaSession.getSessionToken());
    }

    private enum Command implements Serializable {

        CHECK_FOREGROUND,
        START,
        STOP,
        DEFAULT
    }
}
