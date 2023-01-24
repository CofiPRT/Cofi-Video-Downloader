package com.example.cofivideodownloader.downloaders;

import android.util.Log;
import com.example.cofivideodownloader.MainActivity;
import com.example.cofivideodownloader.downloaders.misc.JsonPathException;
import com.example.cofivideodownloader.downloaders.misc.JsonUtil;
import com.example.cofivideodownloader.downloaders.misc.VideoMetadata;
import com.example.cofivideodownloader.downloaders.misc.VideoType;
import com.example.cofivideodownloader.downloaders.reddit.DomainManager;
import com.example.cofivideodownloader.downloaders.reddit.DomainManagerFactory;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class RedditDownloader extends Downloader {

    private static final String TAG = "RedditDownloader";

    private static final String REDDIT_JSON_SUFFIX = ".json";

    // to be set by getVideoMetadata()
    private DomainManager domainManager;

    public RedditDownloader(URL url, MainActivity activity) {
        super(url, activity);
    }

    @Override
    public VideoMetadata getVideoMetadata() {
        HttpsURLConnection connection = null;

        // make a get request on the URL - receive a JSON response
        try {
            // divide the link into parts
            String[] parts = getLink().split("/");

            int len = parts.length;

            if (parts[len - 1].contains("?")) {
                // if the last part has query params, add ".json" before them
                int lastQuestionMark = parts[len - 1].lastIndexOf("?");
                parts[len - 1] = parts[len - 1].substring(0, lastQuestionMark) +
                                 REDDIT_JSON_SUFFIX +
                                 parts[len - 1].substring(lastQuestionMark);
            } else if (parts[len - 1].isEmpty()) {
                // if the last part is empty, just add ".json"
                parts[len - 1] = REDDIT_JSON_SUFFIX;
            } else {
                // add another part with ".json"
                parts = java.util.Arrays.copyOf(parts, len + 1);
                parts[len] = REDDIT_JSON_SUFFIX;
            }

            // reconstruct the URL
            String newLink = String.join("/", parts);
            URL requestURL = new URL(newLink);

            // establish the connection
            connection = (HttpsURLConnection) requestURL.openConnection();

            int responseCode = connection.getResponseCode();
            Log.i(TAG, "Response code: " + responseCode);

            if (responseCode != HttpURLConnection.HTTP_OK)
                return null;

            return getRedditData(JsonUtil.parseJSON(connection, activity));
        } catch (MalformedURLException e) {
            activity.logToast(TAG, "Malformed URL", e);
        } catch (IOException e) {
            activity.logToast(TAG, "IO Exception", e);
        } finally {
            if (connection != null)
                connection.disconnect();
        }

        return null;
    }

    @Override
    public boolean downloadVideo(String filename) {
        return domainManager.downloadVideo(filename);
    }

    private VideoMetadata getRedditData(JsonElement json) {
        if (json == null)
            return null;

        try {
            JsonObject data = JsonUtil.getObject(json, "[0].data.children[0].data");

            String title = JsonUtil.getString(data, "title");
            String thumbnailUrl = JsonUtil.getString(data, "preview.images[0].source.url");

            // get the domain manager
            String domain = JsonUtil.getString(data, "domain");
            domainManager = DomainManagerFactory.getManager(domain, activity, data);
            if (domainManager == null) {
                activity.showToast("Unsupported domain: " + domain);
                return null;
            }

            VideoType videoType = domainManager.computeVideoType();
            if (videoType == null)
                return null;

            return new VideoMetadata(videoType, title, thumbnailUrl);
        } catch (JsonPathException e) {
            activity.logToast(TAG, "Invalid JSON Response", e);
        }

        return null;
    }

}
