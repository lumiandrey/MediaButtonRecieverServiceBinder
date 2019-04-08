package com.lumiandrey.mediabuttonrecieverservicebinder.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.lumiandrey.mediabuttonrecieverservicebinder.MainActivity;
import com.lumiandrey.mediabuttonrecieverservicebinder.MediaButtonControlReceiver;
import com.lumiandrey.mediabuttonrecieverservicebinder.R;

import java.io.Serializable;
import java.util.UUID;

public class MediaButtonListenerService extends Service {

    public static final String TAG = MediaButtonListenerService.class.getName();
    public static final String NAME_COMMAND = TAG + "Command";

    private static final int ID_FOREGROUND_NOTIFICATION = (TAG.hashCode() > 0 ? -1*TAG.hashCode() : TAG.hashCode());

    private int countBindingUser = 0;

    @Nullable
    private AudioManager mAudioManager;
    @Nullable
    private ComponentName mRemoteControlResponder;

    @NonNull
    private MediaButtonListenerServiceBinder mButtonListenerServiceBinder = new MediaButtonListenerServiceBinder(this);

    @Override
    public void onCreate() {
        super.onCreate();

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

                    if(mAudioManager == null || mRemoteControlResponder == null){
                        startListen();
                    }

                } break;
                case START:{

                    startListen();

                } break;
                case STOP: {


                    Log.d(TAG, "onStartCommand: stop listener");

                    mAudioManager.unregisterMediaButtonEventReceiver(
                            mRemoteControlResponder);

                    mAudioManager = null;
                    mRemoteControlResponder = null;

                    stopForeground(true);
                    stopSelf();
                } break;
            }
        }

        return super.onStartCommand(intent, flags, startId);
    }

    private void startListen() {

        Log.d(TAG, "startListen");
        mAudioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        mRemoteControlResponder = new ComponentName(getPackageName(),
                MediaButtonControlReceiver.class.getName());

        mAudioManager.registerMediaButtonEventReceiver(
                mRemoteControlResponder);
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

    private enum Command implements Serializable {

        CHECK_FOREGROUND,
        START,
        STOP,
        DEFAULT
    }
}
