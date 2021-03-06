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
 * Default (no-op) implementation of {@link BeanSerializerAdvice}.
 * @param <T> type of the bean this advice can be applied to
 * @author <a href="mailto:stanley.shyiko@gmail.com">Stanley Shyiko</a>
 */
public abstract class AbstractBeanSerializerAdvice<T> implements BeanSerializerAdvice<T> {

    @Override
    public void before(T bean, JsonGenerator json, SerializerProvider provider) throws IOException {
    }

    @Override
    public void before(T bean, JsonGenerator json, BeanProperty property, SerializerProvider provider)
            throws IOException {
    }

    @Override
    public boolean intercept(T bean, JsonGenerator json, SerializerProvider provider) throws IOException {
        return false;
    }

    @Override
    public boolean intercept(T bean, JsonGenerator json, BeanProperty property, SerializerProvider provider)
            throws IOException {
        return false;
    }

    @Override
    public void after(T bean, JsonGenerator json, BeanProperty property, SerializerProvider provider)
            throws IOException {
    }

    @Override
    public void after(T bean, JsonGenerator json, SerializerProvider provider) throws IOException {
    }

}
