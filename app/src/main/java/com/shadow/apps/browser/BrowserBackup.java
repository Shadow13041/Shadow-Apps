package com.shadow.apps.browser;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;
import org.json.JSONObject;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public final class BrowserBackup {
    public static final int EXPORT_REQUEST = 7310;
    public static final int IMPORT_REQUEST = 7311;

    private static final String[] PREFS = {
        "shadow_browser_data", "shadow_browser_settings",
        "shadow_browser", "shadow_site_permissions"
    };

    private BrowserBackup() {}

    public static void chooseExport(Activity a) {
        Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        i.setType("application/json");
        i.putExtra(Intent.EXTRA_TITLE,"ShadowBrowser-Backup.json");
        i.addCategory(Intent.CATEGORY_OPENABLE);
        a.startActivityForResult(i,EXPORT_REQUEST);
    }

    public static void chooseImport(Activity a) {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.setType("application/json");
        i.addCategory(Intent.CATEGORY_OPENABLE);
        a.startActivityForResult(i,IMPORT_REQUEST);
    }

    public static void export(Context c, Uri uri) {
        try {
            JSONObject root = new JSONObject();
            root.put("format","shadow-browser-backup-v1");
            for (String name : PREFS) {
                JSONObject values = new JSONObject();
                for (Map.Entry<String,?> e :
                        c.getSharedPreferences(name,0).getAll().entrySet()) {
                    Object v = e.getValue();
                    if (v instanceof String || v instanceof Boolean
                            || v instanceof Integer || v instanceof Long
                            || v instanceof Float) values.put(e.getKey(),v);
                }
                root.put(name,values);
            }
            try (OutputStream out = c.getContentResolver().openOutputStream(uri)) {
                if (out == null) throw new Exception("Datei nicht verfügbar");
                out.write(root.toString(2).getBytes(StandardCharsets.UTF_8));
            }
            Toast.makeText(c,"Browser-Backup gespeichert",Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(c,"Backup fehlgeschlagen: "+e.getMessage(),Toast.LENGTH_LONG).show();
        }
    }

    public static void restore(Context c, Uri uri) {
        try {
            StringBuilder data = new StringBuilder();
            try (InputStream in = c.getContentResolver().openInputStream(uri)) {
                if (in == null) throw new Exception("Datei nicht verfügbar");
                byte[] buffer = new byte[4096];
                int n;
                while ((n=in.read(buffer))!=-1) data.append(new String(buffer,0,n,StandardCharsets.UTF_8));
            }
            JSONObject root = new JSONObject(data.toString());
            if (!"shadow-browser-backup-v1".equals(root.optString("format")))
                throw new Exception("Unbekanntes Backup-Format");

            for (String name : PREFS) {
                JSONObject values = root.optJSONObject(name);
                if (values == null) continue;
                android.content.SharedPreferences.Editor editor =
                        c.getSharedPreferences(name,0).edit().clear();
                java.util.Iterator<String> keys = values.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    Object v = values.get(key);
                    if (v instanceof Boolean) editor.putBoolean(key,(Boolean)v);
                    else if (v instanceof Integer) editor.putInt(key,(Integer)v);
                    else if (v instanceof Long) editor.putLong(key,(Long)v);
                    else if (v instanceof Double) editor.putFloat(key,((Double)v).floatValue());
                    else if (v instanceof String) editor.putString(key,(String)v);
                }
                editor.apply();
            }
            Toast.makeText(c,"Backup wiederhergestellt. Browser bitte neu öffnen.",Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(c,"Wiederherstellung fehlgeschlagen: "+e.getMessage(),Toast.LENGTH_LONG).show();
        }
    }
}
