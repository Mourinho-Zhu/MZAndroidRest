package com.mz.android.rest;

/**
 * Rest应用层回调接口
 *
 * @author Mourinho.Zhu on 16-11-24.
 */
public interface MZRestICallback<T> {
    /**
     * Rest出错回调
     *
     * @param code Rest错误码，参见MZRestStatusCode类
     */
    void onRestError(int code);

    /**
     * 获取服务器响应数据成功但返回错误的状态码回调，如登录时用户密码不正确
     *
     * @param code    错误码
     * @param message 错误信息
     */
    void onRestFail(int code, String message);

    /**
     * 获取服务器响应数据成功回调
     *
     * @param response 数据实体类对象
     */
    void onRestSuccess(T response);
}
