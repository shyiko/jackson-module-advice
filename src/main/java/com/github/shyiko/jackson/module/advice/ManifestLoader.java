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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 * @author <a href="mailto:stanley.shyiko@gmail.com">Stanley Shyiko</a>
 */
public final class ManifestLoader {

    private ManifestLoader() {
    }

    public static Manifest load(String implementationVendorId, String implementationTitle) throws IOException {
        ClassLoader classLoader = ManifestLoader.class.getClassLoader();
        Enumeration<URL> resources = classLoader.getResources("META-INF/MANIFEST.MF");
        while (resources.hasMoreElements()) {
            URL url = resources.nextElement();
            InputStream manifestStream = url.openStream();
            try {
                Manifest manifest = new Manifest(manifestStream);
                Attributes attributes = manifest.getMainAttributes();
                if (implementationVendorId.equals(attributes.getValue("Implementation-Vendor-Id")) &&
                    implementationTitle.equals(attributes.getValue("Implementation-Title"))) {
                    return manifest;
                }
            } finally {
                manifestStream.close();
            }
        }
        return null;
    }

}
