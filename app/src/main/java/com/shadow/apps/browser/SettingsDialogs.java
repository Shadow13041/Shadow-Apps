package com.shadow.apps.browser;

import android.app.Activity;
import android.app.AlertDialog;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

public final class SettingsDialogs {
    private SettingsDialogs() {}

    public static void show(Activity activity) {
        LinearLayout layout = new LinearLayout(activity);
        layout.setOrientation(LinearLayout.VERTICAL);
        int padding = Math.round(18 * activity.getResources()
                .getDisplayMetrics().density);
        layout.setPadding(padding, padding, padding, 0);

        EditText homepage = new EditText(activity);
        homepage.setSingleLine(true);
        homepage.setHint("https://example.com/");
        homepage.setText(BrowserSettings.getHomepage(activity));
        layout.addView(homepage, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        Spinner searchEngine = new Spinner(activity);
        String[] engines = {
                BrowserSettings.SEARCH_GOOGLE,
                BrowserSettings.SEARCH_DUCKDUCKGO,
                BrowserSettings.SEARCH_BING,
                BrowserSettings.SEARCH_BRAVE
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                activity, android.R.layout.simple_spinner_item, engines);
        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);
        searchEngine.setAdapter(adapter);

        int selected = 0;
        for (int i = 0; i < engines.length; i++) {
            if (engines[i].equals(
                    BrowserSettings.getSearchEngine(activity))) {
                selected = i;
                break;
            }
        }
        searchEngine.setSelection(selected);
        layout.addView(searchEngine);

        CheckBox dataSaver = new CheckBox(activity);
        dataSaver.setText("Datensparmodus (Bilder blockieren)");
        dataSaver.setChecked(BrowserSettings.isDataSaverEnabled(activity));
        layout.addView(dataSaver);

        CheckBox cache = new CheckBox(activity);
        cache.setText("Cache bevorzugen (Offline-Inhalte)");
        cache.setChecked(BrowserSettings.isCachePreferred(activity));
        layout.addView(cache);

        new AlertDialog.Builder(activity)
                .setTitle("Browser-Einstellungen")
                .setMessage("Startseite, Suchmaschine und Leistung")
                .setView(layout)
                .setPositiveButton("Speichern", (dialog, which) -> {
                    String url = homepage.getText().toString().trim();

                    if (!url.isEmpty()
                            && !url.matches("^[a-zA-Z][a-zA-Z0-9+.-]*://.*")) {
                        url = "https://" + url;
                    }

                    if (!url.isEmpty()
                            && (url.startsWith("https://")
                            || url.startsWith("http://"))) {
                        BrowserSettings.setHomepage(activity, url);
                    } else if (!url.isEmpty()) {
                        Toast.makeText(activity,
                                "Ungültige Startseiten-Adresse",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    BrowserSettings.setSearchEngine(activity,
                            String.valueOf(searchEngine.getSelectedItem()));
                    BrowserSettings.setDataSaverEnabled(activity,
                            dataSaver.isChecked());
                    BrowserSettings.setCachePreferred(activity,
                            cache.isChecked());

                    Toast.makeText(activity, "Einstellungen gespeichert",
                            Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Abbrechen", null)
                .show();
    }
}
