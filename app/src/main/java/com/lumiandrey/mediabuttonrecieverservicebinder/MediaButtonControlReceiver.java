package com.lumiandrey.mediabuttonrecieverservicebinder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;

public class MediaButtonControlReceiver extends BroadcastReceiver {

    public static final String TAG = MediaButtonControlReceiver.class.getName();

    static final long CLICK_DELAY = 500;
    static long lastClick = 0; // oldValue
    static long currentClick = System.currentTimeMillis();

    private Runnable mRunnable = ()->{

        if(currentClick - lastClick > CLICK_DELAY) {
            Log.d(TAG, "This is single click is play/pause balbatun");
        }
    };

    private Handler mHandler = new Handler();

    @Override
    public void onReceive(Context context, Intent intent) {

        Log.d(TAG, "onReceive " + intent);

        if (Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())) {

            KeyEvent event = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);

            if (event.getAction() != KeyEvent.ACTION_DOWN)return;

            lastClick = currentClick ;

            currentClick = System.currentTimeMillis();

            if(currentClick - lastClick < CLICK_DELAY ){

                mHandler.removeCallbacks(mRunnable);

                Log.d(TAG, "This is double click is record");

            } else {

                mHandler.postDelayed(mRunnable, CLICK_DELAY);
            }


            Log.d(TAG, "key " + MediaButtonHelper.getKeyName(event) + "key event " + (event.getAction() == KeyEvent.ACTION_UP ? "KeyEvent.ACTION_UP" : "keyEvent.Action_Down") );
        }

        Log.d(TAG, "action " + intent.getAction());
    }


}
