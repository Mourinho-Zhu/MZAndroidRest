package com.mz.android.rest.callback;

import okhttp3.Headers;

/**
 * Rest应用层回调接口
 *
 * @author Mourinho.Zhu on 16-11-24.
 */
public abstract class MZRestHeaderICallback<T> extends MZRestICallbackBase<T> {
    /**
     * 获取服务器响应数据成功UI回调，用于显示UI
     *
     * @param response 数据实体类对象
     * @param headers  http header
     */
    public abstract void onRestSuccessUI(Headers headers, T response);

    /**
     * 获取服务器响应数据成功IO回调,用于写数据库等IO操作
     *
     * @param response 数据实体类对象
     * @param headers  http header
     */
    public abstract void onRestSuccessIO(Headers headers, T response);
}
