package com.mz.android.rest;

/**
 * Rest 请求状态码
 *
 * @author Mourinho.Zhu on 16-11-24.
 */
public enum MZRestStatusCode {
    /**
     * 网络不可用
     */
    ERROR_NETWORK_UNAVAILABLE,

    /**
     * 请求超时
     */
    ERROR_REQUEST_TIMEOUT,

    /**
     * 解析返回数据出错
     */
    ERROR_PARSE_RESPONSE,

    /**
     * 网络错误
     */
    ERROR_NETWORK,

    /**
     * token错误
     */
    ERROR_TOKEN,

    /**
     * 其他错误
     */
    ERROR_OTHER,

}
