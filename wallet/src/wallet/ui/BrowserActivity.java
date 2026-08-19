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
    private static String lastUrl; // Lưu URL cuối cùng — không bị mất khi ra nền
    private static boolean wasPlaying = false;

    // Hỗ trợ Fullscreen video
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

        // === CẤU HÌNH PHÁT NỀN ===
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setMediaPlaybackRequiresUserGesture(false); // Tự động phát media
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);

        // Giữ WebView hoạt động
        webView.setKeepScreenOn(true);
        webView.setFocusable(true);
        webView.setFocusableInTouchMode(true);

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

        // === Fullscreen không reset ===
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
                wasPlaying = true;

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
                wasPlaying = false;

                if (getActionBar() != null) getActionBar().show();
                getWindow().getDecorView().setSystemUiVisibility(originalSystemUiVisibility);

                customViewCallback.onCustomViewHidden();
                customViewCallback = null;
            }
        });

        // Nút điều hướng
        btnBackWeb.setOnClickListener(v -> {
            if (webView.canGoBack()) webView.goBack();
        });

        btnForwardWeb.setOnClickListener(v -> {
            if (webView.canGoForward()) webView.goForward();
        });

        btnGo.setOnClickListener(v -> handleUrlInput());

        // Enter trên bàn phím
        urlBar.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO ||
                (event != null && event.getKeyCode() == android.view.KeyEvent.KEYCODE_ENTER
                    && event.getAction() == android.view.KeyEvent.ACTION_DOWN)) {
                handleUrlInput();
                return true;
            }
            return false;
        });

        // === KHÔNG RESET KHI VÀO LẠI ===
        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState);
            String currentUrl = webView.getUrl();
            if (currentUrl != null) urlBar.setText(currentUrl);
            lastUrl = currentUrl;
        } else if (lastUrl != null) {
            // Có URL đã lưu → hiển thị lại, không load lại nếu vẫn đang phát
            urlBar.setText(lastUrl);
            if (webView.getUrl() == null || !lastUrl.equals(webView.getUrl())) {
                webView.loadUrl(lastUrl);
            }
        } else {
            Intent intent = getIntent();
            if (intent.getData() != null) {
                String url = intent.getData().toString();
                urlBar.setText(url);
                webView.loadUrl(url);
                lastUrl = url;
            }
        }
    }

    // === QUAN TRỌNG: KHÔNG DỪNG VIDEO KHI RA NỀN ===
    @Override
    protected void onPause() {
        super.onPause();
        // ❌ KHÔNG gọi webView.onPause() — không gọi pauseTimers()
        // Để trống = WebView tiếp tục chạy & phát âm thanh trong nền
    }

    @Override
    protected void onStop() {
        super.onStop();
        // ❌ Không hủy, không dừng
    }

    @Override
    protected void onResume() {
        super.onResume();
        webView.onResume();
        // Tiếp tục phát nếu đang phát
    }

    // === Xử lý khi mở lại Activity (singleTask) ===
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        // Chỉ load URL mới nếu khác URL hiện tại
        if (intent.getData() != null) {
            String url = intent.getData().toString();
            if (!url.equals(lastUrl)) {
                urlBar.setText(url);
                webView.loadUrl(url);
                lastUrl = url;
            }
        }
        // Không load lại trang cũ → giữ nguyên trạng thái
    }

    // === Lưu trạng thái ===
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (webView != null) webView.saveState(outState);
        if (lastUrl != null) outState.putString("last_url", lastUrl);
    }

    // === Chỉ dọn dẹp khi đóng hẳn ===
    @Override
    protected void onDestroy() {
        if (isFinishing() && webView != null) {
            webView.stopLoading();
            webView.destroy();
            lastUrl = null;
            wasPlaying = false;
        }
        super.onDestroy();
    }

    // === Xử lý nhập URL ===
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

    // === Nút ActionBar ===
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

    // === Nút Back hệ thống ===
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
