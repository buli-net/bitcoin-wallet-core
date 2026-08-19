package wallet.ui;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
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

    // ✅ Lưu trạng thái riêng để khôi phục đúng
    private boolean isWebViewRestored = false;

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

        // Cấu hình WebView
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setCacheMode(WebView.LOAD_DEFAULT); // ✅ Lưu cache
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                view.loadUrl(url);
                urlBar.setText(url);
                return true;
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

        // ✅ KHÔNG reset WebView khi có trạng thái đã lưu
        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState);
            isWebViewRestored = true;
            // Cập nhật URL hiển thị từ WebView
            String currentUrl = webView.getUrl();
            if (currentUrl != null) urlBar.setText(currentUrl);
        } else {
            // Lần mở đầu / từ Intent
            Intent intent = getIntent();
            if (intent.getData() != null) {
                String url = intent.getData().toString();
                urlBar.setText(url);
                webView.loadUrl(url);
            }
        }
    }

    // ✅ TẠM DỪNG — LƯU TRẠNG THÁI TRƯỚC KHI VỀ VÍ CHÍNH
    @Override
    protected void onPause() {
        super.onPause();
        // Không hủy WebView — giữ nguyên trạng thái
        isWebViewRestored = false; // đánh dấu cần khôi phục khi quay lại
    }

    // ✅ QUAY LẠI — KHÔI PHỤC TRẠNG THÁI WEB
    @Override
    protected void onResume() {
        super.onResume();
        // Nếu đã có trang trước → cập nhật URL
        if (webView != null && webView.getUrl() != null) {
            urlBar.setText(webView.getUrl());
        }
    }

    // ✅ LƯU TRẠNG THÁI KHI HỆ THỐNG TẠI TẠO
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        webView.saveState(outState); // ✅ Lưu toàn bộ lịch sử + trang hiện tại
    }

    // ✅ XỬ LÝ URL — HỢP LỆ → DUYỆT, SAI → TÌM GOOGLE
    private void handleUrlInput() {
        String input = urlBar.getText().toString().trim();
        hideKeyboard();

        if (input.isEmpty()) return;

        String finalUrl;

        // Kiểm tra xem input có phải URL hợp lệ không
        if (isValidUrl(input)) {
            // Đảm bảo có http/https
            if (!input.startsWith("http://") && !input.startsWith("https://")) {
                finalUrl = "https://" + input;
            } else {
                finalUrl = input;
            }
        } else {
            // ❌ Không phải URL → Tìm kiếm Google
            finalUrl = "https://www.google.com/search?q=" + Uri.encode(input);
        }

        webView.loadUrl(finalUrl);
        urlBar.setText(finalUrl); // Cập nhật ô URL thành link cuối
    }

    // Kiểm tra định dạng URL hợp lệ
    private boolean isValidUrl(String input) {
        if (!input.contains(".") || input.contains(" ")) return false; // Có dấu chấm + không có khoảng trắng
        try {
            // Thêm scheme tạm để kiểm tra
            String check = input.startsWith("http") ? input : "https://" + input;
            URI uri = new URI(check);
            return uri.getHost() != null && uri.getHost().contains(".");
        } catch (URISyntaxException e) {
            return false;
        }
    }

    // Ẩn bàn phím
    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(urlBar.getWindowToken(), 0);
        urlBar.clearFocus();
    }

    // Nút trên ActionBar — Luôn về ví chính
    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish(); // ✅ Về ví chính, WebView được lưu tự động
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Nút Back hệ thống
    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
