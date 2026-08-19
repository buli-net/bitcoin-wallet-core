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
    private static String lastUrl; // ✅ Lưu URL cuối cùng
    private static boolean wasPlaying = false; // ✅ Đánh dấu đang phát

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

        // ✅ CẤU HÌNH PHÁT NỀN — ĐÃ TỐI ƯU
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setMediaPlaybackRequiresUserGesture(false); // ✅ Tự động phát
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);

        // ✅ NGĂN HỆ THỐNG TẠM DỪNG
        webView.setKeepScreenOn(true); // Giữ WebView hoạt động
        webView.setFocusable(true);
        webView.setFocusableInTouchMode(true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                view.loadUrl(url);
                urlBar.setText(url);
                lastUrl = url; // ✅ Lưu URL hiện tại
                return true;
            }
            
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                lastUrl = url; // ✅ Cập nhật khi trang tải xong
            }
        });

        // ✅ WebChromeClient — Fullscreen KHÔNG reset
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
                wasPlaying = true; // ✅ Đánh dấu đang phát video

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
                wasPlaying = false; // ✅ Đã tắt fullscreen

                if (getActionBar() != null) getActionBar().show();
                getWindow().getDecorView().setSystemUiVisibility(originalSystemUiVisibility);

                customViewCallback.onCustomViewHidden();
                customViewCallback = null;
            }
        });

        // Nút Back
        btnBackWeb.setOnClickListener(v -> {
            if (webView.canGoBack()) webView.goBack();
        });

        // Nút Forward
        btnForwardWeb.setOnClickListener(v -> {
            if (webView.canGoForward()) webView.goForward();
        });

        // Nút Go
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

        // ✅ KHÔNG TẠO LẠI — NẾU CÓ URL LƯU THÌ TẢI LẠI
        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState);
            String currentUrl = webView.getUrl();
            if (currentUrl != null) urlBar.setText(currentUrl);
            lastUrl = currentUrl;
        } else if (lastUrl != null) {
            // ✅ TRƯỜNG HỢP VÀO LẠI → TẢI LẠI URL CŨ
            urlBar.setText(lastUrl);
            webView.loadUrl(lastUrl);
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

    // ✅ QUAN TRỌNG — KHÔNG ĐỂ HỆ THỐNG TẠM DỪNG
    @Override
    protected void onPause() {
        super.onPause();
        // ❌ KHÔNG GỌI webView.onPause() — KHÔNG GỌI pauseTimers()
        // → Để trống = WebView tiếp tục chạy trong nền
    }

    // ✅ KHÔNG ĐỂ HỦY WEBVIEW KHI RA NỀN
    @Override
    protected void onStop() {
        super.onStop();
        // ❌ KHÔNG dừng, không hủy = tiếp tục phát
    }

    // ✅ QUAY LẠI — TIẾP TỤC PHÁT
    @Override
    protected void onResume() {
        super.onResume();
        webView.onResume();
        // Nếu đang phát → tiếp tục
        if (wasPlaying && lastUrl != null && webView.getUrl() != null) {
            // Không cần load lại — video vẫn đang phát
        }
    }

    // ✅ LƯU TRẠNG THÁI TRƯỚC KHI BỊ HỦY
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (webView != null) webView.saveState(outState);
        if (lastUrl != null) outState.putString("last_url", lastUrl);
    }

    // ✅ CHỈ DỌN DẸP KHI ĐÓNG HẲN
    @Override
    protected void onDestroy() {
        // ❌ KHÔNG gọi khi chỉ ra nền — chỉ khi thực sự đóng Activity
        if (isFinishing() && webView != null) {
            webView.stopLoading();
            webView.destroy();
            lastUrl = null; // ✅ Reset chỉ khi đóng hẳn
            wasPlaying = false;
        }
        super.onDestroy();
    }

    // Xử lý URL
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

    // Nút trên ActionBar
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

    // Nút Back hệ thống
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
