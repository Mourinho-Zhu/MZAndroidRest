package com.mz.android.rest;

import java.util.List;

/**
 * Rest应用层回调接口
 *
 * @author Mourinho.Zhu on 16-11-24.
 */
public interface MZRestBatchICallback<T> extends MZRestICallbackBase<T> {
    /**
     * 获取服务器响应数据成功UI回调，用于显示UI
     *
     * @param responseList 数据实体类对象列表
     */
    void onRestSuccessUI(List<T> responseList);

    /**
     * 获取服务器响应数据成功IO回调,用于写数据库等IO操作
     *
     * @param responseList 数据实体类对象列表
     */
    void onRestSuccessIO(List<T> responseList);

    /**
     * 获取服务器响应数据成功但返回错误的状态码回调，如登录时用户密码不正确
     *
     * @param errorResultList 错误信息列表
     */
    void onRestFail(List<MZRestResult<T>> errorResultList);
}
