package wallet.ui;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebSettings.MixedContentMode;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import wallet.R;

import java.net.URI;
import java.net.URISyntaxException;

public class BrowserActivity extends AbstractWalletActivity {

    private EditText urlBar;
    private WebView webView;
    private ImageView btnBackWeb;
    private ImageView btnForwardWeb;
    private ImageView btnGo;
    private LinearLayout toolbarContainer;
    private View rootLayout;

    private View customView;
    private WebChromeClient.CustomViewCallback customViewCallback;
    private int originalSystemUiVisibility;

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
        webView = findViewById(R.id.webview);
        btnBackWeb = findViewById(R.id.btn_back_web);
        btnForwardWeb = findViewById(R.id.btn_forward_web);
        btnGo = findViewById(R.id.btn_go);

        updateThemeColors();

        // ==================================================
        // ✅ BẬT FULL HẾT TẤT CẢ TÍNH NĂNG WEB — KHÔNG CHẶN GÌ
        // ==================================================
        WebSettings webSettings = webView.getSettings();

        // 🔑 JavaScript & Logic
        webSettings.setJavaScriptEnabled(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);

        // 📂 Lưu trữ dữ liệu
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);

        // 📁 Truy cập file & nội dung
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setAllowFileAccessFromFileURLs(true);
        webSettings.setAllowUniversalAccessFromFileURLs(true);

        // 🖼️ Tải ảnh & tài nguyên
        webSettings.setLoadsImagesAutomatically(true);
        webSettings.setBlockNetworkImage(false);
        webSettings.setBlockNetworkLoads(false);

        // 🎬 Media & Video
        webSettings.setMediaPlaybackRequiresUserGesture(false);

        // 🔗 Mixed Content (HTTP + HTTPS cùng trang)
        webSettings.setMixedContentMode(MixedContentMode.MIXED_CONTENT_ALWAYS_ALLOW);

        // 📍 Vị trí địa lý
        webSettings.setGeolocationEnabled(true);

        // 📐 Viewport & Zoom
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);

        // ⚡ Hiệu năng
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        webSettings.setRenderPriority(WebSettings.RenderPriority.HIGH);

        // 👤 User Agent
        String ua = webSettings.getUserAgentString();
        webSettings.setUserAgentString(ua + " Chrome/120.0.0.0 Mobile");

        // 🎯 Focus
        webView.setFocusable(true);
        webView.setFocusableInTouchMode(true);
        webView.setBackgroundColor(0);

        // ==================================================
        // WebViewClient
        // ==================================================
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                view.loadUrl(url);
                urlBar.setText(url);
                return true;
            }
        });

        // ==================================================
        // WebChromeClient — Fullscreen Video
        // ==================================================
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

        // Nút bấm
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

        // Khôi phục trạng thái
        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState);
            String currentUrl = webView.getUrl();
            if (currentUrl != null) urlBar.setText(currentUrl);
        } else {
            Intent intent = getIntent();
            if (intent.getData() != null) {
                String url = intent.getData().toString();
                urlBar.setText(url);
                webView.loadUrl(url);
            }
        }
    }

    // ==================================================
    // CẬP NHẬT MÀU NGAY LẬP TỨC
    // ==================================================
    private void updateThemeColors() {
        int bgActionBarColor = getResources().getColor(R.color.bg_action_bar);
        int fgIconColor = getResources().getColor(R.color.fg_on_dark_bg_network_significant);

        if (toolbarContainer != null) toolbarContainer.setBackgroundColor(bgActionBarColor);
        if (btnBackWeb != null) btnBackWeb.setColorFilter(fgIconColor);
        if (btnForwardWeb != null) btnForwardWeb.setColorFilter(fgIconColor);
        if (btnGo != null) btnGo.setColorFilter(fgIconColor);

        int[] textColorAttrs = { android.R.attr.textColorPrimary };
        TypedArray ta = obtainStyledAttributes(textColorAttrs);
        int textColor = ta.getColor(0, 0xFF000000);
        ta.recycle();

        int[] hintColorAttrs = { android.R.attr.textColorHint };
        ta = obtainStyledAttributes(hintColorAttrs);
        int hintColor = ta.getColor(0, 0xFF888888);
        ta.recycle();

        if (urlBar != null) {
            urlBar.setTextColor(textColor);
            urlBar.setHintTextColor(hintColor);
        }

        int[] windowAttrs = { android.R.attr.windowBackground };
        ta = obtainStyledAttributes(windowAttrs);
        int bgColor = ta.getColor(0, 0xFFFFFFFF);
        ta.recycle();

        if (webView != null) webView.setBackgroundColor(bgColor);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateThemeColors();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateThemeColors();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (webView != null) webView.saveState(outState);
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
}
