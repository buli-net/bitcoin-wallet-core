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

public class BrowserActivity extends AbstractWalletActivity {

    private EditText urlBar;
    private WebView webView;
    private ImageView btnBackWeb;     // ← quay lại trang web
    private ImageView btnForwardWeb;  // ➡️ đi tới trang web
    private ImageView btnGo;          // → Tải trang

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browser);

        // Nút trên ActionBar: LUÔN về ví chính
        if (getActionBar() != null) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
            getActionBar().setHomeButtonEnabled(true);
        }

        // Ánh xạ view
        urlBar = findViewById(R.id.url_bar);
        webView = findViewById(R.id.webview);
        btnBackWeb = findViewById(R.id.btn_back_web);
        btnForwardWeb = findViewById(R.id.btn_forward_web);
        btnGo = findViewById(R.id.btn_go);

        // Cấu hình WebView
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                view.loadUrl(request.getUrl().toString());
                urlBar.setText(request.getUrl().toString());
                return true;
            }
        });

        // ✅ Nút ← : Chỉ quay lại trang web, hết bước thì dừng
        btnBackWeb.setOnClickListener(v -> {
            if (webView.canGoBack()) webView.goBack();
        });

        // ✅ Nút ➡️ : Đi tới trang web tiếp theo (ngược lại nút ←)
        btnForwardWeb.setOnClickListener(v -> {
            if (webView.canGoForward()) webView.goForward();
        });

        // ✅ Nút → Go: Tải trang, ẩn bàn phím
        btnGo.setOnClickListener(v -> loadUrlAndHideKeyboard());

        // ✅ Nhấn Enter trên bàn phím = bấm nút Go → tải trang + ẩn bàn phím
        urlBar.setOnEditorActionListener((TextView v, int actionId, android.view.KeyEvent event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO ||
                (event != null && event.getKeyCode() == android.view.KeyEvent.KEYCODE_ENTER &&
                 event.getAction() == android.view.KeyEvent.ACTION_DOWN)) {
                loadUrlAndHideKeyboard();
                return true;
            }
            return false;
        });

        // ✅ Khôi phục trạng thái khi vào lại
        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState);
        } else {
            Intent intent = getIntent();
            if (intent.getData() != null) {
                String url = intent.getData().toString();
                urlBar.setText(url);
                webView.loadUrl(url);
            }
        }
    }

    // ✅ Tải trang + Ẩn bàn phím
    private void loadUrlAndHideKeyboard() {
        String url = urlBar.getText().toString().trim();
        if (!url.isEmpty()) {
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "https://" + url;
            }
            webView.loadUrl(url);
        }
        // Ẩn bàn phím sau khi nhấn Enter / Go
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(urlBar.getWindowToken(), 0);
        urlBar.clearFocus(); // Bỏ con trỏ khỏi ô nhập
    }

    // ✅ Lưu trạng thái WebView
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        webView.saveState(outState);
    }

    // ✅ Nút trên ActionBar: Luôn về ví chính
    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // ✅ Nút Back hệ thống: chỉ back trang web, hết bước thì đóng trình duyệt
    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
