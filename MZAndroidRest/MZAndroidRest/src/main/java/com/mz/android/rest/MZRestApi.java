package com.mz.android.rest;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.jakewharton.retrofit2.adapter.rxjava2.HttpException;
import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import com.mz.android.rest.convert.MZRestGsonConverterFactory;

import org.reactivestreams.Publisher;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.security.cert.CertificateException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.functions.Function3;
import io.reactivex.functions.Function4;
import io.reactivex.functions.Function5;
import io.reactivex.schedulers.Schedulers;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Converter;
import retrofit2.Retrofit;

/**
 * Rest API基类
 *
 * @author Mourinho.Zhu on 16-11-24.
 */
public abstract class MZRestApi<SERVICE> {
    //TAG
    private static final String TAG = "MZRestApi";

    //HTTP 读取超时时间默认30秒
    private int mHttpReadTimeoutSecond = 30;
    //HTTP 读取连接时间30秒
    private int mHttpConnectTimeoutSecond = 30;

    //接口base URL
    private String mBaseUrl;

    //Gson解析类
    private Converter.Factory mConverterFactory = MZRestGsonConverterFactory
            .create();

    //rest service class
    private Class<SERVICE> mServiceClass;

    //rest service
    private SERVICE mService;

    //上下文
    private Context mContext;

    /**
     * 构造函数
     *
     * @param context      上下文
     * @param baseUrl      base URL
     * @param serviceClass service class
     */
    public MZRestApi(Context context, String baseUrl, Class<SERVICE> serviceClass) {
        mContext = context;
        mBaseUrl = baseUrl;
        mServiceClass = serviceClass;
    }

    //获取ok http client
    private OkHttpClient getUnsafeOkHttpClient() {
        try {
            // Create a trust manager that does not validate certificate chains
            final TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
                @Override
                public void checkClientTrusted(
                        java.security.cert.X509Certificate[] chain,
                        String authType) throws CertificateException {
                }

                @Override
                public void checkServerTrusted(
                        java.security.cert.X509Certificate[] chain,
                        String authType) throws CertificateException {
                }

                @Override
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return new java.security.cert.X509Certificate[0];
                }
            }};

            // Install the all-trusting trust manager
            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts,
                    new java.security.SecureRandom());
            // Create an ssl socket factory with our all-trusting manager
            final SSLSocketFactory sslSocketFactory = sslContext
                    .getSocketFactory();
            // Define the interceptor, add authentication headers
            Interceptor interceptor = new Interceptor() {
                @Override
                public okhttp3.Response intercept(Chain chain)
                        throws IOException {
                    Request.Builder builder = chain.request().newBuilder();
                    Map<String, String> headerMap = getHeaders();
                    if (null != headerMap) {
                        Log.d(TAG, "add  headerMap --> " + headerMap);
                        for (String key : headerMap.keySet()) {
                            builder.addHeader(key, headerMap.get(key));
                        }
                    }
                    Request newRequest = builder.build();
                    Log.d(TAG, "request --> " + newRequest.url());
                    Log.d(TAG, "body --> " + newRequest.body());
                    Log.d(TAG, "headers --> " + newRequest.headers());
                    return chain.proceed(newRequest);
                }
            };
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor(new HttpLoggingInterceptor.Logger() {
                @Override
                public void log(String message) {
                    //打印retrofit日志
                    Log.i("RetrofitLog", "retrofitBack = " + message);
                }
            });
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    .hostnameVerifier(new HostnameVerifier() {

                        @Override
                        public boolean verify(String hostname,
                                              SSLSession session) {
                            Log.d(TAG,
                                    "getProtocol --> " + session.getProtocol());
                            return true;
                        }
                    })
                    .readTimeout(mHttpReadTimeoutSecond, TimeUnit.SECONDS)
                    .connectTimeout(mHttpConnectTimeoutSecond,
                            TimeUnit.SECONDS)
                    .addInterceptor(interceptor)
                    .addInterceptor(loggingInterceptor)
                    .sslSocketFactory(sslSocketFactory).build();
            return okHttpClient;
        } catch (Exception e) {
            Log.e(TAG, "getUnsafeOkHttpClient error!");
            throw new RuntimeException(e);
        }
    }

    /**
     * 初始化 rest service
     */
    protected boolean init() {
        if (TextUtils.isEmpty(mBaseUrl)) {
            Log.e(TAG, "createService error empty base url");
        } else if (null == mServiceClass) {
            Log.e(TAG, "createService error empty service class");
        } else {
            try {
                OkHttpClient client = getUnsafeOkHttpClient();
                Retrofit retrofit = new Retrofit.Builder().baseUrl(mBaseUrl)
                        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())//使用RxJava
                        .addConverterFactory(mConverterFactory)
                        .client(client)
                        .build();
                mService = retrofit.create(mServiceClass);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, "createService error class -> " + mServiceClass);
            }
        }
        return false;
    }

    /**
     * 开始异步http请求
     *
     * @param call                请求体
     * @param checkResultFunction 比较返回码的function
     * @param callback            应用层回调
     * @return Subscriber 订阅者
     */
    protected <T, R> Disposable startRestAsync(final Observable<T> call, final Function<T, Publisher<MZRestResult<R>>> checkResultFunction, final MZRestICallback<R> callback) {
        class RestObserver {
            boolean mHasContent = false;
            Consumer<? super T> onNext = new Consumer<T>() {
                @Override
                public void accept(@NonNull T t) throws Exception {
                    Log.d(TAG, "onNext");
                    mHasContent = true;
                    Flowable<T> data = Flowable.just(t);
                    data.subscribeOn(Schedulers.io())
                            .flatMap(checkResultFunction)
                            .observeOn(Schedulers.io())
                            .doOnNext(new Consumer<MZRestResult<R>>() {
                                @Override
                                public void accept(MZRestResult<R> result) throws Exception {
                                    Log.d(TAG, "doOnNext");
                                    if (null != result && result.isSuccess()) {
                                        callback.onRestSuccessIO(result.getData());
                                    }
                                }
                            })
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(new Consumer<MZRestResult<R>>() {
                                @Override
                                public void accept(MZRestResult<R> result) throws Exception {
                                    if (null != result && result.isSuccess()) {
                                        Log.d(TAG, "parse success");
                                        callback.onRestSuccessUI(result.getData());
                                    } else {
                                        Log.d(TAG, "parse success but code is fail");
                                        callback.onRestFail(result.getCode(), result.getMessage());
                                    }
                                }
                            }, new Consumer<Throwable>() {
                                @Override
                                public void accept(Throwable t) throws Exception {
                                    t.printStackTrace();
                                    Log.e(TAG, "parse error ");
                                    callback.onRestError(MZRestStatusCode.ERROR_PARSE_RESPONSE);
                                }
                            });
                }
            };
            Consumer<? super Throwable> onError = new Consumer<Throwable>() {
                @Override
                public void accept(@NonNull Throwable t) throws Exception {
                    mHasContent = true;
                    Log.e(TAG, "onError");
                    handlerHttpError(t, callback);
                }
            };
            Action onComplete = new Action() {
                @Override
                public void run() throws Exception {
                    Log.d(TAG, "onComplete mHasContent --> " + mHasContent);
                    //不处理
                    if (!mHasContent) {
                        callback.onRestSuccessUI(null);
                    }
                }
            };
        }
        RestObserver restObserver = new RestObserver();

        if (null != call && null != checkResultFunction && null != callback) {
            return call.subscribeOn(Schedulers.io()).unsubscribeOn(AndroidSchedulers.mainThread())
                    .observeOn(AndroidSchedulers.mainThread()).subscribe(restObserver.onNext, restObserver.onError, restObserver.onComplete);
        } else {
            Log.e(TAG, "param is null,please check");
        }
        return null;
    }

    /**
     * 开始批量异步http请求,同时返回结果
     *
     * @param call1               请求1
     * @param call2               请求2
     * @param checkResultFunction 比较返回码的function
     * @param callback            应用层回调
     * @return Subscriber 订阅者
     */
    protected <T1, T2, R> Disposable startRestBatchAsync(Observable<T1> call1,
                                                         Observable<T2> call2,
                                                         final BiFunction<T1, T2, MZRestResult<R>> checkResultFunction,
                                                         final MZRestICallback<R> callback) {
        BatchObserver observer = new BatchObserver(callback);
        Disposable disposable = null;

        if (null != call1 && null != call2 && null != checkResultFunction && null != callback) {
            call1 = call1.subscribeOn(Schedulers.io());
            call2 = call2.subscribeOn(Schedulers.io());
            disposable = Observable.zip(call1, call2, checkResultFunction)
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.io())
                    .doOnNext(new Consumer<MZRestResult<R>>() {
                        @Override
                        public void accept(MZRestResult<R> result) throws Exception {
                            Log.d(TAG, "doOnNext");
                            if (null != result && result.isSuccess()) {
                                callback.onRestSuccessIO(result.getData());
                            }
                        }
                    })
                    .unsubscribeOn(AndroidSchedulers.mainThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(observer.onNext, observer.onError);
        } else {
            Log.e(TAG, "param is null,please check");
        }
        return disposable;
    }

    /**
     * 开始批量异步http请求,同时返回结果
     *
     * @param call1               请求1
     * @param call2               请求2
     * @param call3               请求3
     * @param checkResultFunction 比较返回码的function
     * @param callback            应用层回调
     * @return Subscriber 订阅者
     */
    protected <T1, T2, T3, R> Disposable startRestBatchAsync(Observable<T1> call1,
                                                             Observable<T2> call2,
                                                             Observable<T3> call3,
                                                             final Function3<T1, T2, T3, MZRestResult<R>> checkResultFunction,
                                                             final MZRestICallback<R> callback) {
        BatchObserver observer = new BatchObserver(callback);
        Disposable disposable = null;

        if (null != call1 && null != call2 && null != call3 && null != checkResultFunction && null != callback) {
            call1 = call1.subscribeOn(Schedulers.io());
            call2 = call2.subscribeOn(Schedulers.io());
            call3 = call3.subscribeOn(Schedulers.io());

            disposable = Observable.zip(call1, call2, call3, checkResultFunction)
                    .observeOn(Schedulers.io())
                    .doOnNext(new Consumer<MZRestResult<R>>() {
                        @Override
                        public void accept(MZRestResult<R> result) throws Exception {
                            Log.d(TAG, "doOnNext");
                            if (null != result && result.isSuccess()) {
                                callback.onRestSuccessIO(result.getData());
                            }
                        }
                    })
                    .unsubscribeOn(AndroidSchedulers.mainThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(observer.onNext, observer.onError);
        } else {
            Log.e(TAG, "param is null,please check");
        }
        return disposable;
    }

    /**
     * 开始批量异步http请求,同时返回结果
     *
     * @param call1               请求1
     * @param call2               请求2
     * @param call3               请求3
     * @param call4               请求4
     * @param checkResultFunction 比较返回码的function
     * @param callback            应用层回调
     * @return Subscriber 订阅者
     */
    protected <T1, T2, T3, T4, R> Disposable startRestBatchAsync(Observable<T1> call1,
                                                                 Observable<T2> call2,
                                                                 Observable<T3> call3,
                                                                 Observable<T4> call4,
                                                                 final Function4<T1, T2, T3, T4, MZRestResult<R>> checkResultFunction,
                                                                 final MZRestICallback<R> callback) {
        BatchObserver observer = new BatchObserver(callback);
        Disposable disposable = null;

        if (null != call1 && null != call2 && null != call3 && null != call4 && null != checkResultFunction && null != callback) {
            call1 = call1.subscribeOn(Schedulers.io());
            call2 = call2.subscribeOn(Schedulers.io());
            call3 = call3.subscribeOn(Schedulers.io());
            call4 = call4.subscribeOn(Schedulers.io());

            disposable = Observable.zip(call1, call2, call3, call4, checkResultFunction)
                    .observeOn(Schedulers.io())
                    .doOnNext(new Consumer<MZRestResult<R>>() {
                        @Override
                        public void accept(MZRestResult<R> result) throws Exception {
                            Log.d(TAG, "doOnNext");
                            if (null != result && result.isSuccess()) {
                                callback.onRestSuccessIO(result.getData());
                            }
                        }
                    })
                    .unsubscribeOn(AndroidSchedulers.mainThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(observer.onNext, observer.onError);
        } else {
            Log.e(TAG, "param is null,please check");
        }
        return disposable;
    }

    /**
     * 开始批量异步http请求,同时返回结果
     *
     * @param call1               请求1
     * @param call2               请求2
     * @param call3               请求3
     * @param call4               请求4
     * @param call5               请求4
     * @param checkResultFunction 比较返回码的function
     * @param callback            应用层回调
     * @return Subscriber 订阅者
     */
    protected <T1, T2, T3, T4, T5, R> Disposable startRestBatchAsync(Observable<T1> call1,
                                                                     Observable<T2> call2,
                                                                     Observable<T3> call3,
                                                                     Observable<T4> call4,
                                                                     Observable<T5> call5,
                                                                     final Function5<T1, T2, T3, T4, T5, MZRestResult<R>> checkResultFunction,
                                                                     final MZRestICallback<R> callback) {
        BatchObserver observer = new BatchObserver(callback);
        Disposable disposable = null;

        if (null != call1 && null != call2 && null != call3 && null != call4 && null != call5 && null != checkResultFunction && null != callback) {
            call1 = call1.subscribeOn(Schedulers.io());
            call2 = call2.subscribeOn(Schedulers.io());
            call3 = call3.subscribeOn(Schedulers.io());
            call4 = call4.subscribeOn(Schedulers.io());
            call5 = call5.subscribeOn(Schedulers.io());

            disposable = Observable.zip(call1, call2, call3, call4, call5, checkResultFunction)
                    .observeOn(Schedulers.io())
                    .doOnNext(new Consumer<MZRestResult<R>>() {
                        @Override
                        public void accept(MZRestResult<R> result) throws Exception {
                            Log.d(TAG, "doOnNext");
                            if (null != result && result.isSuccess()) {
                                callback.onRestSuccessIO(result.getData());
                            }
                        }
                    })
                    .unsubscribeOn(AndroidSchedulers.mainThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(observer.onNext, observer.onError);
        } else {
            Log.e(TAG, "param is null,please check");
        }
        return disposable;
    }


    //处理http请求失败事件
    private void handlerHttpError(Throwable t, MZRestICallbackBase callback) {
        if (!MZNetwork.isNetworkAvailable(mContext)) {
            Log.w(TAG, "network is not available");
            callback.onRestError(MZRestStatusCode.ERROR_NETWORK_UNAVAILABLE);
        } else if (t instanceof SocketTimeoutException) {
            callback.onRestError(MZRestStatusCode.ERROR_REQUEST_TIMEOUT);
        } else if (t instanceof HttpException) {
            HttpException httpException = (HttpException) t;
            httpException.printStackTrace();
            try {
                Log.e(TAG, "httpException response --> " + httpException.response().errorBody().string());
            } catch (Exception e) {
                e.printStackTrace();
            }
            callback.onRestError(httpException.code());
        } else {
            callback.onRestError(MZRestStatusCode.ERROR_NETWORK);
        }
        if (null != t) {
            t.printStackTrace();
        }
    }

    //比较返回的code
    protected <R> Publisher<MZRestResult<R>> getResult(int code, int successCode, String message, R data) {
        return Flowable.just(new MZRestResult<>(code == successCode, code, message, data));
    }
    //***************************get/set**************************//

    /**
     * 获取rest base URL
     *
     * @return rest base URL
     */
    public String getBaseUrl() {
        return mBaseUrl;
    }

    /**
     * 设置 rest base URL
     *
     * @param baseUrl rest base URL
     */
    protected void setBaseUrl(String baseUrl) {
        this.mBaseUrl = baseUrl;
    }

    /**
     * 获取 context
     *
     * @return context
     */
    public Context getContext() {
        return mContext;
    }

    /**
     * 设置 HTTP 读取超时时间
     *
     * @param httpReadTimeoutSecond 读取超时时间 (单位:秒)
     */
    public void setHttpReadTimeoutSecond(int httpReadTimeoutSecond) {
        mHttpReadTimeoutSecond = httpReadTimeoutSecond;
        init();
    }

    /**
     * 设置 HTTP 连接超时时间
     *
     * @param httpConnectTimeoutSecond 连接超时时间 (单位:秒)
     */
    public void setHttpConntectTimeoutSecond(int httpConnectTimeoutSecond) {
        mHttpConnectTimeoutSecond = httpConnectTimeoutSecond;
        init();
    }

    /**
     * 设置自定义解析类
     *
     * @param factory 自定义解析类
     */
    protected void setConverterFactory(Converter.Factory factory) {
        mConverterFactory = factory;
    }

    /**
     * 获取service实例
     *
     * @return service实例
     */
    protected SERVICE getService() {
        return mService;
    }

    public abstract Map<String, String> getHeaders();

    private class BatchObserver<R> {
        private MZRestICallback<R> callback;

        public BatchObserver(MZRestICallback<R> callback) {
            this.callback = callback;
        }

        Consumer<? super MZRestResult<R>> onNext = new Consumer<MZRestResult<R>>() {

            @Override
            public void accept(@NonNull MZRestResult<R> result) throws Exception {
                if (null != result && result.isSuccess()) {
                    Log.d(TAG, "parse success");
                    callback.onRestSuccessUI(result.getData());
                } else {
                    Log.d(TAG, "parse success but code is fail");
                    callback.onRestFail(result.getCode(), result.getMessage());
                }
            }
        };

        Consumer<? super Throwable> onError = new Consumer<Throwable>() {
            @Override
            public void accept(@NonNull Throwable t) throws Exception {
                Log.e(TAG, "onError");
                handlerHttpError(t, callback);
            }
        };
    }
}
