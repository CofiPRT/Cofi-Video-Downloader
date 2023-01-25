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
            // "gif" must exist under "preview.images[0].variants"
            boolean isGif = JsonUtil.hasPath(data, "preview.images[0].variants.gif");

            if (!isGif) {
                activity.showToast("Invalid link (Not a GIF)");
                return null;
            }

            gifUrl = JsonUtil.getString(data, "url");

            // if the url ends with ".gifv", replace it with ".gif"
            if (gifUrl.endsWith(".gifv"))
                gifUrl = gifUrl.substring(0, gifUrl.length() - 1);

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
