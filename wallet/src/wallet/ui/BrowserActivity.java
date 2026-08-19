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
    
    private static WebView staticWebView = null;
    private static String lastUrl = null;

    private View customView;
    private WebChromeClient.CustomViewCallback customViewCallback;
    private int originalSystemUiVisibility;
    private Intent serviceIntent;

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
        btnBackWeb = findViewById(R.id.btn_back_web);
        btnForwardWeb = findViewById(R.id.btn_forward_web);
        btnGo = findViewById(R.id.btn_go);

        // Nếu đã có WebView cũ — gắn lại
        if (staticWebView != null) {
            FrameLayout container = findViewById(R.id.webview_container);
            if (staticWebView.getParent() != null) {
                ((FrameLayout) staticWebView.getParent()).removeView(staticWebView);
            }
            container.addView(staticWebView);
            webView = staticWebView;
            urlBar.setText(lastUrl != null ? lastUrl : "");
            setupButtons();
            setupWebChromeClient();
            return;
        }

        // Tạo lần đầu
        webView = new WebView(getApplicationContext());
        webView.setLayoutParams(new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ));

        FrameLayout container = findViewById(R.id.webview_container);
        container.addView(webView);
        staticWebView = webView;

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
        webView.setBackgroundColor(0);
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null); // ✅ Tăng tốc media

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
    }

    private void setupButtons() {
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
    }

    // ✅ FIX PHÁT NỀN ANDROID 16
    @Override
    protected void onPause() {
        super.onPause();
        // ❌ KHÔNG GỌI webView.onPause()
        
        // 3 TRICK THEN CHỐT
        webView.onResume();
        webView.resumeTimers();
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        
        // Bắt Foreground Service
        serviceIntent = new Intent(this, BrowserBackgroundService.class);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        webView.onResume();
        webView.resumeTimers();
        if (serviceIntent != null) {
            stopService(serviceIntent);
            serviceIntent = null;
        }
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
        if (serviceIntent != null) stopService(serviceIntent);
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
