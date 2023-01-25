package com.example.cofivideodownloader.downloaders.reddit;

import com.example.cofivideodownloader.downloaders.misc.FileType;

public class DomainManagerMetadata {
    private final FileType fileType;
    private final boolean canConvertVideo;
    private final boolean canConvertToAudio;

    public DomainManagerMetadata(FileType fileType, boolean canConvertVideo, boolean canConvertToAudio) {
        this.fileType = fileType;
        this.canConvertVideo = canConvertVideo;
        this.canConvertToAudio = canConvertToAudio;
    }

    public FileType getFileType() {
        return fileType;
    }

    public boolean canConvertVideo() {
        return canConvertVideo;
    }

    public boolean canConvertToAudio() {
        return canConvertToAudio;
    }
}
