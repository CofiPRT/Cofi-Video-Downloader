package com.example.cofivideodownloader.downloaders.misc;

public enum FileType {
    VIDEO("mp4"),
    AUDIO("mp3"),
    GIF("gif");

    private final String extension;

    FileType(String extension) {
        this.extension = extension;
    }

    public String getExtension() {
        return "." + extension;
    }

    public String getExtensionNoDot() {
        return extension;
    }
}
