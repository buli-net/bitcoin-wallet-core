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

    // ✅ CHỈ LƯU 3 BIẾN — NHẸ NHÀNG
    private static String lastUrl = null;
    private static int scrollX = 0;
    private static int scrollY = 0;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browser);

        toolbarContainer = findViewById(R.id.toolbar_container);
        urlBar = findViewById(R.id.url_bar);
        webView = findViewById(R.id.webview);
        btnBackWeb = findViewById(R.id.btn_back_web);
        btnForwardWeb = findViewById(R.id.btn_forward_web);
        btnGo = findViewById(R.id.btn_go);

        if (getActionBar() != null) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
            getActionBar().setHomeButtonEnabled(true);
        }

        setupWebView();
        setupButtons();
        updateColors();

        // ✅ MỞ LINK MỚI TỪ INTENT (transaction...)
        Uri intentData = getIntent().getData();
        if (intentData != null) {
            String newUrl = intentData.toString();
            if (!newUrl.equals(lastUrl)) {
                loadUrl(newUrl); // link mới → load
            } else {
                urlBar.setText(lastUrl);
                webView.scrollTo(scrollX, scrollY); // trùng → giữ nguyên
            }
        } else if (lastUrl != null) {
            // ✅ QUAY LẠI → KHÔNG LOAD LẠI, CHỈ KHÔI PHỤC
            urlBar.setText(lastUrl);
            webView.scrollTo(scrollX, scrollY);
        }
    }

    private void setupWebView() {
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        s.setAllowFileAccess(true);
        s.setUseWideViewPort(true);
        s.setLoadWithOverviewMode(true);
        s.setSupportZoom(true);
        s.setBuiltInZoomControls(true);
        s.setDisplayZoomControls(false);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req) {
                String url = req.getUrl().toString();
                String scheme = req.getUrl().getScheme();

                if (scheme != null && !scheme.startsWith("http")) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                    return true;
                }

                if (url.equals(lastUrl)) {
                    urlBar.setText(url);
                    return true; // trùng → giữ nguyên
                }

                loadUrl(url);
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                lastUrl = url;
                urlBar.setText(url);
            }
        });

        webView.setWebChromeClient(new WebChromeClient());
    }

    private void loadUrl(String url) {
        lastUrl = url;
        urlBar.setText(url);
        webView.loadUrl(url);
    }

    private void setupButtons() {
        btnBackWeb.setOnClickListener(v -> { if (webView.canGoBack()) webView.goBack(); });
        btnForwardWeb.setOnClickListener(v -> { if (webView.canGoForward()) webView.goForward(); });
        btnGo.setOnClickListener(v -> handleInput());

        urlBar.setOnEditorActionListener((v, act, evt) -> {
            if (act == EditorInfo.IME_ACTION_GO ||
                (evt != null && evt.getKeyCode() == android.view.KeyEvent.KEYCODE_ENTER
                    && evt.getAction() == android.view.KeyEvent.ACTION_DOWN)) {
                handleInput();
                return true;
            }
            return false;
        });
    }

    private void handleInput() {
        String input = urlBar.getText().toString().trim();
        hideKeyboard();
        if (input.isEmpty()) return;

        String finalUrl;
        if (isValidUrl(input)) {
            finalUrl = input.startsWith("http") ? input : "https://" + input;
        } else {
            finalUrl = "https://www.google.com/search?q=" + Uri.encode(input);
        }

        if (!finalUrl.equals(lastUrl)) loadUrl(finalUrl);
    }

    private boolean isValidUrl(String s) {
        return s.contains(".") && !s.contains(" ");
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(urlBar.getWindowToken(), 0);
        urlBar.clearFocus();
    }

    private void updateColors() {
        int bgBar = getResources().getColor(R.color.bg_action_bar);
        int fgIcon = getResources().getColor(R.color.fg_on_dark_bg_network_significant);
        int[] textColorAttr = { android.R.attr.textColorPrimary };
        int[] hintColorAttr = { android.R.attr.textColorHint };

        if (toolbarContainer != null) toolbarContainer.setBackgroundColor(bgBar);
        if (btnBackWeb != null) btnBackWeb.setColorFilter(fgIcon);
        if (btnForwardWeb != null) btnForwardWeb.setColorFilter(fgIcon);
        if (btnGo != null) btnGo.setColorFilter(fgIcon);

        if (urlBar != null) {
            TypedArray ta = obtainStyledAttributes(textColorAttr);
            urlBar.setTextColor(ta.getColor(0, 0xFF000000));
            ta.recycle();
            ta = obtainStyledAttributes(hintColorAttr);
            urlBar.setHintTextColor(ta.getColor(0, 0xFF888888));
            ta.recycle();
            urlBar.setBackgroundResource(R.drawable.edittext_background);
        }
    }

    // ✅ LƯU TRƯỚC KHI THOÁT / ĐA NHIỆM
    @Override
    protected void onPause() {
        super.onPause();
        if (webView != null && webView.getUrl() != null) {
            lastUrl = webView.getUrl();
            scrollX = webView.getScrollX();
            scrollY = webView.getScrollY();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateColors();
        if (lastUrl != null) webView.scrollTo(scrollX, scrollY);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        Uri data = intent.getData();
        if (data != null) {
            String newUrl = data.toString();
            if (!newUrl.equals(lastUrl)) loadUrl(newUrl);
            else webView.scrollTo(scrollX, scrollY);
        }
    }

    // ✅ NÚT UP — LƯU + THOÁT
    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            if (webView != null && webView.getUrl() != null) {
                lastUrl = webView.getUrl();
                scrollX = webView.getScrollX();
                scrollY = webView.getScrollY();
            }
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // ✅ NÚT BACK HỆ THỐNG — LƯU + QUAY LẠI
    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            if (webView != null && webView.getUrl() != null) {
                lastUrl = webView.getUrl();
                scrollX = webView.getScrollX();
                scrollY = webView.getScrollY();
            }
            super.onBackPressed();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateColors();
    }
}
