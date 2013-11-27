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

import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.PropertyMetadata;
import com.fasterxml.jackson.databind.deser.BeanDeserializerBuilder;
import com.fasterxml.jackson.databind.deser.SettableBeanProperty;
import com.fasterxml.jackson.databind.deser.impl.BeanPropertyMap;
import com.fasterxml.jackson.databind.deser.impl.ObjectIdValueProperty;

import java.util.Collection;

/**
 * @author <a href="mailto:stanley.shyiko@gmail.com">Stanley Shyiko</a>
 */
public class AdvisedBeanDeserializerBuilder extends BeanDeserializerBuilder {

    private BeanDeserializerAdvice beanDeserializerAdvice;

    public AdvisedBeanDeserializerBuilder(BeanDeserializerBuilder src,
                                          Class<? extends BeanDeserializerAdvice> beanDeserializerAdvice) {
        super(src);
        try {
            this.beanDeserializerAdvice = beanDeserializerAdvice.newInstance();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create instance of " + beanDeserializerAdvice + ".", e);
        }
    }

    @Override
    public JsonDeserializer<?> build() {
        Collection<SettableBeanProperty> props = _properties.values();
        BeanPropertyMap propertyMap = new BeanPropertyMap(props);
        propertyMap.assignIndexes();
        boolean anyViews = !_defaultViewInclusion;
        if (!anyViews) {
            for (SettableBeanProperty prop : props) {
                if (prop.hasViews()) {
                    anyViews = true;
                    break;
                }
            }
        }
        if (_objectIdReader != null) {
            ObjectIdValueProperty prop = new ObjectIdValueProperty(_objectIdReader, PropertyMetadata.STD_REQUIRED);
            propertyMap = propertyMap.withProperty(prop);
        }
        return new AdvisedBeanDeserializer(this,
                _beanDesc, propertyMap, _backRefProperties, _ignorableProps, _ignoreAllUnknown,
                anyViews, beanDeserializerAdvice);
    }

}
