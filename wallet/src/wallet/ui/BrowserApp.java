package wallet.ui;  // ✅ ĐÚNG PACKAGE — KHỚP VỚI PROJECT CỦA BẠN

import android.app.Application;
import android.webkit.WebView;
import android.webkit.WebSettings;

public class BrowserApp extends Application {
    private static WebView sharedWebView;

    @Override
    public void onCreate() {
        super.onCreate();
        sharedWebView = new WebView(getApplicationContext());
        configureWebView(sharedWebView);
    }

    private void configureWebView(WebView webView) {
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setAllowFileAccessFromFileURLs(true);
        webSettings.setAllowUniversalAccessFromFileURLs(true);
        webSettings.setLoadsImagesAutomatically(true);
        webSettings.setBlockNetworkImage(false);
        webSettings.setBlockNetworkLoads(false);
        webSettings.setMediaPlaybackRequiresUserGesture(false);
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        webSettings.setGeolocationEnabled(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);

        webView.setLayerType(WebView.LAYER_TYPE_HARDWARE, null);
        webSettings.setRenderPriority(WebSettings.RenderPriority.HIGH);

        String ua = webSettings.getUserAgentString();
        webSettings.setUserAgentString(ua + " Chrome/120.0.0.0 Mobile");
        webView.setFocusable(true);
        webView.setFocusableInTouchMode(true);
    }

    public static WebView getSharedWebView() {
        return sharedWebView;
    }

    public static void clearSharedWebView() {
        if (sharedWebView != null) {
            sharedWebView.destroy();
            sharedWebView = null;
        }
    }
}
