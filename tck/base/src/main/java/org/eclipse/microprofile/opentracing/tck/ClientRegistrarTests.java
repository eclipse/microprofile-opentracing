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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.microprofile.opentracing.tck.application.TestClientRegistrarWebServices;
import org.eclipse.microprofile.opentracing.tck.tracer.TestSpan;
import org.eclipse.microprofile.opentracing.tck.tracer.TestSpanTree;
import org.eclipse.microprofile.opentracing.tck.tracer.TestSpanTree.TreeNode;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import io.opentracing.tag.Tags;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

/**
 * @author Pavol Loffay
 */
public class ClientRegistrarTests extends OpenTracingBaseTests {

    @Deployment
    public static WebArchive createDeployment() {
        return OpenTracingBaseTests.createDeployment();
    }

    @Test
    @RunAsClient
    public void testClientRegistrar() {
        testClientRegistrar(TestClientRegistrarWebServices.REST_CLIENT_BUILDER, false);
    }

    @Test
    @RunAsClient
    public void testClientRegistrarAsync() {
        testClientRegistrar(TestClientRegistrarWebServices.REST_CLIENT_BUILDER, true);
    }

    @Test
    @RunAsClient
    public void testClientRegistrarExecutor() {
        testClientRegistrar(TestClientRegistrarWebServices.REST_CLIENT_BUILDER_EXECUTOR, false);
    }

    @Test
    @RunAsClient
    public void testClientRegistrarExecutorAsync() {
        testClientRegistrar(TestClientRegistrarWebServices.REST_CLIENT_BUILDER_EXECUTOR, true);
    }

    public void testClientRegistrar(String path, boolean async) {
        Map<String, Object> queryParams = new HashMap<>();
        if (async) {
            queryParams.put("async", "true");
        }
        Response response = executeRemoteWebServiceRaw(TestClientRegistrarWebServices.REST_SERVICE_PATH,
                path, queryParams, Status.OK);
        response.close();

        TestSpanTree spans = executeRemoteWebServiceTracerTree();

        TestSpanTree expectedTree = new TestSpanTree(
                new TreeNode<>(
                        new TestSpan(
                                getOperationName(
                                        Tags.SPAN_KIND_SERVER,
                                        HttpMethod.GET,
                                        TestClientRegistrarWebServices.class,
                                        getEndpointMethod(TestClientRegistrarWebServices.class, path)),
                                getExpectedSpanTags(
                                        Tags.SPAN_KIND_SERVER,
                                        HttpMethod.GET,
                                        TestClientRegistrarWebServices.REST_SERVICE_PATH,
                                        path,
                                        queryParams,
                                        Status.OK.getStatusCode(),
                                        JAXRS_COMPONENT),
                                Collections.emptyList()),
                        new TreeNode<>(
                                new TestSpan(
                                        getOperationName(
                                                Tags.SPAN_KIND_CLIENT,
                                                HttpMethod.GET,
                                                TestClientRegistrarWebServices.class,
                                                getEndpointMethod(TestClientRegistrarWebServices.class,
                                                        TestClientRegistrarWebServices.REST_OK)),
                                        getExpectedSpanTags(
                                                Tags.SPAN_KIND_CLIENT,
                                                HttpMethod.GET,
                                                TestClientRegistrarWebServices.REST_SERVICE_PATH,
                                                TestClientRegistrarWebServices.REST_OK,
                                                null,
                                                Status.OK.getStatusCode(),
                                                JAXRS_COMPONENT),
                                        Collections.emptyList()),
                                new TreeNode<>(
                                        new TestSpan(
                                                getOperationName(
                                                        Tags.SPAN_KIND_SERVER,
                                                        HttpMethod.GET,
                                                        TestClientRegistrarWebServices.class,
                                                        getEndpointMethod(TestClientRegistrarWebServices.class,
                                                                TestClientRegistrarWebServices.REST_OK)),
                                                getExpectedSpanTags(
                                                        Tags.SPAN_KIND_SERVER,
                                                        HttpMethod.GET,
                                                        TestClientRegistrarWebServices.REST_SERVICE_PATH,
                                                        TestClientRegistrarWebServices.REST_OK,
                                                        null,
                                                        Status.OK.getStatusCode(),
                                                        JAXRS_COMPONENT),
                                                Collections.emptyList())))));
        assertEqualTrees(spans, expectedTree);
    }
}
