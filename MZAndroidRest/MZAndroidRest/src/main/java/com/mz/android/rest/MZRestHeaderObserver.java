package com.mz.android.rest;

import android.content.Context;

import com.mz.android.rest.callback.MZRestHeaderICallback;

import org.reactivestreams.Publisher;

import io.reactivex.functions.Function;
import retrofit2.Response;

/**
 * Rest observer类
 * Created by yuanchangzhu on 18-6-25.
 */

class MZRestHeaderObserver<T, R> extends MZRestObserverBase<Response<T>, R> {
    private String TAG = getClass().getSimpleName();

    public MZRestHeaderObserver(Context context,
                                Function<Response<T>, Publisher<MZRestResult<R>>> checkResultFunction,
                                MZRestHeaderICallback<R> callback) {
        super(context, checkResultFunction, callback);
    }

    @Override
    void onSuccessUI(Response<T> t, R result) {
        getCallback().onRestSuccessUI(t.headers(), result);
    }

    @Override
    void onSuccessIO(Response<T> t, R result) {
        getCallback().onRestSuccessIO(t.headers(), result);
    }

    @Override
    protected MZRestHeaderICallback<R> getCallback() {
        return (MZRestHeaderICallback) super.getCallback();
    }
}
