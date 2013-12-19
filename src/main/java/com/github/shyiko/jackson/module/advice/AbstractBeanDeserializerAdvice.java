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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;

import java.io.IOException;

/**
 * Default (no-op) implementation of {@link BeanDeserializerAdvice}.
 * @param <T> type of the bean this advice can be applied to
 * @author <a href="mailto:stanley.shyiko@gmail.com">Stanley Shyiko</a>
 */
public class AbstractBeanDeserializerAdvice<T> implements BeanDeserializerAdvice<T> {

    @Override
    public void before(T bean, JsonParser json, DeserializationContext context) throws IOException {
    }

    @Override
    public boolean intercept(T bean, String propertyName, JsonParser json, DeserializationContext context)
            throws IOException {
        return false;
    }

    @Override
    public void after(T bean, JsonParser json, DeserializationContext context) throws IOException {
    }

}
