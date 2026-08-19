package wallet.ui;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.EditText;
import android.widget.ImageView;
import wallet.R;

import java.net.URI;
import java.net.URISyntaxException;

public class BrowserActivity extends AbstractWalletActivity {

    private EditText urlBar;
    private WebView webView;
    private ImageView btnBackWeb;
    private ImageView btnForwardWeb;
    private ImageView btnGo;
    private FrameLayout rootLayout;
    private FrameLayout webViewContainer;
    private LinearLayout toolbarContainer;
    
    private static WebView staticWebView = null;
    private static String lastUrl = null;

    private View customView;
    private WebChromeClient.CustomViewCallback customViewCallback;
    private int originalSystemUiVisibility;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browser);

        rootLayout = (FrameLayout) findViewById(android.R.id.content);
        webViewContainer = findViewById(R.id.webview_container);
        toolbarContainer = findViewById(R.id.toolbar_container);

        if (getActionBar() != null) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
            getActionBar().setHomeButtonEnabled(true);
        }

        urlBar = findViewById(R.id.url_bar);
        btnBackWeb = findViewById(R.id.btn_back_web);
        btnForwardWeb = findViewById(R.id.btn_forward_web);
        btnGo = findViewById(R.id.btn_go);

        // Cập nhật màu khi mở app
        updateThemeColors();

        if (staticWebView != null) {
            if (staticWebView.getParent() != null) {
                ((FrameLayout) staticWebView.getParent()).removeView(staticWebView);
            }
            webViewContainer.addView(staticWebView);
            webView = staticWebView;
            urlBar.setText(lastUrl != null ? lastUrl : "");
            setupButtons();
            setupWebChromeClient();
            return;
        }

        webView = new WebView(getApplicationContext());
        webView.setLayoutParams(new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ));

        webViewContainer.addView(webView);
        staticWebView = webView;

        // ==================================================
        // BẬT TẤT CẢ ĐỊNH DẠNG WEB — ĐÃ BỎ setAppCacheEnabled
        // ==================================================
        WebSettings webSettings = webView.getSettings();

        webSettings.setJavaScriptEnabled(true);
        webSettings.setAllowFileAccessFromFileURLs(true);
        webSettings.setAllowUniversalAccessFromFileURLs(true);

        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);

        webSettings.setMediaPlaybackRequiresUserGesture(false);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);

        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        webSettings.setRenderPriority(WebSettings.RenderPriority.HIGH);

        String ua = webSettings.getUserAgentString();
        webSettings.setUserAgentString(ua + " Chrome/120.0.0.0 Mobile");

        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);

        webView.setFocusable(true);
        webView.setFocusableInTouchMode(true);
        webView.setBackgroundColor(0);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                view.loadUrl(url);
                urlBar.setText(url);
                lastUrl = url;
                return true;
            }
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                lastUrl = url;
            }
        });

        setupWebChromeClient();
        setupButtons();

        Intent intent = getIntent();
        if (intent.getData() != null) {
            String url = intent.getData().toString();
            urlBar.setText(url);
            webView.loadUrl(url);
            lastUrl = url;
        }
    }

    // ==================================================
    // CHỈ ĐỔI NỀN + Ô URL — KHÔNG FIX CỨNG MÀU
    // ==================================================
    private void updateThemeColors() {
        // Lấy màu nền từ theme
        int[] windowAttrs = { android.R.attr.windowBackground };
        TypedArray ta = obtainStyledAttributes(windowAttrs);
        int bgColor = ta.getColor(0, 0xFFFFFFFF);
        ta.recycle();

        // Lấy màu chữ từ theme
        int[] textColorAttrs = { android.R.attr.textColorPrimary };
        ta = obtainStyledAttributes(textColorAttrs);
        int textColor = ta.getColor(0, 0xFF000000);
        ta.recycle();

        // Lấy màu chữ gợi ý từ theme
        int[] hintColorAttrs = { android.R.attr.textColorHint };
        ta = obtainStyledAttributes(hintColorAttrs);
        int hintColor = ta.getColor(0, 0xFF888888);
        ta.recycle();

        // === CẬP NHẬT NỀN ===
        if (webViewContainer != null) webViewContainer.setBackgroundColor(bgColor);
        if (webView != null) webView.setBackgroundColor(bgColor);

        // === CẬP NHẬT Ô URL ===
        if (urlBar != null) {
            urlBar.setTextColor(textColor);
            urlBar.setHintTextColor(hintColor);
        }

        // ❌ Không đổi màu icon, không đổi thanh toolbar — để theme xử lý như bản cũ
    }

    // Đổi cấu hình → cập nhật màu ngay lập tức
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateThemeColors();
    }

    private void setupWebChromeClient() {
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
                
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                
                if (getActionBar() != null) getActionBar().hide();
                getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN |
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                );
                rootLayout.addView(view);
            }
            @Override
            public void onHideCustomView() {
                if (customView == null) return;
                
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                
                rootLayout.removeView(customView);
                customView = null;
                if (getActionBar() != null) getActionBar().show();
                getWindow().getDecorView().setSystemUiVisibility(originalSystemUiVisibility);
                customViewCallback.onCustomViewHidden();
                customViewCallback = null;
            }
        });
    }

    private void setupButtons() {
        btnBackWeb.setOnClickListener(v -> { if (webView.canGoBack()) webView.goBack(); });
        btnForwardWeb.setOnClickListener(v -> { if (webView.canGoForward()) webView.goForward(); });
        btnGo.setOnClickListener(v -> handleUrlInput());
        urlBar.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_GO ||
                (event != null && event.getKeyCode() == android.view.KeyEvent.KEYCODE_ENTER
                    && event.getAction() == android.view.KeyEvent.ACTION_DOWN)) {
                handleUrlInput();
                return true;
            }
            return false;
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        webView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        webView.onResume();
        updateThemeColors(); // Cập nhật màu khi quay lại
        if (lastUrl != null) urlBar.setText(lastUrl);
    }

    @Override
    public void onBackPressed() {
        if (customView != null && customViewCallback != null) {
            customViewCallback.onCustomViewHidden();
            return;
        }
        if (webView.canGoBack()) {
            webView.goBack();
            return;
        }
        Intent walletIntent = new Intent(this, WalletActivity.class);
        walletIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(walletIntent);
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            if (customView != null && customViewCallback != null) {
                customViewCallback.onCustomViewHidden();
                return true;
            }
            Intent walletIntent = new Intent(this, WalletActivity.class);
            walletIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(walletIntent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        if (isFinishing() && staticWebView != null) {
            staticWebView.stopLoading();
            staticWebView.destroy();
            staticWebView = null;
            lastUrl = null;
        }
        super.onDestroy();
    }

    private void handleUrlInput() {
        String input = urlBar.getText().toString().trim();
        android.view.inputmethod.InputMethodManager imm =
            (android.view.inputmethod.InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(urlBar.getWindowToken(), 0);
        urlBar.clearFocus();
        
        if (input.isEmpty()) return;
        String finalUrl;
        if (isValidUrl(input)) {
            finalUrl = input.startsWith("http") ? input : "https://" + input;
        } else {
            finalUrl = "https://www.google.com/search?q=" + Uri.encode(input);
        }
        webView.loadUrl(finalUrl);
        urlBar.setText(finalUrl);
        lastUrl = finalUrl;
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
}
