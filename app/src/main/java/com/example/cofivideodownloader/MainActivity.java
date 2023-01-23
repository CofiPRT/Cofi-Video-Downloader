package com.example.cofivideodownloader;

import android.content.ClipboardManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import com.arthenica.ffmpegkit.FFmpegKit;
import com.arthenica.ffmpegkit.FFmpegSession;
import com.arthenica.ffmpegkit.ReturnCode;
import com.example.cofivideodownloader.downloaders.Downloader;
import com.example.cofivideodownloader.downloaders.DownloaderFactory;
import com.example.cofivideodownloader.downloaders.VideoMetadata;
import com.yausername.youtubedl_android.YoutubeDL;
import com.yausername.youtubedl_android.YoutubeDLException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = "MainActivity";

    private EditText linkTextBox;
    private Button clipboardButton;
    private Button searchButton;
    private ProgressBar searchProgressBar;

    private ConstraintLayout resultLayout;
    private ImageView thumbnailImage;
    private TextView titleText;

    private ConstraintLayout downloadLayout;
    private Button downloadButton;
    private ProgressBar downloadProgressBar;
    private TextView downloadProgressText;

    // multi-threading
    private ExecutorService executor;
    private Handler uiHandler;

    // utils
    private ClipboardManager clipboardManager;
    private File ytDlDir;

    // to be set after search
    private Downloader downloader;
    private VideoMetadata videoMetadata;
    private Bitmap thumbnailBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        linkTextBox = findViewById(R.id.link_text_box);
        clipboardButton = findViewById(R.id.clipboard_button);
        searchButton = findViewById(R.id.search_button);
        searchProgressBar = findViewById(R.id.search_progress_bar);

        resultLayout = findViewById(R.id.result_layout);
        thumbnailImage = findViewById(R.id.thumbnail_image);
        titleText = findViewById(R.id.title_text);

        downloadLayout = findViewById(R.id.download_layout);
        downloadButton = findViewById(R.id.download_button);
        downloadProgressBar = findViewById(R.id.download_progress_bar);
        downloadProgressText = findViewById(R.id.download_progress_text);

        executor = Executors.newSingleThreadExecutor();
        uiHandler = new Handler(Looper.getMainLooper());

        clipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        ytDlDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

        // initialize libraries
        try {
            YoutubeDL.getInstance().init(this);
        } catch (YoutubeDLException e) {
            Log.e(TAG, "YoutubeDL init failed", e);
        }

        // disable widgets
        endSearch();
        downloadLayout.setVisibility(View.GONE);
        resultLayout.setVisibility(View.GONE);

        // set button listeners
        clipboardButton.setOnClickListener(v -> onClipboardButtonClicked());
        searchButton.setOnClickListener(v -> onSearchButtonClicked());
        downloadButton.setOnClickListener(v -> onDownloadButtonClicked());
    }

    private void enableButton(Button button) {
        button.setEnabled(true);
        button.setAlpha(1f);
    }

    private void disableButton(Button button) {
        button.setEnabled(false);
        button.setAlpha(0.5f);
    }

    private void enableSearch() {
        enableButton(clipboardButton);
        enableButton(searchButton);
    }

    private void disableSearch() {
        disableButton(clipboardButton);
        disableButton(searchButton);
    }

    private void startSearch() {
        disableSearch();
        searchProgressBar.setVisibility(View.VISIBLE);
        resultLayout.setVisibility(View.GONE);
    }

    private void endSearch() {
        enableSearch();
        searchProgressBar.setVisibility(View.GONE);
    }

    public void onClipboardButtonClicked() {
        // retrieve the link from the clipboard
        String link = clipboardManager.getPrimaryClip().getItemAt(0).getText().toString();

        // set the link to the link text box
        linkTextBox.setText(link);

        onSearchButtonClicked();
    }

    public void onSearchButtonClicked() {
        // get the link from the link text box
        String link = linkTextBox.getText().toString();
        startSearch();

        executor.execute(() -> {
            try {
                downloader = DownloaderFactory.getDownloader(new URL(link), this);
                if (downloader == null) {
                    showToast("Unsupported link");
                    return;
                }

                videoMetadata = downloader.getVideoMetadata();
                if (videoMetadata == null)
                    return;

                String title = videoMetadata.getTitle();
                String thumbnailUrl = videoMetadata.getThumbnailURL();

                setThumbnail(thumbnailUrl);

                uiHandler.post(() -> {
                    // set the title to the title text
                    titleText.setText(title);

                    // enable the result layout
                    resultLayout.setVisibility(View.VISIBLE);

                    endSearch();

                    // enable the download layout
                    enableButton(downloadButton);
                    downloadProgressBar.setVisibility(View.GONE);
                    downloadLayout.setVisibility(View.VISIBLE);
                });
            } catch (MalformedURLException e) {
                logToast(TAG, "Malformed URL", e);
            } finally {
                uiHandler.post(this::endSearch);
            }
        });
    }

    private void setThumbnail(String thumbnailUrl) {
        try (InputStream in = new URL(thumbnailUrl).openStream()) {
            thumbnailBitmap = BitmapFactory.decodeStream(in);
            uiHandler.post(() -> thumbnailImage.setImageBitmap(thumbnailBitmap));
        } catch (IOException e) {
            logToast(TAG, "Failed to download thumbnail", e);
        }
    }

    public void updateDownloadProgress(int progress) {
        uiHandler.post(() -> {
            int clampedProgress = Math.max(0, Math.min(100, progress));

            downloadProgressBar.setProgress(clampedProgress);

            String text = clampedProgress == 0
                ? "Starting download..."
                : String.format(Locale.ENGLISH, "%d%%", clampedProgress);
            downloadProgressText.setText(text);
        });
    }

    private void startDownload() {
        disableSearch();
        disableButton(downloadButton);
        downloadProgressBar.setVisibility(View.VISIBLE);
        downloadProgressText.setVisibility(View.VISIBLE);
        updateDownloadProgress(0);
    }

    private void endDownload() {
        enableSearch();
        enableButton(downloadButton);
        downloadProgressBar.setVisibility(View.GONE);
        downloadProgressText.setVisibility(View.GONE);
    }

    public void onDownloadButtonClicked() {
        String originalFilenameNoExt = new File(
            ytDlDir, Long.toHexString(System.currentTimeMillis()) // obtain current timestamp
        ).getAbsolutePath();

        String filenameNoExt = originalFilenameNoExt;
        String filename = originalFilenameNoExt + ".mp4";

        // if the file already exists, add a suffix to the filename
        int suffix = 1;
        while (new File(filename).exists()) {
            filenameNoExt = originalFilenameNoExt + "_" + suffix;
            filename = filenameNoExt + ".mp4";
        }

        startDownload();

        String finalFilenameNoExt = filenameNoExt;
        String finalFilename = filename;

        executor.execute(() -> {
            try {
                if (downloader == null) {
                    showToast("No video selected");
                    return;
                }

                if (!downloader.downloadVideo(finalFilename)) {
                    showToast("Download failed");
                    return;
                }

                Log.i(TAG, "Downloaded to " + finalFilename);

                // use ffmpeg to apply metadata (title, thumbnail, date)
                if (!applyMetadata(finalFilenameNoExt)) {
                    showToast("Downloaded, but failed to apply metadata");
                    return;
                }

                // notify media store
                MediaScannerConnection.scanFile(this, new String[] {finalFilename}, null, null);

                showToast("Download finished");
            } finally {
                uiHandler.post(this::endDownload);
            }
        });
    }

    private boolean applyMetadata(String filenameNoExt) {
        String filename = filenameNoExt + ".mp4";
        List<String> args = new ArrayList<>(Arrays.asList("-i", filename));

        // save the thumbnail image as a temporary file
        File thumbnailFile = new File(filenameNoExt + ".png.temp");

        try (FileOutputStream out = new FileOutputStream(thumbnailFile)) {
            thumbnailBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);

            // add the thumbnail to the ffmpeg command
            args.addAll(Arrays.asList(
                "-i", thumbnailFile.getAbsolutePath(),
                "-map", "1", "-map", "0", "-c", "copy", "-disposition:0", "attached_pic"
            ));
        } catch (IOException e) {
            Log.e(TAG, "Failed to save thumbnail", e);
        }

        String outputFilename = filenameNoExt + "_temp.mp4";

        // add the title and date to the ffmpeg command
        // escape quotes in the title
        String title = videoMetadata.getTitle().replace("\"", "\\\"");
        Stream.of(
            "title=\"" + title + "\"",
            "date=now",
            "creation_time=now"
        ).forEach(arg -> args.addAll(Arrays.asList("-metadata", arg)));
        args.addAll(Arrays.asList("-y", outputFilename));

        // join the arguments into a single string
        FFmpegSession session = FFmpegKit.execute(String.join(" ", args));
        boolean success = ReturnCode.isSuccess(session.getReturnCode());
        if (!success)
            Log.e(TAG, "FFmpeg Error: " + session.getState() + ": " + session.getFailStackTrace());

        // delete the temporary thumbnail file
        try {
            Files.delete(thumbnailFile.toPath());
        } catch (IOException e) {
            Log.e(TAG, "IO Exception", e);
        }

        if (success) {
            // delete the original file, and rename the output file
            try {
                File originalFile = new File(filename);
                Files.delete(originalFile.toPath());
                Files.move(new File(outputFilename).toPath(), originalFile.toPath());
            } catch (IOException e) {
                Log.e(TAG, "IO Exception", e);
            }
        }

        return success;
    }

    public void logToast(String tag, String message, Throwable e) {
        Log.e(tag, message, e);
        showToast(message);
    }

    public void logToast(String tag, String message, String trace) {
        Log.e(tag, message + trace);
        showToast(message);
    }

    public void showToast(String message) {
        uiHandler.post(() -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show());
    }

}