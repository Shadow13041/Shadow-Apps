package com.shadow.apps.browser;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;

public final class BrowserContextProvider extends ContentProvider {
    private static volatile String title = "";
    private static volatile String url = "";
    private static volatile String pageText = "";

    public static void update(String pageTitle, String pageUrl, String text) {
        title = pageTitle == null ? "" : pageTitle;
        url = pageUrl == null ? "" : pageUrl;
        pageText = text == null ? "" : text;
        if (pageText.length() > 20000) pageText = pageText.substring(0,20000);
    }

    @Override public boolean onCreate() { return true; }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        if (getContext() == null) return null;
        MatrixCursor cursor = new MatrixCursor(new String[]{"title","url","text"});
        cursor.addRow(new Object[]{title,url,pageText});
        return cursor;
    }

    @Override public String getType(Uri uri) { return "vnd.android.cursor.item/vnd.shadow.browser.context"; }
    @Override public Uri insert(Uri uri, ContentValues values) { return null; }
    @Override public int delete(Uri uri, String selection, String[] args) { return 0; }
    @Override public int update(Uri uri, ContentValues values, String selection, String[] args) { return 0; }
}
