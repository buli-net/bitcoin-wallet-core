package wallet.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.app.AlertDialog; //
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;
import wallet.R;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class BrowserActivity extends AbstractWalletActivity {

    private EditText urlBar;
    private WebView webView;
    private ImageView btnBackWeb;
    private ImageView btnForwardWeb;
    private ImageView btnRefreshWeb;
    private LinearLayout toolbarContainer;
    private View rootLayout;

    // === THÊM 3 NÚT MỚI ===
    private ImageView btnHome;
    private ImageView btnHistory;
    private ImageView btnBrowserSettings;
    private static final String PREFS_NAME = "BrowserPrefs";
    private static final String KEY_HOME_URL = "home_url";
    private final List<String> historyList = new ArrayList<>();
    // === KẾT THÚC THÊM ===

    private View customView;
    private WebChromeClient.CustomViewCallback customViewCallback;
    private int originalSystemUiVisibility;

    private static Bundle savedWebViewState = null;
    private static String savedUrl = null;
    private static int scrollX = 0;
    private static int scrollY = 0;
    private static boolean hasSavedState = false;
    private static boolean isNewLink = false;
    private static String savedUrlBeforePause = null;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browser);

        rootLayout = findViewById(android.R.id.content);
        toolbarContainer = findViewById(R.id.toolbar_container);

        if (getActionBar() != null) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }

        urlBar = findViewById(R.id.url_bar);
        webView = findViewById(R.id.webview);
        btnBackWeb = findViewById(R.id.btn_back_web);
        btnForwardWeb = findViewById(R.id.btn_forward_web);
        btnRefreshWeb = findViewById(R.id.btn_refresh_web);

        // === THÊM KHỞI TẠO 3 NÚT ===
        btnHome = findViewById(R.id.btn_home);
        btnHistory = findViewById(R.id.btn_history);
        btnBrowserSettings = findViewById(R.id.btn_browser_settings);
        // === KẾT THÚC THÊM ===

        updateAllColors();

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setAllowFileAccessFromFileURLs(true);
        webSettings.setAllowUniversalAccessFromFileURLs(true);
        webSettings.setLoadsImagesAutomatically(true);
        webSettings.setBlockNetworkImage(false);
        webSettings.setBlockNetworkLoads(false);
        webSettings.setMediaPlaybackRequiresUserGesture(false);
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        webSettings.setGeolocationEnabled(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);

        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        webSettings.setRenderPriority(WebSettings.RenderPriority.HIGH);
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        String ua = webSettings.getUserAgentString();
        webSettings.setUserAgentString(ua + " Chrome/120.0.0.0 Mobile");
        webView.setFocusable(true);
        webView.setFocusableInTouchMode(true);

        webView.setWebViewClient(new WebViewClient() {
            private boolean isRestoring = false;

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                String scheme = request.getUrl().getScheme();

                if (scheme != null && !scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https")) {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        view.getContext().startActivity(intent);
                        urlBar.setText(url);
                        return true;
                    } catch (Exception e) {
                        urlBar.setText(url);
                        return true;
                    }
                }

                if (isRestoring) {
                    isRestoring = false;
                    urlBar.setText(url);
                    return true;
                }

                if (url.equals(view.getUrl()) && !isNewLink) {
                    urlBar.setText(url);
                    return true;
                }

                view.loadUrl(url);
                urlBar.setText(url);
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (urlBar != null && !urlBar.getText().toString().equals(url)) {
                    urlBar.setText(url);
                }
                savedUrl = url;

                // === THÊM LƯU LỊCH SỬ ===
                if (!historyList.contains(url)) {
                    historyList.add(url);
                }
                // === KẾT THÚC THÊM ===

                if (hasSavedState && !isNewLink) {
                    webView.scrollTo(scrollX, scrollY);
                    hasSavedState = false;
                }
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                if (customView != null) {
                    customViewCallback.onCustomViewHidden();
                    return;
                }
                customView = view;
                customViewCallback = callback;
                originalSystemUiVisibility = getWindow().getDecorView().getSystemUiVisibility();
                if (getActionBar() != null) getActionBar().hide();
                getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN |
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                );
                ((FrameLayout) rootLayout).addView(view);
            }

            @Override
            public void onHideCustomView() {
                if (customView == null) return;
                ((FrameLayout) rootLayout).removeView(customView);
                customView = null;
                if (getActionBar() != null) getActionBar().show();
                getWindow().getDecorView().setSystemUiVisibility(originalSystemUiVisibility);
                customViewCallback.onCustomViewHidden();
                customViewCallback = null;
            }
        });

        btnBackWeb.setOnClickListener(v -> { if (webView.canGoBack()) webView.goBack(); });
        btnForwardWeb.setOnClickListener(v -> { if (webView.canGoForward()) webView.goForward(); });
        btnRefreshWeb.setOnClickListener(v -> {
            hideKeyboard();
            webView.reload();
        });

        // === THÊM SỰ KIỆN 3 NÚT ===
        btnHome.setOnClickListener(v -> loadHomeUrl());

        btnHistory.setOnClickListener(v -> showHistoryDialog());

        btnBrowserSettings.setOnClickListener(v -> showSetHomeDialog());
        // === KẾT THÚC THÊM ===

        urlBar.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO ||
                (event != null && event.getKeyCode() == android.view.KeyEvent.KEYCODE_ENTER
                    && event.getAction() == android.view.KeyEvent.ACTION_DOWN)) {
                handleUrlInput();
                return true;
            }
            return false;
        });

        Intent intent = getIntent();
        Uri intentData = intent.getData();

        if (intentData != null) {
            String newUrl = intentData.toString();
            urlBar.setText(newUrl);
            webView.loadUrl(newUrl);
            clearSavedState();
            isNewLink = true;
        } else if (savedWebViewState != null && savedUrlBeforePause != null) {
            webView.restoreState(savedWebViewState);
            urlBar.setText(savedUrlBeforePause);
            isNewLink = false;
            hasSavedState = true;
        } else if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState);
            String restoredUrl = webView.getUrl();
            if (restoredUrl != null) urlBar.setText(restoredUrl);
        }
    }

    @Override
    protected void onPause() {
        if (webView != null && !isFinishing()) {
            savedWebViewState = new Bundle();
            webView.saveState(savedWebViewState);
            savedUrlBeforePause = webView.getUrl();
            scrollX = webView.getScrollX();
            scrollY = webView.getScrollY();
            hasSavedState = true;
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateAllColors();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        Uri data = intent.getData();
        if (data != null) {
            String newUrl = data.toString();
            urlBar.setText(newUrl);
            webView.loadUrl(newUrl);
            clearSavedState();
            isNewLink = true;
        }
    }

    private void clearSavedState() {
        savedWebViewState = null;
        savedUrlBeforePause = null;
        scrollX = 0;
        scrollY = 0;
        hasSavedState = false;
        isNewLink = false;
    }

    @Override
    public void finish() {
        if (webView != null && !isFinishing()) {
            savedWebViewState = new Bundle();
            webView.saveState(savedWebViewState);
            savedUrlBeforePause = webView.getUrl();
            scrollX = webView.getScrollX();
            scrollY = webView.getScrollY();
            hasSavedState = true;
        }
        super.finish();
    }

    @Override
    public void onBackPressed() {
        if (customView != null && customViewCallback != null) {
            customViewCallback.onCustomViewHidden();
            return;
        }
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            if (webView != null && !isFinishing()) {
                savedWebViewState = new Bundle();
                webView.saveState(savedWebViewState);
                savedUrlBeforePause = webView.getUrl();
                scrollX = webView.getScrollX();
                scrollY = webView.getScrollY();
                hasSavedState = true;
            }
            super.onBackPressed();
        }
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            if (webView != null && !isFinishing()) {
                savedWebViewState = new Bundle();
                webView.saveState(savedWebViewState);
                savedUrlBeforePause = webView.getUrl();
                scrollX = webView.getScrollX();
                scrollY = webView.getScrollY();
                hasSavedState = true;
            }
            if (customView != null && customViewCallback != null) {
                customViewCallback.onCustomViewHidden();
            }
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateAllColors() {
        int bgActionBarColor = getResources().getColor(R.color.bg_action_bar);
        int fgIconColor = getResources().getColor(R.color.fg_on_dark_bg_network_significant);

        if (toolbarContainer != null) toolbarContainer.setBackgroundColor(bgActionBarColor);
        if (btnBackWeb != null) btnBackWeb.setColorFilter(fgIconColor);
        if (btnForwardWeb != null) btnForwardWeb.setColorFilter(fgIconColor);
        if (btnRefreshWeb != null) btnRefreshWeb.setColorFilter(fgIconColor);

        // === THÊM MÀU CHO 3 NÚT MỚI ===
        if (btnHome != null) btnHome.setColorFilter(fgIconColor);
        if (btnHistory != null) btnHistory.setColorFilter(fgIconColor);
        if (btnBrowserSettings != null) btnBrowserSettings.setColorFilter(fgIconColor);
        // === KẾT THÚC THÊM ===

        if (urlBar != null) {
            int[] textColorAttr = { android.R.attr.textColorPrimary };
            TypedArray taText = obtainStyledAttributes(textColorAttr);
            int textColor = taText.getColor(0, 0);
            taText.recycle();
            urlBar.setTextColor(textColor);

            int[] hintColorAttr = { android.R.attr.textColorHint };
            TypedArray taHint = obtainStyledAttributes(hintColorAttr);
            int hintColor = taHint.getColor(0, 0);
            taHint.recycle();
            urlBar.setHintTextColor(hintColor);

            Drawable urlBg = getResources().getDrawable(R.drawable.edittext_background);
            urlBar.setBackground(urlBg);
        }

        int[] windowBgAttr = { android.R.attr.windowBackground };
        TypedArray taBg = obtainStyledAttributes(windowBgAttr);
        int windowBg = taBg.getColor(0, 0);
        taBg.recycle();
        if (webView != null) webView.setBackgroundColor(windowBg);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateAllColors();
    }

    private void handleUrlInput() {
        String input = urlBar.getText().toString().trim();
        hideKeyboard();
        if (input.isEmpty()) return;
        String finalUrl;
        if (isValidUrl(input)) {
            finalUrl = input.startsWith("http") ? input : "https://" + input;
        } else {
            finalUrl = "https://www.google.com/search?q=" + Uri.encode(input);
        }
        webView.loadUrl(finalUrl);
        urlBar.setText(finalUrl);
        clearSavedState();
    }

    private boolean isValidUrl(String input) {
        if (!input.contains(".") || input.contains(" ")) return false;
        try {
            URI uri = new URI(input.startsWith("http") ? input : "https://" + input);
            return uri.getHost() != null && uri.getHost().contains(".");
        } catch (URISyntaxException e) {
            return false;
        }
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(urlBar.getWindowToken(), 0);
        urlBar.clearFocus();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (webView != null) webView.saveState(outState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    // === THÊM TOÀN BỘ HÀM MỚI ===
    private void loadHomeUrl() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String homeUrl = prefs.getString(KEY_HOME_URL, "https://www.google.com");
        webView.loadUrl(homeUrl);
        urlBar.setText(homeUrl);
    }

    private void showHistoryDialog() {
        if (historyList.isEmpty()) {
            new AlertDialog.Builder(this)
                .setMessage("Chưa có lịch sử duyệt web")
                .setPositiveButton("Đóng", null)
                .show();
            return;
        }

        CharSequence[] items = historyList.toArray(new CharSequence[0]);
        new AlertDialog.Builder(this)
            .setTitle("Lịch sử duyệt web")
            .setItems(items, (dialog, which) -> {
                String url = historyList.get(which);
                webView.loadUrl(url);
                urlBar.setText(url);
            })
            .setPositiveButton("Xóa tất cả", (dialog, which) -> {
                historyList.clear();
                Toast.makeText(this, "Đã xóa lịch sử", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Đóng", null)
            .show();
    }

    private void showSetHomeDialog() {
        EditText input = new EditText(this);
        input.setHint("Nhập URL trang chủ (vd: google.com)");

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String currentHome = prefs.getString(KEY_HOME_URL, "https://www.google.com");
        input.setText(currentHome);

        new AlertDialog.Builder(this)
            .setTitle("Cài đặt trang chủ")
            .setView(input)
            .setPositiveButton("Lưu", (dialog, which) -> {
                String url = input.getText().toString().trim();
                if (url.isEmpty()) return;
                if (!url.startsWith("http")) url = "https://" + url;
                SharedPreferences.Editor edit = prefs.edit();
                edit.putString(KEY_HOME_URL, url);
                edit.apply();
                Toast.makeText(this, "Đã lưu trang chủ", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Hủy", null)
            .show();
    }
    // === KẾT THÚC THÊM HÀM MỚI ===
}
