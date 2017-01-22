package com.mz.android.rest;

/**
 * Rest解析结果类
 *
 * @author Mourinho.Zhu on 16-11-24.
 */
public class MZRestResult<R> {
    //服务器返回结果是否成功
    private boolean success;
    //服务器返回码
    private int code;
    //服务器返回信息
    private String message;
    //需要传递给调用方的数据对象
    private R data;

    public MZRestResult(boolean success, int code, String message, R data) {
        this.success = success;
        this.code = code;
        this.message = message;
        this.data = data;
    }

    boolean isSuccess() {
        return success;
    }

    int getCode() {
        return code;
    }

    String getMessage() {
        return message;
    }

    R getData() {
        return data;
    }
}
