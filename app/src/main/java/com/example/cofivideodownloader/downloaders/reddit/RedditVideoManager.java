package com.example.cofivideodownloader.downloaders.reddit;

import android.util.Log;
import com.arthenica.ffmpegkit.FFmpegKit;
import com.arthenica.ffmpegkit.FFmpegSession;
import com.arthenica.ffmpegkit.ReturnCode;
import com.example.cofivideodownloader.MainActivity;
import com.example.cofivideodownloader.downloaders.misc.FileType;
import com.example.cofivideodownloader.downloaders.misc.JsonPathException;
import com.example.cofivideodownloader.downloaders.misc.JsonUtil;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

public class RedditVideoManager extends DomainManager {
    private static final String TAG = "RedditVideoManager";

    private static final int VIDEO_TO_AUDIO_RATIO = 3;

    private String videoUrl;

    public RedditVideoManager(MainActivity activity, JsonObject data) {
        super(activity, data);
    }

    @Override
    public DomainManagerMetadata computeMetadataPart() {
        try {
            videoUrl = JsonUtil.getString(data, "media.reddit_video.fallback_url");

            return new DomainManagerMetadata(FileType.VIDEO, true, true);
        } catch (JsonPathException e) {
            Log.e(TAG, "Invalid JSON Response", e);
        }

        return null;
    }

    @Override
    public boolean downloadVideo(String filename) {
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
}
