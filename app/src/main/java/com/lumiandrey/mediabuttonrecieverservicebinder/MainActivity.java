package com.lumiandrey.mediabuttonrecieverservicebinder;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.FragmentActivity;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.widget.TextView;

import com.lumiandrey.mediabuttonrecieverservicebinder.fragment.BlankConnectionFragment;
import com.lumiandrey.mediabuttonrecieverservicebinder.fragment.BlankNoConnectionFragment;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = MainActivity.class.getName();

    private MediaSessionCompat _mediaSession;
    private MediaSessionCompat.Token _mediaSessionToken;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView navigation = findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        _mediaSession = new MediaSessionCompat(getApplicationContext(), MainActivity.class.getName() + ".__");

        if (_mediaSession == null) {
            Log.e(TAG, "initMediaSession: _mediaSession = null");
            return;
        }

        _mediaSessionToken = _mediaSession.getSessionToken();
        Log.d(TAG, "onCreate: " + _mediaSessionToken);

        _mediaSession.setCallback(new MediaSessionCompat.Callback() {
            public boolean onMediaButtonEvent(Intent mediaButtonIntent) {

                Log.d(TAG, "onMediaButtonEvent called: " + mediaButtonIntent);
                return super.onMediaButtonEvent(mediaButtonIntent);
            }
        });

        _mediaSession.setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                        MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

    }


    @Override
    protected void onStart() {
        super.onStart();

        PlaybackStateCompat state = new PlaybackStateCompat.Builder()
                .setActions(
                        PlaybackStateCompat.ACTION_PLAY |
                                PlaybackStateCompat.ACTION_PLAY_PAUSE |
                                PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID |
                                PlaybackStateCompat.ACTION_PAUSE |
                                PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
                                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)
                .setState(PlaybackStateCompat.STATE_PLAYING,
                        0, SystemClock.elapsedRealtime())
                .build();

        _mediaSession.setPlaybackState(state);

        _mediaSession.setActive(true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        _mediaSession.setActive(false);
        _mediaSession.release();
    }
}
