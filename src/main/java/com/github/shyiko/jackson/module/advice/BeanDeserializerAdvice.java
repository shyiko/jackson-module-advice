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
 * @param <T> type of the bean this advice can be applied to
 * @author <a href="mailto:stanley.shyiko@gmail.com">Stanley Shyiko</a>
 */
public interface BeanDeserializerAdvice<T> {

    void before(T bean, JsonParser json, DeserializationContext context) throws IOException;

    /**
     * @param bean bean instance
     * @param propertyName bean property name
     * @param json json parser
     * @param context deserialization context
     * @return true if property deserialization has been taken care of and thus standard processing should not be run,
     * false otherwise
     * @throws IOException if anything goes wrong during json processing
     */
    boolean intercept(T bean, String propertyName, JsonParser json, DeserializationContext context) throws IOException;

    void after(T bean, JsonParser json, DeserializationContext context) throws IOException;
}
