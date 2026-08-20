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
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import wallet.R;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

public class BrowserActivity extends AbstractWalletActivity {

    private EditText urlBar;
    private WebView webView;
    private ImageView btnBackWeb;
    private ImageView btnForwardWeb;
    private ImageView btnGo;
    private LinearLayout toolbarContainer;
    private View rootLayout;

    private View customView;
    private WebChromeClient.CustomViewCallback customViewCallback;
    private int originalSystemUiVisibility;

    private static Bundle savedWebViewState = null;
    private static String currentUrl = null;
    private static boolean hasState = false;

    // ✅ Danh sách scheme cần mở bên ngoài (TikTok, Facebook, Instagram...)
    private static final List<String> EXTERNAL_SCHEMES = Arrays.asList(
        "snssdk1180", "snssdk1128", "snssdk1166", // TikTok
        "fb", "fb-messenger", "instagram", "twitter", "whatsapp",
        "intent", "market", "tel", "mailto", "geo"
    );

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browser);

        rootLayout = findViewById(android.R.id.content);
        toolbarContainer = findViewById(R.id.toolbar_container);

        if (getActionBar() != null) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
            getActionBar().setHomeButtonEnabled(true);
        }

        urlBar = findViewById(R.id.url_bar);
        webView = findViewById(R.id.webview);
        btnBackWeb = findViewById(R.id.btn_back_web);
        btnForwardWeb = findViewById(R.id.btn_forward_web);
        btnGo = findViewById(R.id.btn_go);

        updateAllColors();

        // ==================================================
        // ✅ BẬT FULL HẾT TÍNH NĂNG WEB
        // ==================================================
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setAllowFileAccessFromFileURLs(true);
        webSettings.setAllowUniversalAccessFromFileURLs(true);
        webSettings.setLoadsImagesAutomatically(true);
        webSettings.setBlockNetworkImage(false);
        webSettings.setBlockNetworkLoads(false);
        webSettings.setMediaPlaybackRequiresUserGesture(false);
        webSettings.setMixedContentMode(2);
        webSettings.setGeolocationEnabled(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);

        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        webSettings.setRenderPriority(WebSettings.RenderPriority.HIGH);
        String ua = webSettings.getUserAgentString();
        webSettings.setUserAgentString(ua + " Chrome/120.0.0.0 Mobile");
        webView.setFocusable(true);
        webView.setFocusableInTouchMode(true);

        // ==================================================
        // ✅ FIX CHÍNH — XỬ LÝ SCHEME TIKTOK & URL BÊN NGOÀI
        // ==================================================
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                String scheme = request.getUrl().getScheme();

                // ✅ Nếu là scheme không chuẩn như snssdk1180:// → mở bằng ứng dụng
                if (scheme != null && EXTERNAL_SCHEMES.contains(scheme.toLowerCase())) {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        view.getContext().startActivity(intent);
                        urlBar.setText(url);
                        return true; // ✅ Không load trong WebView
                    } catch (Exception e) {
                        // Không có ứng dụng → mở link web thay thế
                        String webUrl = "https://www.tiktok.com";
                        if (url.contains("aweme/detail")) {
                            // Trích xuất ID video từ URL TikTok
                            String path = request.getUrl().getPath();
                            if (path != null && path.contains("/detail/")) {
                                String videoId = path.replaceAll(".*/detail/", "").replaceAll("[^0-9].*", "");
                                if (!videoId.isEmpty()) {
                                    webUrl = "https://www.tiktok.com/t/ZTR" + videoId + "/";
                                }
                            }
                        }
                        view.loadUrl(webUrl);
                        urlBar.setText(webUrl);
                        currentUrl = webUrl;
                        return true;
                    }
                }

                // ✅ URL bình thường → load trong WebView
                view.loadUrl(url);
                urlBar.setText(url);
                currentUrl = url;
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                currentUrl = url;
                if (urlBar != null && !urlBar.getText().toString().equals(url)) {
                    urlBar.setText(url);
                }
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
                ((FrameLayout) rootLayout).addView(view);
            }

            @Override
            public void onHideCustomView() {
                if (customView == null) return;
                ((FrameLayout) rootLayout).removeView(customView);
                customView = null;
                if (getActionBar() != null) getActionBar().show();
                getWindow().getDecorView().setSystemUiVisibility(originalSystemUiVisibility);
                customViewCallback.onCustomViewHidden();
                customViewCallback = null;
            }
        });

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

        // ==================================================
        // ✅ KHÔI PHỤC TRẠNG THÁI — KHÔNG BỊ RESET
        // ==================================================
        if (hasState && savedWebViewState != null) {
            webView.restoreState(savedWebViewState);
            if (currentUrl != null) urlBar.setText(currentUrl);
            hasState = false;
            savedWebViewState = null;
        } else if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState);
            String url = webView.getUrl();
            if (url != null) urlBar.setText(url);
        } else {
            Intent intent = getIntent();
            if (intent.getData() != null) {
                String url = intent.getData().toString();
                urlBar.setText(url);
                webView.loadUrl(url);
                currentUrl = url;
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (webView != null) {
            webView.onPause();
            savedWebViewState = new Bundle();
            webView.saveState(savedWebViewState);
            hasState = true;
            currentUrl = webView.getUrl();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null) webView.onResume();
        updateAllColors();
    }

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

    // ==================================================
    private void updateAllColors() {
        int bgActionBarColor = getResources().getColor(R.color.bg_action_bar);
        int fgIconColor = getResources().getColor(R.color.fg_on_dark_bg_network_significant);

        if (toolbarContainer != null) toolbarContainer.setBackgroundColor(bgActionBarColor);
        if (btnBackWeb != null) btnBackWeb.setColorFilter(fgIconColor);
        if (btnForwardWeb != null) btnForwardWeb.setColorFilter(fgIconColor);
        if (btnGo != null) btnGo.setColorFilter(fgIconColor);

        if (urlBar != null) {
            int[] textColorAttr = { android.R.attr.textColorPrimary };
            TypedArray taText = obtainStyledAttributes(textColorAttr);
            int textColor = taText.getColor(0, 0xFF000000);
            taText.recycle();
            urlBar.setTextColor(textColor);

            int[] hintColorAttr = { android.R.attr.textColorHint };
            TypedArray taHint = obtainStyledAttributes(hintColorAttr);
            int hintColor = taHint.getColor(0, 0xFF888888);
            taHint.recycle();
            urlBar.setHintTextColor(hintColor);

            Drawable urlBg = getResources().getDrawable(R.drawable.edittext_background);
            urlBar.setBackground(urlBg);
        }

        int[] windowBgAttr = { android.R.attr.windowBackground };
        TypedArray taBg = obtainStyledAttributes(windowBgAttr);
        int windowBg = taBg.getColor(0, 0xFFFFFFFF);
        taBg.recycle();
        if (webView != null) webView.setBackgroundColor(windowBg);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateAllColors();
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
        currentUrl = finalUrl;
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

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (webView != null) webView.saveState(outState);
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}
