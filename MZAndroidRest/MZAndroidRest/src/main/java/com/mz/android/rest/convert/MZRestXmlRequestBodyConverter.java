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

import org.simpleframework.xml.Serializer;

import java.io.IOException;
import java.io.OutputStreamWriter;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.Buffer;
import okio.ByteString;
import retrofit2.Converter;

/**
 * XML请求转化类
 *
 * @author Mourinho.Zhu on 16-11-24.
 */
final class MZRestXmlRequestBodyConverter<T> implements Converter<T, RequestBody> {
    private final String TAG = "RestXmlRequestBodyConverter";
    private static final MediaType MEDIA_TYPE = MediaType
            .parse("application/xml; charset=UTF-8");
    private static final String CHARSET = "UTF-8";

    private final Serializer serializer;

    MZRestXmlRequestBodyConverter(Serializer serializer) {
        this.serializer = serializer;
    }

    @Override
    public RequestBody convert(T value) throws IOException {
        Buffer buffer = new Buffer();
        ByteString body = null;
        try {
            OutputStreamWriter osw = new OutputStreamWriter(
                    buffer.outputStream(), CHARSET);
            serializer.write(value, osw);
            osw.flush();
            body = buffer.readByteString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            buffer.close();
        }
        return RequestBody.create(MEDIA_TYPE, body);
    }
}
