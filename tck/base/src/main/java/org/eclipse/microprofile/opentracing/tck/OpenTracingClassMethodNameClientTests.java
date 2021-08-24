/*
 * Copyright (c) 2017 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.eclipse.microprofile.opentracing.tck;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;

/**
 * @author Pavol Loffay
 */
public class OpenTracingClassMethodNameClientTests extends OpenTracingClientBaseTests {

    public static class TestConfiguration implements ConfigSource {
        private Map<String, String> propMap = new HashMap<>();
        {
            propMap.put("mp.opentracing.server.operation-name-provider", "class-method");
        }

        @Override
        public Map<String, String> getProperties() {
            return propMap;
        }

        @Override
        public String getValue(String s) {
            return propMap.get(s);
        }

        @Override
        public String getName() {
            return this.getClass().getName();
        }

        @Override
        public Set<String> getPropertyNames() {
            return getProperties().keySet();
        }
    }

    @Deployment
    public static WebArchive createDeployment() {
        return OpenTracingBaseTests.createDeployment()
                .addAsServiceProvider(ConfigSource.class, TestConfiguration.class);
    }

}
