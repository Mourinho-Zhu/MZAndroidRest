package com.mz.android.rest;

import android.util.Log;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.functions.Function;

/**
 * Rest 重试类
 * Created by yuanchangzhu on 18-6-25.
 */

public class MZRestRetry implements Function<Observable<? extends Throwable>, Observable<?>> {
    protected String TAG = getClass().getSimpleName();
    private int maxRetries = 0;//最大出错重试次数
    private int retryDelayMillis = 1000;//重试间隔时间
    protected int retryCount = 0;//当前出错重试次数

    public MZRestRetry() {
    }

    public MZRestRetry(int maxRetries, int retryDelayMillis) {
        this.maxRetries = maxRetries;
        this.retryDelayMillis = retryDelayMillis;
    }

    @Override
    public Observable<?> apply(Observable<? extends Throwable> observable) throws Exception {
        return observable
                .flatMap(new Function<Throwable, ObservableSource<?>>() {
                    @Override
                    public ObservableSource<?> apply(Throwable throwable) throws Exception {
                        if (++retryCount <= maxRetries) {
                            Log.i(TAG, "get error, it will try after " + retryDelayMillis * retryCount
                                    + " millisecond, retry count " + retryCount);
                            // When this Observable calls onNext, the original Observable will be retried (i.e. re-subscribed).
                            return Observable.timer(retryDelayMillis * retryCount,
                                    TimeUnit.MILLISECONDS);
                        }
                        return Observable.error(throwable);
                    }
                });
    }

}
