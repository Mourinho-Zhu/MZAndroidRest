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

import com.mz.android.util.log.MZLog;

import org.simpleframework.xml.Serializer;

import java.io.IOException;

import okhttp3.ResponseBody;
import retrofit2.Converter;

/**
 * XML响应转化类
 *
 * @author Mourinho.Zhu on 16-11-24.
 */
final class MZRestXmlResponseBodyConverter<T> implements
        Converter<ResponseBody, T> {
    private final String TAG = "RestXmlResponseBodyConverter";
    private final Class<T> cls;
    private final Serializer serializer;
    private final boolean strict;

    MZRestXmlResponseBodyConverter(Class<T> cls, Serializer serializer,
                                   boolean strict) {
        this.cls = cls;
        this.serializer = serializer;
        this.strict = strict;
    }

    @Override
    public T convert(ResponseBody value) throws IOException {
        T read = null;
        try {
            String xml = value.string();
            MZLog.d(TAG, "response xml --> " + xml);
            read = serializer.read(cls, xml, strict);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            value.close();
        }
        return read;
    }
}
