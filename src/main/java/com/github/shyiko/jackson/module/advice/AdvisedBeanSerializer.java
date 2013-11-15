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
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.BeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerBuilder;
import com.fasterxml.jackson.databind.ser.impl.ObjectIdWriter;
import com.fasterxml.jackson.databind.ser.std.BeanSerializerBase;
import com.fasterxml.jackson.databind.util.NameTransformer;

import java.io.IOException;

/**
 * @author <a href="mailto:stanley.shyiko@gmail.com">Stanley Shyiko</a>
 */
@SuppressWarnings("unchecked")
public class AdvisedBeanSerializer extends BeanSerializerBase {

    private final BeanSerializerAdvice beanSerializerAdvice;
    private final boolean unwrappingSerializer;

    public AdvisedBeanSerializer(JavaType type, BeanSerializerBuilder builder, BeanPropertyWriter[] properties,
             BeanPropertyWriter[] filteredProperties, BeanSerializerAdvice beanSerializerAdvice) {
        super(type, builder, properties, filteredProperties);
        this.beanSerializerAdvice = beanSerializerAdvice;
        this.unwrappingSerializer = false;
    }

    protected AdvisedBeanSerializer(AdvisedBeanSerializer src, String[] toIgnore) {
        super(src, toIgnore);
        this.beanSerializerAdvice = src.beanSerializerAdvice;
        this.unwrappingSerializer = src.unwrappingSerializer;
    }

    protected AdvisedBeanSerializer(AdvisedBeanSerializer src, NameTransformer nameTransformer,
            boolean unwrappingSerializer) {
        super(src, nameTransformer);
        this.beanSerializerAdvice = src.beanSerializerAdvice;
        this.unwrappingSerializer = unwrappingSerializer;
    }

    @Override
    public JsonSerializer<Object> unwrappingSerializer(NameTransformer nameTransformer) {
        return new AdvisedBeanSerializer(this, nameTransformer, true);
    }

    @Override
    public boolean isUnwrappingSerializer() {
        return unwrappingSerializer;
    }

    @Override
    public BeanSerializerBase withObjectIdWriter(ObjectIdWriter objectIdWriter) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected BeanSerializerBase withIgnorals(String[] toIgnore) {
        return new AdvisedBeanSerializer(this, toIgnore);
    }

    @Override
    protected BeanSerializerBase asArraySerializer() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void serialize(Object bean, JsonGenerator jgen, SerializerProvider provider)
            throws IOException {
        if (!unwrappingSerializer) {
            jgen.writeStartObject();
        }
        beanSerializerAdvice.before(bean, jgen, provider);
        if (_propertyFilterId != null) {
            serializeFieldsFiltered(bean, jgen, provider);
        } else {
            serializeFields(bean, jgen, provider);
        }
        beanSerializerAdvice.after(bean, jgen, provider);
        if (!unwrappingSerializer) {
            jgen.writeEndObject();
        }
    }

    protected void serializeFieldsFiltered(Object bean, JsonGenerator jgen, SerializerProvider provider)
            throws IOException {
        /* note: almost verbatim copy of "serializeFields"; copied (instead of merged)
         * so that old method need not add check for existence of filter.
         */

        final BeanPropertyWriter[] props;
        if (_filteredProps != null && provider.getSerializationView() != null) {
            props = _filteredProps;
        } else {
            props = _props;
        }
        final BeanPropertyFilter filter = findFilter(provider);
        // better also allow missing filter actually..
        if (filter == null) {
            serializeFields(bean, jgen, provider);
            return;
        }

        int i = 0;
        try {
            for (final int len = props.length; i < len; ++i) {
                BeanPropertyWriter prop = props[i];
                if (prop != null) { // can have nulls in filtered list
                    beanSerializerAdvice.before(bean, jgen, prop, provider);
                    filter.serializeAsField(bean, jgen, provider, prop);
                    beanSerializerAdvice.after(bean, jgen, prop, provider);
                }
            }
            if (_anyGetterWriter != null) {
                _anyGetterWriter.getAndSerialize(bean, jgen, provider);
            }
        } catch (Exception e) {
            String name = (i == props.length) ? "[anySetter]" : props[i].getName();
            wrapAndThrow(provider, e, bean, name);
        } catch (StackOverflowError e) {
            JsonMappingException mapE = new JsonMappingException("Infinite recursion (StackOverflowError)", e);
            String name = (i == props.length) ? "[anySetter]" : props[i].getName();
            mapE.prependPath(new JsonMappingException.Reference(bean, name));
            throw mapE;
        }
    }

    protected void serializeFields(Object bean, JsonGenerator jgen, SerializerProvider provider)
            throws IOException {
        final BeanPropertyWriter[] props;
        if (_filteredProps != null && provider.getSerializationView() != null) {
            props = _filteredProps;
        } else {
            props = _props;
        }
        int i = 0;
        try {
            for (final int len = props.length; i < len; ++i) {
                BeanPropertyWriter prop = props[i];
                if (prop != null) { // can have nulls in filtered list
                    beanSerializerAdvice.before(bean, jgen, prop, provider);
                    prop.serializeAsField(bean, jgen, provider);
                    beanSerializerAdvice.after(bean, jgen, prop, provider);
                }
            }
            if (_anyGetterWriter != null) {
                _anyGetterWriter.getAndSerialize(bean, jgen, provider);
            }
        } catch (Exception e) {
            String name = (i == props.length) ? "[anySetter]" : props[i].getName();
            wrapAndThrow(provider, e, bean, name);
        } catch (StackOverflowError e) {
            /* 04-Sep-2009, tatu: Dealing with this is tricky, since we do not
             *   have many stack frames to spare... just one or two; can't
             *   make many calls.
             */
            JsonMappingException mapE = new JsonMappingException("Infinite recursion (StackOverflowError)", e);
            String name = (i == props.length) ? "[anySetter]" : props[i].getName();
            mapE.prependPath(new JsonMappingException.Reference(bean, name));
            throw mapE;
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " for " + handledType().getName();
    }

}
