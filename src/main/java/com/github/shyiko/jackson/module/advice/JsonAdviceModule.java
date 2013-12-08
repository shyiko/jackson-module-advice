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

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.deser.BeanDeserializerBuilder;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;

import java.io.IOException;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 * @author <a href="mailto:stanley.shyiko@gmail.com">Stanley Shyiko</a>
 */
public class JsonAdviceModule extends Module {

    private static final Version MODULE_VERSION;

    static {
        String groupId = "com.github.shyiko", artifactId = "jackson-module-advice", build = "UNKNOWN";
        int major = 0, minor = 0, patch = 0;
        try {
            Manifest manifest = ManifestLoader.load(groupId, artifactId);
            if (manifest != null) {
                Attributes attributes = manifest.getMainAttributes();
                String version = attributes.getValue("Implementation-Version");
                if (version != null) {
                    String[] versionSplit = version.split("[.]");
                    if (versionSplit.length == 3) {
                        major = Integer.parseInt(versionSplit[0]);
                        minor = Integer.parseInt(versionSplit[1]);
                        String[] splitBetweenPathAndBuild = versionSplit[2].split("-");
                        patch = Integer.parseInt(splitBetweenPathAndBuild[0]);
                        build = splitBetweenPathAndBuild.length == 2 ? splitBetweenPathAndBuild[1] : null;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        MODULE_VERSION = new Version(major, minor, patch, build, groupId, artifactId);
    }

    @Override
    public String getModuleName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public Version version() {
        return MODULE_VERSION;
    }

    @Override
    public void setupModule(SetupContext context) {
        context.addBeanSerializerModifier(new BeanSerializerModifier() {

            @Override
            public com.fasterxml.jackson.databind.ser.BeanSerializerBuilder updateBuilder(SerializationConfig config,
                    BeanDescription beanDesc, com.fasterxml.jackson.databind.ser.BeanSerializerBuilder builder) {
                JsonSerializerAdvice advice = beanDesc.getClassAnnotations().get(JsonSerializerAdvice.class);
                return advice != null ? new AdvisedBeanSerializerBuilder(builder, advice.value()) : builder;
            }
        });
        context.addBeanDeserializerModifier(new BeanDeserializerModifier() {

            @Override
            public BeanDeserializerBuilder updateBuilder(DeserializationConfig config, BeanDescription beanDesc,
                    BeanDeserializerBuilder builder) {
                JsonDeserializerAdvice advice = beanDesc.getClassAnnotations().get(JsonDeserializerAdvice.class);
                return  advice != null ? new AdvisedBeanDeserializerBuilder(builder, advice.value()) : builder;
            }
        });
    }

}
