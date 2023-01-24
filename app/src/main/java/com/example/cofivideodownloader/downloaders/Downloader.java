package com.example.cofivideodownloader.downloaders;

import com.example.cofivideodownloader.MainActivity;
import com.example.cofivideodownloader.downloaders.misc.VideoMetadata;

import java.net.URL;

public abstract class Downloader {

    private final URL url;
    protected final MainActivity activity;

    protected Downloader(URL url, MainActivity activity) {
        this.url = url;
        this.activity = activity;
    }

    public String getLink() {
        return url.toString();
    }

    public URL getURL() {
        return url;
    }

    public abstract VideoMetadata getVideoMetadata();

    public abstract boolean downloadVideo(String filename);

}
