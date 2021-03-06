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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.BeanDeserializerBase;
import com.fasterxml.jackson.databind.deser.BeanDeserializerBuilder;
import com.fasterxml.jackson.databind.deser.SettableBeanProperty;
import com.fasterxml.jackson.databind.deser.impl.BeanAsArrayDeserializer;
import com.fasterxml.jackson.databind.deser.impl.BeanPropertyMap;
import com.fasterxml.jackson.databind.deser.impl.ExternalTypeHandler;
import com.fasterxml.jackson.databind.deser.impl.ObjectIdReader;
import com.fasterxml.jackson.databind.deser.impl.PropertyBasedCreator;
import com.fasterxml.jackson.databind.deser.impl.PropertyValueBuffer;
import com.fasterxml.jackson.databind.util.NameTransformer;
import com.fasterxml.jackson.databind.util.TokenBuffer;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;

/**
 * Whole class is basically a rip-off of {@link com.fasterxml.jackson.databind.deser.BeanDeserializer} with inlined
 * for-{@link BeanDeserializerAdvice} calls. Original code belongs to FasterXML, LLC (licensed under Apache License 2.0).
 * @author <a href="mailto:stanley.shyiko@gmail.com">Stanley Shyiko</a>
 */
public class AdvisedBeanDeserializer extends BeanDeserializerBase {

    private final BeanDeserializerAdvice beanDeserializerAdvice;

    protected AdvisedBeanDeserializer(BeanDeserializerBuilder builder, BeanDescription beanDesc,
            BeanPropertyMap properties, Map<String, SettableBeanProperty> backRefs, HashSet<String> ignorableProps,
            boolean ignoreAllUnknown, boolean hasViews, BeanDeserializerAdvice beanDeserializerAdvice) {
        super(builder, beanDesc, properties, backRefs, ignorableProps, ignoreAllUnknown, hasViews);
        this.beanDeserializerAdvice = beanDeserializerAdvice;
    }

    protected AdvisedBeanDeserializer(AdvisedBeanDeserializer src, NameTransformer unwrapper) {
        super(src, unwrapper);
        this.beanDeserializerAdvice = src.beanDeserializerAdvice;
    }

    protected AdvisedBeanDeserializer(AdvisedBeanDeserializer src, ObjectIdReader oir) {
        super(src, oir);
        this.beanDeserializerAdvice = src.beanDeserializerAdvice;
    }

    protected AdvisedBeanDeserializer(AdvisedBeanDeserializer src, HashSet<String> ignorableProps) {
        super(src, ignorableProps);
        this.beanDeserializerAdvice = src.beanDeserializerAdvice;
    }

    @Override
    public JsonDeserializer<Object> unwrappingDeserializer(NameTransformer unwrapper) {
        /* bit kludgy but we don't want to accidentally change type; sub-classes
         * MUST override this method to support unwrapped properties...
         */
        if (getClass() != AdvisedBeanDeserializer.class) {
            return this;
        }
        /* main thing really is to just enforce ignoring of unknown
         * properties; since there may be multiple unwrapped values
         * and properties for all may be interleaved...
         */
        return new AdvisedBeanDeserializer(this, unwrapper);
    }

    @Override
    public AdvisedBeanDeserializer withObjectIdReader(ObjectIdReader oir) {
        return new AdvisedBeanDeserializer(this, oir);
    }

    @Override
    public AdvisedBeanDeserializer withIgnorableProperties(HashSet<String> ignorableProps) {
        return new AdvisedBeanDeserializer(this, ignorableProps);
    }

    @Override
    protected BeanDeserializerBase asArrayDeserializer() {
        SettableBeanProperty[] props = _beanProperties.getPropertiesInInsertionOrder();
        return new BeanAsArrayDeserializer(this, props);
    }

    /**
     * Main deserialization method for bean-based objects (POJOs).
     * <p/>
     * NOTE: was declared 'final' in 2.2; should NOT be to let extensions
     * like Afterburner change definition.
     */
    @Override
    public Object deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException {
        JsonToken t = jp.getCurrentToken();
        // common case first:
        if (t == JsonToken.START_OBJECT) {
            if (_vanillaProcessing) {
                return vanillaDeserialize(jp, ctxt, jp.nextToken());
            }
            jp.nextToken();
            if (_objectIdReader != null) {
                return deserializeWithObjectId(jp, ctxt);
            }
            return deserializeFromObject(jp, ctxt);
        }
        return _deserializeOther(jp, ctxt, t);
    }

    protected Object _deserializeOther(JsonParser jp, DeserializationContext ctxt,
                                             JsonToken t)
            throws IOException {
        if (t == null) {
            return _missingToken(jp, ctxt);
        }
        // and then others, generally requiring use of @JsonCreator
        switch (t) {
            case VALUE_STRING:
                return deserializeFromString(jp, ctxt);
            case VALUE_NUMBER_INT:
                return deserializeFromNumber(jp, ctxt);
            case VALUE_NUMBER_FLOAT:
                return deserializeFromDouble(jp, ctxt);
            case VALUE_EMBEDDED_OBJECT:
                return jp.getEmbeddedObject();
            case VALUE_TRUE:
            case VALUE_FALSE:
                return deserializeFromBoolean(jp, ctxt);
            case START_ARRAY:
                // these only work if there's a (delegating) creator...
                return deserializeFromArray(jp, ctxt);
            case FIELD_NAME:
            case END_OBJECT: // added to resolve [JACKSON-319], possible related issues
                if (_vanillaProcessing) {
                    return vanillaDeserialize(jp, ctxt, t);
                }
                if (_objectIdReader != null) {
                    return deserializeWithObjectId(jp, ctxt);
                }
                return deserializeFromObject(jp, ctxt);
            default:
                throw ctxt.mappingException(handledType());
        }
    }

    protected Object _missingToken(JsonParser jp, DeserializationContext ctxt)
            throws JsonProcessingException {
        throw ctxt.endOfInputException(handledType());
    }

    /**
     * Secondary deserialization method, called in cases where POJO
     * instance is created as part of deserialization, potentially
     * after collecting some or all of the properties to set.
     */
    @Override
    public Object deserialize(JsonParser jp, DeserializationContext ctxt, Object bean)
            throws IOException {
        if (_injectables != null) {
            injectValues(ctxt, bean);
        }
        if (_unwrappedPropertyHandler != null) {
            return deserializeWithUnwrapped(jp, ctxt, bean);
        }
        if (_externalTypeIdHandler != null) {
            return deserializeWithExternalTypeId(jp, ctxt, bean);
        }
        JsonToken t = jp.getCurrentToken();
        // 23-Mar-2010, tatu: In some cases, we start with full JSON object too...
        if (t == JsonToken.START_OBJECT) {
            t = jp.nextToken();
        }
        if (_needViewProcesing) {
            Class<?> view = ctxt.getActiveView();
            if (view != null) {
                return deserializeWithView(jp, ctxt, bean, view);
            }
        }
        beanDeserializerAdvice.before(bean, jp, ctxt);
        for (; t == JsonToken.FIELD_NAME; t = jp.nextToken()) {
            String propName = jp.getCurrentName();
            // Skip field name:
            jp.nextToken();

            if (beanDeserializerAdvice.intercept(bean, propName, jp, ctxt)) {
                continue;
            }

            SettableBeanProperty prop = _beanProperties.find(propName);

            if (prop != null) { // normal case
                try {
                    prop.deserializeAndSet(jp, ctxt, bean);
                } catch (Exception e) {
                    wrapAndThrow(e, bean, propName, ctxt);
                }
                continue;
            }
            handleUnknownVanilla(jp, ctxt, bean, propName);
        }
        beanDeserializerAdvice.after(bean, jp, ctxt);
        return bean;
    }

    /**
     * Streamlined version that is only used when no "special"
     * features are enabled.
     */
    private Object vanillaDeserialize(JsonParser jp,
                                      DeserializationContext ctxt, JsonToken t)
            throws IOException {
        final Object bean = _valueInstantiator.createUsingDefault(ctxt);
        beanDeserializerAdvice.before(bean, jp, ctxt);
        for (; jp.getCurrentToken() != JsonToken.END_OBJECT; jp.nextToken()) {
            String propName = jp.getCurrentName();
            // Skip field name:
            jp.nextToken();

            if (beanDeserializerAdvice.intercept(bean, propName, jp, ctxt)) {
                continue;
            }

            SettableBeanProperty prop = _beanProperties.find(propName);
            if (prop != null) { // normal case
                try {
                    prop.deserializeAndSet(jp, ctxt, bean);
                } catch (Exception e) {
                    wrapAndThrow(e, bean, propName, ctxt);
                }
            } else {
                handleUnknownVanilla(jp, ctxt, bean, propName);
            }
        }
        beanDeserializerAdvice.after(bean, jp, ctxt);
        return bean;
    }

    /**
     * General version used when handling needs more advanced
     * features.
     */
    @Override
    public Object deserializeFromObject(JsonParser jp, DeserializationContext ctxt)
            throws IOException {
        if (_nonStandardCreation) {
            if (_unwrappedPropertyHandler != null) {
                return deserializeWithUnwrapped(jp, ctxt);
            }
            if (_externalTypeIdHandler != null) {
                return deserializeWithExternalTypeId(jp, ctxt);
            }
            return deserializeFromObjectUsingNonDefault(jp, ctxt);
        }
        final Object bean = _valueInstantiator.createUsingDefault(ctxt);
        if (jp.canReadObjectId()) {
            Object id = jp.getObjectId();
            if (id != null) {
                _handleTypedObjectId(jp, ctxt, bean, id);
            }
        }
        if (_injectables != null) {
            injectValues(ctxt, bean);
        }
        if (_needViewProcesing) {
            Class<?> view = ctxt.getActiveView();
            if (view != null) {
                return deserializeWithView(jp, ctxt, bean, view);
            }
        }
        beanDeserializerAdvice.before(bean, jp, ctxt);
        for (; jp.getCurrentToken() != JsonToken.END_OBJECT; jp.nextToken()) {
            String propName = jp.getCurrentName();
            // Skip field name:
            jp.nextToken();

            if (beanDeserializerAdvice.intercept(bean, propName, jp, ctxt)) {
                continue;
            }

            SettableBeanProperty prop = _beanProperties.find(propName);
            if (prop != null) { // normal case
                try {
                    prop.deserializeAndSet(jp, ctxt, bean);
                } catch (Exception e) {
                    wrapAndThrow(e, bean, propName, ctxt);
                }
                continue;
            }
            handleUnknownVanilla(jp, ctxt, bean, propName);
        }
        beanDeserializerAdvice.after(bean, jp, ctxt);
        return bean;
    }

    /**
     * Method called to deserialize bean using "property-based creator":
     * this means that a non-default constructor or factory method is
     * called, and then possibly other setters. The trick is that
     * values for creator method need to be buffered, first; and
     * due to non-guaranteed ordering possibly some other properties
     * as well.
     */
    @Override
    @SuppressWarnings("resource")
    protected Object _deserializeUsingPropertyBased(final JsonParser jp, final DeserializationContext ctxt)
            throws IOException {
        final PropertyBasedCreator creator = _propertyBasedCreator;
        PropertyValueBuffer buffer = creator.startBuilding(jp, ctxt, _objectIdReader);

        // 04-Jan-2010, tatu: May need to collect unknown properties for polymorphic cases
        TokenBuffer unknown = null;

        JsonToken t = jp.getCurrentToken();
        for (; t == JsonToken.FIELD_NAME; t = jp.nextToken()) {
            String propName = jp.getCurrentName();
            jp.nextToken(); // to point to value
            // creator property?
            SettableBeanProperty creatorProp = creator.findCreatorProperty(propName);
            if (creatorProp != null) {
                // Last creator property to set?
                Object value = creatorProp.deserialize(jp, ctxt);
                if (buffer.assignParameter(creatorProp.getCreatorIndex(), value)) {
                    jp.nextToken(); // to move to following FIELD_NAME/END_OBJECT
                    Object bean;
                    try {
                        bean = creator.build(ctxt, buffer);
                    } catch (Exception e) {
                        wrapAndThrow(e, _beanType.getRawClass(), propName, ctxt);
                        bean = null; // never gets here
                    }
                    //  polymorphic?
                    if (bean.getClass() != _beanType.getRawClass()) {
                        return handlePolymorphic(jp, ctxt, bean, unknown);
                    }
                    if (unknown != null) { // nope, just extra unknown stuff...
                        bean = handleUnknownProperties(ctxt, bean, unknown);
                    }
                    // or just clean?
                    return deserialize(jp, ctxt, bean);
                }
                continue;
            }
            // Object Id property?
            if (buffer.readIdProperty(propName)) {
                continue;
            }

            // regular property? needs buffering
            SettableBeanProperty prop = _beanProperties.find(propName);
            if (prop != null) {
                buffer.bufferProperty(prop, prop.deserialize(jp, ctxt));
                continue;
            }
            // As per [JACKSON-313], things marked as ignorable should not be
            // passed to any setter
            if (_ignorableProps != null && _ignorableProps.contains(propName)) {
                handleIgnoredProperty(jp, ctxt, handledType(), propName);
                continue;
            }
            // "any property"?
            if (_anySetter != null) {
                buffer.bufferAnyProperty(_anySetter, propName, _anySetter.deserialize(jp, ctxt));
                continue;
            }
            // Ok then, let's collect the whole field; name and value
            if (unknown == null) {
                unknown = new TokenBuffer(jp);
            }
            unknown.writeFieldName(propName);
            unknown.copyCurrentStructure(jp);
        }

        // We hit END_OBJECT, so:
        Object bean;
        try {
            bean = creator.build(ctxt, buffer);
        } catch (Exception e) {
            wrapInstantiationProblem(e, ctxt);
            bean = null; // never gets here
        }
        if (unknown != null) {
            // polymorphic?
            if (bean.getClass() != _beanType.getRawClass()) {
                return handlePolymorphic(null, ctxt, bean, unknown);
            }
            // no, just some extra unknown properties
            return handleUnknownProperties(ctxt, bean, unknown);
        }
        return bean;
    }

    protected Object deserializeWithView(JsonParser jp, DeserializationContext ctxt,
                                         Object bean, Class<?> activeView)
            throws IOException {
        beanDeserializerAdvice.before(bean, jp, ctxt);
        JsonToken t = jp.getCurrentToken();
        for (; t == JsonToken.FIELD_NAME; t = jp.nextToken()) {
            String propName = jp.getCurrentName();
            // Skip field name:
            jp.nextToken();

            if (beanDeserializerAdvice.intercept(bean, propName, jp, ctxt)) {
                continue;
            }

            SettableBeanProperty prop = _beanProperties.find(propName);
            if (prop != null) {
                if (!prop.visibleInView(activeView)) {
                    jp.skipChildren();
                    continue;
                }
                try {
                    prop.deserializeAndSet(jp, ctxt, bean);
                } catch (Exception e) {
                    wrapAndThrow(e, bean, propName, ctxt);
                }
                continue;
            }
            handleUnknownVanilla(jp, ctxt, bean, propName);
        }
        beanDeserializerAdvice.after(bean, jp, ctxt);
        return bean;
    }

    /**
     * Method called when there are declared "unwrapped" properties
     * which need special handling
     */
    @SuppressWarnings("resource")
    protected Object deserializeWithUnwrapped(JsonParser jp, DeserializationContext ctxt)
            throws IOException {
        if (_delegateDeserializer != null) {
            return _valueInstantiator.createUsingDelegate(ctxt, _delegateDeserializer.deserialize(jp, ctxt));
        }
        if (_propertyBasedCreator != null) {
            return deserializeUsingPropertyBasedWithUnwrapped(jp, ctxt);
        }
        TokenBuffer tokens = new TokenBuffer(jp);
        tokens.writeStartObject();
        final Object bean = _valueInstantiator.createUsingDefault(ctxt);

        if (_injectables != null) {
            injectValues(ctxt, bean);
        }
        final Class<?> activeView = _needViewProcesing ? ctxt.getActiveView() : null;
        beanDeserializerAdvice.before(bean, jp, ctxt);
        for (; jp.getCurrentToken() != JsonToken.END_OBJECT; jp.nextToken()) {
            String propName = jp.getCurrentName();
            jp.nextToken();

            if (beanDeserializerAdvice.intercept(bean, propName, jp, ctxt)) {
                continue;
            }

            SettableBeanProperty prop = _beanProperties.find(propName);
            if (prop != null) { // normal case
                if (activeView != null && !prop.visibleInView(activeView)) {
                    jp.skipChildren();
                    continue;
                }
                try {
                    prop.deserializeAndSet(jp, ctxt, bean);
                } catch (Exception e) {
                    wrapAndThrow(e, bean, propName, ctxt);
                }
                continue;
            }
            // ignorable things should be ignored
            if (_ignorableProps != null && _ignorableProps.contains(propName)) {
                handleIgnoredProperty(jp, ctxt, bean, propName);
                continue;
            }
            // but... others should be passed to unwrapped property deserializers
            tokens.writeFieldName(propName);
            tokens.copyCurrentStructure(jp);
            // how about any setter? We'll get copies but...
            if (_anySetter != null) {
                try {
                    _anySetter.deserializeAndSet(jp, ctxt, bean, propName);
                } catch (Exception e) {
                    wrapAndThrow(e, bean, propName, ctxt);
                }
                continue;
            }
        }
        tokens.writeEndObject();
        _unwrappedPropertyHandler.processUnwrapped(jp, ctxt, bean, tokens);
        beanDeserializerAdvice.after(bean, jp, ctxt);
        return bean;
    }

    @SuppressWarnings("resource")
    protected Object deserializeWithUnwrapped(JsonParser jp, DeserializationContext ctxt, Object bean)
            throws IOException {
        JsonToken t = jp.getCurrentToken();
        if (t == JsonToken.START_OBJECT) {
            t = jp.nextToken();
        }
        TokenBuffer tokens = new TokenBuffer(jp);
        tokens.writeStartObject();
        final Class<?> activeView = _needViewProcesing ? ctxt.getActiveView() : null;
        beanDeserializerAdvice.before(bean, jp, ctxt);
        for (; t == JsonToken.FIELD_NAME; t = jp.nextToken()) {
            String propName = jp.getCurrentName();

            if (beanDeserializerAdvice.intercept(bean, propName, jp, ctxt)) {
                continue;
            }

            SettableBeanProperty prop = _beanProperties.find(propName);
            jp.nextToken();
            if (prop != null) { // normal case
                if (activeView != null && !prop.visibleInView(activeView)) {
                    jp.skipChildren();
                    continue;
                }
                try {
                    prop.deserializeAndSet(jp, ctxt, bean);
                } catch (Exception e) {
                    wrapAndThrow(e, bean, propName, ctxt);
                }
                continue;
            }
            if (_ignorableProps != null && _ignorableProps.contains(propName)) {
                handleIgnoredProperty(jp, ctxt, bean, propName);
                continue;
            }
            // but... others should be passed to unwrapped property deserializers
            tokens.writeFieldName(propName);
            tokens.copyCurrentStructure(jp);
            // how about any setter? We'll get copies but...
            if (_anySetter != null) {
                _anySetter.deserializeAndSet(jp, ctxt, bean, propName);
            }
        }
        tokens.writeEndObject();
        _unwrappedPropertyHandler.processUnwrapped(jp, ctxt, bean, tokens);
        beanDeserializerAdvice.after(bean, jp, ctxt);
        return bean;
    }

    @SuppressWarnings("resource")
    protected Object deserializeUsingPropertyBasedWithUnwrapped(JsonParser jp, DeserializationContext ctxt)
            throws IOException {
        final PropertyBasedCreator creator = _propertyBasedCreator;
        PropertyValueBuffer buffer = creator.startBuilding(jp, ctxt, _objectIdReader);

        TokenBuffer tokens = new TokenBuffer(jp);
        tokens.writeStartObject();

        JsonToken t = jp.getCurrentToken();
        for (; t == JsonToken.FIELD_NAME; t = jp.nextToken()) {
            String propName = jp.getCurrentName();
            jp.nextToken(); // to point to value
            // creator property?
            SettableBeanProperty creatorProp = creator.findCreatorProperty(propName);
            if (creatorProp != null) {
                // Last creator property to set?
                Object value = creatorProp.deserialize(jp, ctxt);
                if (buffer.assignParameter(creatorProp.getCreatorIndex(), value)) {
                    t = jp.nextToken(); // to move to following FIELD_NAME/END_OBJECT
                    Object bean;
                    try {
                        bean = creator.build(ctxt, buffer);
                    } catch (Exception e) {
                        wrapAndThrow(e, _beanType.getRawClass(), propName, ctxt);
                        continue; // never gets here
                    }
                    // if so, need to copy all remaining tokens into buffer
                    while (t == JsonToken.FIELD_NAME) {
                        jp.nextToken(); // to skip name
                        tokens.copyCurrentStructure(jp);
                        t = jp.nextToken();
                    }
                    tokens.writeEndObject();
                    if (bean.getClass() != _beanType.getRawClass()) {
                        // !!! 08-Jul-2011, tatu: Could probably support; but for now
                        //   it's too complicated, so bail out
                        tokens.close();
                        throw ctxt.mappingException("Can not create polymorphic instances with unwrapped values");
                    }
                    return _unwrappedPropertyHandler.processUnwrapped(jp, ctxt, bean, tokens);
                }
                continue;
            }
            // Object Id property?
            if (buffer.readIdProperty(propName)) {
                continue;
            }

            // regular property? needs buffering
            SettableBeanProperty prop = _beanProperties.find(propName);
            if (prop != null) {
                buffer.bufferProperty(prop, prop.deserialize(jp, ctxt));
                continue;
            }
            /* As per [JACKSON-313], things marked as ignorable should not be
             * passed to any setter
             */
            if (_ignorableProps != null && _ignorableProps.contains(propName)) {
                handleIgnoredProperty(jp, ctxt, handledType(), propName);
                continue;
            }
            tokens.writeFieldName(propName);
            tokens.copyCurrentStructure(jp);
            // "any property"?
            if (_anySetter != null) {
                buffer.bufferAnyProperty(_anySetter, propName, _anySetter.deserialize(jp, ctxt));
            }
        }

        // We hit END_OBJECT, so:
        Object bean;
        try {
            bean = creator.build(ctxt, buffer);
        } catch (Exception e) {
            wrapInstantiationProblem(e, ctxt);
            return null; // never gets here
        }
        return _unwrappedPropertyHandler.processUnwrapped(jp, ctxt, bean, tokens);
    }

    protected Object deserializeWithExternalTypeId(JsonParser jp, DeserializationContext ctxt)
            throws IOException {
        if (_propertyBasedCreator != null) {
            return deserializeUsingPropertyBasedWithExternalTypeId(jp, ctxt);
        }
        return deserializeWithExternalTypeId(jp, ctxt, _valueInstantiator.createUsingDefault(ctxt));
    }

    protected Object deserializeWithExternalTypeId(JsonParser jp, DeserializationContext ctxt,
                                                   Object bean)
            throws IOException {
        final Class<?> activeView = _needViewProcesing ? ctxt.getActiveView() : null;
        final ExternalTypeHandler ext = _externalTypeIdHandler.start();
        beanDeserializerAdvice.before(bean, jp, ctxt);
        for (; jp.getCurrentToken() != JsonToken.END_OBJECT; jp.nextToken()) {
            String propName = jp.getCurrentName();
            jp.nextToken();

            if (beanDeserializerAdvice.intercept(bean, propName, jp, ctxt)) {
                continue;
            }

            SettableBeanProperty prop = _beanProperties.find(propName);
            if (prop != null) { // normal case
                // [JACKSON-831]: may have property AND be used as external type id:
                if (jp.getCurrentToken().isScalarValue()) {
                    ext.handleTypePropertyValue(jp, ctxt, propName, bean);
                }
                if (activeView != null && !prop.visibleInView(activeView)) {
                    jp.skipChildren();
                    continue;
                }
                try {
                    prop.deserializeAndSet(jp, ctxt, bean);
                } catch (Exception e) {
                    wrapAndThrow(e, bean, propName, ctxt);
                }
                continue;
            }
            // ignorable things should be ignored
            if (_ignorableProps != null && _ignorableProps.contains(propName)) {
                handleIgnoredProperty(jp, ctxt, bean, propName);
                continue;
            }
            // but others are likely to be part of external type id thingy...
            if (ext.handlePropertyValue(jp, ctxt, propName, bean)) {
                continue;
            }
            // if not, the usual fallback handling:
            if (_anySetter != null) {
                try {
                    _anySetter.deserializeAndSet(jp, ctxt, bean, propName);
                } catch (Exception e) {
                    wrapAndThrow(e, bean, propName, ctxt);
                }
                continue;
            }
            // Unknown: let's call handler method
            handleUnknownProperty(jp, ctxt, bean, propName);
        }
        // and when we get this far, let's try finalizing the deal:
        ext.complete(jp, ctxt, bean);
        beanDeserializerAdvice.after(bean, jp, ctxt);
        return bean;
    }

    protected Object deserializeUsingPropertyBasedWithExternalTypeId(JsonParser jp, DeserializationContext ctxt)
            throws IOException {
        final ExternalTypeHandler ext = _externalTypeIdHandler.start();
        final PropertyBasedCreator creator = _propertyBasedCreator;
        PropertyValueBuffer buffer = creator.startBuilding(jp, ctxt, _objectIdReader);

        TokenBuffer tokens = new TokenBuffer(jp);
        tokens.writeStartObject();

        JsonToken t = jp.getCurrentToken();
        for (; t == JsonToken.FIELD_NAME; t = jp.nextToken()) {
            String propName = jp.getCurrentName();
            jp.nextToken(); // to point to value
            // creator property?
            SettableBeanProperty creatorProp = creator.findCreatorProperty(propName);
            if (creatorProp != null) {
                // first: let's check to see if this might be part of value with external type id:
                if (!ext.handlePropertyValue(jp, ctxt, propName, buffer)) {
                    // Last creator property to set?
                    Object value = creatorProp.deserialize(jp, ctxt);
                    if (buffer.assignParameter(creatorProp.getCreatorIndex(), value)) {
                        t = jp.nextToken(); // to move to following FIELD_NAME/END_OBJECT
                        Object bean;
                        try {
                            bean = creator.build(ctxt, buffer);
                        } catch (Exception e) {
                            wrapAndThrow(e, _beanType.getRawClass(), propName, ctxt);
                            continue; // never gets here
                        }
                        // if so, need to copy all remaining tokens into buffer
                        while (t == JsonToken.FIELD_NAME) {
                            jp.nextToken(); // to skip name
                            tokens.copyCurrentStructure(jp);
                            t = jp.nextToken();
                        }
                        if (bean.getClass() != _beanType.getRawClass()) {
                            // !!! 08-Jul-2011, tatu: Could probably support; but for now
                            //   it's too complicated, so bail out
                            throw ctxt.mappingException("Can not create polymorphic instances with unwrapped values");
                        }
                        return ext.complete(jp, ctxt, bean);
                    }
                }
                continue;
            }
            // Object Id property?
            if (buffer.readIdProperty(propName)) {
                continue;
            }

            // regular property? needs buffering
            SettableBeanProperty prop = _beanProperties.find(propName);
            if (prop != null) {
                buffer.bufferProperty(prop, prop.deserialize(jp, ctxt));
                continue;
            }
            // external type id (or property that depends on it)?
            if (ext.handlePropertyValue(jp, ctxt, propName, null)) {
                continue;
            }
            /* As per [JACKSON-313], things marked as ignorable should not be
             * passed to any setter
             */
            if (_ignorableProps != null && _ignorableProps.contains(propName)) {
                handleIgnoredProperty(jp, ctxt, handledType(), propName);
                continue;
            }
            // "any property"?
            if (_anySetter != null) {
                buffer.bufferAnyProperty(_anySetter, propName, _anySetter.deserialize(jp, ctxt));
            }
        }

        // We hit END_OBJECT; resolve the pieces:
        try {
            return ext.complete(jp, ctxt, buffer, creator);
        } catch (Exception e) {
            wrapInstantiationProblem(e, ctxt);
            return null; // never gets here
        }
    }

}
