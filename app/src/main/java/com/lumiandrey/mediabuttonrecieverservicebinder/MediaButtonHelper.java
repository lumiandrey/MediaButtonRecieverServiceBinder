package com.lumiandrey.mediabuttonrecieverservicebinder;

import android.content.Intent;
import android.util.Log;
import android.view.KeyEvent;

public class MediaButtonHelper {

    public static String getKeyName(Intent intent){
        if (Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())) {

            KeyEvent event = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);

            return getKeyName(event);
        }
        return "No action media button";
    }

    public static String getKeyName(KeyEvent event) {

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
                buffer.append("Not found");
        }

        return buffer.toString();
    }
}
