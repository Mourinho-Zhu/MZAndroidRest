package com.mz.android.rest;

import android.text.TextUtils;

/**
 * Rest 请求状态码
 *
 * @author Mourinho.Zhu on 16-11-24.
 */
public enum MZRestStatusCode {
    /**
     * 成功
     */
    SUCCESS("0",""),
    /**
     * 网络不可用
     */
    ERROR_NETWORK_UNAVAILABLE("90001","网络不可用，请检查网络连接状态"),

    /**
     * 请求超时
     */
    ERROR_REQUEST_TIMEOUT("90002","请求数据超时"),

    /**
     * 解析返回数据出错
     */
    ERROR_PARSE_RESPONSE("90003","解析服务器数据出错"),

    /**
     * HTTP错误
     */
    ERROR_HTTP("90004","Http请求出错"),

    /**
     * token错误
     */
    ERROR_TOKEN("90005","请求token为空或者不正确"),

    /**
     * 已经接受此次请求，但操作未完成
     */
    ERROR_ONLY_ACCEPT("90006","已经接受此次请求，但操作未完成"),

    /**
     * 其他错误
     */
    ERROR_OTHER("99999","其他未知错误");

    private String errorCode;
    private String errorMessage;

    MZRestStatusCode(String errorCode,String errorMessage) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public static MZRestStatusCode getMZRestStatusCode(String errorCode) {
        MZRestStatusCode statusCode = null;
        for(MZRestStatusCode code : MZRestStatusCode.values()) {
            if(TextUtils.equals(code.errorCode,errorCode)) {
                statusCode = code;
                break;
            }
        }
        return statusCode;
    }
}
