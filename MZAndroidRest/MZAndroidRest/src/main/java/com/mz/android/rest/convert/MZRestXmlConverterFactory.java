/*
 * Copyright (C) 2013 Square, Inc.
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

import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Converter;
import retrofit2.Retrofit;

/**
 * A {@linkplain Converter.Factory converter} which uses Simple Framework for
 * XML.
 * <p>
 * This converter only applies for class types. Parameterized types (e.g.,
 * {@code List<Foo>}) are not handled.
 */
public final class MZRestXmlConverterFactory extends Converter.Factory {
    /**
     * Create an instance using a default {@link Persister} instance for
     * conversion.
     */
    public static MZRestXmlConverterFactory create() {
        return create(new Persister());
    }

    /** Create an instance using {@code serializer} for conversion. */
    public static MZRestXmlConverterFactory create(Serializer serializer) {
        return new MZRestXmlConverterFactory(serializer, true);
    }

    /**
     * Create an instance using a default {@link Persister} instance for
     * non-strict conversion.
     */
    public static MZRestXmlConverterFactory createNonStrict() {
        return createNonStrict(new Persister());
    }

    /** Create an instance using {@code serializer} for non-strict conversion. */
    public static MZRestXmlConverterFactory createNonStrict(Serializer serializer) {
        return new MZRestXmlConverterFactory(serializer, false);
    }

    private final Serializer serializer;
    private final boolean strict;

    private MZRestXmlConverterFactory(Serializer serializer, boolean strict) {
        if (serializer == null)
            throw new NullPointerException("serializer == null");
        this.serializer = serializer;
        this.strict = strict;
    }

    public boolean isStrict() {
        return strict;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public Converter<ResponseBody, ?> responseBodyConverter(Type type,
            Annotation[] annotations, Retrofit retrofit) {
        if (!(type instanceof Class)) {
            return null;
        }
        Class<?> cls = (Class<?>) type;
        return new MZRestXmlResponseBodyConverter(cls, serializer, strict);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public Converter<?, RequestBody> requestBodyConverter(Type type,
            Annotation[] parameterAnnotations, Annotation[] methodAnnotations,
            Retrofit retrofit) {
        if (!(type instanceof Class)) {
            return null;
        }
        return new MZRestXmlRequestBodyConverter(serializer);
    }
}
