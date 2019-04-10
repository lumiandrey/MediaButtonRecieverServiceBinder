package com.lumiandrey.mediabuttonrecieverservicebinder.service;

import android.net.Uri;

import com.lumiandrey.mediabuttonrecieverservicebinder.R;

final public class MusicRepository {
    private final Track[] data = {
            new Track("Triangle", "Jason Shaw", R.drawable.ic_dashboard_black_24dp, Uri.parse("https://freepd.com/music/Amazing Grace.mp3"), (3 * 60 + 9) * 1000),
            new Track("Rubix Cube", "Jason Shaw", R.drawable.ic_home_black_24dp, Uri.parse("https://freepd.com/music/Amazing Grace.mp3"), (3 * 60 + 44) * 1000),
            new Track("MC Ballad S Early Eighties", "Frank Nora", R.drawable.ic_launcher_foreground, Uri.parse("https://freepd.com/music/Brothers Unite.mp3"), (2 * 60 + 50) * 1000),
            new Track("Folk Song", "Brian Boyko", R.drawable.ic_notifications_black_24dp, Uri.parse("https://freepd.com/Acoustic/Folk Song.mp3"), (3 * 60 + 5) * 1000),
            new Track("Morning Snowflake", "Kevin MacLeod", R.drawable.ic_home_black_24dp, Uri.parse("https://freepd.com/Acoustic/Morning Snowflake.mp3"), (2 * 60 + 0) * 1000),
    };

    private final int maxIndex = data.length - 1;
    private int currentItemIndex = 0;

    Track getNext() {
        if (currentItemIndex == maxIndex)
            currentItemIndex = 0;
        else
            currentItemIndex++;
        return getCurrent();
    }

    Track getPrevious() {
        if (currentItemIndex == 0)
            currentItemIndex = maxIndex;
        else
            currentItemIndex--;
        return getCurrent();
    }

    Track getCurrent() {
        return data[currentItemIndex];
    }

    static class Track {

        private String title;
        private String artist;
        private int bitmapResId;
        private Uri uri;
        private long duration; // in ms

        Track(String title, String artist, int bitmapResId, Uri uri, long duration) {
            this.title = title;
            this.artist = artist;
            this.bitmapResId = bitmapResId;
            this.uri = uri;
            this.duration = duration;
        }

        String getTitle() {
            return title;
        }

        String getArtist() {
            return artist;
        }

        int getBitmapResId() {
            return bitmapResId;
        }

        Uri getUri() {
            return uri;
        }

        long getDuration() {
            return duration;
        }
    }
}
