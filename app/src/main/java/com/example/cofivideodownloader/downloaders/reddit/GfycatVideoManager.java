package com.example.cofivideodownloader.downloaders.reddit;

import android.util.Log;
import com.example.cofivideodownloader.MainActivity;
import com.example.cofivideodownloader.downloaders.misc.FileType;
import com.example.cofivideodownloader.downloaders.misc.JsonPathException;
import com.example.cofivideodownloader.downloaders.misc.JsonUtil;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class GfycatVideoManager extends RedditVideoManager {
    private static final String TAG = "GfycatVideoManager";

    private String videoUrl;

    public GfycatVideoManager(MainActivity activity, JsonObject data) {
        super(activity, data);
    }

    @Override
    public DomainManagerMetadata computeMetadataPart() {
        String gfycatLink;

        // get the gfycat video url
        try {
            gfycatLink = JsonUtil.getString(data, "url");
        } catch (JsonPathException e) {
            Log.e(TAG, "Invalid JSON Response", e);
            return null;
        }

        // the URL is in the form of "https://gfycat.com/<video_id>" - retrieve the video id
        String videoId = gfycatLink.substring(gfycatLink.lastIndexOf('/') + 1);

        // send a request to the gfycat API to get the video URL
        HttpsURLConnection connection = null;

        try {
            URL gfycatURL = new URL("https://api.gfycat.com/v1/gfycats/" + videoId);

            // establish the connection
            connection = (HttpsURLConnection) gfycatURL.openConnection();

            int responseCode = connection.getResponseCode();
            Log.i(TAG, "Gfycat response code: " + responseCode);

            if (responseCode != HttpURLConnection.HTTP_OK)
                return null;

            return parseGfycatResponse(JsonUtil.parseJSON(connection, activity));
        } catch (MalformedURLException e) {
            Log.e(TAG, "Malformed URL", e);
        } catch (IOException e) {
            Log.e(TAG, "IO Exception", e);
        } finally {
            if (connection != null)
                connection.disconnect();
        }

        return null;
    }

    private DomainManagerMetadata parseGfycatResponse(JsonElement response) {
        try {
            videoUrl = JsonUtil.getString(response, "gfyItem.mp4Url");

            boolean hasAudio = JsonUtil.getBoolean(response, "gfyItem.hasAudio");

            return new DomainManagerMetadata(FileType.VIDEO, true, hasAudio);
        } catch (JsonPathException e) {
            Log.e(TAG, "Invalid JSON Response", e);
        }

        return null;
    }

    @Override
    public boolean downloadVideo(String filename) {
        try {
            URL gifURL = new URL(videoUrl);
            return downloadFile(gifURL, filename, 0, 1);
        } catch (MalformedURLException e) {
            Log.e(TAG, "Malformed URL", e);
        }

        return false;
    }

}
