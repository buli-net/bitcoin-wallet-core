package wallet.ui;

import android.content.Context;
import android.content.Intent;
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
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import wallet.R;

import java.net.URI;
import java.net.URISyntaxException;

public class BrowserActivity extends AbstractWalletActivity {

    private EditText urlBar;
    private WebView webView;
    private ImageView btnBackWeb;
    private ImageView btnForwardWeb;
    private ImageView btnGo;

    // ✅ Hỗ trợ Fullscreen video
    private View customView;
    private WebChromeClient.CustomViewCallback customViewCallback;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browser);

        if (getActionBar() != null) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
            getActionBar().setHomeButtonEnabled(true);
        }

        urlBar = findViewById(R.id.url_bar);
        webView = findViewById(R.id.webview);
        btnBackWeb = findViewById(R.id.btn_back_web);
        btnForwardWeb = findViewById(R.id.btn_forward_web);
        btnGo = findViewById(R.id.btn_go);

        // ✅ Cấu hình WebView — Hỗ trợ video toàn màn hình
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
        webView.getSettings().setAllowFileAccess(true);
        webView.getSettings().setAllowContentAccess(true);
        webView.getSettings().setMediaPlaybackRequiresUserGesture(false); // ✅ Tự động phát video

        // ✅ WebViewClient — Cập nhật URL khi chuyển trang
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                view.loadUrl(url);
                urlBar.setText(url);
                return true;
            }
        });

        // ✅ WebChromeClient — HỖ TRỢ TOÀN MÀN HÌNH VIDEO
        webView.setWebChromeClient(new WebChromeClient() {
            // Bật toàn màn hình
            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                if (customView != null) {
                    customViewCallback.onCustomViewHidden();
                    return;
                }
                customView = view;
                customViewCallback = callback;

                // Ẩn ActionBar & thanh trạng thái → toàn màn hình
                if (getActionBar() != null) getActionBar().hide();
                getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN |
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                );

                // Thêm view video toàn màn hình
                setContentView(customView);
            }

            // Tắt toàn màn hình
            @Override
            public void onHideCustomView() {
                if (customView == null) return;

                // Khôi phục giao diện bình thường
                setContentView(R.layout.activity_browser);
                if (getActionBar() != null) getActionBar().show();
                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);

                customView = null;
                customViewCallback.onCustomViewHidden();

                // ✅ Tải lại trạng thái sau khi thoát fullscreen
                rebindViews();
            }
        });

        // Nút ← Back
        btnBackWeb.setOnClickListener(v -> {
            if (webView.canGoBack()) webView.goBack();
        });

        // Nút → Forward
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

        // ✅ KHÔNG mất trang khi xoay màn hình — configChanges đã ngăn tạo lại Activity
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

    // ✅ Khôi phục tham chiếu view sau khi thoát fullscreen
    private void rebindViews() {
        urlBar = findViewById(R.id.url_bar);
        webView = findViewById(R.id.webview);
        btnBackWeb = findViewById(R.id.btn_back_web);
        btnForwardWeb = findViewById(R.id.btn_forward_web);
        btnGo = findViewById(R.id.btn_go);
    }

    // ✅ Lưu trạng thái khi xoay màn hình / tạm dừng
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (webView != null) {
            webView.saveState(outState);
        }
    }

    // ✅ XỬ LÝ URL — Hợp lệ → duyệt, sai → tìm Google
    private void handleUrlInput() {
        String input = urlBar.getText().toString().trim();
        hideKeyboard();

        if (input.isEmpty()) return;

        String finalUrl;

        if (isValidUrl(input)) {
            if (!input.startsWith("http://") && !input.startsWith("https://")) {
                finalUrl = "https://" + input;
            } else {
                finalUrl = input;
            }
        } else {
            finalUrl = "https://www.google.com/search?q=" + Uri.encode(input);
        }

        webView.loadUrl(finalUrl);
        urlBar.setText(finalUrl);
    }

    private boolean isValidUrl(String input) {
        if (!input.contains(".") || input.contains(" ")) return false;
        try {
            String check = input.startsWith("http") ? input : "https://" + input;
            URI uri = new URI(check);
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

    // ✅ Nút trên ActionBar — Luôn về ví chính
    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // Nếu đang fullscreen → thoát trước khi đóng
            if (customView != null && customViewCallback != null) {
                customViewCallback.onCustomViewHidden();
            }
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // ✅ Nút Back hệ thống
    @Override
    public void onBackPressed() {
        // Nếu đang fullscreen → thoát fullscreen trước
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
