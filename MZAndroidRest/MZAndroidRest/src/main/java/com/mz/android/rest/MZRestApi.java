package com.mz.android.rest;

import android.content.Context;
import android.text.TextUtils;

import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import com.mz.android.rest.callback.MZRestHeaderICallback;
import com.mz.android.rest.callback.MZRestICallback;
import com.mz.android.rest.convert.MZRestGsonConverterFactory;
import com.mz.android.rest.persistentcookiejar.ClearableCookieJar;
import com.mz.android.rest.persistentcookiejar.PersistentCookieJar;
import com.mz.android.rest.persistentcookiejar.cache.SetCookieCache;
import com.mz.android.rest.persistentcookiejar.persistence.SharedPrefsCookiePersistor;

import org.reactivestreams.Publisher;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
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
import okhttp3.Cookie;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Converter;
import retrofit2.Response;
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

    //打印的log最大字符
    private int mMaxLogLength = Integer.MAX_VALUE;

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

    //cookie jar
    private ClearableCookieJar mCookieJar;

    //cookie enable flag
    private boolean mIsCookieEnabled;

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
        mCookieJar = new PersistentCookieJar(new SetCookieCache(), new SharedPrefsCookiePersistor(context));
    }

    //获取ok http client
    private OkHttpClient getOkHttpClient(final SSLContext sslContext) {
        try {
            // Create an ssl socket factory with our all-trusting manager
            final SSLSocketFactory sslSocketFactory = sslContext
                    .getSocketFactory();
            // Define the interceptor, add authentication headers
            Interceptor requestInterceptor = new Interceptor() {
                @Override
                public okhttp3.Response intercept(Chain chain)
                        throws IOException {
                    Request.Builder builder = chain.request().newBuilder();
                    Map<String, String> headerMap = getHeaders();
                    if (null != headerMap) {
                        for (String key : headerMap.keySet()) {
                            builder.addHeader(key, headerMap.get(key));
                        }
                    }
                    Request newRequest = builder.build();
                    return chain.proceed(newRequest);
                }
            };

            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor(new HttpLoggingInterceptor.Logger() {
                @Override
                public void log(String message) {
                    if (null != message && message.length() > mMaxLogLength) {
                        message = message.substring(0, mMaxLogLength);
                    }
                    //打印retrofit日志
                    MZLog.d(TAG, message);
                }
            });
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient.Builder builder = new OkHttpClient.Builder()
                    .hostnameVerifier(new HostnameVerifier() {

                        @Override
                        public boolean verify(String hostname,
                                              SSLSession session) {
                            return true;
                        }
                    })
                    .readTimeout(mHttpReadTimeoutSecond, TimeUnit.SECONDS)
                    .connectTimeout(mHttpConnectTimeoutSecond,
                            TimeUnit.SECONDS)
                    .addInterceptor(requestInterceptor)
                    .addInterceptor(loggingInterceptor)
                    .sslSocketFactory(sslSocketFactory);

            if (mIsCookieEnabled) {
                MZLog.d(TAG, "cookie is enabled");
                builder = builder.cookieJar(mCookieJar);
            }

            return builder.build();
        } catch (Exception e) {
            MZLog.e(TAG, "getUnsafeOkHttpClient error!");
            throw new RuntimeException(e);
        }
    }

    private SSLContext buildDefaultSSLContext() {
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
            return sslContext;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    protected boolean init() {
        return init(buildDefaultSSLContext());
    }

    /**
     * 初始化 rest service
     *
     * @param sslContext ssl context
     * @see com.mz.android.rest.MZRestApi buildDefaultSSLContext function
     */
    protected boolean init(SSLContext sslContext) {
        if (TextUtils.isEmpty(mBaseUrl)) {
            MZLog.e(TAG, "createService error empty base url");
        } else if (null == mServiceClass) {
            MZLog.e(TAG, "createService error empty service class");
        } else {
            try {
                OkHttpClient client = getOkHttpClient(sslContext);
                Retrofit retrofit = new Retrofit.Builder().baseUrl(mBaseUrl)
                        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())//使用RxJava
                        .addConverterFactory(mConverterFactory)
                        .client(client)
                        .build();
                mService = retrofit.create(mServiceClass);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                MZLog.e(TAG, "createService error class -> " + mServiceClass);
            }
        }
        return false;
    }

    private final static String CERTIFICATE_STANDARD = "X509";

    /**
     * 构建双向认证SSLContext
     *
     * @param context               上下文
     * @param clientBksPath         客户端bks文件路径
     * @param clientBksPassword     客户端bks密码
     * @param trustStoreBksPath     信任机构bks文件路径
     * @param trustStoreBksPassword 信任机构bks密码
     * @param sslProtocol           ssl协议类型，例如"BKS"
     * @param trustManagerProtocol  trust manager协议类型，例如"TLS"
     * @param certificateAlgorithm  证书算法,例如"X509"
     * @return SSLContext
     */
    public SSLContext getSSLCertifcation(Context context, String clientBksPath, String clientBksPassword,
                                         String trustStoreBksPath, String trustStoreBksPassword,
                                         String sslProtocol, String trustManagerProtocol,
                                         String certificateAlgorithm) {
        SSLContext sslContext = null;
        try {
            // 服务器端需要验证的客户端证书，其实就是客户端的keystore
            KeyStore keyStore = KeyStore.getInstance(sslProtocol);
            // 客户端信任的服务器端证书
            KeyStore trustStore = KeyStore.getInstance(sslProtocol);

            //读取证书
//            InputStream ksIn = context.getAssets().open(clientBksPath);
//            InputStream tsIn = context.getAssets().open(clientBksPassword);
            InputStream ksIn = new FileInputStream(new File(clientBksPath));
            InputStream tsIn = new FileInputStream(new File(trustStoreBksPath));
            //加载证书
            keyStore.load(ksIn, clientBksPassword.toCharArray());
            trustStore.load(tsIn, trustStoreBksPassword.toCharArray());
            ksIn.close();
            tsIn.close();

            //初始化SSLContext
            sslContext = SSLContext.getInstance(trustManagerProtocol);
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(certificateAlgorithm);
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(certificateAlgorithm);
            trustManagerFactory.init(trustStore);
            keyManagerFactory.init(keyStore, trustStoreBksPath.toCharArray());
            sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);
        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (UnrecoverableKeyException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }
        return sslContext;
    }

    /**
     * 开始异步http请求
     *
     * @param call                请求体
     * @param checkResultFunction 比较返回码的function
     * @param callback            应用层回调
     * @return Subscriber 订阅者
     */
    protected <T, R> Disposable startRestAsync(final Observable<T> call,
                                               final Function<T, Publisher<MZRestResult<R>>> checkResultFunction,
                                               final MZRestICallback<R> callback) {
        if (null != call && null != checkResultFunction && null != callback) {
            MZRestObserver<T, R> restObserver = new MZRestObserver<>(mContext, checkResultFunction, callback);
            return startRestAsync(call, restObserver);
        }

        MZLog.e(TAG, "param is null,please check");
        return null;
    }

    /**
     * 开始异步http请求
     *
     * @param call                请求体
     * @param checkResultFunction 比较返回码的function
     * @param callback            带http头的应用层回调
     * @return Subscriber 订阅者
     */
    protected <T, R> Disposable startRestAsync(final Observable<Response<T>> call,
                                               final Function<Response<T>, Publisher<MZRestResult<R>>> checkResultFunction,
                                               final MZRestHeaderICallback<R> callback) {
        if (null != call && null != checkResultFunction && null != callback) {
            MZRestHeaderObserver<T, R> restObserver = new MZRestHeaderObserver<>(mContext, checkResultFunction, callback);
            return startRestAsync(call, restObserver);
        }
        MZLog.e(TAG, "param is null,please check");
        return null;
    }

    private <T, R> Disposable startRestAsync(final Observable<T> call,
                                             MZRestObserverBase<T, R> observer) {
        return call.subscribeOn(Schedulers.io()).unsubscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread()).subscribe(observer.getOnNext(),
                        observer.getOnError(), observer.getOnComplete());
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
        MZRestBatchObserver observer = new MZRestBatchObserver(mContext, callback);
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
                            if (null != result && result.isSuccess()) {
                                callback.onRestSuccessIO(result.getData());
                            }
                        }
                    })
                    .unsubscribeOn(AndroidSchedulers.mainThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(observer.getOnNext(), observer.getOnError());
        } else {
            MZLog.e(TAG, "param is null,please check");
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
        MZRestBatchObserver observer = new MZRestBatchObserver(mContext, callback);
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
                            if (null != result && result.isSuccess()) {
                                callback.onRestSuccessIO(result.getData());
                            }
                        }
                    })
                    .unsubscribeOn(AndroidSchedulers.mainThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(observer.getOnNext(), observer.getOnError());
        } else {
            MZLog.e(TAG, "param is null,please check");
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
        MZRestBatchObserver observer = new MZRestBatchObserver(mContext, callback);
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
                            if (null != result && result.isSuccess()) {
                                callback.onRestSuccessIO(result.getData());
                            }
                        }
                    })
                    .unsubscribeOn(AndroidSchedulers.mainThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(observer.getOnNext(), observer.getOnError());
        } else {
            MZLog.e(TAG, "param is null,please check");
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
        MZRestBatchObserver observer = new MZRestBatchObserver(mContext, callback);
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
                            if (null != result && result.isSuccess()) {
                                callback.onRestSuccessIO(result.getData());
                            }
                        }
                    })
                    .unsubscribeOn(AndroidSchedulers.mainThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(observer.getOnNext(), observer.getOnError());
        } else {
            MZLog.e(TAG, "param is null,please check");
        }
        return disposable;
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
    }

    /**
     * 设置 HTTP 连接超时时间
     *
     * @param httpConnectTimeoutSecond 连接超时时间 (单位:秒)
     */
    public void setHttpConntectTimeoutSecond(int httpConnectTimeoutSecond) {
        mHttpConnectTimeoutSecond = httpConnectTimeoutSecond;
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

    /**
     * 设置自定义Header
     *
     * @return 自定义Header map
     */
    public abstract Map<String, String> getHeaders();

    /**
     * 设置 log最大长度
     *
     * @param length log最大长度
     */
    public void setMaxLogLength(int length) {
        if (length >= 0) {
            mMaxLogLength = length;
        }
    }

    /**
     * 设置cookie是否可用
     *
     * @param enabled true表示可用
     */
    public void setCookieEnabled(boolean enabled) {
        mIsCookieEnabled = enabled;
    }

    /**
     * 获取cookies
     *
     * @param url 需要获取cookies的url
     * @return cookies
     */
    public String getCookies(String url) {
        String cookieStr = "";

        if (url == null) {
            return null;
        }

        HttpUrl httpUrl = HttpUrl.parse(url).newBuilder().build();
        List<Cookie> cookies = mCookieJar.loadForRequest(httpUrl);

        for (int i = 0; i < cookies.size(); i++) {
            Cookie cookie = cookies.get(i);

            cookieStr += cookie.name() + "=" + cookie.value();
            if (i != cookies.size() - 1) {
                cookieStr += "; ";
            }
        }

        return cookieStr;
    }
}
