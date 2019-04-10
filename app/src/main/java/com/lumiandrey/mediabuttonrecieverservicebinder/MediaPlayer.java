package com.lumiandrey.mediabuttonrecieverservicebinder;

public class MediaPlayer {

    private boolean isPlaying;

    public boolean isPlaying() {
        return isPlaying;
    }

    public void start(){

        isPlaying = true;
    }

    public void stop(){

        isPlaying = false;
    }
}
