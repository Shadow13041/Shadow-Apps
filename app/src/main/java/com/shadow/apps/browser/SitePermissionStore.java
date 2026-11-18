package com.shadow.apps.browser;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

public final class SitePermissionStore {
    private static final String PREFS = "shadow_site_permissions";

    private SitePermissionStore() {}

    private static String key(String url, String permission) {
        String host = Uri.parse(url == null ? "" : url).getHost();
        if (host == null || host.isEmpty()) host = "unknown";
        return host.toLowerCase() + "_" + permission;
    }

    private static boolean get(Context context, String url,
                               String permission, boolean fallback) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getBoolean(key(url, permission), fallback);
    }

    private static void set(Context context, String url,
                            String permission, boolean value) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putBoolean(key(url, permission), value).apply();
    }

    public static boolean isJavaScriptAllowed(Context c, String u) {
        return get(c, u, "javascript", true);
    }

    public static void setJavaScriptAllowed(Context c, String u, boolean v) {
        set(c, u, "javascript", v);
    }

    public static boolean arePopupsAllowed(Context c, String u) {
        return get(c, u, "popups", false);
    }

    public static void setPopupsAllowed(Context c, String u, boolean v) {
        set(c, u, "popups", v);
    }

    public static boolean areImagesAllowed(Context c, String u) {
        return get(c, u, "images", true);
    }

    public static void setImagesAllowed(Context c, String u, boolean v) {
        set(c, u, "images", v);
    }

    public static boolean isLocationAllowed(Context c, String u) {
        return get(c, u, "location", false);
    }

    public static void setLocationAllowed(Context c, String u, boolean v) {
        set(c, u, "location", v);
    }

    public static boolean isCameraAllowed(Context c, String u) {
        return get(c, u, "camera", false);
    }

    public static void setCameraAllowed(Context c, String u, boolean v) {
        set(c, u, "camera", v);
    }
}
