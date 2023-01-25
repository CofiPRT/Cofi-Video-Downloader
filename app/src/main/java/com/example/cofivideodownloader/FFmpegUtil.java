package com.example.cofivideodownloader;

import android.util.Log;
import com.arthenica.ffmpegkit.FFmpegKit;
import com.arthenica.ffmpegkit.FFmpegSession;
import com.arthenica.ffmpegkit.ReturnCode;
import com.example.cofivideodownloader.downloaders.misc.FileType;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FFmpegUtil {
    private static final String TAG = "FFmpegUtil";

    private static final Map<FileType, FFmpegConverter> CONVERTERS = new HashMap<>();

    static {
        CONVERTERS.put(
            FileType.VIDEO,
            new FFmpegConverter(
                FileType.VIDEO,
                Arrays.asList(
                    "-movflags", "faststart",
                    "-pix_fmt", "yuv420p",
                    "-vf", "scale=trunc(iw/2)*2:trunc(ih/2)*2"
                )
            )
        );
        CONVERTERS.put(
            FileType.GIF,
            new FFmpegConverter(
                FileType.GIF,
                Arrays.asList(
                    "-vf", "fps=10,scale=320:-1:flags=lanczos",
                    "-loop", "0"
                )
            )
        );
        CONVERTERS.put(
            FileType.AUDIO,
            new FFmpegConverter(
                FileType.AUDIO,
                Collections.singletonList(
                    "-vn"
                )
            )
        );
    }

    private FFmpegUtil() { }

    public static FFmpegConverter getConverter(FileType targetType) {
        return CONVERTERS.get(targetType);
    }

    public static boolean execute(List<String> args, String originalFilename, String newFilename) {
        // join the arguments into a single string
        FFmpegSession session = FFmpegKit.execute(String.join(" ", args));
        boolean success = ReturnCode.isSuccess(session.getReturnCode());
        if (success)
            cleanup(originalFilename, newFilename);
        else
            Log.e(TAG, "FFmpeg Error: " + session.getState() + ": " + session.getFailStackTrace());

        return success;
    }

    public static boolean execute(List<String> args, String originalFilename) {
        return execute(args, originalFilename, null);
    }

    private static void cleanup(String originalFilename, String newFilename) {
        // delete the original file
        try {
            File originalFile = new File(originalFilename);
            Files.delete(originalFile.toPath());
            if (newFilename != null) // rename if provided
                Files.move(new File(newFilename).toPath(), originalFile.toPath());
        } catch (IOException e) {
            Log.e(TAG, "IO Exception", e);
        }
    }

}
