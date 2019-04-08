package com.lumiandrey.mediabuttonrecieverservicebinder;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.widget.TextView;

import com.lumiandrey.mediabuttonrecieverservicebinder.fragment.BlankConnectionFragment;
import com.lumiandrey.mediabuttonrecieverservicebinder.fragment.BlankNoConnectionFragment;

public class MainActivity extends AppCompatActivity {


    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = item -> {
                switch (item.getItemId()) {
                    case R.id.navigation_home:

                        getSupportFragmentManager()
                                .beginTransaction()
                                .replace(R.id.frame_fragment_container, BlankNoConnectionFragment.newInstance("HOME ", "No connection"))
                                .commit();

                        return true;
                    case R.id.navigation_dashboard:


                        getSupportFragmentManager()
                                .beginTransaction()
                                .replace(R.id.frame_fragment_container, BlankConnectionFragment.newInstance("Navigation ", "Connection"))
                                .commit();

                        return true;
                    case R.id.navigation_notifications:

                        getSupportFragmentManager()
                                .beginTransaction()
                                .replace(R.id.frame_fragment_container, BlankNoConnectionFragment.newInstance("Notification ", "No connection"))
                                .commit();

                        return true;
                }

                return false;
            };

    private AudioManager mAudioManager;
    private ComponentName mRemoteControlResponder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView navigation = findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
    }


    @Override
    protected void onStart() {
        super.onStart();

        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        /*mAudioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        mRemoteControlResponder = new ComponentName(getPackageName(),
                MediaButtonControlReceiver.class.getName());

        mAudioManager.registerMediaButtonEventReceiver(
                mRemoteControlResponder);*/
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
/*
        mAudioManager.unregisterMediaButtonEventReceiver(
                mRemoteControlResponder);

        mAudioManager = null;
        mRemoteControlResponder = null;*/
    }
}
