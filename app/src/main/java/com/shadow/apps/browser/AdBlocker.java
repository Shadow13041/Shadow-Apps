package com.shadow.apps.browser;

import android.content.Context;
import android.net.Uri;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class AdBlocker {
    private static final String PREFS = "shadow_browser";
    private static final String KEY_ENABLED = "ad_blocker";

    private static final Set<String> BLOCKED = new HashSet<>(Arrays.asList(
            "doubleclick.net",
            "googlesyndication.com",
            "googleadservices.com",
            "googletagmanager.com",
            "google-analytics.com",
            "adservice.google.com",
            "adnxs.com",
            "adform.net",
            "taboola.com",
            "outbrain.com",
            "scorecardresearch.com",
            "hotjar.com",
            "clarity.ms",
            "facebook.net",
            "connect.facebook.net",
            "criteo.com",
            "adsrvr.org",
            "rubiconproject.com",
            "pubmatic.com"
    ));

    private AdBlocker() {}

    public static boolean isEnabled(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getBoolean(KEY_ENABLED, true);
    }

    public static void setEnabled(Context context, boolean enabled) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_ENABLED, enabled).apply();
    }

    public static boolean shouldBlock(Context context, String url) {
        if (!isEnabled(context) || url == null) return false;

        String host = Uri.parse(url).getHost();
        if (host == null) return false;

        host = host.toLowerCase(java.util.Locale.ROOT);

        for (String domain : BLOCKED) {
            if (host.equals(domain) || host.endsWith("." + domain)) {
                return true;
            }
        }

        return false;
    }
}
