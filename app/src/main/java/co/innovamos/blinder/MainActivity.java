package co.innovamos.blinder;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private WebView mWebviewPop;
    private FrameLayout mContainer;
    private Context mContext;
    public static final String USER_AGENT = "Mozilla/5.0 (Linux; Android 4.1.1; Galaxy Nexus Build/JRO03C) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.166 Mobile Safari/535.19";

    private String url = "https://blinder.com.co";
    private String target_url_prefix = "blinder.com.co";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Get outer container
        mContainer = (FrameLayout) findViewById(R.id.webview_frame);
        webView = (WebView)findViewById(R.id.webView);

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

       /** webView.setOnTouchListener(new View.OnTouchListener() {
            float m_downX;
            public boolean onTouch(View v, MotionEvent event) {

                if (event.getPointerCount() > 1) {
                    //Multi touch detected
                    return true;
                }

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN: {
                        // save the x
                        m_downX = event.getX();
                        break;
                    }
                    case MotionEvent.ACTION_MOVE:
                    case MotionEvent.ACTION_CANCEL:
                    case MotionEvent.ACTION_UP: {
                        // set x so that it doesn't move
                        event.setLocation(m_downX, event.getY());
                        break;
                    }

                }
                return false;
            }
        });*/


        WebSettings webSettings = webView.getSettings();
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(false);
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setAppCacheEnabled(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webSettings.setSupportMultipleWindows(true);

        //These two lines are specific for my need. These are not necessary
        if (Build.VERSION.SDK_INT >= 21) {
            webSettings.setMixedContentMode( WebSettings.MIXED_CONTENT_ALWAYS_ALLOW );
        }


        webView.setWebViewClient(new MyCustomWebViewClient());
        webView.setWebChromeClient(new UriWebChromeClient());
        webView.loadUrl("https://blinder.com.co");

        mContext=this.getApplicationContext();
    }



    private class MyCustomWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            String host = Uri.parse(url).getHost();

            if( url.startsWith("http:") || url.startsWith("https:") ) {

                if(Uri.parse(url).getPath().equals("/connection-compte.html")) {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://blinder.com.co"));
                    startActivity(browserIntent);

                    return true ;
                }

                if (host.equals(target_url_prefix)) {
                    if (mWebviewPop != null) {
                        mWebviewPop.setVisibility(View.GONE);
                        mContainer.removeView(mWebviewPop);
                        mWebviewPop = null;
                    }
                    return false;
                }
                if (host.equals("m.facebook.com") || host.equals("www.facebook.com") || host.equals("facebook.com") || host.equals("accounts.google.com") || host.equals("google.com") || host.equals("www.google.com") ) {
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
                String AdressMail = new String(url.replace("mailto:" , "")) ;
                mail.putExtra(Intent.EXTRA_EMAIL, new String[]{ AdressMail });
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
            if(url.startsWith("https://m.facebook.com/v2.7/dialog/oauth")){
                if(mWebviewPop!=null)
                {
                    mWebviewPop.setVisibility(View.GONE);
                    mContainer.removeView(mWebviewPop);
                    mWebviewPop=null;
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

    }


    @Override
    public void onBackPressed() {
        if(webView.isFocused() && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }


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



}