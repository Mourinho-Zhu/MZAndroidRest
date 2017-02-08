package com.mz.android.rest;

/**
 * Rest应用层回调接口
 *
 * @author Mourinho.Zhu on 16-11-24.
 */
interface MZRestICallbackBase<T>{
    /**
     * Rest出错回调
     *
     * @param code Rest错误码，参见MZRestStatusCode类
     */
    void onRestError(int code);
}
