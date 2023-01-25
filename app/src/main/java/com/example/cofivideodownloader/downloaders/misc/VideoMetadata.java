package com.example.cofivideodownloader.downloaders.misc;

public class VideoMetadata {

    private final FileType fileType;
    private final boolean canConvertVideo;
    private final boolean canConvertToAudio;
    private final String title;
    private final String thumbnailURL;

    public VideoMetadata(
        FileType fileType, String title, String thumbnailURL, boolean canConvertVideo, boolean canConvertToAudio
    ) {
        this.fileType = fileType;
        this.title = title;
        this.thumbnailURL = thumbnailURL;
        this.canConvertVideo = canConvertVideo;
        this.canConvertToAudio = canConvertToAudio;
    }

    public FileType getFileType() {
        return fileType;
    }

    public String getTitle() {
        return title;
    }

    public String getThumbnailURL() {
        return thumbnailURL;
    }

    public boolean canConvertVideo() {
        return canConvertVideo;
    }

    public boolean canConvertToAudio() {
        return canConvertToAudio;
    }

}
