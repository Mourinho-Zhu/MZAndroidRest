package com.mz.android.rest;

/**
 * Rest应用层回调接口
 *
 * @author Mourinho.Zhu on 16-11-24.
 */
public interface MZRestICallback<T> extends MZRestICallbackBase<T> {
    /**
     * 获取服务器响应数据成功UI回调，用于显示UI
     *
     * @param response 数据实体类对象
     */
    void onRestSuccessUI(T response);

    /**
     * 获取服务器响应数据成功IO回调,用于写数据库等IO操作
     *
     * @param response 数据实体类对象
     */
    void onRestSuccessIO(T response);

    /**
     * 获取服务器响应数据成功但返回错误的状态码回调，如登录时用户密码不正确
     *
     * @param code    错误码
     * @param message 错误信息
     */
    void onRestFail(int code, String message);
}
