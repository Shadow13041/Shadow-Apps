package com.shadow.apps.browser;

import android.app.AlertDialog;
import android.content.Context;
import android.net.Uri;

public final class SiteInfoDialog {
    private SiteInfoDialog() {}

    public static void show(Context context, String title, String url) {
        Uri uri = Uri.parse(url == null ? "" : url);

        String host = uri.getHost();
        if (host == null) host = "Unbekannt";

        String protocol = uri.getScheme();
        if (protocol == null) protocol = "Unbekannt";

        String message =
                "Titel: " + (title == null || title.isEmpty()
                        ? "Unbekannt" : title)
                + "\n\nAdresse:\n" + url
                + "\n\nDomain: " + host
                + "\nProtokoll: " + protocol
                + "\n\nJavaScript: "
                + allowed(SitePermissionStore.isJavaScriptAllowed(
                        context, url))
                + "\nBilder: "
                + allowed(SitePermissionStore.areImagesAllowed(
                        context, url))
                + "\nPop-ups: "
                + allowed(SitePermissionStore.arePopupsAllowed(
                        context, url))
                + "\nStandort: "
                + allowed(SitePermissionStore.isLocationAllowed(
                        context, url))
                + "\nKamera: "
                + allowed(SitePermissionStore.isCameraAllowed(
                        context, url));

        new AlertDialog.Builder(context)
                .setTitle("Website-Informationen")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    private static String allowed(boolean value) {
        return value ? "Erlaubt" : "Blockiert";
    }
}
