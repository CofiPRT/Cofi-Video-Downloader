package com.example.cofivideodownloader.downloaders.misc;

public class VideoMetadata {

    private final VideoType videoType;
    private final String title;
    private final String thumbnailURL;

    public VideoMetadata(VideoType videoType, String title, String thumbnailURL) {
        this.videoType = videoType;
        this.title = title;
        this.thumbnailURL = thumbnailURL;
    }

    public VideoType getVideoType() {
        return videoType;
    }

    public String getTitle() {
        return title;
    }

    public String getThumbnailURL() {
        return thumbnailURL;
    }

}
