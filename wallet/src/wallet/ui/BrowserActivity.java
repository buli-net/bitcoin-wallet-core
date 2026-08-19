package wallet.ui;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import wallet.R;

public class BrowserActivity extends Activity {

    private WebView webView;
    private View customView;
    private android.webkit.WebChromeClient.CustomViewCallback customViewCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browser);

        // Enable ActionBar back button
        if (getActionBar() != null) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
            getActionBar().setTitle(R.string.menu_web_browser);
        }

        // Initialize views
        webView = findViewById(R.id.webview);
        final EditText urlBar = findViewById(R.id.url_bar);
        final Button btnBack = findViewById(R.id.btn_back);
        final Button btnForward = findViewById(R.id.btn_forward);
        final Button btnGo = findViewById(R.id.btn_go);

        // Configure WebView for full web support including YouTube
        final WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);                 // Required for YouTube & modern sites
        settings.setDomStorageEnabled(true);                 // Enable local storage
        settings.setLoadWithOverviewMode(true);              // Fit content to screen
        settings.setUseWideViewPort(true);                   // Support wide viewport
        settings.setBuiltInZoomControls(true);               // Allow pinch zoom
        settings.setDisplayZoomControls(false);              // Hide zoom buttons
        settings.setMediaPlaybackRequiresUserGesture(false); // Allow auto-play videos

        // Handle page loading within WebView (do NOT open external browser)
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                view.loadUrl(request.getUrl());
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                urlBar.setText(url); // Update URL bar when page loads
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                urlBar.setText(url);
            }
        });

        // Enable full HTML5 video support including YouTube fullscreen
        webView.setWebChromeClient(new android.webkit.WebChromeClient() {
            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                super.onShowCustomView(view, callback);
                customView = view;
                customViewCallback = callback;
                setContentView(view);
            }

            @Override
            public void onHideCustomView() {
                super.onHideCustomView();
                if (customView != null) {
                    setContentView(R.layout.activity_browser);
                    customView = null;
                    customViewCallback.onCustomViewHidden();
                }
            }
        });

        // Load URL if opened from external intent
        final Intent intent = getIntent();
        if (intent.getData() != null) {
            final String url = intent.getData().toString();
            urlBar.setText(url);
            webView.loadUrl(url);
        }

        // Navigate back in web history
        btnBack.setOnClickListener(v -> {
            if (webView.canGoBack()) webView.goBack();
        });

        // Navigate forward in web history
        btnForward.setOnClickListener(v -> {
            if (webView.canGoForward()) webView.goForward();
        });

        // Load URL from address bar
        btnGo.setOnClickListener(v -> {
            String url = urlBar.getText().toString().trim();
            if (!url.isEmpty()) {
                // Auto prepend https:// if missing
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    url = "https://" + url;
                }
                webView.loadUrl(url);
            }
        });
    }

    // Handle ActionBar back button → close browser, return to wallet
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish(); // Close browser and go back
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Handle system back button press
    @Override
    public void onBackPressed() {
        // Exit fullscreen video first if active
        if (customView != null) {
            customViewCallback.onCustomViewHidden();
            customView = null;
            return;
        }
        // Go back in web history if possible
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
