package wallet.ui;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;
import wallet.R;

public class BrowserActivity extends AppCompatActivity {

    private EditText urlBar;
    private WebView webView;
    private Button btnBack;
    private Button btnGo;
    // ❌ Không có btnForward trong layout → xóa hoặc khai báo sau
    // private Button btnForward;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browser);

        // Ánh xạ view — KHỚP với layout activity_browser.xml
        urlBar = findViewById(R.id.url_bar);
        webView = findViewById(R.id.webview);
        btnBack = findViewById(R.id.btn_back);
        btnGo = findViewById(R.id.btn_go);
        // btnForward = findViewById(R.id.btn_forward); // ❌ Layout chưa có → comment/xóa

        // Cấu hình WebView
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                // ✅ SỬA LỖI 2: Uri → String
                view.loadUrl(request.getUrl().toString());
                return true;
            }
        });

        // Nút Quay lại
        btnBack.setOnClickListener(v -> {
            if (webView.canGoBack()) webView.goBack();
            else finish();
        });

        // Nút Đi
        btnGo.setOnClickListener(v -> {
            String url = urlBar.getText().toString().trim();
            if (!url.isEmpty()) {
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    url = "https://" + url;
                }
                webView.loadUrl(url);
            }
        });

        // Tải URL từ intent (nếu có)
        Intent intent = getIntent();
        if (intent.getData() != null) {
            String url = intent.getData().toString();
            urlBar.setText(url);
            webView.loadUrl(url);
        }
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }
}
