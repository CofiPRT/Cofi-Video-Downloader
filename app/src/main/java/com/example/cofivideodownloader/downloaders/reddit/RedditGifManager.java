package com.example.cofivideodownloader.downloaders.reddit;

import android.util.Log;
import com.example.cofivideodownloader.MainActivity;
import com.example.cofivideodownloader.downloaders.misc.FileType;
import com.example.cofivideodownloader.downloaders.misc.JsonPathException;
import com.example.cofivideodownloader.downloaders.misc.JsonUtil;
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
    public DomainManagerMetadata computeMetadataPart() {
        try {
            String url = JsonUtil.getString(data, "url");

            // if the url ends with ".gifv", replace it with ".gif"
            if (url.endsWith(".gifv"))
                url = url.substring(0, url.length() - 1);

            // if the url doesn't end with ".gif", it's not a valid link
            if (!url.endsWith(".gif")) {
                activity.showToast("Invalid link (Not a GIF)");
                return null;
            }

            gifUrl = url;

            return new DomainManagerMetadata(FileType.GIF, true, false);
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
