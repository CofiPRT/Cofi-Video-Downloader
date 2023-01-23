package com.example.cofivideodownloader.downloaders;

import com.example.cofivideodownloader.MainActivity;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Predicate;

public abstract class DownloaderFactory {

    private static final Map<Predicate<String>, BiFunction<URL, MainActivity, Downloader>> resourceMatcher =
        new HashMap<>();

    static {
        resourceMatcher.put(DownloaderFactory::isYoutubeLink, YoutubeDownloader::new);
        resourceMatcher.put(DownloaderFactory::isRedditLink, RedditDownloader::new);
    }

    private DownloaderFactory() { }

    public static Downloader getDownloader(URL url, MainActivity activity) {
        return resourceMatcher.entrySet().stream()
            .filter(entry -> entry.getKey().test(url.toString()))
            .findFirst()
            .map(entry -> entry.getValue().apply(url, activity))
            .orElse(null);
    }

    private static boolean isYoutubeLink(String link) {
        return link.contains("youtube.com") || link.contains("youtu.be");
    }

    private static boolean isRedditLink(String link) {
        return link.contains("reddit.com") || link.contains("redd.it");
    }
}
