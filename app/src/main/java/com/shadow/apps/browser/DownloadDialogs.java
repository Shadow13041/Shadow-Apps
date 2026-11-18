package com.shadow.apps.browser;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;

public final class DownloadDialogs {
    private DownloadDialogs() {}

    private static final class Item {
        long id;
        String title;
        String mime;
        int status;
        int progress;
    }

    public static void show(Activity activity) {
        DownloadManager manager = (DownloadManager)
                activity.getSystemService(Activity.DOWNLOAD_SERVICE);
        if (manager == null) return;

        ArrayList<Item> items = new ArrayList<>();
        Cursor cursor = manager.query(new DownloadManager.Query());

        if (cursor != null) {
            try {
                while (cursor.moveToNext()) {
                    Item item = new Item();
                    item.id = cursor.getLong(cursor.getColumnIndexOrThrow(
                            DownloadManager.COLUMN_ID));
                    item.title = cursor.getString(cursor.getColumnIndexOrThrow(
                            DownloadManager.COLUMN_TITLE));
                    item.mime = cursor.getString(cursor.getColumnIndexOrThrow(
                            DownloadManager.COLUMN_MEDIA_TYPE));
                    item.status = cursor.getInt(cursor.getColumnIndexOrThrow(
                            DownloadManager.COLUMN_STATUS));

                    int total = cursor.getInt(cursor.getColumnIndexOrThrow(
                            DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                    int done = cursor.getInt(cursor.getColumnIndexOrThrow(
                            DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));

                    item.progress = total > 0
                            ? Math.min(100, (int) (done * 100L / total)) : 0;
                    items.add(item);
                }
            } finally {
                cursor.close();
            }
        }

        LinearLayout list = new LinearLayout(activity);
        list.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(activity, 14);
        list.setPadding(pad, pad, pad, pad);

        if (items.isEmpty()) {
            TextView empty = new TextView(activity);
            empty.setText("Noch keine Downloads vorhanden.");
            list.addView(empty);
        }

        for (Item item : items) {
            LinearLayout card = new LinearLayout(activity);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(activity, 10), dp(activity, 10),
                    dp(activity, 10), dp(activity, 10));

            TextView title = new TextView(activity);
            title.setText(item.title == null ? "Download" : item.title);
            title.setTextSize(15);
            card.addView(title);

            TextView state = new TextView(activity);
            state.setText(statusText(item.status, item.progress));
            card.addView(state);

            ProgressBar progress = new ProgressBar(activity, null,
                    android.R.attr.progressBarStyleHorizontal);
            progress.setMax(100);
            progress.setProgress(item.progress);
            card.addView(progress, new LinearLayout.LayoutParams(
                    -1, dp(activity, 5)));

            LinearLayout actions = new LinearLayout(activity);
            actions.setGravity(Gravity.END);

            if (item.status == DownloadManager.STATUS_SUCCESSFUL) {
                Button open = new Button(activity);
                open.setText("Öffnen");
                open.setOnClickListener(v ->
                        openDownload(activity, manager, item));
                actions.addView(open);
            }

            boolean active = item.status == DownloadManager.STATUS_RUNNING
                    || item.status == DownloadManager.STATUS_PENDING
                    || item.status == DownloadManager.STATUS_PAUSED;

            Button remove = new Button(activity);
            remove.setText(active ? "Abbrechen" : "Löschen");
            remove.setOnClickListener(v -> {
                manager.remove(item.id);
                Toast.makeText(activity,
                        active ? "Download abgebrochen" : "Download gelöscht",
                        Toast.LENGTH_SHORT).show();
                show(activity);
            });
            actions.addView(remove);
            card.addView(actions);

            LinearLayout.LayoutParams lp =
                    new LinearLayout.LayoutParams(-1, -2);
            lp.bottomMargin = dp(activity, 10);
            list.addView(card, lp);
        }

        android.widget.ScrollView scroll =
                new android.widget.ScrollView(activity);
        scroll.addView(list);

        new AlertDialog.Builder(activity)
                .setTitle("Downloads · " + items.size())
                .setView(scroll)
                .setPositiveButton("Schließen", null)
                .show();
    }

    private static String statusText(int status, int progress) {
        if (status == DownloadManager.STATUS_SUCCESSFUL) return "Abgeschlossen";
        if (status == DownloadManager.STATUS_FAILED) return "Fehlgeschlagen";
        if (status == DownloadManager.STATUS_RUNNING)
            return "Wird heruntergeladen · " + progress + "%";
        if (status == DownloadManager.STATUS_PAUSED) return "Pausiert";
        if (status == DownloadManager.STATUS_PENDING) return "Wartet";
        return "Unbekannter Status";
    }

    private static void openDownload(Activity activity,
                                     DownloadManager manager, Item item) {
        try {
            Uri uri = manager.getUriForDownloadedFile(item.id);
            if (uri == null) throw new Exception();

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, item.mime == null ? "*/*" : item.mime);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            activity.startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(activity,
                    "Datei kann nicht geöffnet werden.",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private static int dp(Activity activity, int value) {
        return Math.round(value * activity.getResources()
                .getDisplayMetrics().density);
    }
}
