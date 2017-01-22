package com.mz.android.rest;

/**
 * Rest 请求状态码
 *
 * @author Mourinho.Zhu on 16-11-24.
 */
public class MZRestStatusCode {
    /**
     * 网络不可用
     */
    public static final int ERROR_NETWORK_UNAVAILABLE = 0x01;


    /**
     * 请求超时
     */
    public static final int ERROR_REQUEST_TIMEOUT = 0x02;

    /**
     * 解析返回数据出错
     */
    public static final int ERROR_PARSE_RESPONSE = 0x03;

    /**
     * 网络错误
     */
    public static final int ERROR_NETWORK = 0x04;
}
