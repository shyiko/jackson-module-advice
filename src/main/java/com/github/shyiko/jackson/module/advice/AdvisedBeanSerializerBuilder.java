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

import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerBuilder;

/**
 * @author <a href="mailto:stanley.shyiko@gmail.com">Stanley Shyiko</a>
 */
public class AdvisedBeanSerializerBuilder extends BeanSerializerBuilder {

    private final static BeanPropertyWriter[] EMPTY_PROPERTY_LIST = new BeanPropertyWriter[0];
    private BeanSerializerAdvice beanSerializerAdvice;

    public AdvisedBeanSerializerBuilder(BeanSerializerBuilder src,
            Class<? extends BeanSerializerAdvice> beanSerializerAdvice) {
        super(src);
        try {
            this.beanSerializerAdvice = beanSerializerAdvice.newInstance();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create instance of " + beanSerializerAdvice + ".", e);
        }
    }

    @Override
    public JsonSerializer<?> build() {
        BeanPropertyWriter[] properties;
        if (_properties == null || _properties.isEmpty()) {
            if (_anyGetter == null) {
                return null;
            }
            properties = EMPTY_PROPERTY_LIST;
        } else {
            properties = _properties.toArray(new BeanPropertyWriter[_properties.size()]);
        }
        return new AdvisedBeanSerializer(_beanDesc.getType(), this, properties, _filteredProperties,
                beanSerializerAdvice);
    }

}