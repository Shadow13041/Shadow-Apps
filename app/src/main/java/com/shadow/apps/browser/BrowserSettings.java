package com.shadow.apps.browser;

import android.content.Context;
import android.content.SharedPreferences;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public final class BrowserSettings {
    private static final String PREFS = "shadow_browser_settings";

    public static final String SEARCH_GOOGLE = "Google";
    public static final String SEARCH_DUCKDUCKGO = "DuckDuckGo";
    public static final String SEARCH_BING = "Bing";
    public static final String SEARCH_BRAVE = "Brave";

    private BrowserSettings() {}

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static String getHomepage(Context context) {
        return prefs(context).getString(
                "homepage", "https://shadow.local/");
    }

    public static void setHomepage(Context context, String homepage) {
        prefs(context).edit().putString("homepage", homepage).apply();
    }

    public static String getSearchEngine(Context context) {
        return prefs(context).getString("search_engine", SEARCH_GOOGLE);
    }

    public static void setSearchEngine(Context context, String engine) {
        prefs(context).edit().putString("search_engine", engine).apply();
    }

    public static boolean isDataSaverEnabled(Context context) {
        return prefs(context).getBoolean("data_saver", false);
    }

    public static void setDataSaverEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean("data_saver", enabled).apply();
    }

    public static boolean isCachePreferred(Context context) {
        return prefs(context).getBoolean("cache_preferred", false);
    }

    public static void setCachePreferred(Context context, boolean enabled) {
        prefs(context).edit().putBoolean("cache_preferred", enabled).apply();
    }

    public static String buildSearchUrl(Context context, String query)
            throws Exception {
        String encoded = URLEncoder.encode(
                query, StandardCharsets.UTF_8.name());

        switch (getSearchEngine(context)) {
            case SEARCH_DUCKDUCKGO:
                return "https://duckduckgo.com/?q=" + encoded;
            case SEARCH_BING:
                return "https://www.bing.com/search?q=" + encoded;
            case SEARCH_BRAVE:
                return "https://search.brave.com/search?q=" + encoded;
            default:
                return "https://www.google.com/search?q=" + encoded;
        }
    }
}
