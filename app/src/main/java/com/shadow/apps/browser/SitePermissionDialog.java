package com.shadow.apps.browser;

import android.app.Activity;
import android.app.AlertDialog;
import android.net.Uri;
import android.webkit.WebView;
import android.widget.Toast;

public final class SitePermissionDialog {
    private SitePermissionDialog() {}

    public static void show(Activity activity, WebView webView) {
        if (webView == null || webView.getUrl() == null) return;

        String url = webView.getUrl();
        String host = Uri.parse(url).getHost();

        if (host == null) {
            Toast.makeText(activity, "Keine Website erkannt",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        boolean[] checked = {
                SitePermissionStore.isJavaScriptAllowed(activity, url),
                SitePermissionStore.arePopupsAllowed(activity, url)
        };

        String[] options = {
                "JavaScript erlauben",
                "Pop-ups erlauben"
        };

        new AlertDialog.Builder(activity)
                .setTitle("Berechtigungen · " + host)
                .setMultiChoiceItems(options, checked,
                        (dialog, which, enabled) -> checked[which] = enabled)
                .setPositiveButton("Speichern", (dialog, which) -> {
                    boolean oldJavaScript =
                            SitePermissionStore.isJavaScriptAllowed(activity, url);

                    SitePermissionStore.setJavaScriptAllowed(
                            activity, url, checked[0]);
                    SitePermissionStore.setPopupsAllowed(
                            activity, url, checked[1]);

                    webView.getSettings().setJavaScriptEnabled(checked[0]);

                    if (oldJavaScript != checked[0]) {
                        webView.reload();
                    }

                    Toast.makeText(activity,
                            "Website-Berechtigungen gespeichert",
                            Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Abbrechen", null)
                .show();
    }
}
