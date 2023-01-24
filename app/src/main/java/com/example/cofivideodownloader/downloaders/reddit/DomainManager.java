package com.example.cofivideodownloader.downloaders.reddit;

import android.util.Log;
import com.example.cofivideodownloader.MainActivity;
import com.example.cofivideodownloader.downloaders.misc.VideoType;
import com.google.gson.JsonObject;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

public abstract class DomainManager {
    private static final String TAG = "RedditDomainManager";

    private static final int BUFFER_SIZE = 4096;

    protected final MainActivity activity;
    protected final JsonObject data;

    protected DomainManager(MainActivity activity, JsonObject data) {
        this.activity = activity;
        this.data = data;
    }

    public abstract VideoType computeVideoType();

    public abstract boolean downloadVideo(String filename);

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    protected boolean downloadFile(URL fileURL, String destinationFilename, int startProgress, double ratio) {
        HttpsURLConnection connection = null;

        try (OutputStream outputStream = Files.newOutputStream(Paths.get(destinationFilename))) {
            // establish the connection
            connection = (HttpsURLConnection) fileURL.openConnection();

            int responseCode = connection.getResponseCode();
            Log.i(TAG, "Response code: " + responseCode);

            if (responseCode != HttpURLConnection.HTTP_OK)
                return false;

            long fileSize = connection.getContentLengthLong();

            InputStream inputStream = connection.getInputStream();

            byte[] data = new byte[BUFFER_SIZE];
            long total = 0;
            int count;
            while ((count = inputStream.read(data)) != -1) {
                total += count;
                activity.updateDownloadProgress(startProgress + (int) (total * ratio * 100 / fileSize));
                outputStream.write(data, 0, count);
            }

            return true;
        } catch (IOException e) {
            activity.logToast(TAG, "IO Exception", e);
        } finally {
            if (connection != null)
                connection.disconnect();
        }

        return false;
    }
}
