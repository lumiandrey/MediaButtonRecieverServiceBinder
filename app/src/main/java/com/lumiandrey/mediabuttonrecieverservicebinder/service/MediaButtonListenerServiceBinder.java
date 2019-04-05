package com.lumiandrey.mediabuttonrecieverservicebinder.service;

import android.os.Binder;
import android.support.annotation.NonNull;

public class MediaButtonListenerServiceBinder extends Binder {

    @NonNull
    private final MediaButtonListenerService mMediaButtonListenerService;

    MediaButtonListenerServiceBinder(
            @NonNull final MediaButtonListenerService mediaButtonListenerService
    ) {

        mMediaButtonListenerService = mediaButtonListenerService;
    }
}
