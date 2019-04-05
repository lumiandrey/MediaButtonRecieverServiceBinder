package com.lumiandrey.mediabuttonrecieverservicebinder.fragment;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.lumiandrey.mediabuttonrecieverservicebinder.service.MediaButtonListenerService;
import com.lumiandrey.mediabuttonrecieverservicebinder.service.MediaButtonListenerServiceBinder;

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
    private MediaButtonListenerServiceBinder mButtonListenerServiceBinder = null;

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
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        TextView textView = new TextView(getActivity());
        textView.setText(mParam1 + " " + mParam2);

        return textView;
    }

    @Override
    public void onStart() {
        super.onStart();

        if(getContext() != null && mButtonListenerServiceBinder == null)
            MediaButtonListenerService.bindingMediaButtonListenerService(getContext(), this);
    }

    @Override
    public void onStop() {
        super.onStop();

        if(getContext() != null)
            MediaButtonListenerService.unbindingMediaButtonListenerService(getContext(), this);

        mButtonListenerServiceBinder = null;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {

        if(service instanceof MediaButtonListenerServiceBinder){

            mButtonListenerServiceBinder = (MediaButtonListenerServiceBinder) service;
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {

        mButtonListenerServiceBinder = null;
    }
}
