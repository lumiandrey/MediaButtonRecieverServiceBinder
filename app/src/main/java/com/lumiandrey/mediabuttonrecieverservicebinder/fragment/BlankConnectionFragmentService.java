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
import android.support.v4.media.session.MediaSessionCompat;
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
 * Use the {@link BlankConnectionFragmentService#newInstance} factory method to
 * create an instance of this fragment.
 */
public class BlankConnectionFragmentService
        extends Fragment
        implements ServiceConnection {

    public static final String TAG = BlankConnectionFragmentService.class.getName();

    private static final String ARG_PARAM1 = TAG + "param1";
    private static final String ARG_PARAM2 = TAG + "param2";

    private String mParam1;
    private String mParam2;

    private Button playButton = null;


    @Nullable
    private MediaButtonListenerServiceBinder playerServiceBinder;
    @Nullable
    private MediaControllerCompat mediaController;

    private MediaControllerCompat.Callback callback;

    public BlankConnectionFragmentService() {
        // Required empty public constructor
    }

    public static BlankConnectionFragmentService newInstance(String param1, String param2) {
        BlankConnectionFragmentService fragment = new BlankConnectionFragmentService();
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

        linearLayout.addView(playButton, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        playButton.setOnClickListener(v -> {

            Log.d(TAG, "onCreateView: play click");
            if (mediaController != null)
                mediaController.getTransportControls().play();
        });

        getContext().bindService(new Intent(getContext(), MediaButtonListenerService.class), this, BIND_AUTO_CREATE);

        getContext().startService(new Intent(getContext(), MediaButtonListenerService.class));

        return linearLayout;
    }

    @Override
    public void onStart() {
        super.onStart();

    }

    @Override
    public void onResume() {
        super.onResume();

    }

    @Override
    public void onStop() {
        super.onStop();

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

        if(service instanceof MediaButtonListenerServiceBinder){

            playerServiceBinder = (MediaButtonListenerServiceBinder) service;

            try {
                MediaSessionCompat.Token sessionToken = playerServiceBinder.getMediaSessionToken();

                if(sessionToken != null) {
                    mediaController = new MediaControllerCompat(
                            getContext(), sessionToken);
                    mediaController.registerCallback(callback);
                }


            } catch (RemoteException e) {
                mediaController = null;
            }

            Log.d(TAG, "onServiceConnected: ");
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
