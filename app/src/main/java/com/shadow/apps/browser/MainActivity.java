package com.shadow.apps.browser;

import com.shadow.apps.R;
import org.json.JSONArray;
import org.json.JSONObject;

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
import android.os.Message;
import android.os.Build;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.content.Intent;
import android.webkit.WebChromeClient;
import android.webkit.CookieManager;
import android.webkit.URLUtil;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.ValueCallback;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
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
    private static final int COLOR_SURFACE = Color.rgb(25, 25, 25);
    private static final int COLOR_ACCENT = Color.rgb(245, 245, 245);

    private final ArrayList<WebView> tabs = new ArrayList<>();
    private final ArrayList<String> addressSuggestionUrls = new ArrayList<>();
    private final java.util.HashMap<WebView, String> readerOriginalUrls =
            new java.util.HashMap<>();

    private WebView webView;
    private ValueCallback<Uri[]> fileChooserCallback;
    private static final int FILE_CHOOSER_REQUEST = 4102;
    private boolean desktopMode;
    private String mobileUserAgent;
    private LinearLayout webContainer;
    private EditText addressBar;
    private ProgressBar progressBar;
    private Button backButton;
    private Button forwardButton;
    private Button tabsButton;
    private Button reloadButton;
    private Button customButton;
    private View fullscreenView;
    private WebChromeClient.CustomViewCallback fullscreenCallback;
    private int normalSystemUiVisibility;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(COLOR_BACKGROUND);
        getWindow().setNavigationBarColor(COLOR_BACKGROUND);
        setTitle("");
        desktopMode = getSharedPreferences("shadow_browser", MODE_PRIVATE).getBoolean("desktop_mode", false);
        buildInterface();

        ArrayList<String> savedTabs = savedInstanceState == null
                ? null : savedInstanceState.getStringArrayList("shadow_saved_tabs");
        if (savedTabs == null) {
            savedTabs = loadSession();
        }
        if (savedTabs == null || savedTabs.isEmpty()) {
            createTab(BrowserSettings.getHomepage(this));
        } else {
            for (String savedUrl : savedTabs) {
                if (savedUrl != null && (savedUrl.startsWith("https://")
                        || savedUrl.startsWith("http://"))) {
                    createTab(savedUrl);
                }
            }
            if (tabs.isEmpty()) {
                createTab(BrowserSettings.getHomepage(this));
            } else {
                int selected = savedInstanceState.getInt("shadow_selected_tab", 0);
                switchTab(Math.max(0, Math.min(selected, tabs.size() - 1)));
            }
        }
    }

        private void buildInterface() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(COLOR_BACKGROUND);
        root.setPadding(dp(8), dp(2), dp(8), dp(5));

        progressBar = new ProgressBar(this, null,
                android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        progressBar.setProgressTintList(
                android.content.res.ColorStateList.valueOf(Color.WHITE));
        progressBar.setProgressBackgroundTintList(
                android.content.res.ColorStateList.valueOf(COLOR_SURFACE));
        progressBar.setVisibility(View.GONE);
        root.addView(progressBar, new LinearLayout.LayoutParams(-1, dp(2)));

        webContainer = new LinearLayout(this);
        webContainer.setOrientation(LinearLayout.VERTICAL);
        root.addView(webContainer, new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.setOrientation(LinearLayout.HORIZONTAL);
        toolbar.setPadding(0, dp(6), 0, 0);

        backButton = createButton("‹", false);
        forwardButton = createButton("›", false);
        reloadButton = createButton("↻", false);
        customButton = createButton("▢", false);
        tabsButton = customButton;
        Button menuButton = createButton("⋮", false);

        toolbar.addView(backButton, new LinearLayout.LayoutParams(dp(34), dp(44)));
        toolbar.addView(forwardButton, new LinearLayout.LayoutParams(dp(34), dp(44)));

        LinearLayout addressContainer = new LinearLayout(this);
        addressContainer.setGravity(Gravity.CENTER_VERTICAL);
        addressContainer.setOrientation(LinearLayout.HORIZONTAL);

        GradientDrawable addressBackground = new GradientDrawable();
        addressBackground.setColor(COLOR_SURFACE);
        addressBackground.setCornerRadius(dp(22));
        addressBackground.setStroke(dp(1), Color.rgb(48, 48, 48));
        addressContainer.setBackground(addressBackground);

        addressBar = new AutoCompleteTextView(this);
        ((AutoCompleteTextView) addressBar).setThreshold(1);
        addressBar.setSingleLine(true);
        addressBar.setTextIsSelectable(false);
        addressBar.setTextColor(Color.WHITE);
        addressBar.setHintTextColor(Color.rgb(155, 155, 155));
        addressBar.setHint("Suchen oder Adresse eingeben");
        addressBar.setTextSize(13);
        addressBar.setPadding(dp(12), 0, dp(4), 0);
        addressBar.setBackgroundColor(Color.TRANSPARENT);
        addressBar.setImeOptions(EditorInfo.IME_ACTION_GO);
        addressBar.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_VARIATION_URI);
        addressContainer.addView(addressBar, new LinearLayout.LayoutParams(0, dp(44), 1));

        reloadButton.setBackgroundColor(Color.TRANSPARENT);
        reloadButton.setPadding(0, 0, 0, 0);
        addressContainer.addView(reloadButton, new LinearLayout.LayoutParams(dp(36), dp(42)));

        LinearLayout.LayoutParams addressParams =
                new LinearLayout.LayoutParams(0, dp(44), 1);
        addressParams.leftMargin = dp(3);
        addressParams.rightMargin = dp(3);
        toolbar.addView(addressContainer, addressParams);

        toolbar.addView(customButton, new LinearLayout.LayoutParams(dp(36), dp(44)));
        toolbar.addView(menuButton, new LinearLayout.LayoutParams(dp(34), dp(44)));

        root.addView(toolbar);
        com.shadow.apps.ui.SystemBars.apply(root);
        setContentView(root);

        backButton.setOnClickListener(v -> {
            if (webView != null && webView.canGoBack()) webView.goBack();
        });
        forwardButton.setOnClickListener(v -> {
            if (webView != null && webView.canGoForward()) webView.goForward();
        });
        reloadButton.setOnClickListener(v -> {
            if (webView != null) webView.reload();
        });
        customButton.setOnClickListener(v -> performCustomAction());
        customButton.setOnLongClickListener(v -> { showCustomActionDialog(); return true; });
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

        addressBar.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence text, int start,
                    int count, int after) {}

            @Override
            public void onTextChanged(CharSequence text, int start,
                    int before, int count) {
                if (addressBar.hasFocus()) {
                    updateAddressSuggestions(text.toString());
                }
            }

            @Override
            public void afterTextChanged(android.text.Editable text) {}
        });

        ((AutoCompleteTextView) addressBar).setOnItemClickListener(
                (parent, view, position, id) -> {
                    if (position < addressSuggestionUrls.size()) {
                        addressBar.setText(addressSuggestionUrls.get(position));
                        navigateFromAddressBar();
                    }
                });

        refreshCustomButton();
        updateNavigationButtons();
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
        newWebView.getSettings().setJavaScriptEnabled(
                SitePermissionStore.isJavaScriptAllowed(this, url));
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
        readerOriginalUrls.remove(closing);
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
        if (url.startsWith("https://shadow.local")) return "";
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
                .setNeutralButton("Alle schließen", (d, w) -> confirmCloseAllTabs())
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

    private void confirmCloseAllTabs() {
        new AlertDialog.Builder(this)
                .setTitle("Alle Tabs schließen?")
                .setMessage("Möchtest du wirklich alle geöffneten Tabs schließen? Ein neuer Tab mit deiner Startseite bleibt geöffnet.")
                .setNegativeButton("Abbrechen", null)
                .setPositiveButton("Alle schließen", (dialog, which) -> closeAllTabs())
                .show();
    }

    private void closeAllTabs() {
        for (WebView tab : new ArrayList<>(tabs)) {
            tab.stopLoading();
            tab.setWebChromeClient(null);
            tab.setWebViewClient(null);
            webContainer.removeView(tab);
            tab.destroy();
        }
        tabs.clear();
        webView = null;
        createTab(BrowserSettings.getHomepage(this));
        updateTabButton();
    }

    private void updateTabButton() {
        refreshCustomButton();
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
        if (mobileUserAgent == null) mobileUserAgent = settings.getUserAgentString();
        settings.setUserAgentString(desktopMode ? "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36" : mobileUserAgent);
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
        settings.setSupportMultipleWindows(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            settings.setSafeBrowsingEnabled(PrivacySettings.safeBrowsing(this));
        }


        target.setBackgroundColor(COLOR_BACKGROUND);

        target.setOnLongClickListener(v -> {
            WebView.HitTestResult result = target.getHitTestResult();
            if (result == null) return false;
            int type = result.getType();
            if (type != WebView.HitTestResult.SRC_ANCHOR_TYPE
                    && type != WebView.HitTestResult.IMAGE_ANCHOR_TYPE) return false;
            String url = result.getExtra();
            if (url == null || !(url.startsWith("https://")
                    || url.startsWith("http://"))) return false;
            showLinkContextMenu(url);
            return true;
        });

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
                return handleInternalUrl(view, request.getUrl().toString());
            }


            
            @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return handleInternalUrl(view, url);
            }

            @Override
            public void onPageStarted(WebView view, String url,
                    android.graphics.Bitmap favicon) {
                if (url != null) {
                    view.getSettings().setJavaScriptEnabled(
                            SitePermissionStore.isJavaScriptAllowed(
                                    MainActivity.this, url));
                }
                super.onPageStarted(view, url, favicon);
            }

            @Override
            public void onReceivedError(WebView view,
                    WebResourceRequest request,
                    android.webkit.WebResourceError error) {
                if (request.isForMainFrame()
                        && view == webView
                        && !request.isRedirect()) {
                    Toast.makeText(MainActivity.this,
                            "Webseite konnte nicht geladen werden. Internetverbindung prüfen.",
                            Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onReceivedHttpError(WebView view,
                    WebResourceRequest request,
                    android.webkit.WebResourceResponse response) {
                if (request.isForMainFrame() && view == webView) {
                    Toast.makeText(MainActivity.this,
                            "Webseitenfehler: HTTP " + response.getStatusCode(),
                            Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                if (url != null) BrowserDataStore.addHistory(MainActivity.this, url, view.getTitle());
                saveSession();
                if (view == webView) {
                    addressBar.setText(displayAddress(url));
                    updateNavigationButtons();
                }
            }
        });

        target.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onCreateWindow(WebView view, boolean isDialog,
                    boolean isUserGesture, Message resultMsg) {
                if (!SitePermissionStore.arePopupsAllowed(
                        MainActivity.this, view.getUrl())) {
                    Toast.makeText(MainActivity.this,
                            "Pop-ups für diese Website sind blockiert",
                            Toast.LENGTH_SHORT).show();
                    return false;
                }

                WebView popup = new WebView(MainActivity.this);
                popup.getSettings().setJavaScriptEnabled(true);
                popup.getSettings().setDomStorageEnabled(true);

                popup.setWebViewClient(new WebViewClient() {
                    private boolean opened;

                    private boolean openInNewTab(String url) {
                        if (opened || url == null
                                || !(url.startsWith("https://")
                                || url.startsWith("http://"))) {
                            return false;
                        }
                        opened = true;
                        createTab(url);
                        popup.stopLoading();
                        popup.postDelayed(() -> {
                            try {
                                popup.destroy();
                            } catch (Exception ignored) {
                            }
                        }, 100);
                        return true;
                    }

                    @Override
                    public boolean shouldOverrideUrlLoading(
                            WebView popupView, WebResourceRequest request) {
                        return openInNewTab(request.getUrl().toString());
                    }

                    @Override
                    public boolean shouldOverrideUrlLoading(
                            WebView popupView, String url) {
                        return openInNewTab(url);
                    }
                });

                WebView.WebViewTransport transport =
                        (WebView.WebViewTransport) resultMsg.obj;
                transport.setWebView(popup);
                resultMsg.sendToTarget();
                return true;
            }


            @Override
            public boolean onShowFileChooser(WebView view,
                    ValueCallback<Uri[]> filePathCallback,
                    FileChooserParams fileChooserParams) {
                if (fileChooserCallback != null) {
                    fileChooserCallback.onReceiveValue(null);
                }
                fileChooserCallback = filePathCallback;
                try {
                    Intent intent = fileChooserParams.createIntent();
                    startActivityForResult(intent, FILE_CHOOSER_REQUEST);
                    return true;
                } catch (Exception e) {
                    fileChooserCallback = null;
                    filePathCallback.onReceiveValue(null);
                    Toast.makeText(MainActivity.this,
                            "Dateiauswahl konnte nicht geöffnet werden",
                            Toast.LENGTH_SHORT).show();
                    return false;
                }
            }

            @Override
            public void onShowCustomView(View view, WebChromeClient.CustomViewCallback callback) {
                if (fullscreenView != null) {
                    callback.onCustomViewHidden();
                    return;
                }
                fullscreenView = view;
                fullscreenCallback = callback;
                View decor = getWindow().getDecorView();
                normalSystemUiVisibility = decor.getSystemUiVisibility();
                ((ViewGroup) decor).addView(view,
                        new ViewGroup.LayoutParams(-1, -1));
                decor.setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
            }

            @Override
            public void onHideCustomView() {
                hideFullscreenVideo();
            }

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
                    setTitle(title);
                }
            }
        });
    }

        private void hideFullscreenVideo() {
        if (fullscreenView == null) return;
        View view = fullscreenView;
        WebChromeClient.CustomViewCallback callback = fullscreenCallback;
        fullscreenView = null;
        fullscreenCallback = null;
        if (view.getParent() instanceof ViewGroup) {
            ((ViewGroup) view.getParent()).removeView(view);
        }
        getWindow().getDecorView().setSystemUiVisibility(normalSystemUiVisibility);
        if (callback != null) callback.onCustomViewHidden();
    }

    private String getCustomAction() {
        return getSharedPreferences("browser_ui", MODE_PRIVATE)
                .getString("custom_action", "tabs");
    }

    private void refreshCustomButton() {
        if (customButton == null) return;

        String action = getCustomAction();
        String symbol;
        String description;

        switch (action) {
            case "home":
                symbol = "⌂";
                description = "Startseite";
                break;
            case "bookmarks":
                symbol = "★";
                description = "Lesezeichen";
                break;
            case "history":
                symbol = "◷";
                description = "Verlauf";
                break;
            case "downloads":
                symbol = "↓";
                description = "Downloads";
                break;
            default:
                symbol = String.valueOf(tabs.size());
                description = "Tabs";
                break;
        }

        customButton.setCompoundDrawables(null, null, null, null);
        customButton.setText(symbol);
        customButton.setContentDescription(description);
    }

    private void performCustomAction() {
        switch (getCustomAction()) {
            case "home":
                if (webView != null) webView.loadUrl(BrowserSettings.getHomepage(this));
                break;
            case "bookmarks":
                BrowserDataDialogs.show(this, webView, false);
                break;
            case "history":
                BrowserDataDialogs.show(this, webView, true);
                break;
            case "downloads":
                DownloadDialogs.show(this);
                break;
            default:
                showTabOverview();
                break;
        }
    }

    private void showCustomActionDialog() {
        String[] labels = {
                "Tabs", "Startseite", "Lesezeichen", "Verlauf", "Downloads"
        };
        String[] values = {
                "tabs", "home", "bookmarks", "history", "downloads"
        };

        String current = getCustomAction();
        int selected = 0;
        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(current)) selected = i;
        }

        new AlertDialog.Builder(this)
                .setTitle("Funktion der Schaltfläche")
                .setSingleChoiceItems(labels, selected, (dialog, which) -> {
                    getSharedPreferences("browser_ui", MODE_PRIVATE)
                            .edit().putString("custom_action", values[which]).apply();
                    refreshCustomButton();
                    dialog.dismiss();
                })
                .setNegativeButton("Abbrechen", null)
                .show();
    }


    private void showBrowserDataMenu() {
        String[] options = {
                "Neuer Tab",
                "Lesezeichen hinzufügen",
                "Lesezeichen",
                "Verlauf",
                "Downloads",
                "Datenschutz",
                "Einstellungen",
                "Auf Seite suchen",
                "Teilen",
                "Desktopwebsite: " + (desktopMode ? "Ein" : "Aus"),
                "Lesemodus",
                "Website-Berechtigungen"
        };

        new AlertDialog.Builder(this)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        createTab(BrowserSettings.getHomepage(this));
                    } else if (which == 1) {
                        if (webView != null && webView.getUrl() != null) {
                            BrowserDataStore.addBookmark(
                                    this, webView.getUrl(), webView.getTitle());
                            Toast.makeText(this, "Lesezeichen gespeichert",
                                    Toast.LENGTH_SHORT).show();
                        }
                    } else if (which == 2) {
                        BrowserDataDialogs.show(this, webView, false);
                    } else if (which == 3) {
                        BrowserDataDialogs.show(this, webView, true);
                    } else if (which == 4) {
                        DownloadDialogs.show(this);
                    } else if (which == 5) {
                        PrivacyDialogs.show(this, webView);
                    } else if (which == 6) {
                        SettingsDialogs.show(this);
                    } else if (which == 7) {
                        FindInPageDialog.show(this, webView);
                    } else if (which == 8 && webView != null
                            && webView.getUrl() != null) {
                        Intent share = new Intent(Intent.ACTION_SEND);
                        share.setType("text/plain");
                        share.putExtra(Intent.EXTRA_TEXT, webView.getUrl());
                        startActivity(Intent.createChooser(share, "Seite teilen"));
                    } else if (which == 10) {
                        toggleReaderMode();
                    } else if (which == 11) {
                        SitePermissionDialog.show(this, webView);
                    } else if (which == 9) {
                        desktopMode = !desktopMode;
                        getSharedPreferences("shadow_browser", MODE_PRIVATE).edit().putBoolean("desktop_mode", desktopMode).apply();
                        for (WebView tab : tabs) {
                            WebSettings ws = tab.getSettings();
                            if (mobileUserAgent == null) mobileUserAgent = ws.getUserAgentString();
                            ws.setUserAgentString(desktopMode ? "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36" : mobileUserAgent);
                        }
                        if (webView != null) webView.reload();
                    }
                })
                .show();
    }
    private void showLinkContextMenu(String url) {
    new AlertDialog.Builder(this)
            .setTitle("Link")
            .setItems(new String[]{"In neuem Tab öffnen", "Link kopieren"}, (dialog, which) -> {
                if (which == 0) {
                    createTab(url);
                } else {
                    ClipboardManager clipboard =
                            (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                    clipboard.setPrimaryClip(ClipData.newPlainText("Link", url));
                    Toast.makeText(this, "Link kopiert", Toast.LENGTH_SHORT).show();
                }
            })
            .show();
}

private boolean handleInternalUrl(WebView view, String url) {
        if (url == null || !url.startsWith("shadow-search://search/")) return false;
        try {
            String query = Uri.parse(url).getLastPathSegment();
            if (query == null || query.trim().isEmpty()) return true;
            view.loadUrl(BrowserSettings.buildSearchUrl(this, query));
        } catch (Exception e) {
            Toast.makeText(this, "Suche konnte nicht gestartet werden", Toast.LENGTH_SHORT).show();
        }
        return true;
    }

    private void updateAddressSuggestions(String query) {
        String search = query == null ? "" : query.trim().toLowerCase();
        addressSuggestionUrls.clear();

        ArrayList<String> labels = new ArrayList<>();
        if (search.isEmpty()) return;

        JSONArray[] sources = {
                BrowserDataStore.getHistory(this),
                BrowserDataStore.getBookmarks(this)
        };

        for (JSONArray source : sources) {
            for (int i = source.length() - 1; i >= 0; i--) {
                JSONObject item = source.optJSONObject(i);
                if (item == null) continue;

                String url = item.optString("url", "");
                String title = item.optString("title", url);

                if (url.isEmpty()
                        || !(title.toLowerCase().contains(search)
                        || url.toLowerCase().contains(search))
                        || addressSuggestionUrls.contains(url)) {
                    continue;
                }

                addressSuggestionUrls.add(url);
                labels.add(title + " · " + url);
                if (labels.size() >= 8) break;
            }
            if (labels.size() >= 8) break;
        }

        AutoCompleteTextView input = (AutoCompleteTextView) addressBar;
        input.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, labels));

        if (!labels.isEmpty() && input.hasFocus()) {
            input.showDropDown();
        } else {
            input.dismissDropDown();
        }
    }

    private void toggleReaderMode() {
        if (webView == null || webView.getUrl() == null) return;

        WebView target = webView;
        String originalUrl = readerOriginalUrls.get(target);

        if (originalUrl != null) {
            readerOriginalUrls.remove(target);
            target.loadUrl(originalUrl);
            return;
        }

        final String sourceUrl = target.getUrl();

        target.evaluateJavascript(
                "(function(){return JSON.stringify({title:document.title,"
                + "text:document.body?document.body.innerText:''});})()",
                result -> {
                    if (result == null || "null".equals(result)) return;

                    try {
                        JSONArray wrapper = new JSONArray("[" + result + "]");
                        JSONObject data = wrapper.optJSONObject(0);
                        if (data == null) return;

                        String title = data.optString("title", "Lesemodus");
                        String text = data.optString("text", "");
                        if (text.trim().isEmpty()) {
                            Toast.makeText(MainActivity.this,
                                    "Kein lesbarer Text gefunden",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }

                        String html = "<!doctype html><html><head>"
                                + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">"
                                + "<style>body{background:#090d12;color:#e8edf4;"
                                + "font-family:sans-serif;line-height:1.75;"
                                + "padding:24px;max-width:760px;margin:auto}"
                                + "h1{font-size:25px;color:#55cfff}"
                                + "pre{white-space:pre-wrap;font:inherit}</style>"
                                + "</head><body><h1>" + escapeReaderHtml(title)
                                + "</h1><pre>" + escapeReaderHtml(text)
                                + "</pre></body></html>";

                        readerOriginalUrls.put(target, sourceUrl);
                        target.loadDataWithBaseURL(sourceUrl, html,
                                "text/html", "UTF-8", null);
                    } catch (Exception e) {
                        Toast.makeText(MainActivity.this,
                                "Lesemodus konnte nicht geöffnet werden",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private String escapeReaderHtml(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
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

        int icon = 0;
        String description = text;

        switch (text) {
            case "➜":
                icon = R.drawable.ic_browser_go;
                description = "Suchen oder öffnen";
                break;
            case "⋮":
                icon = R.drawable.ic_browser_menu;
                description = "Browser-Menü";
                break;
            case "‹":
                icon = R.drawable.ic_browser_back;
                description = "Zurück";
                break;
            case "›":
                icon = R.drawable.ic_browser_forward;
                description = "Vorwärts";
                break;
            case "⌂":
                icon = R.drawable.ic_browser_home;
                description = "Startseite";
                break;
            case "↻":
                icon = R.drawable.ic_browser_reload;
                description = "Neu laden";
                break;
            case "▢":
                icon = R.drawable.ic_browser_tabs;
                description = "Tabs";
                break;
            case "+":
                icon = R.drawable.ic_browser_add;
                description = "Neuer Tab";
                break;
            case "×":
                icon = R.drawable.ic_browser_close;
                description = "Schließen";
                break;
        }

        button.setContentDescription(description);
        if (icon != 0) {
            android.graphics.drawable.Drawable drawable =
                    getDrawable(icon).mutate();
            drawable.setTint(accent ? Color.rgb(0, 19, 29) : Color.WHITE);
            button.setText("");
            button.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null);
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_CHOOSER_REQUEST && fileChooserCallback != null) {
            Uri[] results = resultCode == Activity.RESULT_OK
                    ? WebChromeClient.FileChooserParams.parseResult(resultCode, data)
                    : null;
            fileChooserCallback.onReceiveValue(results);
            fileChooserCallback = null;
        }
    }

    private void saveSession() {
        JSONArray saved = new JSONArray();
        for (WebView tab : tabs) {
            String url = tab.getUrl();
            if (url != null && (url.startsWith("https://")
                    || url.startsWith("http://"))) {
                saved.put(url);
            }
        }
        getSharedPreferences("shadow_browser", MODE_PRIVATE).edit()
                .putString("shadow_persistent_tabs", saved.toString())
                .putInt("shadow_persistent_selected",
                        Math.max(0, tabs.indexOf(webView)))
                .apply();
    }

    private ArrayList<String> loadSession() {
        ArrayList<String> result = new ArrayList<>();
        try {
            String data = getSharedPreferences("shadow_browser", MODE_PRIVATE)
                    .getString("shadow_persistent_tabs", "[]");
            JSONArray saved = new JSONArray(data);
            for (int i = 0; i < saved.length(); i++) {
                String url = saved.optString(i, "");
                if (url.startsWith("https://") || url.startsWith("http://")) {
                    result.add(url);
                }
            }
        } catch (Exception ignored) {
        }
        return result;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        ArrayList<String> savedTabs = new ArrayList<>();
        for (WebView tab : tabs) {
            String url = tab.getUrl();
            if (url != null && (url.startsWith("https://")
                    || url.startsWith("http://"))) {
                savedTabs.add(url);
            }
        }

        outState.putStringArrayList("shadow_saved_tabs", savedTabs);
        outState.putInt("shadow_selected_tab", Math.max(0, tabs.indexOf(webView)));
    }

    @Override
    public void onBackPressed() {
        if (fullscreenView != null) {
            hideFullscreenVideo();
            return;
        }
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
        saveSession();
        hideFullscreenVideo();
        if (fileChooserCallback != null) {
            fileChooserCallback.onReceiveValue(null);
            fileChooserCallback = null;
        }
        for (WebView tab : new ArrayList<>(tabs)) {
            tab.stopLoading();
            tab.setWebChromeClient(null);
            tab.setWebViewClient(null);
            webContainer.removeView(tab);
            tab.destroy();
        }
        tabs.clear();
        readerOriginalUrls.clear();
        webView = null;
        super.onDestroy();
    }
}
