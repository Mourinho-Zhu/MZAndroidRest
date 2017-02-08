package com.mz.android.rest;

import android.content.Context;
import android.text.TextUtils;

import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import com.mz.android.rest.convert.MZRestGsonConverterFactory;
import com.mz.android.util.log.MZLog;
import com.mz.android.util.network.MZNetwork;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import io.reactivex.Flowable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
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
                    Request newRequest = chain.request().newBuilder()
                            .addHeader("User-Agent", "Retrofit-Sample-App")
                            .build();
                    return chain.proceed(newRequest);
                }
            };
            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    .hostnameVerifier(new HostnameVerifier() {

                        @Override
                        public boolean verify(String hostname,
                                              SSLSession session) {
                            MZLog.d(TAG,
                                    "getProtocol --> " + session.getProtocol());
                            return true;
                        }
                    })
                    .readTimeout(mHttpReadTimeoutSecond, TimeUnit.SECONDS)
                    .connectTimeout(mHttpConnectTimeoutSecond,
                            TimeUnit.SECONDS).addInterceptor(interceptor)
                    .sslSocketFactory(sslSocketFactory).build();
            return okHttpClient;
        } catch (Exception e) {
            MZLog.e(TAG, "getUnsafeOkHttpClient error!");
            throw new RuntimeException(e);
        }
    }

    /**
     * 初始化 rest service
     */
    protected void init() {
        if (TextUtils.isEmpty(mBaseUrl)) {
            MZLog.e(TAG, "createService error empty base url");
        } else if (null == mServiceClass) {
            MZLog.e(TAG, "createService error empty service class");
        } else {
            try {
                OkHttpClient client = getUnsafeOkHttpClient();
                Retrofit retrofit = new Retrofit.Builder().baseUrl(mBaseUrl)
                        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())//使用RxJava
                        .addConverterFactory(mConverterFactory).client(client)
                        .build();
                mService = retrofit.create(mServiceClass);
            } catch (Exception e) {
                e.printStackTrace();
                MZLog.e(TAG, "createService error class -> " + mServiceClass);
            }
        }
    }

    /**
     * 开始异步http请求
     *
     * @param call                请求体
     * @param checkResultFunction 比较返回码的function
     * @param callback            应用层回调
     * @return Subscriber 订阅者
     */
    protected <T, R> Subscriber startRestAsync(final Flowable<T> call, final Function<T, Publisher<MZRestResult<R>>> checkResultFunction, final MZRestICallback<R> callback) {
        Subscriber subscriber = new Subscriber<T>() {
            @Override
            public void onSubscribe(Subscription s) {
                s.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(final T t) {
                Flowable<T> data = Flowable.just(t);
                data.subscribeOn(Schedulers.io())
                        .flatMap(checkResultFunction)
                        .observeOn(Schedulers.io())
                        .doOnNext(new Consumer<MZRestResult<R>>() {
                            @Override
                            public void accept(MZRestResult<R> result) throws Exception {
                                MZLog.d(TAG, "doOnNext");
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
                                    MZLog.d(TAG, "parse success");
                                    callback.onRestSuccessUI(result.getData());
                                } else {
                                    MZLog.d(TAG, "parse success but code is fail");
                                    callback.onRestFail(result.getCode(), result.getMessage());
                                }
                            }
                        }, new Consumer<Throwable>() {
                            @Override
                            public void accept(Throwable t) throws Exception {
                                t.printStackTrace();
                                MZLog.e(TAG, "parse error ");
                                callback.onRestError(MZRestStatusCode.ERROR_PARSE_RESPONSE);
                            }
                        });
            }

            @Override
            public void onError(Throwable t) {
                MZLog.m(TAG);
                handlerHttpError(t, callback);
            }

            @Override
            public void onComplete() {
                //不处理
            }
        };
        if (null != call && null != checkResultFunction && null != callback) {
            call.subscribeOn(Schedulers.io()).unsubscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread()).subscribe(subscriber);
        } else {
            MZLog.e(TAG, "param is null,please check");
        }
        return subscriber;
    }

    /**
     * 开始批量异步http请求
     *
     * @param calls                请求体集合
     * @param checkResultFunction 比较返回码的function
     * @param callback            应用层回调
     * @return Subscriber 订阅者
     */
    protected <T, R> Subscriber startRestBatchAsync(final List<Flowable<T>> calls, final Function<T, Publisher<MZRestResult<R>>> checkResultFunction
            , final MZRestBatchICallback<R> callback) {
        Subscriber subscriber = new Subscriber<T>() {
            List<R> resultList = new ArrayList<R>();
            List<MZRestResult<R>> errorList = new ArrayList<MZRestResult<R>>();

            @Override
            public void onSubscribe(Subscription s) {
                s.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(final T t) {
                Flowable<T> data = Flowable.just(t);
                data.subscribeOn(Schedulers.io())
                        .flatMap(checkResultFunction)
                        .observeOn(Schedulers.io())
                        .subscribe(new Consumer<MZRestResult<R>>() {
                            @Override
                            public void accept(MZRestResult<R> result) throws Exception {
                                if (null != result && result.isSuccess()) {
                                    MZLog.d(TAG, "parse success");
                                    resultList.add(result.getData());
                                } else {
                                    errorList.add(result);
                                }
                            }
                        }, new Consumer<Throwable>() {
                            @Override
                            public void accept(Throwable t) throws Exception {
                                t.printStackTrace();
                                MZLog.e(TAG, "parse error ");
                                callback.onRestError(MZRestStatusCode.ERROR_PARSE_RESPONSE);
                            }
                        });
            }

            @Override
            public void onError(Throwable t) {
                if(!resultList.isEmpty()) {
                    onComplete();
                } else {
                    MZLog.m(TAG);
                    handlerHttpError(t, callback);
                }
            }

            @Override
            public void onComplete() {
                Flowable<MZRestBatchICallback<R>> data = Flowable.just(callback);
                data.subscribeOn(Schedulers.io())
                        .observeOn(Schedulers.io())
                        .doOnNext(new Consumer<MZRestBatchICallback<R>>() {
                            @Override
                            public void accept(MZRestBatchICallback<R> callback) throws Exception {
                                MZLog.d(TAG, "doOnNext");
                                if (!resultList.isEmpty()) {
                                    MZLog.d(TAG, "parse success resultList size --> " + resultList.size());
                                    callback.onRestSuccessIO(resultList);
                                }
                            }
                        })
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Consumer<MZRestBatchICallback<R>>() {
                            @Override
                            public void accept(MZRestBatchICallback<R> callback) throws Exception {
                                if (!resultList.isEmpty()) {
                                    MZLog.d(TAG, "parse success resultList size --> " + resultList.size());
                                    callback.onRestSuccessUI(resultList);
                                } else {
                                    MZLog.d(TAG, "parse success but code is fail");
                                    callback.onRestFail(errorList);
                                }
                            }
                        });
            }
        };

        if (null != calls && null != checkResultFunction && null != callback) {
            Flowable.merge(calls).mergeDelayError(calls).subscribeOn(Schedulers.io()).unsubscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread()).subscribe(subscriber);
        } else {
            MZLog.e(TAG, "param is null,please check");
        }
        return subscriber;
    }


    //处理http请求失败事件
    private void handlerHttpError(Throwable t, MZRestICallbackBase callback) {
        if (!MZNetwork.isNetworkAvailable(mContext)) {
            MZLog.w(TAG, "network is not available");
            callback.onRestError(MZRestStatusCode.ERROR_NETWORK_UNAVAILABLE);
        } else if (t instanceof SocketTimeoutException) {
            callback.onRestError(MZRestStatusCode.ERROR_REQUEST_TIMEOUT);
        } else {
            callback.onRestError(MZRestStatusCode.ERROR_NETWORK);
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
}
