package com.example.cofivideodownloader.downloaders.reddit;

import com.example.cofivideodownloader.MainActivity;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Predicate;

public abstract class DomainManagerFactory {

    private static final Map<Predicate<String>, BiFunction<MainActivity, JsonObject, DomainManager>>
        resourceMatcher = new HashMap<>();

    static {
        resourceMatcher.put(DomainManagerFactory::isRedditVideoDomain, RedditVideoManager::new);
        resourceMatcher.put(DomainManagerFactory::isRedditGifDomain, RedditGifManager::new);
        resourceMatcher.put(DomainManagerFactory::isImgurDomain, RedditGifManager::new); // same parsing
        resourceMatcher.put(DomainManagerFactory::isGfycatDomain, GfycatVideoManager::new);
    }

    private DomainManagerFactory() { }

    public static DomainManager getManager(String domain, MainActivity activity, JsonObject data) {
        return resourceMatcher.entrySet().stream()
            .filter(entry -> entry.getKey().test(domain))
            .findFirst()
            .map(entry -> entry.getValue().apply(activity, data))
            .orElse(null);
    }

    private static boolean isRedditVideoDomain(String link) {
        return link.contains("v.redd.it");
    }

    private static boolean isRedditGifDomain(String link) {
        return link.contains("i.redd.it");
    }

    private static boolean isImgurDomain(String link) {
        return link.contains("imgur.com") || link.contains("i.imgur.com");
    }

    private static boolean isGfycatDomain(String link) {
        return link.contains("gfycat.com");
    }
}
