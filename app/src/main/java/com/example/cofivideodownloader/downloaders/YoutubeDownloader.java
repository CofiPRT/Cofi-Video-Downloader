package com.example.cofivideodownloader.downloaders;

import com.example.cofivideodownloader.MainActivity;
import com.example.cofivideodownloader.downloaders.misc.FileType;
import com.example.cofivideodownloader.downloaders.misc.VideoMetadata;
import com.yausername.youtubedl_android.YoutubeDL;
import com.yausername.youtubedl_android.YoutubeDLException;
import com.yausername.youtubedl_android.YoutubeDLRequest;
import com.yausername.youtubedl_android.mapper.VideoInfo;

import java.net.URL;

public class YoutubeDownloader extends Downloader {

    private static final String TAG = "YoutubeDownloader";

    public YoutubeDownloader(URL url, MainActivity activity) {
        super(url, activity);
    }

    @Override
    public VideoMetadata getVideoMetadata() {
        try {
            VideoInfo info = YoutubeDL.getInstance().getInfo(getLink());
            String title = info.getTitle();
            String thumbnailUrl = info.getThumbnail();

            return new VideoMetadata(FileType.VIDEO, title, thumbnailUrl, false, true);
        } catch (YoutubeDLException e) {
            activity.logToast(TAG, "Video not found", e);
        } catch (InterruptedException e) {
            activity.logToast(TAG, "Interrupted", e);
            Thread.currentThread().interrupt();
        }

        return null;
    }

    @Override
    public boolean downloadVideo(String filename) {
        // create the file
        YoutubeDLRequest request = new YoutubeDLRequest(getLink());
        request.addOption("-o", filename);

        // start the download
        try {
            YoutubeDL.getInstance().execute(
                request, (progress, etaInSeconds, outputLine) -> activity.updateDownloadProgress((int) progress)
            );

            return true;
        } catch (YoutubeDLException e) {
            activity.logToast(TAG, "Video download failed", e);
        } catch (InterruptedException e) {
            activity.logToast(TAG, "Interrupted", e);
            Thread.currentThread().interrupt();
        }

        return false;
    }

}
