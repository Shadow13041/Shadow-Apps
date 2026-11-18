package com.shadow.apps.browser;

import android.app.Activity;
import android.app.AlertDialog;
import android.net.Uri;
import android.os.Environment;
import android.webkit.WebView;
import android.widget.Toast;
import java.io.File;
import java.util.ArrayList;

public final class ArchiveDialogs {
    private ArchiveDialogs() {}

    private static File directory(Activity activity) {
        File base = activity.getExternalFilesDir(
                Environment.DIRECTORY_DOCUMENTS);
        if (base == null) return null;
        File dir = new File(base, "ShadowArchives");
        if (!dir.exists() && !dir.mkdirs()) return null;
        return dir;
    }

    public static void show(Activity activity, WebView webView) {
        String[] options = {"Aktuelle Seite speichern", "Archive anzeigen"};
        new AlertDialog.Builder(activity)
                .setTitle("Offline-Archive")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) save(activity, webView);
                    else list(activity, webView);
                })
                .setNegativeButton("Schließen", null)
                .show();
    }

    private static void save(Activity activity, WebView webView) {
        if (webView == null || webView.getUrl() == null
                || !webView.getUrl().startsWith("http")) {
            Toast.makeText(activity, "Keine speicherbare Webseite geöffnet",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        File dir = directory(activity);
        if (dir == null) {
            Toast.makeText(activity, "Speicherordner nicht verfügbar",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        String name = "shadow_" + System.currentTimeMillis() + ".mht";
        File destination = new File(dir, name);

        webView.saveWebArchive(destination.getAbsolutePath(), false, path -> {
            if (path != null) {
                Toast.makeText(activity, "Offline-Archiv gespeichert",
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(activity, "Archiv konnte nicht gespeichert werden",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private static void list(Activity activity, WebView webView) {
        File dir = directory(activity);
        if (dir == null) return;

        File[] files = dir.listFiles((d, name) ->
                name.toLowerCase().endsWith(".mht"));

        ArrayList<File> archives = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();

        if (files != null) {
            java.util.Arrays.sort(files,
                    (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
            for (File file : files) {
                archives.add(file);
                labels.add(file.getName());
            }
        }

        if (archives.isEmpty()) {
            new AlertDialog.Builder(activity)
                    .setTitle("Offline-Archive")
                    .setMessage("Noch keine Archive gespeichert.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        new AlertDialog.Builder(activity)
                .setTitle("Offline-Archive")
                .setItems(labels.toArray(new String[0]), (dialog, which) -> {
                    File selected = archives.get(which);
                    new AlertDialog.Builder(activity)
                            .setTitle(selected.getName())
                            .setItems(new String[]{"Öffnen", "Löschen"},
                                    (d, action) -> {
                                        if (action == 0) {
                                            if (webView == null) return;
                                            webView.getSettings()
                                                    .setJavaScriptEnabled(false);
                                            webView.getSettings()
                                                    .setAllowFileAccess(true);
                                            webView.getSettings()
                                                    .setBlockNetworkLoads(true);
                                            webView.loadUrl(
                                                    Uri.fromFile(selected).toString());
                                        } else {
                                            if (selected.delete()) {
                                                Toast.makeText(activity,
                                                        "Archiv gelöscht",
                                                        Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    }).show();
                })
                .setNegativeButton("Schließen", null)
                .show();
    }
}
