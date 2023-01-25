package com.example.cofivideodownloader;

import com.example.cofivideodownloader.downloaders.misc.FileType;

import java.util.ArrayList;
import java.util.List;

public class FFmpegConverter {
    private final FileType targetType;
    private final List<String> args;

    public FFmpegConverter(FileType targetType, List<String> args) {
        this.targetType = targetType;
        this.args = args;
    }

    public boolean convert(String filenameNoExt, String originalExt) {
        String filename = filenameNoExt + originalExt;
        String outputFilename = filenameNoExt + targetType.getExtension();

        List<String> commandArgs = new ArrayList<>();
        commandArgs.add("-i");
        commandArgs.add(filename);
        commandArgs.addAll(this.args);
        commandArgs.add("-y");
        commandArgs.add(outputFilename);

        return FFmpegUtil.execute(commandArgs, filename);
    }
}
