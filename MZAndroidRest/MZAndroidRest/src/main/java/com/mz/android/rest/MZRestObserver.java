package com.mz.android.rest;

import android.content.Context;

import com.mz.android.rest.callback.MZRestICallback;

import org.reactivestreams.Publisher;

import io.reactivex.functions.Function;

/**
 * Rest observer类
 * Created by yuanchangzhu on 18-6-25.
 */

class MZRestObserver<T, R> extends MZRestObserverBase<T, R> {
    private String TAG = getClass().getSimpleName();

    public MZRestObserver(Context context,
                          Function<T, Publisher<MZRestResult<R>>> checkResultFunction,
                          MZRestICallback<R> callback) {
        super(context, checkResultFunction, callback);
    }

    @Override
    void onSuccessUI(T t, R result) {
        getCallback().onRestSuccessUI(result);
    }

    @Override
    void onSuccessIO(T t, R result) {
        getCallback().onRestSuccessIO(result);
    }


    @Override
    protected MZRestICallback<R> getCallback() {
        return (MZRestICallback) super.getCallback();
    }
}
