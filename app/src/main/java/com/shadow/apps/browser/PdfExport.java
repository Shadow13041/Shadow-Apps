package com.shadow.apps.browser;

import android.app.Activity;
import android.print.PrintAttributes;
import android.print.PrintManager;
import android.webkit.WebView;

public final class PdfExport {
    private PdfExport() {}

    public static void export(Activity activity, WebView webView) {
        if (webView == null || webView.getUrl() == null) return;

        PrintManager printManager =
                (PrintManager) activity.getSystemService(
                        Activity.PRINT_SERVICE);

        if (printManager == null) return;

        String title = webView.getTitle();
        if (title == null || title.trim().isEmpty()) {
            title = "Shadow-Browser-Seite";
        }

        PrintAttributes attributes = new PrintAttributes.Builder()
                .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                .setResolution(new PrintAttributes.Resolution(
                        "shadow_pdf", "Shadow PDF", 300, 300))
                .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                .build();

        printManager.print(
                title,
                webView.createPrintDocumentAdapter(title),
                attributes
        );
    }
}
