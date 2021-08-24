/*
 * Copyright (c) 2018-2021 Contributors to the Eclipse Foundation
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
import org.eclipse.microprofile.opentracing.tck.application.TestServerSkipAllWebServices;
import org.eclipse.microprofile.opentracing.tck.application.TestServerWebServices;
import org.eclipse.microprofile.opentracing.tck.tracer.TestSpanTree;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

/**
 * @author Pavol Loffay
 */
public class OpenTracingSkipPatternTests extends OpenTracingBaseTests {

    public static class TestConfiguration implements ConfigSource {
        private Map<String, String> propMap = new HashMap<>();

        {
            propMap.put("mp.opentracing.server.skip-pattern", "/skipAll/.*|/testServices/skipSimple");
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

    /**
     * Test a web service endpoint that shouldn't be traced.
     */
    @Test
    @RunAsClient
    private void testSkipSimple() {
        Response response = executeRemoteWebServiceRaw(TestServerWebServices.REST_TEST_SERVICE_PATH,
                TestServerWebServices.REST_SKIP_SIMPLE, Status.NO_CONTENT);
        response.close();
        TestSpanTree spans = executeRemoteWebServiceTracerTree();
        assertEqualTrees(spans, new TestSpanTree());
    }

    /**
     * Test a web service endpoint that shouldn't be traced.
     */
    @Test
    @RunAsClient
    private void testSkipFoo() {
        Response response = executeRemoteWebServiceRaw(TestServerSkipAllWebServices.REST_TEST_SKIP_SERVICE_PATH,
                TestServerSkipAllWebServices.REST_SIMPLE_PATH, Status.OK);
        response.close();
        TestSpanTree spans = executeRemoteWebServiceTracerTree();
        assertEqualTrees(spans, new TestSpanTree());
    }

    /**
     * Test a web service endpoint that shouldn't be traced.
     */
    @Test
    @RunAsClient
    private void testSkipFooBar() {
        Response response = executeRemoteWebServiceRaw(TestServerSkipAllWebServices.REST_TEST_SKIP_SERVICE_PATH,
                TestServerSkipAllWebServices.REST_NESTED_PATH, Status.OK);
        response.close();
        TestSpanTree spans = executeRemoteWebServiceTracerTree();
        assertEqualTrees(spans, new TestSpanTree());
    }

    /**
     * Test a web service endpoint that shouldn't be traced.
     */
    @Test
    @RunAsClient
    private void testExplicitlyTraced() {
        Response response = executeRemoteWebServiceRaw(TestServerSkipAllWebServices.REST_TEST_SKIP_SERVICE_PATH,
                TestServerSkipAllWebServices.REST_EXPLICITLY_TRACED, Status.OK);
        response.close();
        TestSpanTree spans = executeRemoteWebServiceTracerTree();
        assertEqualTrees(spans, new TestSpanTree());
    }

    @Test
    @RunAsClient
    private void testHealthNotTraced() {
        Client client = ClientBuilder.newClient();
        String url = String.format("%s/health", baseUrl());
        debug("Executing " + url);
        client.target(url).request().get();

        TestSpanTree spans = executeRemoteWebServiceTracerTree();
        assertEqualTrees(spans, new TestSpanTree());
    }

    @Test
    @RunAsClient
    private void testOpenAPINotTraced() {
        Client client = ClientBuilder.newClient();
        String url = String.format("%s/openapi", baseUrl());
        debug("Executing " + url);
        client.target(url).request().get();

        TestSpanTree spans = executeRemoteWebServiceTracerTree();
        assertEqualTrees(spans, new TestSpanTree());
    }

    @Test
    @RunAsClient
    private void testMetricsNotTraced() {
        Client client = ClientBuilder.newClient();
        String url = String.format("%s/metrics", baseUrl());
        debug("Executing " + url);
        client.target(url).request().get();

        TestSpanTree spans = executeRemoteWebServiceTracerTree();
        assertEqualTrees(spans, new TestSpanTree());
    }

    @Test
    @RunAsClient
    private void testMetricsBaseNotTraced() {
        Client client = ClientBuilder.newClient();
        String url = String.format("%s/metrics/base", baseUrl());
        debug("Executing " + url);
        client.target(url).request().get();

        TestSpanTree spans = executeRemoteWebServiceTracerTree();
        assertEqualTrees(spans, new TestSpanTree());
    }

    @Test
    @RunAsClient
    private void testMetricsVendorNotTraced() {
        Client client = ClientBuilder.newClient();
        String url = String.format("%s/metrics/vendor", baseUrl());
        debug("Executing " + url);
        client.target(url).request().get();

        TestSpanTree spans = executeRemoteWebServiceTracerTree();
        assertEqualTrees(spans, new TestSpanTree());
    }

    @Test
    @RunAsClient
    private void testMetricsApplicationNotTraced() {
        Client client = ClientBuilder.newClient();
        String url = String.format("%s/metrics/application", baseUrl());
        debug("Executing " + url);
        client.target(url).request().get();

        TestSpanTree spans = executeRemoteWebServiceTracerTree();
        assertEqualTrees(spans, new TestSpanTree());
    }

    private String baseUrl() {
        return String.format("http://%s:%d", deploymentURL.getHost(), deploymentURL.getPort());
    }
}
