package com.shadow.apps.browser;

import android.app.AlertDialog;
import android.content.Context;
import android.widget.CheckBox;
import android.widget.LinearLayout;

public final class SitePermissionDialog {
    private SitePermissionDialog() {}

    public static void show(Context context, String url,
                            android.webkit.WebView webView) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (16 * context.getResources()
                .getDisplayMetrics().density);
        layout.setPadding(padding, padding, padding, padding);

        CheckBox javascript = new CheckBox(context);
        javascript.setText("JavaScript erlauben");
        javascript.setChecked(
                SitePermissionStore.isJavaScriptAllowed(context, url));

        CheckBox popups = new CheckBox(context);
        popups.setText("Pop-ups erlauben");
        popups.setChecked(
                SitePermissionStore.arePopupsAllowed(context, url));

        CheckBox images = new CheckBox(context);
        images.setText("Bilder erlauben");
        images.setChecked(
                SitePermissionStore.areImagesAllowed(context, url));

        CheckBox location = new CheckBox(context);
        location.setText("Standortzugriff erlauben");
        location.setChecked(
                SitePermissionStore.isLocationAllowed(context, url));

        CheckBox camera = new CheckBox(context);
        camera.setText("Kamerazugriff erlauben");
        camera.setChecked(
                SitePermissionStore.isCameraAllowed(context, url));

        layout.addView(javascript);
        layout.addView(popups);
        layout.addView(images);
        layout.addView(location);
        layout.addView(camera);

        new AlertDialog.Builder(context)
                .setTitle("Website-Berechtigungen")
                .setMessage(url)
                .setView(layout)
                .setNegativeButton("Abbrechen", null)
                .setPositiveButton("Speichern", (dialog, which) -> {
                    SitePermissionStore.setJavaScriptAllowed(
                            context, url, javascript.isChecked());
                    SitePermissionStore.setPopupsAllowed(
                            context, url, popups.isChecked());
                    SitePermissionStore.setImagesAllowed(
                            context, url, images.isChecked());
                    SitePermissionStore.setLocationAllowed(
                            context, url, location.isChecked());
                    SitePermissionStore.setCameraAllowed(
                            context, url, camera.isChecked());

                    if (webView != null) {
                        webView.getSettings().setJavaScriptEnabled(
                                javascript.isChecked());
                        webView.getSettings().setBlockNetworkImage(
                                !images.isChecked());
                        webView.reload();
                    }
                })
                .show();
    }
}
