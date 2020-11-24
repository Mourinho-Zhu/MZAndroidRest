package com.mz.android.rest.callback;

import com.mz.android.rest.MZRestStatusCode;

/**
 * Rest应用层回调接口
 *
 * @author Mourinho.Zhu on 16-11-24.
 */
public abstract class MZRestICallbackBase<T> {
    /**
     * Rest出错回调
     *
     * @param code Rest错误码，参见MZRestStatusCode类
     */
    public abstract void onRestError(MZRestStatusCode code);

    /**
     * Rest出错回调
     *
     * @param code          Rest错误码，参见MZRestStatusCode类
     * @param httpException http异常
     */
    @Deprecated
    public void onRestError(MZRestStatusCode code, String httpException) {
        onRestError(code);
    }

    /**
     * Rest出错回调
     *
     * @param statusCode    Rest错误码，参见MZRestStatusCode类
     * @param httpException http异常
     */
    public void onRestError(MZRestStatusCode statusCode, int httpCode, String httpException) {
        onRestError(statusCode);
    }

    /**
     * Rest出错回调
     *
     * @param code          Rest错误码，参见MZRestStatusCode类
     * @param httpException http异常
     */

    /**
     * 获取服务器响应数据成功但返回错误的状态码回调，如登录时用户密码不正确
     *
     * @param code    错误码
     * @param message 错误信息
     */
    public abstract void onRestFail(String code, String message);
}
