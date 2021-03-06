package com.fairhr.module_support.network;

//import android.support.annotation.NonNull;

import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.fairhr.module_support.tools.inter.OnUploadFileObserver;
import com.fairhr.module_support.tools.inter.UploadFileRequestBody;
import com.fairhr.module_support.utils.FileUtils;

import java.io.File;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableEmitter;
import io.reactivex.rxjava3.core.ObservableOnSubscribe;
import io.reactivex.rxjava3.schedulers.Schedulers;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.CookieJar;
import okhttp3.FormBody;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * 封装OKHTTP网络框架
 */
public class OkHttpManager {
    private final static long DEFAULT_TIMEOUT = 30;
    private OkHttpClient mOkHttpClient;
    private OkHttpClient mCookieOkHttpClient;
    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private Map<String, Call> mCallMap = new HashMap<>();//用于取消请求
    KeyStore trustStore;

    private OkHttpManager() {
    }

    public OkHttpClient getOkHttpClient() {
        return mOkHttpClient;
    }

    public void setOkHttpClient(OkHttpClient okHttpClient) {
        mOkHttpClient = okHttpClient;
    }

    private static class OkHttpManagerInstance {
        private static final OkHttpManager sOkHttpManager = new OkHttpManager();
    }

    public static OkHttpManager getInstance() {
        return OkHttpManagerInstance.sOkHttpManager;
    }


    /**
     * 默认配置的okHttpClient
     *
     * @return
     */
    public OkHttpClient getDefaultOkHttpClient() {
        if (mOkHttpClient == null) {
            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            builder.connectTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS);
            builder.writeTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS);
            builder.readTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS);
//            builder.sslSocketFactory(RxUtils.createSSLSocketFactory());
//            builder.hostnameVerifier(new RxUtils.TrustAllHostnameVerifier());
            try {
                trustStore = KeyStore.getInstance(KeyStore
                        .getDefaultType());
                trustStore.load(null, null);
                SSLSocketFactoryImp ssl = new SSLSocketFactoryImp(KeyStore.getInstance(KeyStore.getDefaultType()));

                HostnameVerifier DO_NOT_VERIFY = new HostnameVerifier() {
                    @Override
                    public boolean verify(String hostname, SSLSession session) {
                        return true;
                    }
                };
                builder.sslSocketFactory(ssl.getSSLContext().getSocketFactory(), ssl.getTrustManager())
                        .hostnameVerifier(DO_NOT_VERIFY);
            } catch (Exception  e) {
                e.printStackTrace();
            }


            mOkHttpClient = builder.build();
        }
        return mOkHttpClient;
    }

    /**
     * 默认配置的okHttpClient
     *
     * @return
     */
    public OkHttpClient getCookieOkHttpClient(CookieJar cookieJar) {
        if (mCookieOkHttpClient == null) {
            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            builder.connectTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS);
            builder.writeTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS);
            builder.readTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS);
//            builder.sslSocketFactory(RxUtils.createSSLSocketFactory());
//            builder.hostnameVerifier(new RxUtils.TrustAllHostnameVerifier());
            try {
                trustStore = KeyStore.getInstance(KeyStore
                        .getDefaultType());
                trustStore.load(null, null);
                SSLSocketFactoryImp ssl = new SSLSocketFactoryImp(KeyStore.getInstance(KeyStore.getDefaultType()));

                HostnameVerifier DO_NOT_VERIFY = new HostnameVerifier() {
                    @Override
                    public boolean verify(String hostname, SSLSession session) {
                        return true;
                    }
                };
                builder.sslSocketFactory(ssl.getSSLContext().getSocketFactory(), ssl.getTrustManager())
                        .hostnameVerifier(DO_NOT_VERIFY);
            } catch (Exception  e) {
                e.printStackTrace();
            }

            if (cookieJar != null)
                builder.cookieJar(cookieJar);
            mCookieOkHttpClient = builder.build();
        }
        return mCookieOkHttpClient;
    }

    /**
     * 移除缓存的call
     *
     * @param key
     */
    private void removeCall(String key) {
        if (mCallMap.containsKey(key))
            mCallMap.remove(key);
    }

    /**
     * 取消请求
     *
     * @param key
     */
    public void cancelCall(String key) {
        if (mCallMap.containsKey(key)) {
            Call call = mCallMap.get(key);
            if (call != null && call.isExecuted() && !call.isCanceled()) {
                call.cancel();
            }
            mCallMap.remove(key);
        }
    }


    /**
     * 默认配置的okHttpClient
     *
     * @return
     */
    public OkHttpClient getOkHttpClient(List<Interceptor> interceptorList) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.connectTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS);
        builder.writeTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS);
        builder.readTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS);
        if (interceptorList != null && interceptorList.size() > 0) {
            for (Interceptor interceptor : interceptorList) {
                builder.addInterceptor(interceptor);
            }
        }
        return builder.build();
    }

    /**************************************GET请求START***********************************************/
    /**
     * get请求
     *
     * @param urlPath url
     */
    public Call get(String urlPath) {
        if (mOkHttpClient == null)
            getDefaultOkHttpClient();
        Request request = new Request.Builder().url(urlPath).build();
        return mOkHttpClient.newCall(request);
    }


    /**
     * get请求
     *
     * @param urlPath  url
     * @param callback 请求结果回调
     */
    public Call get(String urlPath, Callback callback) {
        if (mOkHttpClient == null)
            getDefaultOkHttpClient();
        Request request = new Request.Builder().url(urlPath).build();
        Call call = mOkHttpClient.newCall(request);
        call.enqueue(callback);
        return call;
    }


    /**
     * get请求
     *
     * @param urlPath url
     * @param parmas  额外参数
     */

    public Call get(String urlPath, Map<String, String> parmas) {
        if (mOkHttpClient == null)
            getDefaultOkHttpClient();
        Request.Builder builder = new Request.Builder().url(urlPath);
        Request request = setGetParmas(builder, parmas).build();
        return mOkHttpClient.newCall(request);
    }

    /**
     * get请求
     *
     * @param urlPath  url
     * @param parmas   额外参数
     * @param callback 结果回到
     */

    public Call get(String urlPath, Map<String, String> parmas, Callback callback) {
        if (mOkHttpClient == null)
            getDefaultOkHttpClient();
        Request.Builder builder = new Request.Builder().url(urlPath);
        Request request = setGetParmas(builder, parmas).build();
        Call call = mOkHttpClient.newCall(request);
        call.enqueue(callback);
        return call;
    }

    /**
     * get请求设置额外参数
     *
     * @param builder get builder
     * @param params  额外参数
     * @return 返回添加参数的get builder
     */
    public Request.Builder setGetParmas(Request.Builder builder, Map<String, String> params) {
        if (params == null)
            return builder;
        Set<Map.Entry<String, String>> entries = params.entrySet();
        for (Map.Entry<String, String> entry : entries) {
            if (!TextUtils.isEmpty(entry.getKey()) && !TextUtils.isEmpty(entry.getValue()))
                builder.addHeader(entry.getKey(), entry.getValue());
        }
        return builder;
    }

    /**************************************GET请求END***********************************************/


    /**************************************POST请求START***********************************************/


    /**
     * post请求
     *
     * @param urlPath url
     * @param parmas  参数
     */
    public Call post(String urlPath, Map<String, String> parmas) {
        if (mOkHttpClient == null)
            getDefaultOkHttpClient();
        FormBody.Builder builder = new FormBody.Builder();
        FormBody build = setPostParmas(builder, parmas).build();
        Request request = new Request.Builder().url(urlPath).post(build).build();
        return mOkHttpClient.newCall(request);
    }


    /**
     * post请求
     *
     * @param urlPath url
     * @param parmas  参数
     */
    public Call post(String urlPath, Map<String, String> headers, Map<String, String> parmas) {
        if (mOkHttpClient == null)
            getDefaultOkHttpClient();
        FormBody.Builder builder = new FormBody.Builder();
        FormBody build = setPostParmas(builder, parmas).build();
        Request.Builder reqBuilder = new Request.Builder();
        if (headers != null && headers.size() > 0) {
            Set<Map.Entry<String, String>> entries = headers.entrySet();
            for (Map.Entry<String, String> entry : entries) {
                reqBuilder.addHeader(entry.getKey(), entry.getValue());
            }
        }
        Request request = reqBuilder.url(urlPath).post(build).build();
        return mOkHttpClient.newCall(request);
    }


    /**
     * post请求
     *
     * @param urlPath  url
     * @param parmas   参数
     * @param callback 结果回调
     */
    public Call post(String urlPath, Map<String, String> parmas, Callback callback) {
        if (mOkHttpClient == null)
            getDefaultOkHttpClient();
        FormBody.Builder builder = new FormBody.Builder();
        FormBody build = setPostParmas(builder, parmas).build();
        Request request = new Request.Builder().url(urlPath).post(build).build();
        Call call = mOkHttpClient.newCall(request);
        call.enqueue(callback);
        return call;
    }


    /**
     * post请求
     *
     * @param urlPath  url
     * @param parmas   参数
     * @param callback 结果回调
     */
    public Call post(String urlPath, Map<String, String> headers, Map<String, String> parmas, Callback callback) {
        if (mOkHttpClient == null)
            getDefaultOkHttpClient();
        FormBody.Builder builder = new FormBody.Builder();
        FormBody build = setPostParmas(builder, parmas).build();
        Request.Builder reqBuilder = new Request.Builder();
        if (headers != null && headers.size() > 0) {
            Set<Map.Entry<String, String>> entries = headers.entrySet();
            for (Map.Entry<String, String> entry : entries) {
                reqBuilder.addHeader(entry.getKey(), entry.getValue());
            }
        }
        Request request = reqBuilder.url(urlPath).post(build).build();
        Call call = mOkHttpClient.newCall(request);
        call.enqueue(callback);
        return call;
    }


    /**
     * post请求
     *
     * @param urlPath url
     * @param parmas  参数
     */
    public Call postJson(String urlPath, String parmas) {
        if (mOkHttpClient == null)
            getDefaultOkHttpClient();
        RequestBody body = RequestBody.create(JSON, parmas);
        Request request = new Request.Builder().url(urlPath).post(body).build();
        return mOkHttpClient.newCall(request);
    }


    /**
     * post请求
     *
     * @param urlPath url
     * @param parmas  参数
     */
    public Call postJson(String urlPath, Map<String, String> headers, String parmas) {
        if (mOkHttpClient == null)
            getDefaultOkHttpClient();
        RequestBody body = RequestBody.create(JSON, parmas);
        Request.Builder reqBuilder = new Request.Builder();
        if (headers != null && headers.size() > 0) {
            Set<Map.Entry<String, String>> entries = headers.entrySet();
            for (Map.Entry<String, String> entry : entries) {
                reqBuilder.addHeader(entry.getKey(), entry.getValue());
            }
        }
        Request request = reqBuilder.url(urlPath).post(body).build();
        return mOkHttpClient.newCall(request);
    }


    /**
     * post请求
     *
     * @param urlPath  url
     * @param parmas   参数
     * @param callback 结果回调
     */
    public Call postJson(String urlPath, String parmas, Callback callback) {
        if (mOkHttpClient == null)
            getDefaultOkHttpClient();
        RequestBody body = RequestBody.create(JSON, parmas);
        Request request = new Request.Builder().url(urlPath).post(body).build();
        Call call = mOkHttpClient.newCall(request);
        call.enqueue(callback);
        return call;
    }


    /**
     * post请求
     *
     * @param urlPath  url
     * @param parmas   参数
     * @param callback 结果回调
     */
    public Call postJson(String urlPath, Map<String, String> headers, String parmas, Callback callback) {
        if (mOkHttpClient == null)
            getDefaultOkHttpClient();
        RequestBody body = RequestBody.create(JSON, parmas);
        Request.Builder reqBuilder = new Request.Builder();
        if (headers != null && headers.size() > 0) {
            Set<Map.Entry<String, String>> entries = headers.entrySet();
            for (Map.Entry<String, String> entry : entries) {
                reqBuilder.addHeader(entry.getKey(), entry.getValue());
            }
        }
        Request request = reqBuilder.url(urlPath).post(body).build();
        Call call = mOkHttpClient.newCall(request);
        call.enqueue(callback);
        return call;
    }


    /**
     * post请求
     *
     * @param urlPath url
     * @param parmas  参数
     */
    public Call postJson(String urlPath, String parmas, CookieJar cookieJar) {
        if (mCookieOkHttpClient == null)
            getCookieOkHttpClient(cookieJar);
        RequestBody body = RequestBody.create(JSON, parmas);
        Request request = new Request.Builder().url(urlPath).post(body).build();
        return mCookieOkHttpClient.newCall(request);
    }


    /**
     * post请求
     *
     * @param urlPath url
     * @param parmas  参数
     */
    public Call postJson(String urlPath, Map<String, String> headers, String parmas, CookieJar cookieJar) {
        if (mCookieOkHttpClient == null)
            getCookieOkHttpClient(cookieJar);
        RequestBody body = RequestBody.create(JSON, parmas);
        Request.Builder reqBuilder = new Request.Builder();
        if (headers != null && headers.size() > 0) {
            Set<Map.Entry<String, String>> entries = headers.entrySet();
            for (Map.Entry<String, String> entry : entries) {
                reqBuilder.addHeader(entry.getKey(), entry.getValue());
            }
        }
        Request request = reqBuilder.url(urlPath).post(body).build();
        return mCookieOkHttpClient.newCall(request);
    }

    /**
     * post请求
     *
     * @param urlPath url
     * @param parmas  参数
     */
    public Call postJson11(String urlPath, Map<String, String> headers, String parmas, CookieJar cookieJar) {
        if (mCookieOkHttpClient == null)
            getCookieOkHttpClient(cookieJar);

        Request.Builder reqBuilder = new Request.Builder();
        if (headers != null && headers.size() > 0) {
            Set<Map.Entry<String, String>> entries = headers.entrySet();
            for (Map.Entry<String, String> entry : entries) {
                reqBuilder.addHeader(entry.getKey(), entry.getValue());
            }
        }
        Request request;
        if (!TextUtils.isEmpty(parmas)) {
            RequestBody body = RequestBody.create(JSON, parmas);
            request = reqBuilder.url(urlPath).post(body).build();
        }else{
            request = reqBuilder.url(urlPath).post(null).build();
        }

        return mCookieOkHttpClient.newCall(request);
    }


    /**
     * post请求
     *
     * @param urlPath  url
     * @param parmas   参数
     * @param callback 结果回调
     */
    public Call postJson(String urlPath, String parmas, Callback callback, CookieJar cookieJar) {
        if (mCookieOkHttpClient == null)
            getCookieOkHttpClient(cookieJar);
        RequestBody body = RequestBody.create(JSON, parmas);
        Request request = new Request.Builder().url(urlPath).post(body).build();
        Call call = mCookieOkHttpClient.newCall(request);
        call.enqueue(callback);
        return call;
    }


    /**
     * post请求
     *
     * @param urlPath  url
     * @param parmas   参数
     * @param callback 结果回调
     */
    public Call postJson(String urlPath, Map<String, String> headers, String parmas, Callback callback, CookieJar cookieJar) {
        if (mCookieOkHttpClient == null)
            getCookieOkHttpClient(cookieJar);
        RequestBody body = RequestBody.create(JSON, parmas);
        Request.Builder reqBuilder = new Request.Builder();
        if (headers != null && headers.size() > 0) {
            Set<Map.Entry<String, String>> entries = headers.entrySet();
            for (Map.Entry<String, String> entry : entries) {
                reqBuilder.addHeader(entry.getKey(), entry.getValue());
            }
        }
        Request request = reqBuilder.url(urlPath).post(body).build();
        Call call = mCookieOkHttpClient.newCall(request);
        call.enqueue(callback);
        return call;
    }


    /**
     * 设置post builder body
     *
     * @param builder form builder
     * @param params  body
     * @return form builder
     */
    private FormBody.Builder setPostParmas(FormBody.Builder builder, Map<String, String> params) {
        if (params == null)
            return builder;
        Set<Map.Entry<String, String>> entries = params.entrySet();
        for (Map.Entry<String, String> entry : entries) {
            if (!TextUtils.isEmpty(entry.getKey()) && !TextUtils.isEmpty(entry.getValue()))
                builder.add(entry.getKey(), entry.getValue());
        }
        return builder;
    }

    /**************************************POST请求END***********************************************/

    /**************************************上传文件请求START***********************************************/
    /**
     * 上传文件
     *
     * @param urlPath  url
     * @param filePath 文件path
     */
    public Call uploadFile(String urlPath, String filePath) {
        if (mOkHttpClient == null)
            getDefaultOkHttpClient();
        File file = new File(filePath);
        String fileName = FileUtils.getFileNameNoFormat(filePath);
        RequestBody fileRequestBody = RequestBody.create(MediaType.parse("application/octet-stream"), file);
        /* form的分割线,自己定义 */
        String boundary = "xx--------------------------------------------------------------xx";
        MultipartBody mBody = new MultipartBody.Builder(boundary).setType(MultipartBody.FORM)
                .addFormDataPart("file", fileName, fileRequestBody)
                .build();
        Request request = new Request.Builder().url(urlPath).post(mBody).build();
        return mOkHttpClient.newCall(request);
    }

    /**
     * 上传文件
     *
     * @param urlPath  url
     * @param filePath 文件path
     * @param callback 结果回调
     */
    public void uploadFile(String urlPath, String filePath, Callback callback) {
        if (mOkHttpClient == null)
            getDefaultOkHttpClient();
        File file = new File(filePath);
        String fileName = FileUtils.getFileNameNoFormat(filePath);
        RequestBody fileRequestBody = RequestBody.create(MediaType.parse("application/octet-stream"), file);
        /* form的分割线,自己定义 */
        String boundary = "xx--------------------------------------------------------------xx";
        MultipartBody mBody = new MultipartBody.Builder(boundary).setType(MultipartBody.FORM)
                .addFormDataPart("file", fileName, fileRequestBody)
                .build();
        Request request = new Request.Builder().url(urlPath).post(mBody).build();
        mOkHttpClient.newCall(request).enqueue(callback);
    }

    /**
     * 带参数的文件上传
     *
     * @param urlPath  url
     * @param parmas   params
     * @param filePath filePath
     */
    public Call uploadFile(String urlPath, Map<String, String> parmas, String filePath) {
        if (mOkHttpClient == null)
            getDefaultOkHttpClient();
        File file = new File(filePath);
        String fileName = FileUtils.getFileName(filePath);
        RequestBody fileRequestBody = RequestBody.create(MediaType.parse("application/octet-stream"), file);
        /* form的分割线,自己定义 */
        String boundary = "xx--------------------------------------------------------------xx";
        MultipartBody.Builder builder = new MultipartBody.Builder(boundary).setType(MultipartBody.FORM);
        MultipartBody mBody = setUploadFileParmas(builder, parmas).addFormDataPart("file", fileName, fileRequestBody)
                .build();
        Request request = new Request.Builder().url(urlPath).post(mBody).build();
        return mOkHttpClient.newCall(request);
    }


    /**
     * 带参数的文件上传
     *
     * @param urlPath  url
     * @param parmas   params
     * @param filePath filePath
     * @param callback callback
     */
    public void uploadFile(String urlPath, Map<String, String> parmas, String filePath, Callback callback) {
        if (mOkHttpClient == null)
            getDefaultOkHttpClient();
        File file = new File(filePath);
        String fileName = FileUtils.getFileName(filePath);
        RequestBody fileRequestBody = RequestBody.create(MediaType.parse("application/octet-stream"), file);
        /* form的分割线,自己定义 */
        String boundary = "xx--------------------------------------------------------------xx";
        MultipartBody.Builder builder = new MultipartBody.Builder(boundary).setType(MultipartBody.FORM);
        MultipartBody mBody = setUploadFileParmas(builder, parmas).addFormDataPart("file", fileName, fileRequestBody)
                .build();
        Request request = new Request.Builder().url(urlPath).post(mBody).build();
        mOkHttpClient.newCall(request).enqueue(callback);
    }


    /**
     * 带参数的文件上传
     *
     * @param urlPath   url
     * @param parmas    params
     * @param filePaths filePaths
     */
    public Call uploadFile(@NonNull String urlPath, Map<String, String> parmas, @NonNull List<String> filePaths) {
        if (mOkHttpClient == null)
            getDefaultOkHttpClient();
        /* form的分割线,自己定义 */
        String boundary = "xx--------------------------------------------------------------xx";
        MultipartBody.Builder builder = new MultipartBody.Builder(boundary).setType(MultipartBody.FORM);
        setUploadFileParmas(builder, parmas);
        for (String filePath : filePaths) {
            File file = new File(filePath);
            String fileName = FileUtils.getFileName(filePath);
            builder.addFormDataPart("file", fileName, RequestBody.create(MediaType.parse("application/octet-stream"), file));
        }
        Request request = new Request.Builder().url(urlPath).post(builder.build()).build();
        return mOkHttpClient.newCall(request);
    }


    /**
     * 带参数的文件上传
     *
     * @param urlPath   url
     * @param parmas    params
     * @param filePaths filePath
     * @param callback  callback
     */
    public void uploadFile(@NonNull String urlPath, Map<String, String> parmas, @NonNull List<String> filePaths, @NonNull Callback callback) {
        if (mOkHttpClient == null)
            getDefaultOkHttpClient();
        /* form的分割线,自己定义 */
        String boundary = "xx--------------------------------------------------------------xx";
        MultipartBody.Builder builder = new MultipartBody.Builder(boundary).setType(MultipartBody.FORM);
        setUploadFileParmas(builder, parmas);
        for (String filePath : filePaths) {
            File file = new File(filePath);
            String fileName = FileUtils.getFileName(filePath);
            builder.addFormDataPart("file", fileName, RequestBody.create(MediaType.parse("application/octet-stream"), file));
        }
        Request request = new Request.Builder().url(urlPath).post(builder.build()).build();
        mOkHttpClient.newCall(request).enqueue(callback);
    }


    /**
     * 文件上传
     *
     * @param urlPath
     * @param parmas
     * @param filePath
     * @param onUploadFilObserver
     */
    public void uploadFile(String urlPath, Map<String, String> parmas, String filePath, OnUploadFileObserver<String> onUploadFilObserver) {
        if (mOkHttpClient == null)
            getDefaultOkHttpClient();
        File file = new File(filePath);
        String fileName = FileUtils.getFileName(filePath);
        RequestBody fileRequestBody = new UploadFileRequestBody(file, onUploadFilObserver);
        /* form的分割线,自己定义 */
        String boundary = "xx--------------------------------------------------------------xx";
        MultipartBody.Builder builder = new MultipartBody.Builder(boundary).setType(MultipartBody.FORM);
        MultipartBody mBody = setUploadFileParmas(builder, parmas).addFormDataPart("file", fileName, fileRequestBody)
                .build();
        final Request request = new Request.Builder().url(urlPath).post(mBody).build();
        Observable.create(new ObservableOnSubscribe<String>() {
            @Override
            public void subscribe(ObservableEmitter<String> e) throws Exception {
                Response response = mOkHttpClient.newCall(request).execute();
                ResponseBody body = response.body();
                if (response.code() == 200) {
                    e.onNext(body == null ? "" : body.string());
                } else {
                    e.onError(new Throwable(response.code() + "" + (body == null ? "" : body.toString())));
                }
            }
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(onUploadFilObserver);
    }


    /**
     * 文件上传
     *
     * @param urlPath
     * @param parmas
     * @param filePaths
     * @param onUploadFilObserver
     */
    public void uploadFile(String urlPath, Map<String, String> parmas, List<String> filePaths, OnUploadFileObserver<String> onUploadFilObserver) {
        if (mOkHttpClient == null)
            getDefaultOkHttpClient();
        /* form的分割线,自己定义 */
        String boundary = "xx--------------------------------------------------------------xx";
        MultipartBody.Builder builder = new MultipartBody.Builder(boundary).setType(MultipartBody.FORM);
        setUploadFileParmas(builder, parmas);
        for (String filePath : filePaths) {
            File file = new File(filePath);
            String fileName = FileUtils.getFileName(filePath);
            RequestBody fileRequestBody = new UploadFileRequestBody(file, onUploadFilObserver);
            builder.addFormDataPart("file", fileName, fileRequestBody);
        }
        final Request request = new Request.Builder().url(urlPath).post(builder.build()).build();
        Observable.create(new ObservableOnSubscribe<String>() {
            @Override
            public void subscribe(ObservableEmitter<String> e) throws Exception {
                Response response = mOkHttpClient.newCall(request).execute();
                ResponseBody body = response.body();
                if (response.code() == 200) {
                    e.onNext(body == null ? "" : body.string());
                } else {
                    e.onError(new Throwable(response.code() + "" + (body == null ? "" : body.toString())));
                }
            }
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(onUploadFilObserver);
    }


    /**
     * 设置额外参数 MultipartBody.Builder
     *
     * @param builder MultipartBody.Builder
     * @param params  params
     * @return MultipartBody.Builder
     */
    public static MultipartBody.Builder setUploadFileParmas(MultipartBody.Builder builder, Map<String, String> params) {
        if (params == null)
            return builder;
        Set<Map.Entry<String, String>> entries = params.entrySet();
        for (Map.Entry<String, String> entry : entries) {
            if (!TextUtils.isEmpty(entry.getKey()))
                builder.addFormDataPart(entry.getKey(), entry.getValue());
        }
        return builder;
    }


    /**************************************上传文件请求END***********************************************/


}
