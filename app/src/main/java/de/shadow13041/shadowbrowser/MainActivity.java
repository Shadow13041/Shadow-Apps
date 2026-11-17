package de.shadow13041.shadowbrowser;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.graphics.Color;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Environment;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Build;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.webkit.WebChromeClient;
import android.webkit.CookieManager;
import android.webkit.URLUtil;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class MainActivity extends Activity {
    private static final int COLOR_BACKGROUND = Color.rgb(0, 0, 0);
    private static final int COLOR_SURFACE = Color.rgb(32, 38, 50);
    private static final int COLOR_ACCENT = Color.rgb(69, 196, 255);

    private final ArrayList<WebView> tabs = new ArrayList<>();

    private WebView webView;
    private LinearLayout webContainer;
    private EditText addressBar;
    private ProgressBar progressBar;
    private Button backButton;
    private Button forwardButton;
    private Button tabsButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(COLOR_BACKGROUND);
        getWindow().setNavigationBarColor(COLOR_BACKGROUND);
        buildInterface();
        createTab(BrowserSettings.getHomepage(this));
    }

    private void buildInterface() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(COLOR_BACKGROUND);
        root.setPadding(dp(10), dp(8), dp(10), dp(6));

        LinearLayout addressRow = new LinearLayout(this);
        addressRow.setGravity(Gravity.CENTER_VERTICAL);
        addressRow.setOrientation(LinearLayout.HORIZONTAL);

        addressBar = new EditText(this);
        addressBar.setSingleLine(true);
        addressBar.setLongClickable(false);
        addressBar.setTextIsSelectable(false);
        addressBar.setTextColor(Color.WHITE);
        addressBar.setHintTextColor(Color.rgb(168, 178, 194));
        addressBar.setHint("Search or enter address");
        addressBar.setTextSize(14);
        addressBar.setPadding(dp(14), 0, dp(10), 0);
        addressBar.setImeOptions(EditorInfo.IME_ACTION_GO);
        addressBar.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_VARIATION_URI);

        GradientDrawable addressBackground = new GradientDrawable();
        addressBackground.setColor(COLOR_SURFACE);
        addressBackground.setCornerRadius(dp(24));
        addressBackground.setStroke(dp(1), Color.rgb(48, 59, 75));
        addressBar.setBackground(addressBackground);
        addressRow.addView(addressBar, new LinearLayout.LayoutParams(0, dp(46), 1));

        Button goButton = createButton("➜", true);
        LinearLayout.LayoutParams goParams = new LinearLayout.LayoutParams(dp(54), dp(46));
        goParams.leftMargin = dp(7);
        addressRow.addView(goButton, goParams);
        Button menuButton = createButton("⋮", false);
        LinearLayout.LayoutParams menuParams = new LinearLayout.LayoutParams(dp(42), dp(46));
        menuParams.leftMargin = dp(5);
        addressRow.addView(menuButton, menuParams);
        root.addView(addressRow);

        progressBar = new ProgressBar(this, null,
                android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        progressBar.setProgressTintList(
                android.content.res.ColorStateList.valueOf(COLOR_ACCENT));
        progressBar.setProgressBackgroundTintList(
                android.content.res.ColorStateList.valueOf(COLOR_SURFACE));
        root.addView(progressBar, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(3)));

        webContainer = new LinearLayout(this);
        webContainer.setOrientation(LinearLayout.VERTICAL);
        root.addView(webContainer, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));

        LinearLayout navigation = new LinearLayout(this);
        navigation.setGravity(Gravity.CENTER);
        navigation.setOrientation(LinearLayout.HORIZONTAL);
        navigation.setPadding(0, dp(6), 0, 0);

        backButton = createButton("‹", false);
        forwardButton = createButton("›", false);
        Button homeButton = createButton("⌂", false);
        Button reloadButton = createButton("↻", false);
        tabsButton = createButton("▢", false);
        Button newTabButton = createButton("+", true);

        addNavigationButton(navigation, backButton);
        addNavigationButton(navigation, forwardButton);
        addNavigationButton(navigation, homeButton);
        addNavigationButton(navigation, reloadButton);
        addNavigationButton(navigation, tabsButton);
        addNavigationButton(navigation, newTabButton);

        root.addView(navigation);
        com.shadow.apps.ui.SystemBars.apply(root);
        setContentView(root);

        goButton.setOnClickListener(v -> navigateFromAddressBar());
        menuButton.setOnClickListener(v -> showBrowserDataMenu());

        addressBar.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO
                    || (event != null
                    && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                    && event.getAction() == KeyEvent.ACTION_DOWN)) {
                navigateFromAddressBar();
                return true;
            }
            return false;
        });

        backButton.setOnClickListener(v -> {
            if (webView != null && webView.canGoBack()) {
                webView.goBack();
            }
        });

        forwardButton.setOnClickListener(v -> {
            if (webView != null && webView.canGoForward()) {
                webView.goForward();
            }
        });

        homeButton.setOnClickListener(v -> {
            if (webView != null) webView.loadUrl(BrowserSettings.getHomepage(this));
        });

        reloadButton.setOnClickListener(v -> {
            if (webView != null) webView.reload();
        });

        tabsButton.setOnClickListener(v -> showTabOverview());
        newTabButton.setOnClickListener(v -> createTab(BrowserSettings.getHomepage(this)));
    }

    private void createTab(String url) {
        WebView newWebView = new WebView(this);
        tabs.add(newWebView);
        webContainer.addView(newWebView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT));

        if (webView != null) webView.setVisibility(View.GONE);

        webView = newWebView;
        configureWebView(newWebView);
        newWebView.setVisibility(View.VISIBLE);
        addressBar.setText(displayAddress(url));
        updateTabButton();
        updateNavigationButtons();
        newWebView.loadUrl(url);
    }

    private void switchTab(int index) {
        if (index < 0 || index >= tabs.size()) return;

        if (webView != null) webView.setVisibility(View.GONE);

        webView = tabs.get(index);
        webView.setVisibility(View.VISIBLE);

        String url = webView.getUrl();
        addressBar.setText(displayAddress(url));

        updateTabButton();
        updateNavigationButtons();
    }

    private void closeCurrentTab() {
        if (webView == null || tabs.isEmpty()) return;

        int index = tabs.indexOf(webView);
        WebView closing = webView;
        tabs.remove(closing);
        webContainer.removeView(closing);

        closing.stopLoading();
        closing.setWebChromeClient(null);
        closing.setWebViewClient(null);
        closing.destroy();

        if (tabs.isEmpty()) {
            webView = null;
            createTab(BrowserSettings.getHomepage(this));
        } else {
            switchTab(Math.min(index, tabs.size() - 1));
        }

        updateTabButton();
    }

    private String displayAddress(String url) {
        if (url == null || url.isEmpty()) return "";
        if (url.startsWith("https://shadow.local")) return "Startseite";
        return url;
    }

    private void showTabOverview() {
        if (tabs.isEmpty()) return;

        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(dp(16), dp(8), dp(16), dp(8));

        ScrollView scroll = new ScrollView(this);
        scroll.addView(list);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Registerkarten · " + tabs.size())
                .setView(scroll)
                .setNegativeButton("Schließen", null)
                .setPositiveButton("Neuer Tab", (d, w) -> createTab(BrowserSettings.getHomepage(this)))
                .create();

        for (int i = 0; i < tabs.size(); i++) {
            final int tabIndex = i;
            WebView tab = tabs.get(i);
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(14), dp(12), dp(12), dp(12));
            GradientDrawable cardBg = new GradientDrawable();
            cardBg.setColor(COLOR_SURFACE);
            cardBg.setCornerRadius(dp(16));
            cardBg.setStroke(dp(1), Color.rgb(48, 59, 75));
            card.setBackground(cardBg);

            ImageView preview = new ImageView(this);
            preview.setBackgroundColor(Color.rgb(13, 17, 24));
            preview.setScaleType(ImageView.ScaleType.CENTER_CROP);
            preview.setImageBitmap(createTabPreview(tab));
            LinearLayout.LayoutParams previewLp = new LinearLayout.LayoutParams(-1, dp(154));
            previewLp.bottomMargin = dp(11);
            card.addView(preview, previewLp);

            String pageTitle = tab.getTitle();
            if (pageTitle == null || pageTitle.trim().isEmpty()) pageTitle = "Neuer Tab";
            TextView title = new TextView(this);
            title.setText(pageTitle);
            title.setTextColor(Color.WHITE);
            title.setTextSize(15);
            title.setTypeface(null, android.graphics.Typeface.BOLD);
            card.addView(title);

            TextView address = new TextView(this);
            address.setText(displayAddress(tab.getUrl()));
            address.setTextColor(Color.rgb(168, 178, 194));
            address.setTextSize(12);
            address.setMaxLines(2);
            LinearLayout.LayoutParams addressLp = new LinearLayout.LayoutParams(-1, -2);
            addressLp.topMargin = dp(5);
            card.addView(address, addressLp);

            LinearLayout actions = new LinearLayout(this);
            actions.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
            Button close = createButton("×", false);
            Button open = createButton("Öffnen", true);
            LinearLayout.LayoutParams closeLp = new LinearLayout.LayoutParams(dp(46), dp(40));
            closeLp.rightMargin = dp(8);
            actions.addView(close, closeLp);
            actions.addView(open, new LinearLayout.LayoutParams(dp(94), dp(40)));
            LinearLayout.LayoutParams actionsLp = new LinearLayout.LayoutParams(-1, -2);
            actionsLp.topMargin = dp(10);
            card.addView(actions, actionsLp);

            card.setOnClickListener(v -> {
                dialog.dismiss();
                switchTab(tabIndex);
            });
            open.setOnClickListener(v -> {
                dialog.dismiss();
                switchTab(tabIndex);
            });
            close.setOnClickListener(v -> {
                dialog.dismiss();
                closeTabAt(tabIndex);
                if (!tabs.isEmpty()) showTabOverview();
            });

            LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(-1, -2);
            cardLp.bottomMargin = dp(10);
            list.addView(card, cardLp);
        }

        dialog.show();
    }

    private Bitmap createTabPreview(WebView source) {
        int width = 480;
        int height = 270;
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.rgb(13, 17, 24));
        float scale = Math.min((float) width / Math.max(1, source.getWidth()),
                (float) height / Math.max(1, source.getHeight()));
        canvas.save();
        canvas.scale(scale, scale);
        source.draw(canvas);
        canvas.restore();
        return bitmap;
    }

    private void closeTabAt(int index) {
        if (index < 0 || index >= tabs.size()) return;
        WebView closing = tabs.get(index);
        boolean wasCurrent = closing == webView;
        tabs.remove(index);
        webContainer.removeView(closing);
        closing.stopLoading();
        closing.setWebChromeClient(null);
        closing.setWebViewClient(null);
        closing.destroy();

        if (tabs.isEmpty()) {
            webView = null;
            createTab(BrowserSettings.getHomepage(this));
        } else if (wasCurrent) {
            webView = null;
            switchTab(Math.min(index, tabs.size() - 1));
        }
        updateTabButton();
    }

    private void updateTabButton() {
        if (tabsButton != null) {
            tabsButton.setText(String.valueOf(tabs.size()));
        }
    }

    private void configureWebView(WebView target) {
        target.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) -> {
            try {
                String fileName = URLUtil.guessFileName(url, contentDisposition, mimeType);
                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                request.setTitle(fileName);
                request.setDescription("Shadow Browser Download");
                request.setMimeType(mimeType);
                request.addRequestHeader("User-Agent", userAgent);
                String cookies = CookieManager.getInstance().getCookie(url);
                if (cookies != null) request.addRequestHeader("Cookie", cookies);
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
                DownloadManager manager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                manager.enqueue(request);
                Toast.makeText(this, "Download gestartet: " + fileName, Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "Download konnte nicht gestartet werden", Toast.LENGTH_LONG).show();
            }
        });
        WebSettings settings = target.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        android.webkit.CookieManager.getInstance().setAcceptThirdPartyCookies(
                target, !PrivacySettings.blockThirdPartyCookies(this));
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setSupportMultipleWindows(false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            settings.setSafeBrowsingEnabled(PrivacySettings.safeBrowsing(this));
        }


        target.setBackgroundColor(COLOR_BACKGROUND);

        target.setWebViewClient(new WebViewClient() {
            @Override
            public android.webkit.WebResourceResponse shouldInterceptRequest(
                    WebView view, WebResourceRequest request) {
                String requestedUrl = request.getUrl().toString();
                if (requestedUrl.equals("https://shadow.local/")
                        || requestedUrl.equals("https://shadow.local")) {
                    try {
                        return new android.webkit.WebResourceResponse(
                                "text/html", "UTF-8",
                                getAssets().open("shadow_home.html"));
                    } catch (Exception ignored) {
                        return null;
                    }
                }
                return super.shouldInterceptRequest(view, request);
            }
            @Override
            public boolean shouldOverrideUrlLoading(
                    WebView view, WebResourceRequest request) {
                return false;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                if (url != null) BrowserDataStore.addHistory(MainActivity.this, url, view.getTitle());
                if (view == webView) {
                    addressBar.setText(displayAddress(url));
                    updateNavigationButtons();
                }
            }
        });

        target.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (view == webView) {
                    progressBar.setProgress(newProgress);
                    progressBar.setVisibility(
                            newProgress >= 100 ? View.GONE : View.VISIBLE);
                }
            }

            @Override
            public void onReceivedTitle(WebView view, String title) {
                if (view == webView && title != null && !title.isEmpty()) {
                    setTitle(title + " - Shadow Browser");
                }
            }
        });
    }

    private void showBrowserDataMenu() {
        String[] options = {"Aktuelle Seite speichern", "Lesezeichen anzeigen", "Verlauf anzeigen", "Downloads anzeigen", "Datenschutz", "Einstellungen", "Auf Seite suchen"};
        new AlertDialog.Builder(this)
                .setTitle("Browser-Daten")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        if (webView != null && webView.getUrl() != null) {
                            BrowserDataStore.addBookmark(this, webView.getUrl(), webView.getTitle());
                            Toast.makeText(this, "Lesezeichen gespeichert", Toast.LENGTH_SHORT).show();
                        }
                    } else if (which == 1) {
                        BrowserDataDialogs.show(this, webView, false);
                    } else if (which == 2) {
                        BrowserDataDialogs.show(this, webView, true);
                    } else if (which == 3) {
                        DownloadDialogs.show(this);
                    } else if (which == 4) {
                        PrivacyDialogs.show(this, webView);
                    } else if (which == 5) {
                        SettingsDialogs.show(this);
                    } else if (which == 6) {
                        FindInPageDialog.show(this, webView);
                    }
                }).show();
    }

    private void navigateFromAddressBar() {
        String input = addressBar.getText().toString().trim();
        if (input.isEmpty() || webView == null) return;

        String target;

        if (input.matches("^[a-zA-Z][a-zA-Z0-9+.-]*://.*")) {
            target = input;
        } else if (input.matches(
                "^[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}(:[0-9]+)?(/.*)?$")) {
            target = "https://" + input;
        } else {
            try {
                target = BrowserSettings.buildSearchUrl(this, input);
            } catch (Exception exception) {
                Toast.makeText(this, "Search could not be started",
                        Toast.LENGTH_SHORT).show();
                return;
            }
        }

        ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE))
                .hideSoftInputFromWindow(addressBar.getWindowToken(), 0);
        addressBar.clearFocus();
        webView.loadUrl(target);
    }


    private Button createButton(String text, boolean accent) {
        Button button = new Button(this);
        button.setTextSize(accent ? 14 : 20);
        button.setTextColor(accent ? Color.rgb(0, 19, 29) : Color.WHITE);
        button.setAllCaps(false);
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setPadding(dp(5), 0, dp(5), 0);
        button.setContentDescription(text);

        int icon = 0;
        if ("➜".equals(text)) icon = android.R.drawable.ic_menu_send;
        else if ("⋮".equals(text)) icon = android.R.drawable.ic_menu_more;
        else if ("‹".equals(text)) icon = android.R.drawable.ic_media_previous;
        else if ("›".equals(text)) icon = android.R.drawable.ic_media_next;
        else if ("⌂".equals(text)) icon = android.R.drawable.ic_menu_myplaces;
        else if ("↻".equals(text)) icon = android.R.drawable.ic_popup_sync;
        else if ("▢".equals(text)) icon = android.R.drawable.ic_menu_view;
        else if ("+".equals(text)) icon = android.R.drawable.ic_input_add;
        else if ("×".equals(text)) icon = android.R.drawable.ic_menu_close_clear_cancel;

        if (icon != 0) {
            button.setText("");
            button.setCompoundDrawablesWithIntrinsicBounds(icon, 0, 0, 0);
            button.setGravity(Gravity.CENTER);
        } else {
            button.setText(text);
        }

        GradientDrawable background = new GradientDrawable();
        background.setColor(accent ? COLOR_ACCENT : COLOR_SURFACE);
        background.setCornerRadius(dp(18));
        background.setStroke(dp(1), accent ? COLOR_ACCENT : Color.rgb(48, 59, 75));
        button.setBackground(background);
        return button;
    }

    private void addNavigationButton(LinearLayout parent, Button button) {
        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(0, dp(42), 1);
        params.leftMargin = dp(2);
        params.rightMargin = dp(2);
        parent.addView(button, params);
    }

    private void updateNavigationButtons() {
        if (backButton != null) {
            backButton.setEnabled(webView != null && webView.canGoBack());
        }
        if (forwardButton != null) {
            forwardButton.setEnabled(
                    webView != null && webView.canGoForward());
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources()
                .getDisplayMetrics().density);
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else if (tabs.size() > 1) {
            closeCurrentTab();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        for (WebView tab : new ArrayList<>(tabs)) {
            tab.stopLoading();
            tab.setWebChromeClient(null);
            tab.setWebViewClient(null);
            webContainer.removeView(tab);
            tab.destroy();
        }
        tabs.clear();
        webView = null;
        super.onDestroy();
    }
}
