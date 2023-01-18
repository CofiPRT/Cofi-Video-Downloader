package com.example.cofivideodownloader.downloaders;

public class VideoMetadata {

    private final String title;
    private final String thumbnailURL;

    public VideoMetadata(String title, String thumbnailURL) {
        this.title = title;
        this.thumbnailURL = thumbnailURL;
    }

    public String getTitle() {
        return title;
    }

    public String getThumbnailURL() {
        return thumbnailURL;
    }

}
