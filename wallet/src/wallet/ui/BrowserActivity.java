package wallet.ui;

import android.content.Context;
import android.content.Intent;
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
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import wallet.R;

import java.net.URI;
import java.net.URISyntaxException;

public class BrowserActivity extends AbstractWalletActivity {

    private EditText urlBar;
    private static WebView webView; // ✅ static — GIỮ NGUYÊN, KHÔNG TẠO LẠI
    private ImageView btnBackWeb;
    private ImageView btnForwardWeb;
    private ImageView btnGo;
    private LinearLayout toolbarContainer;
    private View rootLayout;

    private View customView;
    private WebChromeClient.CustomViewCallback customViewCallback;
    private int originalSystemUiVisibility;

    // ========== LƯU TRẠNG THÁI — KHÔNG BỊ RESET VIDEO ==========
    private static String savedUrl = null;
    private static int scrollX = 0;
    private static int scrollY = 0;
    private static boolean isFirstInit = true;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browser);

        rootLayout = findViewById(android.R.id.content);
        toolbarContainer = findViewById(R.id.toolbar_container);

        if (getActionBar() != null) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
            getActionBar().setHomeButtonEnabled(true);
        }

        urlBar = findViewById(R.id.url_bar);
        btnBackWeb = findViewById(R.id.btn_back_web);
        btnForwardWeb = findViewById(R.id.btn_forward_web);
        btnGo = findViewById(R.id.btn_go);

        // ✅ TẠO WEBVIEW 1 LẦN DUY NHẤT — KHÔNG TẠO LẠI
        if (webView == null) {
            webView = findViewById(R.id.webview);
            setupWebView(webView);
            isFirstInit = true;
        } else {
            // ✅ MỞ LẠI — GỬI LẠI VIEW CŨ, KHÔNG TẠO MỚI
            ViewGroup parent = (ViewGroup) webView.getParent();
            if (parent != null) parent.removeView(webView);
            FrameLayout container = findViewById(R.id.webview_container);
            if (container != null) container.addView(webView);
            isFirstInit = false;
        }

        updateAllColors();

        // ✅ KHÔI PHỤC TRẠNG THÁI — KHÔNG LOAD LẠI
        if (savedUrl != null && !isFirstInit) {
            urlBar.setText(savedUrl);
            webView.scrollTo(scrollX, scrollY);
        }

        // ==================================================
        // BẬT TẤT CẢ TÍNH NĂNG — ĐỊNH DẠNG VIDEO + CACHE
        // ==================================================
        if (isFirstInit) {
            WebSettings webSettings = webView.getSettings();
            webSettings.setJavaScriptEnabled(true);
            webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
            webSettings.setDomStorageEnabled(true);
            webSettings.setDatabaseEnabled(true);
            webSettings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK); // ✅ Ưu tiên cache — không tải lại
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

            setupWebViewClients();
        }

        // ==================================================
        // NÚT BẤM — GIỮ NGUYÊN
        // ==================================================
        btnBackWeb.setOnClickListener(v -> { if (webView.canGoBack()) webView.goBack(); });
        btnForwardWeb.setOnClickListener(v -> { if (webView.canGoForward()) webView.goForward(); });
        btnGo.setOnClickListener(v -> handleUrlInput());

        urlBar.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO ||
                (event != null && event.getKeyCode() == android.view.KeyEvent.KEYCODE_ENTER
                    && event.getAction() == android.view.KeyEvent.ACTION_DOWN)) {
                handleUrlInput();
                return true;
            }
            return false;
        });

        // ==================================================
        // LINK MỚI — CHỈ LOAD NẾU KHÁC URL HIỆN TẠI
        // ==================================================
        Intent intent = getIntent();
        Uri intentData = intent.getData();
        if (intentData != null) {
            String newUrl = intentData.toString();
            if (!newUrl.equals(savedUrl)) {
                urlBar.setText(newUrl);
                webView.loadUrl(newUrl);
                savedUrl = newUrl;
            }
        }
    }

    // ✅ TÁCH RIÊNG SETUP — CHỈ CHẠY 1 LẦN
    private void setupWebView(WebView wv) {}

    private void setupWebViewClients() {
        // ==================================================
        // XỬ LÝ LINK — CÙNG URL → KHÔNG LOAD LẠI
        // ==================================================
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                String scheme = request.getUrl().getScheme();

                if (scheme != null && !scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https")) {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        view.getContext().startActivity(intent);
                        urlBar.setText(url);
                        savedUrl = url;
                        return true;
                    } catch (Exception e) {
                        urlBar.setText(url);
                        return true;
                    }
                }

                // ✅ CÙNG URL → BỎ QUA, KHÔNG LOAD LẠI
                if (url.equals(savedUrl)) {
                    urlBar.setText(url);
                    return true;
                }

                view.loadUrl(url);
                urlBar.setText(url);
                savedUrl = url;
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                savedUrl = url;
                if (urlBar != null && !urlBar.getText().toString().equals(url)) {
                    urlBar.setText(url);
                }
                webView.scrollTo(scrollX, scrollY);
            }
        });

        // ✅ GIỮ NGUYÊN FULLSCREEN VIDEO
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
    }

    // ==================================================
    // ✅ LƯU TRƯỚC KHI TẠM DỪNG — KHÔNG DỪNG WEBVIEW
    // ==================================================
    @Override
    protected void onPause() {
        super.onPause();
        if (webView != null && !isFinishing()) {
            savedUrl = webView.getUrl();
            scrollX = webView.getScrollX();
            scrollY = webView.getScrollY();
            // ❌ KHÔNG GỌI webView.onPause() — VIDEO TIẾP TỤC PHÁT
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateAllColors();
        if (webView != null) {
            webView.scrollTo(scrollX, scrollY);
            // ❌ KHÔNG GỌI webView.onResume() — TRÁNH RESET
        }
    }

    // ==================================================
    // ✅ NHẬN LINK MỚI KHI ĐANG MỞ
    // ==================================================
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        Uri data = intent.getData();
        if (data != null) {
            String newUrl = data.toString();
            if (!newUrl.equals(savedUrl)) {
                savedUrl = newUrl;
                urlBar.setText(newUrl);
                webView.loadUrl(newUrl);
            }
        }
    }

    @Override
    public void finish() {
        if (webView != null) {
            savedUrl = webView.getUrl();
            scrollX = webView.getScrollX();
            scrollY = webView.getScrollY();
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
            super.onBackPressed();
        }
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            if (customView != null && customViewCallback != null) {
                customViewCallback.onCustomViewHidden();
            }
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // ==================================================
    // ✅ GIỮ NGUYÊN TOÀN BỘ UPDATE MÀU
    // ==================================================
    private void updateAllColors() {
        int bgActionBarColor = getResources().getColor(R.color.bg_action_bar);
        int fgIconColor = getResources().getColor(R.color.fg_on_dark_bg_network_significant);

        if (toolbarContainer != null) toolbarContainer.setBackgroundColor(bgActionBarColor);
        if (btnBackWeb != null) btnBackWeb.setColorFilter(fgIconColor);
        if (btnForwardWeb != null) btnForwardWeb.setColorFilter(fgIconColor);
        if (btnGo != null) btnGo.setColorFilter(fgIconColor);

        if (urlBar != null) {
            int[] textColorAttr = { android.R.attr.textColorPrimary };
            TypedArray taText = obtainStyledAttributes(textColorAttr);
            int textColor = taText.getColor(0, 0xFF000000);
            taText.recycle();
            urlBar.setTextColor(textColor);

            int[] hintColorAttr = { android.R.attr.textColorHint };
            TypedArray taHint = obtainStyledAttributes(hintColorAttr);
            int hintColor = taHint.getColor(0, 0xFF888888);
            taHint.recycle();
            urlBar.setHintTextColor(hintColor);

            Drawable urlBg = getResources().getDrawable(R.drawable.edittext_background);
            urlBar.setBackground(urlBg);
        }

        int[] windowBgAttr = { android.R.attr.windowBackground };
        TypedArray taBg = obtainStyledAttributes(windowBgAttr);
        int windowBg = taBg.getColor(0, 0xFFFFFFFF);
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
        if (!finalUrl.equals(savedUrl)) {
            savedUrl = finalUrl;
            webView.loadUrl(finalUrl);
            urlBar.setText(finalUrl);
        }
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

    // ✅ KHÔNG DESTROY WEBVIEW — giữ cache + video
    @Override
    protected void onDestroy() {
        if (webView != null && webView.getParent() != null) {
            ((ViewGroup) webView.getParent()).removeView(webView);
        }
        super.onDestroy();
    }
}
