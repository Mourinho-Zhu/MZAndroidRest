package com.mz.android.download;


import android.content.Context;
import android.util.Log;

import com.mz.android.rest.MZLog;
import com.mz.android.rest.MZRestApi;
import com.mz.android.rest.MZRestResult;
import com.mz.android.rest.MZRestStatusCode;
import com.mz.android.rest.callback.MZRestICallback;
import com.mz.android.rest.util.MZHttpUtils;

import org.reactivestreams.Publisher;

import java.util.Map;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import okhttp3.ResponseBody;

public class MZDownloadApi extends MZRestApi<MZDownloadService> {

    public static final String TAG = "MZDownloadApi";

    //单例
    private static MZDownloadApi mApi;

    protected MZDownloadApi(Context context, Class<MZDownloadService> serviceClass) {
        //随便写个base url以防报错
        super(context, "https://www.baidu.com", serviceClass);
    }


    /**
     * 获取单例
     *
     * @param context 上下文
     * @return
     */
    public static MZDownloadApi getInstance(Context context) {
        if (null == mApi) {
            mApi = new MZDownloadApi(context, MZDownloadService.class);
            if (!mApi.init()) {
                mApi = null;
            }
        }
        return mApi;
    }

    @Override
    public Map<String, String> getHeaders() {
        return null;
    }


    /**
     * 下载文件
     *
     * @param callback
     * @return
     */
    public Disposable downloadFile(String fileUrl, final String localFilePath, MZRestICallback<Boolean> callback) {

        return doStartRestAsync(getService().downloadFile(fileUrl), new Function<ResponseBody, Publisher<MZRestResult<Boolean>>>() {
            @Override
            public Publisher<MZRestResult<Boolean>> apply(ResponseBody responseBean) throws Exception {
                MZLog.d(TAG, "server contacted and has file");

                boolean writtenResult = MZHttpUtils.writeResponseBodyToDisk(localFilePath, responseBean);

                MZLog.d(TAG, "file download was a success  --> " + writtenResult);

                return getResult(writtenResult, MZRestStatusCode.SUCCESS.getErrorCode(),
                        MZRestStatusCode.SUCCESS.getErrorMessage(), writtenResult);
            }
        }, callback);
    }
}
