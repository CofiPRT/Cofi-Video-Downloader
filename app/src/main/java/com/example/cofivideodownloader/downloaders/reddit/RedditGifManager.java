package com.example.cofivideodownloader.downloaders.reddit;

import android.util.Log;
import com.example.cofivideodownloader.MainActivity;
import com.example.cofivideodownloader.downloaders.misc.JsonPathException;
import com.example.cofivideodownloader.downloaders.misc.JsonUtil;
import com.example.cofivideodownloader.downloaders.misc.VideoType;
import com.google.gson.JsonObject;

import java.net.MalformedURLException;
import java.net.URL;

public class RedditGifManager extends RedditVideoManager {
    private static final String TAG = "RedditGifManager";

    private String gifUrl;

    public RedditGifManager(MainActivity activity, JsonObject data) {
        super(activity, data);
    }

    @Override
    public VideoType computeVideoType() {
        try {
            // "gif" must exist under "preview.images[0].variants"
            boolean isGif = JsonUtil.has(data, "preview.images[0].variants.gif");

            if (!isGif) {
                activity.showToast("Invalid link (Not a GIF)");
                return null;
            }

            gifUrl = JsonUtil.getString(data, "url");

            return VideoType.GIF;
        } catch (JsonPathException e) {
            Log.e(TAG, "Invalid JSON Response", e);
        }

        return null;
    }

    @Override
    public boolean downloadVideo(String filename) {
        try {
            URL gifURL = new URL(gifUrl);
            return downloadFile(gifURL, filename, 0, 1);
        } catch (MalformedURLException e) {
            Log.e(TAG, "Malformed URL", e);
        }

        return false;
    }

}
