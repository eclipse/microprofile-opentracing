/*
 * Copyright (c) 2017-2021 Contributors to the Eclipse Foundation
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

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.opentracing.tck.application.WildcardClassService;
import org.eclipse.microprofile.opentracing.tck.tracer.TestSpan;
import org.eclipse.microprofile.opentracing.tck.tracer.TestSpanTree;
import org.eclipse.microprofile.opentracing.tck.tracer.TestSpanTree.TreeNode;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import io.opentracing.tag.Tags;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

/**
 * @author Pavol Loffay
 */
public class OpenTracingHTTPPathNameTests extends OpenTracingClientBaseTests {

    public static class TestConfiguration implements ConfigSource {
        private Map<String, String> propMap = new HashMap<>();

        {
            propMap.put("mp.opentracing.server.operation-name-provider", "http-path");
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

    @Override
    protected String getOperationName(String spanKind, String httpMethod, Class<?> clazz, Method method) {
        if (spanKind.equals(Tags.SPAN_KIND_SERVER)) {
            StringBuilder operationName = new StringBuilder(httpMethod.toUpperCase() + ":");
            Path classPath = clazz.getAnnotation(Path.class);
            if (classPath == null) {
                throw new IllegalArgumentException("Supplied clazz is not JAX-RS resource");
            }
            if (!classPath.value().startsWith("/")) {
                operationName.append("/");
            }
            operationName.append(classPath.value());
            if (!classPath.value().endsWith("/")) {
                operationName.append("/");
            }
            Path methodPath = method.getAnnotation(Path.class);
            String methodPathStr = methodPath.value();
            if (methodPathStr.startsWith("/")) {
                methodPathStr = methodPathStr.replaceFirst("/", "");
            }
            operationName.append(methodPathStr);
            return operationName.toString();
        }
        return super.getOperationName(spanKind, httpMethod, clazz, method);
    }

    /**
     * Test that server endpoint is adding standard tags
     */
    @Test
    @RunAsClient
    private void testWildcard() {
        Response response = executeRemoteWebServiceRaw("wildcard/10/foo",
                "getFoo/ten", Status.OK);
        response.close();

        TestSpanTree spans = executeRemoteWebServiceTracerTree();

        TestSpanTree expectedTree = new TestSpanTree(
                new TreeNode<>(
                        new TestSpan(
                                getOperationName(
                                        Tags.SPAN_KIND_SERVER,
                                        HttpMethod.GET,
                                        WildcardClassService.class,
                                        getEndpointMethod(WildcardClassService.class,
                                                WildcardClassService.REST_FOO_PATH)),
                                getExpectedSpanTags(
                                        Tags.SPAN_KIND_SERVER,
                                        HttpMethod.GET,
                                        "wildcard/10/foo",
                                        "getFoo/ten",
                                        null,
                                        Status.OK.getStatusCode(),
                                        JAXRS_COMPONENT),
                                Collections.emptyList())));
        assertEqualTrees(spans, expectedTree);
    }

    /**
     * Test that server endpoint is adding standard tags
     */
    @Test
    @RunAsClient
    private void testTwoSameParams() {
        Response response = executeRemoteWebServiceRaw("wildcard/1/foo",
                "twoIds/1/1", Status.OK);
        response.close();

        TestSpanTree spans = executeRemoteWebServiceTracerTree();

        TestSpanTree expectedTree = new TestSpanTree(
                new TreeNode<>(
                        new TestSpan(
                                getOperationName(
                                        Tags.SPAN_KIND_SERVER,
                                        HttpMethod.GET,
                                        WildcardClassService.class,
                                        getEndpointMethod(WildcardClassService.class,
                                                WildcardClassService.REST_TWO_IDS)),
                                getExpectedSpanTags(
                                        Tags.SPAN_KIND_SERVER,
                                        HttpMethod.GET,
                                        "wildcard/1/foo",
                                        "twoIds/1/1",
                                        null,
                                        Status.OK.getStatusCode(),
                                        JAXRS_COMPONENT),
                                Collections.emptyList())));
        assertEqualTrees(spans, expectedTree);
    }
}
