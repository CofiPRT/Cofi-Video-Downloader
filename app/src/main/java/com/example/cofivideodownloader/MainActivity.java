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
import androidx.appcompat.widget.SwitchCompat;
import androidx.constraintlayout.widget.ConstraintLayout;
import com.example.cofivideodownloader.downloaders.Downloader;
import com.example.cofivideodownloader.downloaders.DownloaderFactory;
import com.example.cofivideodownloader.downloaders.misc.FileType;
import com.example.cofivideodownloader.downloaders.misc.VideoMetadata;
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

    private ConstraintLayout conversionLayout;
    private SwitchCompat conversionVideoButton;
    private SwitchCompat conversionAudioButton;

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

        conversionLayout = findViewById(R.id.conversion_layout);
        conversionVideoButton = findViewById(R.id.conversion_video_button);
        conversionAudioButton = findViewById(R.id.conversion_audio_button);

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

        // set switch listeners
        conversionVideoButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                conversionAudioButton.setChecked(false);
            }
        });

        conversionAudioButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                conversionVideoButton.setChecked(false);
            }
        });
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

                    // enable conversion layout
                    enableConversionLayout(true);
                });
            } catch (MalformedURLException e) {
                logToast(TAG, "Malformed URL", e);
            } finally {
                uiHandler.post(this::endSearch);
            }
        });
    }

    private void enableConversionLayout(boolean reset) {
        if (videoMetadata.canConvertVideo()) {
            FileType targetType = videoMetadata.getFileType() == FileType.VIDEO ? FileType.GIF : FileType.VIDEO;
            conversionVideoButton.setText(String.format("As %s", targetType.getExtensionNoDot().toUpperCase()));
            conversionVideoButton.setVisibility(View.VISIBLE);
        } else {
            conversionVideoButton.setVisibility(View.GONE);
        }

        if (videoMetadata.canConvertToAudio())
            conversionAudioButton.setVisibility(View.VISIBLE);
        else
            conversionAudioButton.setVisibility(View.GONE);

        if (reset) {
            conversionVideoButton.setChecked(false);
            conversionAudioButton.setChecked(false);
        }

        conversionLayout.setVisibility(View.VISIBLE);
    }

    private void disableConversionLayout() {
        conversionLayout.setVisibility(View.GONE);
    }

    private void setThumbnail(String thumbnailUrl) {
        try (InputStream in = new URL(thumbnailUrl).openStream()) {
            thumbnailBitmap = BitmapFactory.decodeStream(in);
            uiHandler.post(() -> thumbnailImage.setImageBitmap(thumbnailBitmap));
        } catch (IOException e) {
            logToast(TAG, "Failed to download thumbnail", e);
            uiHandler.post(() -> thumbnailImage.setImageResource(android.R.drawable.ic_menu_gallery));
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
        disableConversionLayout();
    }

    private void endDownload() {
        enableSearch();
        enableButton(downloadButton);
        downloadProgressBar.setVisibility(View.GONE);
        downloadProgressText.setVisibility(View.GONE);
        enableConversionLayout(false);
    }

    public void onDownloadButtonClicked() {
        String originalFilenameNoExt = new File(
            ytDlDir, Long.toHexString(System.currentTimeMillis()) // obtain current timestamp
        ).getAbsolutePath();

        String filenameNoExt = originalFilenameNoExt;
        String originalExt = videoMetadata.getFileType().getExtension();
        String filename = originalFilenameNoExt + originalExt;

        // if the file already exists, add a suffix to the filename
        int suffix = 1;
        while (new File(filename).exists()) {
            filenameNoExt = originalFilenameNoExt + "_" + suffix;
            filename = filenameNoExt + originalExt;
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

                // convert
                String ext = originalExt;
                FileType targetType = null;

                if (conversionVideoButton.isChecked()) {
                    if (videoMetadata.getFileType() == FileType.VIDEO)
                        targetType = FileType.GIF;
                    else
                        targetType = FileType.VIDEO;
                } else if (conversionAudioButton.isChecked()) {
                    targetType = FileType.AUDIO;
                }

                String failureMessage = null;

                if (targetType != null) {
                    FFmpegConverter converter = FFmpegUtil.getConverter(targetType);
                    startConversion(targetType);

                    boolean conversionResult = converter.convert(finalFilenameNoExt, originalExt);
                    if (conversionResult)
                        ext = targetType.getExtension();
                    else
                        failureMessage = "Downloaded, but failed to convert";
                }

                // use ffmpeg to apply metadata (title, thumbnail, date)
                if (!applyMetadata(finalFilenameNoExt, ext) && failureMessage == null)
                    failureMessage = "Downloaded, but failed to apply metadata";

                // notify media store
                MediaScannerConnection.scanFile(
                    this, new String[] {finalFilenameNoExt + ext}, null, null
                );

                if (failureMessage != null)
                    showToast(failureMessage);
                else
                    showToast("Download finished");
            } finally {
                uiHandler.post(this::endDownload);
            }
        });
    }

    public void startConversion(FileType targetType) {
        uiHandler.post(() -> downloadProgressText.setText(
            String.format("Converting to %s...", targetType.getExtension().toUpperCase())
        ));
    }

    private boolean applyMetadata(String filenameNoExt, String ext) {
        String filename = filenameNoExt + ext;
        List<String> args = new ArrayList<>(Arrays.asList("-i", filename));

        // save the thumbnail image as a temporary file
        File thumbnailFile = new File(filenameNoExt + ".png.temp");

        if (!ext.equals(FileType.GIF.getExtension())) {
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
        }

        String outputFilename = filenameNoExt + "_temp" + ext;

        // add the title and date to the ffmpeg command
        // escape quotes in the title
        String title = videoMetadata.getTitle().replace("\"", "\\\"");
        Stream.of(
            "title=\"" + title + "\"",
            "date=now",
            "creation_time=now"
        ).forEach(arg -> args.addAll(Arrays.asList("-metadata", arg)));
        args.addAll(Arrays.asList("-y", outputFilename));

        boolean success = FFmpegUtil.execute(args, filename, outputFilename);

        // delete the temporary thumbnail file
        try {
            Files.deleteIfExists(thumbnailFile.toPath());
        } catch (IOException e) {
            Log.e(TAG, "IO Exception", e);
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