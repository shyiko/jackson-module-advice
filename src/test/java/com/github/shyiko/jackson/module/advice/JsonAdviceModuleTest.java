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
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import org.testng.annotations.Test;

import java.io.IOException;

import static org.testng.Assert.assertEquals;

/**
 * @author <a href="mailto:stanley.shyiko@gmail.com">Stanley Shyiko</a>
 */
public class JsonAdviceModuleTest {

    @Test
    public void testSerialization() throws Exception {
        assertEquals(objectMapper().writeValueAsString(new User("Sponge", "Bob", "loves you")),
                "{\"firstName\":\"Sponge\",\"lastName\":\"Bob\"}");
        assertEquals(objectMapper(new JsonAdviceModule()).writeValueAsString(new User("Sponge", "Bob", "loves you")),
                "{\"firstName\":\"Sponge\",\"fieldInTheMiddle\":\"value\",\"lastName\":\"Bob\"}");
    }

    @Test
    public void testDeserialization() throws Exception {
        String JSON = "{\"firstName\":\"Sponge\",\"lastName\":\"Bob\",\"password\":\"loves you\"}";
        assertEquals(objectMapper().readValue(JSON, User.class), new User("Sponge", "Bob", null));
        assertEquals(objectMapper(new JsonAdviceModule()).readValue(JSON, User.class),
                new User("Sponge", "Bob", "loves you"));
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
    @JsonDeserializerAdvice(UserDeserializationAdvice.class)
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

    static class UserDeserializationAdvice implements BeanDeserializerAdvice<User> {

        @Override
        public boolean intercept(User bean, String propertyName, JsonParser json, DeserializationContext context)
                throws IOException {
            System.out.println(propertyName);
            if ("password".equals(propertyName)) {
                bean.password = json.getText();
                return true;
            }
            return false;
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

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            User user = (User) o;
            return !(firstName != null ? !firstName.equals(user.firstName) : user.firstName != null) &&
                    !(lastName != null ? !lastName.equals(user.lastName) : user.lastName != null) &&
                    !(password != null ? !password.equals(user.password) : user.password != null);
        }

        @Override
        public int hashCode() {
            int result = firstName != null ? firstName.hashCode() : 0;
            result = 31 * result + (lastName != null ? lastName.hashCode() : 0);
            result = 31 * result + (password != null ? password.hashCode() : 0);
            return result;
        }
    }

}
