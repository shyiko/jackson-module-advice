/*
 * Copyright 2013 Stanley Shyiko
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.shyiko.jackson.module.advice;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

/**
 * @param <T> type of the bean this advice can be applied to
 * @author <a href="mailto:stanley.shyiko@gmail.com">Stanley Shyiko</a>
 */
public interface BeanSerializerAdvice<T> {

    void before(T bean, JsonGenerator json, SerializerProvider provider) throws IOException;

    void before(T bean, JsonGenerator json, BeanProperty property, SerializerProvider provider) throws IOException;

    /**
     * @param bean bean instance
     * @param json json generator
     * @param provider serializer provider
     * @return true if instance serialization has been taken care of (neither of
     * {@link #before(Object, com.fasterxml.jackson.core.JsonGenerator,
     * com.fasterxml.jackson.databind.SerializerProvider)}, standard processing,
     * {@link #after(Object, com.fasterxml.jackson.core.JsonGenerator,
     * com.fasterxml.jackson.databind.SerializerProvider)} will be called in this case), false otherwise
     * @throws IOException if anything goes wrong during json generation
     */
    boolean intercept(T bean, JsonGenerator json, SerializerProvider provider) throws IOException;

    /**
     * @param bean bean instance
     * @param json json generator
     * @param property bean property
     * @param provider serializer provider
     * @return true if property serialization has been taken care of (neither of
     * {@link #before(Object, com.fasterxml.jackson.core.JsonGenerator, com.fasterxml.jackson.databind.BeanProperty,
     * com.fasterxml.jackson.databind.SerializerProvider)}, standard processing,
     * {@link #after(Object, com.fasterxml.jackson.core.JsonGenerator, com.fasterxml.jackson.databind.BeanProperty,
     * com.fasterxml.jackson.databind.SerializerProvider)} will be called in this case), false otherwise
     * @throws IOException if anything goes wrong during json generation
     */
    boolean intercept(T bean, JsonGenerator json, BeanProperty property, SerializerProvider provider)
            throws IOException;

    void after(T bean, JsonGenerator json, BeanProperty property, SerializerProvider provider) throws IOException;

    void after(T bean, JsonGenerator json, SerializerProvider provider) throws IOException;

}
