package wallet.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import wallet.R;

public class BrowserActivity extends AbstractWalletActivity {

    private EditText urlBar;
    private WebView webView;
    private ImageView btnBackWeb;
    private ImageView btnForwardWeb;
    private ImageView btnRefreshWeb;
    private LinearLayout toolbarContainer;
    private View rootLayout;

    private static final String PREFS_NAME = "BrowserPrefs";
    private static final String KEY_HOME_URL = "home_url";
    private static final String KEY_HISTORY = "history_list";

    private final List<String> historyList = new ArrayList<>();

    private View customView;
    private WebChromeClient.CustomViewCallback customViewCallback;
    private int originalSystemUiVisibility;

    private static Bundle savedWebViewState = null;
    private static String savedUrlBeforePause = null;
    private static int scrollX = 0;
    private static int scrollY = 0;
    private static boolean hasSavedState = false;
    private static boolean isNewLink = false;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browser);

        rootLayout = findViewById(android.R.id.content);
        toolbarContainer = findViewById(R.id.toolbar_container);

        if (getActionBar()!= null) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }

        urlBar = findViewById(R.id.url_bar);
        webView = findViewById(R.id.webview);
        btnBackWeb = findViewById(R.id.btn_back_web);
        btnForwardWeb = findViewById(R.id.btn_forward_web);
        btnRefreshWeb = findViewById(R.id.btn_refresh_web);

        updateAllColors();

        SharedPreferences prefsLoad = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String savedHistory = prefsLoad.getString(KEY_HISTORY, null);
        if (savedHistory!= null &&!savedHistory.isEmpty()) {
            String[] arr = savedHistory.split("\\|\\|");
            for (String s : arr) {
                if (!s.isEmpty()) {
                    historyList.add(s);
                }
            }
        }

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

        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        webSettings.setRenderPriority(WebSettings.RenderPriority.HIGH);
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        String ua = webSettings.getUserAgentString();
        webSettings.setUserAgentString(ua + " Chrome/120.0.0.0 Mobile");
        webView.setFocusable(true);
        webView.setFocusableInTouchMode(true);

        webView.setWebViewClient(new WebViewClient() {

            private boolean isRestoring = false;

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                String scheme = request.getUrl().getScheme();

                if (scheme!= null &&!scheme.equalsIgnoreCase("http") &&!scheme.equalsIgnoreCase("https")) {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        view.getContext().startActivity(intent);
                        urlBar.setText(url);
                        return true;
                    } catch (Exception ignored) {
                        urlBar.setText(url);
                        return true;
                    }
                }

                if (isRestoring) {
                    isRestoring = false;
                    urlBar.setText(url);
                    return true;
                }

                if (url.equals(view.getUrl()) &&!isNewLink) {
                    urlBar.setText(url);
                    return true;
                }

                view.loadUrl(url);
                urlBar.setText(url);
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);

                if (urlBar!= null &&!urlBar.getText().toString().equals(url)) {
                    urlBar.setText(url);
                }

                saveHistory(url);

                if (hasSavedState &&!isNewLink) {
                    webView.scrollTo(scrollX, scrollY);
                    hasSavedState = false;
                }

                isNewLink = false;
            }

            @Override
            public void doUpdateVisitedHistory(WebView view, String url, boolean isReload) {
                super.doUpdateVisitedHistory(view, url, isReload);

                if (urlBar!= null && url!= null &&!url.equals("about:blank")) {
                    urlBar.setText(url);
                }

                saveHistory(url);
            }

            private void saveHistory(String url) {
                if (url == null) {
                    return;
                }
                if (url.equals("about:blank")) {
                    return;
                }
                if (url.isEmpty()) {
                    return;
                }
                if (historyList.contains(url)) {
                    return;
                }

                historyList.add(url);

                SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString(KEY_HISTORY, String.join("||", historyList));
                editor.apply();
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {

            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                if (customView!= null) {
                    customViewCallback.onCustomViewHidden();
                    return;
                }

                customView = view;
                customViewCallback = callback;
                originalSystemUiVisibility = getWindow().getDecorView().getSystemUiVisibility();

                if (getActionBar()!= null) {
                    getActionBar().hide();
                }

                getWindow().getDecorView().setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                );

                ((FrameLayout) rootLayout).addView(view);
            }

            @Override
            public void onHideCustomView() {
                if (customView == null) {
                    return;
                }

                ((FrameLayout) rootLayout).removeView(customView);
                customView = null;

                if (getActionBar()!= null) {
                    getActionBar().show();
                }

                getWindow().getDecorView().setSystemUiVisibility(originalSystemUiVisibility);
                customViewCallback.onCustomViewHidden();
            }
        });

        btnBackWeb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (webView.canGoBack()) {
                    webView.goBack();
                }
            }
        });

        btnForwardWeb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (webView.canGoForward()) {
                    webView.goForward();
                }
            }
        });

        btnRefreshWeb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideKeyboard();
                webView.reload();
            }
        });

        urlBar.setImeOptions(EditorInfo.IME_ACTION_GO);
        urlBar.setSingleLine(true);

        urlBar.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean isEnterKey = event!= null
                        && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                        && event.getAction() == KeyEvent.ACTION_DOWN;

                if (actionId == EditorInfo.IME_ACTION_GO
                        || actionId == EditorInfo.IME_ACTION_SEARCH
                        || actionId == EditorInfo.IME_ACTION_DONE
                        || actionId == EditorInfo.IME_ACTION_SEND
                        || actionId == EditorInfo.IME_NULL
                        || isEnterKey) {
                    handleUrlInput();
                    return true;
                }
                return false;
            }
        });

        urlBar.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN) {
                    handleUrlInput();
                    return true;
                }
                return false;
            }
        });

        Intent intent = getIntent();
        Uri intentData = intent.getData();

        if (intentData!= null) {
            String newUrl = intentData.toString();
            urlBar.setText(newUrl);
            webView.loadUrl(newUrl);
            clearSavedState();
            isNewLink = true;
        } else if (savedWebViewState!= null && savedUrlBeforePause!= null) {
            webView.restoreState(savedWebViewState);
            urlBar.setText(savedUrlBeforePause);
            isNewLink = false;
            hasSavedState = true;
        } else if (savedInstanceState!= null) {
            webView.restoreState(savedInstanceState);
            String restoredUrl = webView.getUrl();
            if (restoredUrl!= null) {
                urlBar.setText(restoredUrl);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(final android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.browser_options, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(android.view.Menu menu) {
        final int networkSignificantColor = getResources().getColor(R.color.fg_on_dark_bg_network_significant);
        final View decor = getWindow().getDecorView();

        decor.post(new Runnable() {
            @Override
            public void run() {
                ArrayList<View> actionMenuViews = new ArrayList<>();
                findViewsByClass(decor, "ActionMenuView", actionMenuViews);

                for (View amv : actionMenuViews) {
                    if (!(amv instanceof ViewGroup)) {
                        continue;
                    }

                    ViewGroup vg = (ViewGroup) amv;

                    for (int i = 0; i < vg.getChildCount(); i++) {
                        View itemView = vg.getChildAt(i);
                        if (itemView.getClass().getSimpleName().contains("ActionMenuItemView")) {
                            findAndWhiteText(itemView, networkSignificantColor);
                        }
                    }
                }
            }
        });

        return super.onPrepareOptionsMenu(menu);
    }

    private void findAndWhiteText(View root, int color) {
        if (root instanceof TextView) {
            ((TextView) root).setTextColor(color);
            return;
        }

        if (root instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) root;
            for (int i = 0; i < vg.getChildCount(); i++) {
                findAndWhiteText(vg.getChildAt(i), color);
            }
        }
    }

    private void findViewsByClass(View root, String className, ArrayList<View> out) {
        if (root.getClass().getSimpleName().contains(className)) {
            out.add(root);
        }

        if (root instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) root;
            for (int i = 0; i < vg.getChildCount(); i++) {
                findViewsByClass(vg.getChildAt(i), className, out);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(final android.view.MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.menu_browser_home) {
            loadHomeUrl();
            return true;
        } else if (id == R.id.menu_browser_history) {
            showHistoryDialog();
            return true;
        } else if (id == R.id.menu_browser_set_home) {
            showSetHomeDialog();
            return true;
        } else if (id == R.id.menu_browser_clear_history) {
            historyList.clear();
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.remove(KEY_HISTORY);
            editor.apply();
            Toast.makeText(this, R.string.browser_clear_history, Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.menu_browser_settings_root) {
            return true;
        } else if (id == android.R.id.home) {
            luuTrangThai();
            if (customView!= null && customViewCallback!= null) {
                customViewCallback.onCustomViewHidden();
            }
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void luuTrangThai() {
        if (webView!= null &&!isFinishing()) {
            savedWebViewState = new Bundle();
            webView.saveState(savedWebViewState);
            savedUrlBeforePause = webView.getUrl();
            scrollX = webView.getScrollX();
            scrollY = webView.getScrollY();
            hasSavedState = true;
        }
    }

    @Override
    protected void onPause() {
        luuTrangThai();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateAllColors();
        invalidateOptionsMenu();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);

        Uri data = intent.getData();
        if (data!= null) {
            String newUrl = data.toString();
            urlBar.setText(newUrl);
            webView.loadUrl(newUrl);
            clearSavedState();
            isNewLink = true;
        }
    }

    private void clearSavedState() {
        savedWebViewState = null;
        savedUrlBeforePause = null;
        scrollX = 0;
        scrollY = 0;
        hasSavedState = false;
        isNewLink = false;
    }

    @Override
    public void finish() {
        luuTrangThai();
        super.finish();
    }

    @Override
    public void onBackPressed() {
        if (customView!= null && customViewCallback!= null) {
            customViewCallback.onCustomViewHidden();
            return;
        }

        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            luuTrangThai();
            super.onBackPressed();
        }
    }

    private void updateAllColors() {
        int bgActionBarColor = getResources().getColor(R.color.bg_action_bar);
        int fgIconColor = getResources().getColor(R.color.fg_on_dark_bg_network_significant);

        if (toolbarContainer!= null) {
            toolbarContainer.setBackgroundColor(bgActionBarColor);
        }

        if (btnBackWeb!= null) {
            btnBackWeb.setColorFilter(fgIconColor);
        }

        if (btnForwardWeb!= null) {
            btnForwardWeb.setColorFilter(fgIconColor);
        }

        if (btnRefreshWeb!= null) {
            btnRefreshWeb.setColorFilter(fgIconColor);
        }

        if (urlBar!= null) {
            int[] textColorAttr = {android.R.attr.textColorPrimary};
            TypedArray taText = obtainStyledAttributes(textColorAttr);
            int textColor = taText.getColor(0, 0);
            taText.recycle();
            urlBar.setTextColor(textColor);

            int[] hintColorAttr = {android.R.attr.textColorHint};
            TypedArray taHint = obtainStyledAttributes(hintColorAttr);
            int hintColor = taHint.getColor(0, 0);
            taHint.recycle();
            urlBar.setHintTextColor(hintColor);

            Drawable urlBg = getResources().getDrawable(R.drawable.edittext_background);
            urlBar.setBackground(urlBg);
        }

        int[] windowBgAttr = {android.R.attr.windowBackground};
        TypedArray taBg = obtainStyledAttributes(windowBgAttr);
        int windowBg = taBg.getColor(0, 0);
        taBg.recycle();

        if (webView!= null) {
            webView.setBackgroundColor(windowBg);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateAllColors();
        invalidateOptionsMenu();
    }

    private void handleUrlInput() {
        String input = urlBar.getText().toString().trim();
        hideKeyboard();

        if (input.isEmpty()) {
            return;
        }

        String finalUrl;

        if (isValidUrl(input)) {
            if (input.startsWith("http")) {
                finalUrl = input;
            } else {
                finalUrl = "https://" + input;
            }
        } else {
            finalUrl = "https://www.google.com/search?q=" + Uri.encode(input);
        }

        isNewLink = true;
        webView.loadUrl(finalUrl);
        urlBar.setText(finalUrl);
        clearSavedState();
    }

    private boolean isValidUrl(String input) {
        if (!input.contains(".")) {
            return false;
        }

        if (input.contains(" ")) {
            return false;
        }

        try {
            String checkUrl;
            if (input.startsWith("http")) {
                checkUrl = input;
            } else {
                checkUrl = "https://" + input;
            }

            URI uri = new URI(checkUrl);
            return uri.getHost()!= null && uri.getHost().contains(".");
        } catch (URISyntaxException e) {
            return false;
        }
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm!= null && urlBar!= null) {
            imm.hideSoftInputFromWindow(urlBar.getWindowToken(), 0);
        }

        if (urlBar!= null) {
            urlBar.clearFocus();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (webView!= null) {
            webView.saveState(outState);
        }
    }

    private void loadHomeUrl() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String homeUrl = prefs.getString(KEY_HOME_URL, null);

        if (homeUrl!= null) {
            webView.loadUrl(homeUrl);
            urlBar.setText(homeUrl);
        } else {
            webView.loadUrl("about:blank");
            urlBar.setText("");
        }
    }

    private void showHistoryDialog() {
        if (historyList.isEmpty()) {
            new AlertDialog.Builder(this)
                   .setMessage(R.string.browser_no_history)
                   .setPositiveButton(R.string.browser_close, null)
                   .show();
            return;
        }

        CharSequence[] items = historyList.toArray(new CharSequence[0]);

        new AlertDialog.Builder(this)
               .setTitle(R.string.browser_history_title)
               .setItems(items, (dialog, which) -> {
                    String url = historyList.get(which);
                    webView.loadUrl(url);
                    urlBar.setText(url);
                })
               .setPositiveButton(R.string.browser_clear_history, (dialog, which) -> {
                    historyList.clear();
                    SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.remove(KEY_HISTORY);
                    editor.apply();
                    Toast.makeText(this, R.string.browser_clear_history, Toast.LENGTH_SHORT).show();
                })
               .setNegativeButton(R.string.browser_close, null)
               .show();
    }

    private void showSetHomeDialog() {
        EditText input = new EditText(this);
        input.setHint(R.string.browser_home_hint);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String currentHome = prefs.getString(KEY_HOME_URL, null);

        if (currentHome!= null) {
            input.setText(currentHome);
        }

        new AlertDialog.Builder(this)
               .setTitle(R.string.browser_set_home_title)
               .setView(input)
               .setPositiveButton(R.string.browser_save, (dialog, which) -> {
                    String url = input.getText().toString().trim();
                    SharedPreferences.Editor edit = prefs.edit();

                    if (url.isEmpty()) {
                        edit.remove(KEY_HOME_URL);
                        Toast.makeText(this, R.string.browser_home_cleared, Toast.LENGTH_SHORT).show();
                    } else {
                        if (!url.startsWith("http")) {
                            url = "https://" + url;
                        }
                        edit.putString(KEY_HOME_URL, url);
                        Toast.makeText(this, R.string.browser_home_saved, Toast.LENGTH_SHORT).show();
                    }

                    edit.apply();
                })
               .setNegativeButton(R.string.browser_cancel, null)
               .show();
    }
}
