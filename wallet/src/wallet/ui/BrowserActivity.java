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
    private WebView webView;
    private ImageView btnBackWeb;
    private ImageView btnForwardWeb;
    private ImageView btnGo;
    private LinearLayout toolbarContainer;
    private View rootLayout;

    private View customView;
    private WebChromeClient.CustomViewCallback customViewCallback;
    private int originalSystemUiVisibility;

    // ========== LƯU TRẠNG THÁI — KHÔNG BỊ RESET VIDEO ==========
    private static Bundle savedWebViewState = null;
    private static String savedUrl = null;
    private static int scrollX = 0;
    private static int scrollY = 0;
    private static boolean hasSavedState = false;
    private static boolean isNewLink = false;
    private static String savedUrlBeforePause = null; // ✅ Lưu URL trước khi tạm dừng

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

        updateAllColors();

        // ==================================================
        // BẬT TẤT CẢ TÍNH NĂNG — QUAN TRỌNG NHẤT CHO VIDEO
        // ==================================================
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK); // ✅ ƯU TIÊN DÙNG CACHE TRƯỚC
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setAllowFileAccessFromFileURLs(true);
        webSettings.setAllowUniversalAccessFromFileURLs(true);
        webSettings.setLoadsImagesAutomatically(true);
        webSettings.setBlockNetworkImage(false);
        webSettings.setBlockNetworkLoads(false);
        webSettings.setMediaPlaybackRequiresUserGesture(false); // ✅ Tự động phát
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        webSettings.setGeolocationEnabled(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);

        // ✅ BẮT BUỘC — HARDWARE ACCEL + KEEP SCREEN ON = VIDEO KHÔNG DỪNG
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        webSettings.setRenderPriority(WebSettings.RenderPriority.HIGH);
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); // ✅ MÀN HÌNH KHÔNG TẮT KHI PHÁT VIDEO

        String ua = webSettings.getUserAgentString();
        webSettings.setUserAgentString(ua + " Chrome/120.0.0.0 Mobile");
        webView.setFocusable(true);
        webView.setFocusableInTouchMode(true);

        // ==================================================
        // XỬ LÝ LINK — KHÔNG RESET NẾU CÙNG URL
        // ==================================================
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

                // ✅ NẾU URL GIỐNG HỆT → KHÔNG LOAD LẠI
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
                // ✅ KHÔI PHỤC VỊ TRÍ CUỘN — CHỈ 1 LẦN KHI MỞ LẠI
                if (hasSavedState && !isNewLink) {
                    webView.scrollTo(scrollX, scrollY);
                    hasSavedState = false; // ✅ ĐỂ KHÔNG GỌI LẠI NHIỀU LẦN
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
        // ✅ KHÔI PHỤC TRẠNG THÁI — CHỈ KHI CÓ DỮ LIỆU LƯU
        // ==================================================
        Intent intent = getIntent();
        Uri intentData = intent.getData();

        if (intentData != null) {
            // 🔴 LINK MỚI → LOAD MỚI, XÓA TRẠNG THÁI CŨ
            String newUrl = intentData.toString();
            urlBar.setText(newUrl);
            webView.loadUrl(newUrl);
            clearSavedState();
            isNewLink = true;
        } else if (savedUrlBeforePause != null && savedWebViewState != null) {
            // ✅ QUAY LẠI TỪ NỀN → KHÔI PHỤC TRẠNG THÁI, KHÔNG LOAD LẠI
            webView.restoreState(savedWebViewState);
            urlBar.setText(savedUrlBeforePause);
            isNewLink = false;
            // scrollTo sẽ gọi trong onPageFinished
        } else if (savedInstanceState != null) {
            // ✅ XOAY MÀN HÌNH
            webView.restoreState(savedInstanceState);
            String restoredUrl = webView.getUrl();
            if (restoredUrl != null) urlBar.setText(restoredUrl);
        }
    }

    // ==================================================
    // ✅ QUAN TRỌNG NHẤT — KHÔNG DỪNG WEBVIEW KHI RA NỀN
    // ==================================================
    @Override
    protected void onPause() {
        // ❌ KHÔNG GỌI webView.onPause() → video tiếp tục phát
        // ❌ KHÔNG gọi super.onPause() trước khi lưu
        if (webView != null && !isFinishing()) {
            // ✅ LƯU TRƯỚC KHI TẠM DỪNG — KHÔNG ĐỢI ĐẾN DESTROY
            savedWebViewState = new Bundle();
            webView.saveState(savedWebViewState);
            savedUrlBeforePause = webView.getUrl();
            scrollX = webView.getScrollX();
            scrollY = webView.getScrollY();
            hasSavedState = true;
        }
        // ✅ CHỈ GỌI super — KHÔNG DỪNG WebView
        super.onPause();
        // ❌ KHÔNG gọi webView.onPause() — ĐỂ VIDEO TIẾP TỤC PHÁT
    }

    @Override
    protected void onResume() {
        super.onResume();
        // ✅ KHÔNG GỌI webView.onResume() — tránh reset
        updateAllColors();
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
        // ✅ LƯU TRƯỚC KHI ĐÓNG
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
    // ✅ CẬP NHẬT MÀU THEO THEME
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
        webView.loadUrl(finalUrl);
        urlBar.setText(finalUrl);
        clearSavedState(); // ✅ Nhập URL mới → reset trạng thái cũ
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
        // ❌ KHÔNG gọi webView.destroy() — để hệ thống quản lý
        super.onDestroy();
    }
}
