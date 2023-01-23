package com.example.cofivideodownloader.downloaders;

import android.util.Log;
import com.arthenica.ffmpegkit.FFmpegKit;
import com.arthenica.ffmpegkit.FFmpegSession;
import com.arthenica.ffmpegkit.ReturnCode;
import com.example.cofivideodownloader.MainActivity;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

public class RedditDownloader extends Downloader {

    private static final String TAG = "RedditDownloader";

    private static final String REDDIT_JSON_SUFFIX = ".json";
    private static final int BUFFER_SIZE = 4096;
    private static final int VIDEO_TO_AUDIO_RATIO = 3;

    // to be set by getVideoMetadata()
    private boolean isRedditMedia;
    private String videoUrl;
    private boolean isGif;

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

            return getRedditData(parseJSON(connection));
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

    private JsonElement parseJSON(HttpsURLConnection connection) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            StringBuilder builder = new StringBuilder();

            String line;
            while ((line = reader.readLine()) != null)
                builder.append(line).append(System.lineSeparator());

            return JsonParser.parseString(builder.toString());
        } catch (IOException e) {
            activity.logToast(TAG, "IO Exception", e);
        }

        return null;
    }

    private VideoMetadata getRedditData(JsonElement json) {
        if (json == null)
            return null;

        // under json[0].data.children[0].data
        try {
            JsonObject data = json.getAsJsonArray()
                .get(0).getAsJsonObject()
                .getAsJsonObject("data")
                .getAsJsonArray("children")
                .get(0).getAsJsonObject()
                .getAsJsonObject("data");

            String title = data.get("title").getAsString();
            String thumbnailUrl = data.get("thumbnail").getAsString();

            isRedditMedia = data.get("is_reddit_media_domain").getAsBoolean();

            if (isRedditMedia) {
                // save the video URL, under data.secure_media.reddit_video.fallback_url
                JsonObject redditVideo = data
                    .getAsJsonObject("secure_media")
                    .getAsJsonObject("reddit_video");
                videoUrl = redditVideo.get("fallback_url").getAsString();
                isGif = redditVideo.get("is_gif").getAsBoolean();
            } else {
                // save the video URL, under data.secure_media_embed.media_domain_url
                videoUrl = data.getAsJsonObject("secure_media_embed")
                    .get("media_domain_url").getAsString();
            }

            return new VideoMetadata(title, thumbnailUrl);
        } catch (Exception e) {
            activity.logToast(TAG, "Invalid JSON Response", e);
        }

        return null;
    }

    @Override
    public boolean downloadVideo(String filename) {
        return isRedditMedia || isGif ? downloadTwoPartVideo(filename) : downloadSinglePartVideo(filename);
    }

    private boolean downloadTwoPartVideo(String filename) {
        String tempVideoFilename = filename + ".video.temp";
        String tempAudioFilename = filename + ".audio.temp";

        try {
            // download the video
            URL tempVideoURL = new URL(videoUrl);
            double videoRatio = (double) VIDEO_TO_AUDIO_RATIO / (VIDEO_TO_AUDIO_RATIO + 1);

            if (!downloadFile(tempVideoURL, tempVideoFilename, 0, videoRatio)) {
                activity.showToast("Failed to download video");
                return false;
            }

            Log.i(TAG, "Video downloaded from " + videoUrl);

            // download the audio - the URL will contain at some point "DASH_<any_character>.mp4"
            // replace it with "DASH_audio.mp4"
            String audioUrl = videoUrl.replaceFirst("DASH_.*\\.mp4", "DASH_audio.mp4");
            URL tempAudioURL = new URL(audioUrl);
            double audioRatio = (double) 1 / (VIDEO_TO_AUDIO_RATIO + 1);

            if (!downloadFile(tempAudioURL, tempAudioFilename, (int) (100 * videoRatio), audioRatio)) {
                activity.showToast("Failed to download audio");
                return false;
            }

            Log.i(TAG, "Audio downloaded from " + audioUrl);

            // merge the video and audio using ffmpeg
            String command = "-i " + tempAudioFilename + " -i " + tempVideoFilename +
                             " -acodec copy -vcodec copy " + filename;

            FFmpegSession session = FFmpegKit.execute(command);
            if (ReturnCode.isSuccess(session.getReturnCode()))
                return true;

            // otherwise, log the error
            activity.logToast(TAG, "FFmpeg Error: " + session.getState(), session.getFailStackTrace());

            return false;
        } catch (MalformedURLException e) {
            activity.logToast(TAG, "Malformed URL", e);
        } finally {
            // delete the temporary files
            try {
                Files.deleteIfExists(Paths.get(tempVideoFilename));
                Files.deleteIfExists(Paths.get(tempAudioFilename));
            } catch (IOException e) {
                activity.logToast(TAG, "IO Exception", e);
            }
        }

        return false;
    }

    private boolean downloadSinglePartVideo(String filename) {
        try {
            URL videoURL = new URL(videoUrl);
            return downloadFile(videoURL, filename, 0, 1);
        } catch (MalformedURLException e) {
            activity.logToast(TAG, "Malformed URL", e);
        }

        return false;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean downloadFile(URL fileURL, String destinationFilename, int startProgress, double ratio) {
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
