package com.mz.android.rest;

import android.content.Context;
import android.util.Log;

import com.mz.android.rest.callback.MZRestICallback;

import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Consumer;

/**
 * Rest observer类
 * Created by yuanchangzhu on 18-6-25.
 */

class MZRestBatchObserver<R> extends MZRestObserverBase<MZRestResult<R>, R> {
    public MZRestBatchObserver(Context context, MZRestICallback<R> callback) {
        super(context, callback);
    }

    private Consumer<? super MZRestResult<R>> onNext = new Consumer<MZRestResult<R>>() {

        @Override
        public void accept(@NonNull MZRestResult<R> result) throws Exception {
            if (null != result && result.isSuccess()) {
                Log.d(TAG, "parse success");
                getCallback().onRestSuccessUI(result.getData());
            } else {
                Log.d(TAG, "parse success but code is fail");
                getCallback().onRestFail(result.getCode(), result.getMessage());
            }
        }
    };

    private Consumer<? super Throwable> onError = new Consumer<Throwable>() {
        @Override
        public void accept(@NonNull Throwable t) throws Exception {
            Log.e(TAG, "onError");
            handlerHttpError(t, callback);
        }
    };


    @Override
    void onSuccessUI(MZRestResult<R> result, R result2) {
        //do nothing
    }

    @Override
    void onSuccessIO(MZRestResult<R> result, R result2) {
        //do nothing
    }

    @Override
    protected MZRestICallback<R> getCallback() {
        return (MZRestICallback) super.getCallback();
    }

    @Override
    protected Consumer<? super MZRestResult<R>> getOnNext() {
        return this.onNext;
    }

    @Override
    protected Consumer<? super Throwable> getOnError() {
        return this.onError;
    }

}
