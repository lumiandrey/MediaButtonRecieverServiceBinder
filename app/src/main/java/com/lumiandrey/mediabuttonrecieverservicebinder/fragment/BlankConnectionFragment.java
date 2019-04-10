package com.lumiandrey.mediabuttonrecieverservicebinder.fragment;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.lumiandrey.mediabuttonrecieverservicebinder.service.MediaButtonListenerService;
import com.lumiandrey.mediabuttonrecieverservicebinder.service.MediaButtonListenerServiceBinder;
import com.lumiandrey.mediabuttonrecieverservicebinder.service.PlayerService;

import static android.content.Context.BIND_AUTO_CREATE;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link BlankConnectionFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class BlankConnectionFragment
        extends Fragment
        implements ServiceConnection {

    public static final String TAG = BlankConnectionFragment.class.getName();

    private static final String ARG_PARAM1 = TAG + "param1";
    private static final String ARG_PARAM2 = TAG + "param2";

    private String mParam1;
    private String mParam2;

    @Nullable
    private PlayerService.PlayerServiceBinder playerServiceBinder;
    @Nullable
    private MediaControllerCompat mediaController;

    private MediaControllerCompat.Callback callback;

    Button playButton = null;
    Button pauseButton = null;
    Button stopButton = null;
    public BlankConnectionFragment() {
        // Required empty public constructor
    }

    public static BlankConnectionFragment newInstance(String param1, String param2) {
        BlankConnectionFragment fragment = new BlankConnectionFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }

        callback = new MediaControllerCompat.Callback() {
            @Override
            public void onPlaybackStateChanged(PlaybackStateCompat state) {
                if (state == null)
                    return;
                boolean playing = state.getState() == PlaybackStateCompat.STATE_PLAYING;
                Log.d(TAG, "onPlaybackStateChanged: playing " + playing);
            }
        };
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        TextView textView = new TextView(getActivity());
        textView.setText(mParam1 + " " + mParam2);

        LinearLayout linearLayout = new LinearLayout(getContext());

        linearLayout.setOrientation(LinearLayout.VERTICAL);

        linearLayout.addView(textView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        playButton = new Button(getContext());
        playButton.setText("play");
        pauseButton = new Button(getContext());
        pauseButton.setText("pause");
        stopButton = new Button(getContext());
        stopButton.setText("stop");

        linearLayout.addView(playButton, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        linearLayout.addView(pauseButton, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        linearLayout.addView(stopButton, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        playButton.setOnClickListener(v -> {
            if (mediaController != null)
                mediaController.getTransportControls().play();
        });

        pauseButton.setOnClickListener(v -> {
            if (mediaController != null)
                mediaController.getTransportControls().pause();
        });

        stopButton.setOnClickListener(v -> {
            if (mediaController != null)
                mediaController.getTransportControls().stop();
        });

        getContext().bindService(new Intent(getContext(), PlayerService.class), this, BIND_AUTO_CREATE);

        return linearLayout;
    }

    @Override
    public void onStart() {
        super.onStart();

       /* if(getContext() != null && playerServiceBinder == null)
            MediaButtonListenerService.bindingMediaButtonListenerService(getContext(), this);*/
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mediaController != null)
            mediaController.getTransportControls().play();
    }

    @Override
    public void onStop() {
        super.onStop();
/*

        if(getContext() != null)
            MediaButtonListenerService.unbindingMediaButtonListenerService(getContext(), this);
*/

        playerServiceBinder = null;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (mediaController != null) {
            mediaController.unregisterCallback(callback);
            mediaController = null;
        }

        if(getContext() != null && playerServiceBinder != null) {
            getContext().unbindService(this);
            playerServiceBinder = null;
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {

        if(service instanceof PlayerService.PlayerServiceBinder){

            playerServiceBinder = (PlayerService.PlayerServiceBinder) service;

            try {
                mediaController = new MediaControllerCompat(
                        getContext(), playerServiceBinder.getMediaSessionToken());
                mediaController.registerCallback(callback);


            } catch (RemoteException e) {
                mediaController = null;
            }
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {

        playerServiceBinder = null;

        if (mediaController != null) {
            mediaController.unregisterCallback(callback);
            mediaController = null;
        }
    }
}
