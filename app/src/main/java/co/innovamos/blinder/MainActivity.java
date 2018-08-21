package co.innovamos.blinder;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Message;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.SslErrorHandler;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {


    static boolean ASWP_JSCRIPT = SmartWebView.ASWP_JSCRIPT;
    static boolean ASWP_FUPLOAD = SmartWebView.ASWP_FUPLOAD;
    static boolean ASWP_CAMUPLOAD = SmartWebView.ASWP_CAMUPLOAD;
    static boolean ASWP_ONLYCAM = SmartWebView.ASWP_ONLYCAM;
    static boolean ASWP_MULFILE = SmartWebView.ASWP_MULFILE;
    static boolean ASWP_LOCATION = SmartWebView.ASWP_LOCATION;
    static boolean ASWP_RATINGS = SmartWebView.ASWP_RATINGS;
    static boolean ASWP_PBAR = SmartWebView.ASWP_PBAR;
    static boolean ASWP_ZOOM = SmartWebView.ASWP_ZOOM;
    static boolean ASWP_SFORM = SmartWebView.ASWP_SFORM;
    static boolean ASWP_OFFLINE = SmartWebView.ASWP_OFFLINE;
    static boolean ASWP_EXTURL = SmartWebView.ASWP_EXTURL;

    //Configuration variables
    private static String ASWV_URL = SmartWebView.ASWV_URL;
    private static String ASWV_F_TYPE = SmartWebView.ASWV_F_TYPE;

    ProgressBar asw_progress;
    NotificationManager asw_notification;
    Notification asw_notification_new;

    private String asw_cam_message;
    private ValueCallback<Uri> asw_file_message;
    private ValueCallback<Uri[]> asw_file_path;
    private final static int asw_file_req = 1;

    private final static int loc_perm = 1;
    private final static int file_perm = 2;


    private WebView webView;
    private WebView mWebviewPop;
    private FrameLayout mContainer;
    private Context mContext;
    public static final String USER_AGENT = "Mozilla/5.0 (Linux; Android 4.1.1; Galaxy Nexus Build/JRO03C) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.166 Mobile Safari/535.19";
    private static final String TAG = MainActivity.class.getSimpleName();

    private String url = "https://blinder.com.co";
    private String target_url_prefix = "blinder.com.co";

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (Build.VERSION.SDK_INT >= 21) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().setStatusBarColor(getResources().getColor(R.color.colorPrimary));
            Uri[] results = null;
            if (resultCode == Activity.RESULT_OK) {
                if (requestCode == asw_file_req) {
                    if (null == asw_file_path) {
                        return;
                    }
                    if (intent == null || intent.getData() == null) {
                        if (asw_cam_message != null) {
                            results = new Uri[]{Uri.parse(asw_cam_message)};
                        }
                    } else {
                        String dataString = intent.getDataString();
                        if (dataString != null) {
                            results = new Uri[]{ Uri.parse(dataString) };
                        } else {
                            if(ASWP_MULFILE) {
                                if (intent.getClipData() != null) {
                                    final int numSelectedFiles = intent.getClipData().getItemCount();
                                    results = new Uri[numSelectedFiles];
                                    for (int i = 0; i < numSelectedFiles; i++) {
                                        results[i] = intent.getClipData().getItemAt(i).getUri();
                                    }
                                }
                            }
                        }
                    }
                }
            }
            asw_file_path.onReceiveValue(results);
            asw_file_path = null;
        } else {
            if (requestCode == asw_file_req) {
                if (null == asw_file_message) return;
                Uri result = intent == null || resultCode != RESULT_OK ? null : intent.getData();
                asw_file_message.onReceiveValue(result);
                asw_file_message = null;
            }
        }
    }


    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.w("READ_PERM MISAT = ", Manifest.permission.READ_EXTERNAL_STORAGE);
        Log.w("WRITE_PERM  MISAT= ", Manifest.permission.WRITE_EXTERNAL_STORAGE);

        //Prevent the app from being started again when it is still alive in the background
        if (!isTaskRoot()) {
            finish();
            return;
        }

        setContentView(R.layout.activity_main);


        //verifica si se desea mostrar la barra de carga
        if (ASWP_PBAR) {
            //asw_progress = findViewById(R.id.msw_progress);
        } else {
            // findViewById(R.id.msw_progress).setVisibility(View.GONE);
        }

        //Getting GPS location of device if given permission
        if(!check_permission(1)){
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, loc_perm);
        }

        //Obtener informacion basica del dispositivo
        get_info();


        //Get outer container
        mContainer = (FrameLayout) findViewById(R.id.webview_frame);
        webView = (WebView) findViewById(R.id.webView);

        /**
         * @author javier misat
         * Funcion para evitar el scrool horizontal
         */

        webView.setOnTouchListener(new View.OnTouchListener() {

            public boolean onTouch(View v, MotionEvent event) {
                return (event.getAction() == MotionEvent.ACTION_MOVE);
            }
        });

        webView.setHorizontalScrollBarEnabled(false);

        WebSettings webSettings = webView.getSettings();
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowFileAccessFromFileURLs(true);
        webSettings.setAllowUniversalAccessFromFileURLs(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(false);
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setAppCacheEnabled(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webSettings.setSupportMultipleWindows(true);

        //These two lines are specific for my need. These are not necessary
        if (Build.VERSION.SDK_INT >= 21) {
            webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }

        webView.setWebViewClient(new MyCustomWebViewClient());
        webView.setWebChromeClient(new UriWebChromeClient());
        webView.loadUrl("https://blinder.com.co");

        mContext = this.getApplicationContext();
    }


    private class MyCustomWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            String host = Uri.parse(url).getHost();
            if (url.startsWith("http:") || url.startsWith("https:")) {
                if (Uri.parse(url).getPath().equals("/connection-compte.html")) {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://blinder.com.co"));
                    startActivity(browserIntent);
                    return true;
                }

                if (host.equals(target_url_prefix)) {
                    if (mWebviewPop != null) {
                        mWebviewPop.setVisibility(View.GONE);
                        mContainer.removeView(mWebviewPop);
                        mWebviewPop = null;
                    }
                    return false;
                }
                if (host.equals("m.facebook.com") || host.equals("www.facebook.com") || host.equals("facebook.com") || host.equals("accounts.google.com") || host.equals("google.com") || host.equals("www.google.com")) {
                    return false;
                }
                // Otherwise, the link is not for a page on my site, so launch
                // another Activity that handles URLs
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(intent);
                return true;
            }

            // Otherwise allow the OS to handle it
            else if (url.startsWith("tel:")) {
                Intent tel = new Intent(Intent.ACTION_DIAL, Uri.parse(url));
                startActivity(tel);
                return true;
            }
            //This is again specific for my website
            else if (url.startsWith("mailto:")) {
                Intent mail = new Intent(Intent.ACTION_SEND);
                mail.setType("application/octet-stream");
                String AdressMail = new String(url.replace("mailto:", ""));
                mail.putExtra(Intent.EXTRA_EMAIL, new String[]{AdressMail});
                mail.putExtra(Intent.EXTRA_SUBJECT, "");
                mail.putExtra(Intent.EXTRA_TEXT, "");
                startActivity(mail);
                return true;
            }
            return true;
        }

        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
            Log.d("onReceivedSslError", "onReceivedSslError");
            //super.onReceivedSslError(view, handler, error);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            if (url.startsWith("https://m.facebook.com/v2.7/dialog/oauth")) {
                if (mWebviewPop != null) {
                    mWebviewPop.setVisibility(View.GONE);
                    mContainer.removeView(mWebviewPop);
                    mWebviewPop = null;
                }
                view.loadUrl("https://blinder.com.co");
                return;
            }
            super.onPageFinished(view, url);
        }
    }

    private class UriWebChromeClient extends WebChromeClient {

        @Override
        public boolean onCreateWindow(WebView view, boolean isDialog,
                                      boolean isUserGesture, Message resultMsg) {
            mWebviewPop = new WebView(mContext);
            mWebviewPop.getSettings().setUserAgentString(USER_AGENT);
            mWebviewPop.setVerticalScrollBarEnabled(false);
            mWebviewPop.setHorizontalScrollBarEnabled(false);
            mWebviewPop.setWebViewClient(new MyCustomWebViewClient());
            mWebviewPop.getSettings().setJavaScriptEnabled(true);
            mWebviewPop.getSettings().setSavePassword(false);
            mWebviewPop.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
            mContainer.addView(mWebviewPop);
            WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
            transport.setWebView(mWebviewPop);
            resultMsg.sendToTarget();

            return true;
        }

        @Override
        public void onCloseWindow(WebView window) {
            Log.d("onCloseWindow", "called");
        }


        /**
         * @author javier misat
         * Metodos para permisos de subida de archivos y localizacion
         */

        //Handling input[type="file"] requests for android API 16+
        public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture) {
            if (ASWP_FUPLOAD) {
                asw_file_message = uploadMsg;
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType(ASWV_F_TYPE);
                if (ASWP_MULFILE) {
                    i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                }
                startActivityForResult(Intent.createChooser(i, getString(R.string.fl_chooser)), asw_file_req);
            }
        }

        //Handling input[type="file"] requests for android API 21+
        public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
            get_file();
            if (ASWP_FUPLOAD) {
                if (asw_file_path != null) {
                    asw_file_path.onReceiveValue(null);
                }
                asw_file_path = filePathCallback;
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (ASWP_CAMUPLOAD) {
                    if (takePictureIntent.resolveActivity(MainActivity.this.getPackageManager()) != null) {
                        File photoFile = null;
                        try {
                            photoFile = create_image();
                            takePictureIntent.putExtra("PhotoPath", asw_cam_message);
                        } catch (IOException ex) {
                            Log.e(TAG, "Image file creation failed", ex);
                        }
                        if (photoFile != null) {
                            asw_cam_message = "file:" + photoFile.getAbsolutePath();
                            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
                        } else {
                            takePictureIntent = null;
                        }
                    }
                }
                Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
                if (!ASWP_ONLYCAM) {
                    contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
                    contentSelectionIntent.setType(ASWV_F_TYPE);
                    if (ASWP_MULFILE) {
                        contentSelectionIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                    }
                }
                Intent[] intentArray;
                if (takePictureIntent != null) {
                    intentArray = new Intent[]{takePictureIntent};
                } else {
                    intentArray = new Intent[0];
                }

                Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
                chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
                chooserIntent.putExtra(Intent.EXTRA_TITLE, "File Chooser");
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);
                startActivityForResult(chooserIntent, asw_file_req);
            }
            return true;
        }

        /*Getting webview rendering progress
        @Override
        public void onProgressChanged(WebView view, int p) {
            if (ASWP_PBAR) {
                asw_progress.setProgress(p);
                if (p == 100) {
                    asw_progress.setProgress(0);
                }
            }
        }*/

    }


    @Override
    public void onBackPressed() {
        if (webView.isFocused() && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }


    /**
     * @author javier misat
     * funciones complementarias de permisos
     */

    //Getting host name
    public static String aswm_host(String url) {
        if (url == null || url.length() == 0) {
            return "";
        }
        int dslash = url.indexOf("//");
        if (dslash == -1) {
            dslash = 0;
        } else {
            dslash += 2;
        }
        int end = url.indexOf('/', dslash);
        end = end >= 0 ? end : url.length();
        int port = url.indexOf(':', dslash);
        end = (port > 0 && port < end) ? port : end;
        Log.w("URL Host: ", url.substring(dslash, end));
        return url.substring(dslash, end);
    }

    //Getting device basic information
    public void get_info() {
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setCookie(ASWV_URL, "DEVICE=android");
        cookieManager.setCookie(ASWV_URL, "DEV_API=" + Build.VERSION.SDK_INT);
    }

    //Checking permission for storage and camera for writing and uploading images
    public void get_file() {
        String[] perms = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.CAMERA};

        //Checking for storage permission to write images for upload
        if (ASWP_FUPLOAD && ASWP_CAMUPLOAD && !check_permission(2) && !check_permission(3)) {
            ActivityCompat.requestPermissions(MainActivity.this, perms, file_perm);

            //Checking for WRITE_EXTERNAL_STORAGE permission
        } else if (ASWP_FUPLOAD && !check_permission(2)) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, file_perm);

            //Checking for CAMERA permissions
        } else if (ASWP_CAMUPLOAD && !check_permission(3)) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA}, file_perm);
        }
    }

    //Checking if particular permission is given or not
    public boolean check_permission(int permission) {
        switch (permission) {
            case 1:
                return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;

            case 2:
                return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;

            case 3:
                return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;

        }
        return false;
    }

    //Creating image file for upload
    private File create_image() throws IOException {
        @SuppressLint("SimpleDateFormat")
        String file_name = new SimpleDateFormat("yyyy_mm_ss").format(new Date());
        String new_name = "file_" + file_name + "_";
        File sd_directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(new_name, ".jpg", sd_directory);
    }


    /**
     * Sobreescribiendo metodos de la actividad
     */


//Action on back key tap/click
    @Override
    public boolean onKeyDown(int keyCode, @NonNull KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_BACK:
                    if (webView.canGoBack()) {
                        webView.goBack();
                    } else {
                        finish();
                    }
                    return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }


    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        webView.saveState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        webView.restoreState(savedInstanceState);
    }


}