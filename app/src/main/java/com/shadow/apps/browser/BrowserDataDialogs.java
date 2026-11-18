package com.shadow.apps.browser;

import android.app.Activity;
import android.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;

public final class BrowserDataDialogs {
    private BrowserDataDialogs() {}

    public static void show(Activity activity, WebView webView,
                            boolean history) {
        JSONArray data = history
                ? BrowserDataStore.getHistory(activity)
                : BrowserDataStore.getBookmarks(activity);

        ArrayList<String> urls = new ArrayList<>();
        ArrayList<String> titles = new ArrayList<>();
        ArrayList<String> folders = new ArrayList<>();

        for (int i = data.length() - 1; i >= 0; i--) {
            JSONObject item = data.optJSONObject(i);
            if (item == null) continue;

            String url = item.optString("url");
            if (url.isEmpty()) continue;

            urls.add(url);
            titles.add(item.optString("title", url));
            folders.add(item.optString("folder", ""));
        }

        LinearLayout layout = new LinearLayout(activity);
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(activity, 14);
        layout.setPadding(pad, pad, pad, 0);

        EditText search = new EditText(activity);
        search.setSingleLine(true);
        search.setHint(history ? "Verlauf durchsuchen" : "Lesezeichen suchen");
        layout.addView(search);

        ListView list = new ListView(activity);
        layout.addView(list, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(activity, 360)));

        ArrayList<Integer> visible = new ArrayList<>();

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                activity, android.R.layout.simple_list_item_2,
                android.R.id.text1, new ArrayList<String>()) {
            @Override
            public View getView(int position, View convertView,
                                ViewGroup parent) {
                View row = super.getView(position, convertView, parent);
                android.widget.TextView first =
                        row.findViewById(android.R.id.text1);
                android.widget.TextView second =
                        row.findViewById(android.R.id.text2);

                int index = visible.get(position);
                first.setText(titles.get(index));
                second.setText((history ? "" :
                        (folders.get(index).isEmpty()
                                ? "Ohne Ordner · "
                                : folders.get(index) + " · "))
                        + urls.get(index));
                return row;
            }
        };

        list.setAdapter(adapter);

        Runnable filter = () -> {
            String q = search.getText().toString().trim().toLowerCase();
            visible.clear();
            adapter.clear();

            for (int i = 0; i < urls.size(); i++) {
                if (titles.get(i).toLowerCase().contains(q)
                        || urls.get(i).toLowerCase().contains(q)
                        || folders.get(i).toLowerCase().contains(q)) {
                    visible.add(i);
                    adapter.add(titles.get(i));
                }
            }
            adapter.notifyDataSetChanged();
        };

        filter.run();

        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {
                filter.run();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle(history ? "Verlauf" : "Lesezeichen")
                .setView(layout)
                .setNeutralButton(history ? "Verlauf löschen" : "Schließen",
                        (d, w) -> {
                            if (history) {
                                new AlertDialog.Builder(activity)
                                        .setTitle("Verlauf löschen?")
                                        .setMessage("Der gesamte Verlauf wird gelöscht.")
                                        .setNegativeButton("Abbrechen", null)
                                        .setPositiveButton("Löschen", (x, y) ->
                                                BrowserDataStore.clearHistory(activity))
                                        .show();
                            }
                        })
                .setNegativeButton("Schließen", null)
                .create();

        list.setOnItemClickListener((parent, view, position, id) -> {
            int index = visible.get(position);
            if (webView != null) webView.loadUrl(urls.get(index));
            dialog.dismiss();
        });

        list.setOnItemLongClickListener((parent, view, position, id) -> {
            int index = visible.get(position);
            String url = urls.get(index);

            if (history) {
                new AlertDialog.Builder(activity)
                        .setTitle(titles.get(index))
                        .setItems(new String[]{"Öffnen", "Eintrag löschen"},
                                (d, which) -> {
                                    if (which == 0 && webView != null) {
                                        webView.loadUrl(url);
                                        dialog.dismiss();
                                    } else if (which == 1) {
                                        BrowserDataStore.removeHistory(activity, url);
                                        dialog.dismiss();
                                        show(activity, webView, true);
                                    }
                                }).show();
            } else {
                new AlertDialog.Builder(activity)
                        .setTitle(titles.get(index))
                        .setItems(new String[]{"Öffnen", "Bearbeiten",
                                        "Ordner zuweisen", "Löschen"},
                                (d, which) -> {
                                    if (which == 0) {
                                        if (webView != null) webView.loadUrl(url);
                                        dialog.dismiss();
                                    } else if (which == 1 || which == 2) {
                                        EditText input = new EditText(activity);
                                        input.setSingleLine(true);
                                        input.setText(which == 1
                                                ? titles.get(index)
                                                : folders.get(index));
                                        input.setHint(which == 1
                                                ? "Titel" : "Ordnername");

                                        new AlertDialog.Builder(activity)
                                                .setTitle(which == 1
                                                        ? "Lesezeichen bearbeiten"
                                                        : "Ordner zuweisen")
                                                .setView(input)
                                                .setNegativeButton("Abbrechen", null)
                                                .setPositiveButton("Speichern", (x, y) -> {
                                                    String title = which == 1
                                                            ? input.getText().toString().trim()
                                                            : titles.get(index);
                                                    String folder = which == 2
                                                            ? input.getText().toString().trim()
                                                            : folders.get(index);

                                                    BrowserDataStore.addBookmark(
                                                            activity, url, title, folder);
                                                    dialog.dismiss();
                                                    show(activity, webView, false);
                                                }).show();
                                    } else {
                                        BrowserDataStore.removeBookmark(activity, url);
                                        dialog.dismiss();
                                        show(activity, webView, false);
                                    }
                                }).show();
            }
            return true;
        });

        dialog.show();
    }

    private static int dp(Activity activity, int value) {
        return Math.round(value * activity.getResources()
                .getDisplayMetrics().density);
    }
}
