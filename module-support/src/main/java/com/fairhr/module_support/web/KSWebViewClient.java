package com.fairhr.module_support.web;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.http.SslError;
import android.os.Build;
import android.text.TextUtils;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;


import com.fairhr.module_support.ThreadUtils;
import com.fairhr.module_support.utils.DeviceInfo;
import com.fairhr.module_support.utils.LogUtil;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.functions.Consumer;
import io.reactivex.rxjava3.schedulers.Schedulers;


/**
 * Author:kingstar
 * Time:2019-09-24
 * PackageName:com.kingstar.ksframework.webview
 * Description:
 */
public class KSWebViewClient extends WebViewClient {
    private final static long LOAD_TIME_OUT = 30 * 1000;
    private Context mContext;
    private Set<IKSWebviewInterceptor> mIKSWebviewInterceptors = new HashSet<>();
    private boolean mShouldClearHistory;
    private Disposable mTimeOutDisposable;

    public boolean isShouldClearHistory() {
        return mShouldClearHistory;
    }

    public void setShouldClearHistory(boolean shouldClearHistory) {
        mShouldClearHistory = shouldClearHistory;
    }

    public KSWebViewClient(Context context) {
        mContext = context;
        mIKSWebviewInterceptors.addAll(WebViewManager.getInstance().getDefaultIKSWebviewInterceptors());
    }

    public KSWebViewClient(Context context, IKSWebviewInterceptor... iksWebviewInterceptors) {
        mContext = context;
        if (iksWebviewInterceptors != null) {
            mIKSWebviewInterceptors.addAll(Arrays.asList(iksWebviewInterceptors));
        }
        mIKSWebviewInterceptors.addAll(WebViewManager.getInstance().getDefaultIKSWebviewInterceptors());
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        boolean result;
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            String url = request.getUrl().toString();
            result = shouldOverrideUrlLoading(view, url);
        } else {
            result = super.shouldOverrideUrlLoading(view, request);
        }
        return result;
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        LogUtil.d(url);
        if (TextUtils.isEmpty(url)) {
            return true;
        } else {
            setCurrentUrl(url);
            if (mIKSWebviewInterceptors.size() > 0) {
                for (IKSWebviewInterceptor iksWebviewInterceptor : mIKSWebviewInterceptors) {
                    if (iksWebviewInterceptor.onWebviewInterceptor(view, url)) {
                        return true;
                    } else {
                        view.loadUrl(url);
                        return true;
                    }
                }
            } else {
                view.loadUrl(url);
                return true;
            }
        }
        return false;
    }

    @Override
    public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
        // ??????H5?????????https?????????????????????
        handler.proceed();
    }

    // ??? API 11 ?????????API 21 ??????
    @Override
    public WebResourceResponse shouldInterceptRequest(final WebView view, String url) {
        return super.shouldInterceptRequest(view, url);
    }

    // ??? API 21 ????????????
    @Override
    public WebResourceResponse shouldInterceptRequest(final WebView view, WebResourceRequest request) {
        WebResourceResponse result;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            String url = request.getUrl().toString();
            result = shouldInterceptRequest(view, url);
        } else {
            // ???????????????????????????
            result = super.shouldInterceptRequest(view, request);
        }
        return result;
    }


    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        super.onPageStarted(view, url, favicon);
        ThreadUtils.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String js = String.format(BaseJsInterface.INJECT_STATUS_BAR_HEIGHT, DeviceInfo.px2dp(DeviceInfo.getStatusBarHeight(mContext)) + "");
                view.loadUrl(js);
            }
        });

        startCountdown();
    }

    /**
     * H5????????????
     */
    private void startCountdown() {
        cancleTimeout();
        mTimeOutDisposable = Observable.timer(LOAD_TIME_OUT, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Long>() {
                    @Override
                    public void accept(Long aLong) throws Exception {
                        onErrorShowDefaultView();
                    }
                });
    }

    @Override
    public void onPageFinished(final WebView view, final String url) {
        super.onPageFinished(view, url);
        cancleTimeout();
    }

    @Override
    public void onReceivedError(WebView view,
                                int errorCode,
                                String description,
                                String failingUrl) {
        // API23???????????????
        // "?????????"??????????????????????????????
        // ??????????????????/????????????/???????????????
        // ???????????????????????????????????????
        // ?????????????????????????????????????????????????????????css?????????/js????????????
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            if (!onErrorShowDefaultView()) {
                super.onReceivedError(view, errorCode, description, failingUrl);
            }
        } else {
            // API23??????API23??????????????????, ??????????????????????????? (^_^)v
        }
    }

    @TargetApi(23)
    @Override
    public void onReceivedError(WebView view,
                                WebResourceRequest request,
                                WebResourceError error) {
        // API23??????API23???????????????
        if (request.isForMainFrame()) {
            if (!onErrorShowDefaultView()) {
                super.onReceivedError(view, request, error);
            }
        }
    }

    @Override
    public void onReceivedHttpError(WebView view,
                                    WebResourceRequest request,
                                    WebResourceResponse errorResponse) {
        // PS:??????????????????????????????Http??????????????????????????????????????????????????????????????????
        super.onReceivedHttpError(view, request, errorResponse);
    }

    @Override
    public void doUpdateVisitedHistory(WebView view, String url, boolean isReload) {
        super.doUpdateVisitedHistory(view, url, isReload);
        if (mShouldClearHistory) {
            mShouldClearHistory = false;
            view.clearHistory();
        }
    }

    /**
     * ?????????????????????url
     *
     * @param currentUrl
     */
    public void setCurrentUrl(String currentUrl) {

    }

    /**
     * ??????????????????
     *
     * @return ???????????????????????????
     */
    public boolean onErrorShowDefaultView() {
        cancleTimeout();
        return false;
    }

    /**
     * ??????????????????
     */
    public void cancleTimeout() {
        if (mTimeOutDisposable != null && !mTimeOutDisposable.isDisposed()) {
            mTimeOutDisposable.dispose();
        }
    }

}
