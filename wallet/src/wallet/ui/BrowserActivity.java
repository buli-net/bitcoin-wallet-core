package wallet.ui;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
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
    
    // ✅ Lưu trạng thái toàn cục — không bị mất khi back
    private static String lastUrl = null;
    private static Bundle savedState = null;
    private static boolean isFirstCreate = true;

    private View customView;
    private WebChromeClient.CustomViewCallback customViewCallback;
    private int originalSystemUiVisibility;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browser);

        rootLayout = (FrameLayout) findViewById(android.R.id.content);

        if (getActionBar() != null) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
            getActionBar().setHomeButtonEnabled(true);
        }

        urlBar = findViewById(R.id.url_bar);
        webView = findViewById(R.id.webview);
        btnBackWeb = findViewById(R.id.btn_back_web);
        btnForwardWeb = findViewById(R.id.btn_forward_web);
        btnGo = findViewById(R.id.btn_go);

        // ✅ CẤU HÌNH PHÁT NỀN
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setMediaPlaybackRequiresUserGesture(false);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webView.setKeepScreenOn(true);
        webView.setFocusable(true);

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
                rootLayout.addView(view);
            }
            @Override
            public void onHideCustomView() {
                if (customView == null) return;
                rootLayout.removeView(customView);
                customView = null;
                if (getActionBar() != null) getActionBar().show();
                getWindow().getDecorView().setSystemUiVisibility(originalSystemUiVisibility);
                customViewCallback.onCustomViewHidden();
                customViewCallback = null;
            }
        });

        // Nút điều hướng
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

        // ✅ KHÔNG TẠO LẠI — KHÔNG RESET
        if (savedState != null) {
            webView.restoreState(savedState);
            savedState = null;
            if (lastUrl != null) urlBar.setText(lastUrl);
        } else if (lastUrl != null) {
            urlBar.setText(lastUrl);
            String current = webView.getUrl();
            if (current == null || !current.equals(lastUrl)) {
                webView.loadUrl(lastUrl);
            }
        } else if (getIntent().getData() != null) {
            String url = getIntent().getData().toString();
            urlBar.setText(url);
            webView.loadUrl(url);
            lastUrl = url;
        }
        
        isFirstCreate = false;
    }

    // ✅ QUAN TRỌNG NHẤT — KHÔNG DỪNG VIDEO KHI RA NỀN
    @Override
    protected void onPause() {
        super.onPause();
        // ❌ KHÔNG GỌI webView.onPause() — KHÔNG DỪNG MEDIA
        // webView.onPause(); // ĐỂ COMMENT DÒNG NÀY
    }

    @Override
    protected void onResume() {
        super.onResume();
        webView.onResume();
        if (lastUrl != null) urlBar.setText(lastUrl);
    }

    // ✅ LƯU TRẠNG THÁI TRƯỚC KHI BỊ CHE
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        webView.saveState(outState);
        savedState = new Bundle(outState); // Lưu vào biến static
        if (lastUrl != null) outState.putString("last_url", lastUrl);
    }

    // ✅ NÚT BACK — KHÔNG ĐÓNG ACTIVITY, CHỈ QUAY LẠI MÀN HÌNH CHÍNH
    @Override
    public void onBackPressed() {
        // Đóng fullscreen trước nếu đang mở
        if (customView != null && customViewCallback != null) {
            customViewCallback.onCustomViewHidden();
            return;
        }
        
        // ✅ KHÔNG finish() — chỉ quay về Activity cha (Wallet)
        // → Activity Browser vẫn tồn tại, WebView không bị hủy
        super.onBackPressed();
        // ⚠️ KHÔNG GỌI finish() → KHÔNG HỦY → KHÔNG RESET
    }

    // ✅ NÚT HOME TRÊN ACTION BAR — CŨNG KHÔNG ĐÓNG
    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            if (customView != null && customViewCallback != null) {
                customViewCallback.onCustomViewHidden();
                return true;
            }
            // ✅ KHÔNG finish() — chỉ quay về màn hình cha
            super.onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // ✅ CHỈ DỌN DẸP KHI THỰC SỰ ĐÓNG APP (không phải back)
    @Override
    protected void onDestroy() {
        // Chỉ hủy khi hệ thống hủy hoàn toàn hoặc người dùng đóng từ Recents
        if (isFinishing()) {
            webView.stopLoading();
            webView.destroy();
            webView = null;
            lastUrl = null;
            savedState = null;
            isFirstCreate = true;
        }
        super.onDestroy();
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

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(urlBar.getWindowToken(), 0);
        urlBar.clearFocus();
    }
}
