package com.lumiandrey.mediabuttonrecieverservicebinder.service;

import android.os.Binder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.media.session.MediaSessionCompat;

public class MediaButtonListenerServiceBinder extends Binder {

    @NonNull
    private final MediaButtonListenerService mMediaButtonListenerService;

    MediaButtonListenerServiceBinder(
            @NonNull final MediaButtonListenerService mediaButtonListenerService
    ) {

        mMediaButtonListenerService = mediaButtonListenerService;
    }

    @Nullable
    public MediaSessionCompat.Token getMediaSessionToken() {
        return mMediaButtonListenerService.getSessionToken();
    }
}
