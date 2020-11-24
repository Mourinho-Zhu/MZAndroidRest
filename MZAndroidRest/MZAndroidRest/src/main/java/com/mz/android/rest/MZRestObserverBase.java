package com.mz.android.rest;

import android.content.Context;
import android.util.Log;

import com.jakewharton.retrofit2.adapter.rxjava2.HttpException;
import com.mz.android.rest.callback.MZRestICallbackBase;

import org.reactivestreams.Publisher;

import java.net.SocketTimeoutException;

import io.reactivex.Flowable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

/**
 * Rest observer类
 * Created by yuanchangzhu on 18-6-25.
 */

abstract class MZRestObserverBase<T, R> {
    protected String TAG = getClass().getSimpleName();
    protected Context context;
    protected boolean hasContent = false;
    protected MZRestICallbackBase<R> callback;
    protected Function<T, Publisher<MZRestResult<R>>> checkResultFuntion;

    MZRestObserverBase(Context context,
                       Function<T, Publisher<MZRestResult<R>>> checkResultFunction,
                       MZRestICallbackBase<R> callback) {
        this.context = context;
        this.checkResultFuntion = checkResultFunction;
        this.callback = callback;
    }

    MZRestObserverBase(Context context,
                       MZRestICallbackBase<R> callback) {
        this(context, null, callback);
    }

    private Consumer<? super T> onNext = new Consumer<T>() {
        @Override
        public void accept(final @NonNull T t) throws Exception {
            hasContent = true;
            Flowable<T> data = Flowable.just(t);
            data.subscribeOn(Schedulers.io())
                    .flatMap(checkResultFuntion)
                    .observeOn(Schedulers.io())
                    .doOnNext(new Consumer<MZRestResult<R>>() {
                        @Override
                        public void accept(MZRestResult<R> result) throws Exception {
                            if (null != result && result.isSuccess()) {
                                onSuccessIO(t, result.getData());
                            }
                        }
                    })
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Consumer<MZRestResult<R>>() {
                        @Override
                        public void accept(MZRestResult<R> result) throws Exception {
                            if (null != result && result.isSuccess()) {
                                Log.d(TAG, "parse success");
                                onSuccessUI(t, result.getData());
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

    private Consumer<? super Throwable> onError = new Consumer<Throwable>() {
        @Override
        public void accept(@NonNull Throwable t) throws Exception {
            hasContent = true;
            Log.e(TAG, "onError");
            handlerHttpError(t, callback);
        }
    };
    private Action onComplete = new Action() {
        @Override
        public void run() throws Exception {
            Log.d(TAG, "onComplete hasContent --> " + hasContent);
            //不处理
            if (!hasContent) {
                onSuccessUI(null, null);
            }
        }
    };

    //处理http请求失败事件
    protected void handlerHttpError(Throwable t, MZRestICallbackBase callback) {
        if (!MZNetwork.isNetworkAvailable(context)) {
            Log.w(TAG, "network is not available");
            callback.onRestError(MZRestStatusCode.ERROR_NETWORK_UNAVAILABLE);
        } else if (t instanceof SocketTimeoutException) {
            callback.onRestError(MZRestStatusCode.ERROR_REQUEST_TIMEOUT);
        } else if (t instanceof HttpException) {
            HttpException httpException = (HttpException) t;
            httpException.printStackTrace();

            try {
                if (httpException.code() == 202) {
                    callback.onRestError(MZRestStatusCode.ERROR_ONLY_ACCEPT);
                } else {
                    String errorBody = httpException.response().errorBody().string();
                    Log.e(TAG, "httpException response --> " + errorBody);
                    callback.onRestError(MZRestStatusCode.ERROR_HTTP, httpException.response().code(), errorBody);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        } else {
            callback.onRestError(MZRestStatusCode.ERROR_OTHER);
        }
        if (null != t) {
            t.printStackTrace();
        }
    }

    abstract void onSuccessUI(T t, R result);

    abstract void onSuccessIO(T t, R result);

    protected MZRestICallbackBase<R> getCallback() {
        return callback;
    }

    protected Consumer<? super T> getOnNext() {
        return onNext;
    }

    protected Consumer<? super Throwable> getOnError() {
        return onError;
    }

    protected Action getOnComplete() {
        return onComplete;
    }
}
