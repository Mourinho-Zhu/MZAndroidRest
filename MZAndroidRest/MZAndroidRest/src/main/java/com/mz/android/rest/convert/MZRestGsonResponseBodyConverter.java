/*
 * Copyright (C) 2015 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mz.android.rest.convert;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;

import java.io.IOException;

import okhttp3.ResponseBody;
import retrofit2.Converter;

/**
 * XML响应转化类
 *
 * @author Mourinho.Zhu on 16-11-24.
 */
public class MZRestGsonResponseBodyConverter<T> implements
        Converter<ResponseBody, T> {
    private final String TAG = "RestGsonResponseBodyConverter";
    private final TypeAdapter<T> adapter;

    protected MZRestGsonResponseBodyConverter(TypeAdapter<T> adapter) {
        this.adapter = adapter;
    }

    @Override
    public T convert(ResponseBody value) throws IOException {
        try {
            String json = value.string();
            Log.d(TAG, "response json --> " + json);
            T object = null;
            try {
                object = adapter.fromJson(json);
            } catch (Exception e) {
                Log.e(TAG, "handle exception");
                object = handleJsonSyntaxException(object, json);
            }
            return object;
        } finally {
            value.close();
        }
    }

    protected T handleJsonSyntaxException(T object, String json)
            throws IOException {
        try {
            Gson gson = new Gson();
            json = gson.toJson(json);
            Log.e(TAG, "new json --> " + json);
            object = adapter.fromJson(json);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return object;
    }

    protected TypeAdapter<T> getAdapter() {
        return adapter;
    }

}
