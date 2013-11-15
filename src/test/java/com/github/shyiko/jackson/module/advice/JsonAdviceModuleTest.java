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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.testng.annotations.Test;

import java.io.IOException;

import static org.testng.Assert.assertEquals;

/**
 * @author <a href="mailto:stanley.shyiko@gmail.com">Stanley Shyiko</a>
 */
public class JsonAdviceModuleTest {

    @Test
    public void testModule() throws Exception {
        assertEquals("{\"firstName\":\"Sponge\",\"lastName\":\"Bob\"}",
                objectMapper().writeValueAsString(new User("Sponge", "Bob", "loves you")));
        assertEquals(objectMapper(new JsonAdviceModule()).writeValueAsString(new User("Sponge", "Bob", "loves you")),
                "{\"firstName\":\"Sponge\",\"fieldInTheMiddle\":\"value\",\"lastName\":\"Bob\"}");
    }

    private ObjectMapper objectMapper(Module... modules) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.addMixInAnnotations(User.class, UserMixin.class);
        for (Module module : modules) {
            objectMapper.registerModule(module);
        }
        return objectMapper;
    }

    @JsonSerializerAdvice(UserSerializationAdvice.class)
    abstract class UserMixin {

        @JsonIgnore
        public String password;

    }

    static class UserSerializationAdvice extends AbstractBeanSerializerAdvice<User> {

        @Override
        public void after(User bean, JsonGenerator json, BeanProperty property, SerializerProvider provider)
                throws IOException {
            if ("firstName".equals(property.getName())) {
                json.writeStringField("fieldInTheMiddle", "value");
            }
        }
    }

    static class User {

        public String firstName;
        public String lastName;
        public String password;

        User() {
        }

        User(String firstName, String lastName, String password) {
            this.firstName = firstName;
            this.lastName = lastName;
            this.password = password;
        }
    }

}
