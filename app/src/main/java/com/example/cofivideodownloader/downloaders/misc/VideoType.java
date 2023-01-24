package com.example.cofivideodownloader.downloaders.misc;

public enum VideoType {
    VIDEO("mp4"),
    GIF("gif");

    private final String extension;

    VideoType(String extension) {
        this.extension = extension;
    }

    public String getExtension() {
        return extension;
    }
}
