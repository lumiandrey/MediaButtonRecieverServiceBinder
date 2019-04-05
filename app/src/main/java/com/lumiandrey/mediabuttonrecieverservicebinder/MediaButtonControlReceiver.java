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


            Log.d(TAG, "key " + getKeyName(event) + "key event " + (event.getAction() == KeyEvent.ACTION_UP ? "KeyEvent.ACTION_UP" : "keyEvent.Action_Down") );
        }

        Log.d(TAG, "action " + intent.getAction());
    }

    private String getKeyName(KeyEvent event) {

        StringBuilder buffer = new StringBuilder();

        switch (event.getKeyCode()) {

            case KeyEvent.KEYCODE_MEDIA_STOP: {
                buffer.append("KEYCODE_MEDIA_STOP");

            } break;
            case KeyEvent.KEYCODE_MEDIA_PLAY: {

                buffer.append("KEYCODE_MEDIA_PLAY");
            }
            break;
            case KeyEvent.KEYCODE_MEDIA_PAUSE: {
                buffer.append("KEYCODE_MEDIA_PLAY");
            }
            break;
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:{

                buffer.append("KEYCODE_MEDIA_PLAY_PAUSE");
            } break;
            case KeyEvent.KEYCODE_HEADSETHOOK:{

                buffer.append("KEYCODE_HEADSETHOOK");
            } break;

            //--------------- BEGIN MEDIA_STEP -----------------//
            case KeyEvent.KEYCODE_MEDIA_STEP_FORWARD:{
                buffer.append("KEYCODE_MEDIA_STEP_FORWARD");

            } break;
            case KeyEvent.KEYCODE_MEDIA_STEP_BACKWARD:{
                buffer.append("KEYCODE_MEDIA_STEP_BACKWARD");

            } break;
            //---------------END MEDIA_STEP -----------------//

            //--------------- BEGIN MEDIA_SKIP -----------------//
            case KeyEvent.KEYCODE_MEDIA_SKIP_FORWARD:{
                buffer.append("KEYCODE_MEDIA_SKIP_FORWARD");

            } break;
            case KeyEvent.KEYCODE_MEDIA_SKIP_BACKWARD:{
                buffer.append("KEYCODE_MEDIA_SKIP_BACKWARD");

            } break;
            //---------------END MEDIA_SKIP -----------------//

            case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:{
                buffer.append("KEYCODE_MEDIA_FAST_FORWARD");

            } break;
            case KeyEvent.KEYCODE_MEDIA_RECORD:{
                buffer.append("KEYCODE_MEDIA_RECORD");

            }break;

            case KeyEvent.KEYCODE_MEDIA_NEXT:{
                buffer.append("KEYCODE_MEDIA_STEP_FORWARD");

            } break;
            case KeyEvent.KEYCODE_MEDIA_PREVIOUS:{
                buffer.append("KEYCODE_MEDIA_PREVIOUS");

            } break;

            case KeyEvent.KEYCODE_CHANNEL_DOWN:{

                buffer.append("KEYCODE_CHANNEL_DOWN");

            } break;
            case KeyEvent.KEYCODE_CHANNEL_UP:{
                buffer.append("KEYCODE_CHANNEL_UP");

            } break;

            case KeyEvent.KEYCODE_VOLUME_UP:{

                buffer.append("KEYCODE_VOLUME_UP");
            } break;
            case KeyEvent.KEYCODE_VOLUME_DOWN:{

                buffer.append("KEYCODE_VOLUME_DOWN");
            } break;

            default:
                buffer.append("Default");
        }

        return buffer.toString();
    }
}
