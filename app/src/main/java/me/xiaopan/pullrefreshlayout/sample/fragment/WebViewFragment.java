package me.xiaopan.pullrefreshlayout.sample.fragment;

import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import me.xiaopan.android.inject.InjectContentView;
import me.xiaopan.android.inject.InjectParentMember;
import me.xiaopan.android.inject.InjectView;
import me.xiaopan.pullrefreshlayout.R;
import me.xiaopan.pullrefreshlayout.sample.PullRefreshFragment;

@InjectParentMember
@InjectContentView(R.layout.fragment_web_view)
public class WebViewFragment extends PullRefreshFragment {
    @InjectView(R.id.web) private WebView webView;

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        webView.setWebViewClient(new WebViewClient() {

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                pullRefreshLayout.stopRefresh();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                    setHasOptionsMenu(false);
                    setHasOptionsMenu(true);
                }
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
                pullRefreshLayout.stopRefresh();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                    setHasOptionsMenu(false);
                    setHasOptionsMenu(true);
                }
            }
        });
    }

    @Override
    protected void onRefreshContent() {
        String url = webView.getUrl();
        if(url == null){
            url = "http://www.baidu.com";
        }
        webView.loadUrl(url);
    }

    public boolean onBackPressed(){
        if(webView.canGoBack()){
            webView.goBack();
            return true;
        }else{
            return false;
        }
    }
}
