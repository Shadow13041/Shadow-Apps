package com.shadow.apps.browser;

import android.content.Context;
import android.net.Uri;

import java.util.Locale;

public final class SitePermissionStore {
    private static final String PREFS = "shadow_site_permissions";

    private SitePermissionStore() {}

    private static String key(String url, String permission) {
        if (url == null) return null;
        String host = Uri.parse(url).getHost();
        if (host == null || host.isEmpty()) return null;
        return host.toLowerCase(Locale.ROOT) + "." + permission;
    }

    public static boolean isJavaScriptAllowed(Context context, String url) {
        String key = key(url, "javascript");
        return key == null || context.getSharedPreferences(PREFS, 0)
                .getBoolean(key, true);
    }

    public static void setJavaScriptAllowed(
            Context context, String url, boolean allowed) {
        String key = key(url, "javascript");
        if (key == null) return;
        context.getSharedPreferences(PREFS, 0).edit()
                .putBoolean(key, allowed).apply();
    }

    public static boolean arePopupsAllowed(Context context, String url) {
        String key = key(url, "popups");
        return key != null && context.getSharedPreferences(PREFS, 0)
                .getBoolean(key, false);
    }

    public static void setPopupsAllowed(
            Context context, String url, boolean allowed) {
        String key = key(url, "popups");
        if (key == null) return;
        context.getSharedPreferences(PREFS, 0).edit()
                .putBoolean(key, allowed).apply();
    }
}
