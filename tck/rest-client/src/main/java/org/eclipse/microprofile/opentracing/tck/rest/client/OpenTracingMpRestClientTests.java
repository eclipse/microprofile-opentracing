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

package org.eclipse.microprofile.opentracing.tck.rest.client;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.eclipse.microprofile.opentracing.tck.OpenTracingBaseTests;
import org.eclipse.microprofile.opentracing.tck.application.TestServerWebServices;
import org.eclipse.microprofile.opentracing.tck.application.TestWebServicesApplication;
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
public class OpenTracingMpRestClientTests extends OpenTracingBaseTests {

    @Deployment
    public static WebArchive createDeployment() {
        WebArchive deployment = OpenTracingBaseTests.createDeployment();
        deployment.addPackages(true, OpenTracingMpRestClientTests.class.getPackage());
        deployment.deleteClass(TestWebServicesApplication.class.getCanonicalName());
        return deployment;
    }

    /**
     * Test a web service call that makes nested calls.
     */
    @Test
    @RunAsClient
    private void testNestedSpans() {
        int nestDepth = 1;
        int nestBreadth = 2;
        int uniqueId = getRandomNumber();
        boolean failNest = false;
        boolean async = false;
        testNestedSpans(RestClientServices.REST_NESTED_MP_REST_CLIENT, nestDepth, nestBreadth, uniqueId, failNest,
                async);
    }

    /**
     * Test the nested web service concurrently. A unique ID is generated in the URL of each request and propagated down
     * the nested spans. We extract this out of the resulting spans and ensure the unique IDs are correct.
     * 
     * @throws InterruptedException
     *             Problem executing web service.
     * @throws ExecutionException
     *             Thread pool problem.
     *
     */
    @Test
    @RunAsClient
    private void testMultithreadedNestedSpans() throws ExecutionException, InterruptedException {
        int numberOfCalls = 100;
        int nestDepth = 1;
        int nestBreadth = 2;
        boolean failNest = false;
        boolean async = false;

        testMultithreadedNestedSpans(RestClientServices.REST_NESTED_MP_REST_CLIENT, numberOfCalls, nestDepth,
                nestBreadth, failNest, async);
    }

    /**
     * Same as testMultithreadedNestedSpans but asynchronous client and nested requests.
     * 
     * @throws InterruptedException
     *             Problem executing web service.
     * @throws ExecutionException
     *             Thread pool problem.
     *
     */
    @Test
    @RunAsClient
    private void testMultithreadedNestedSpansAsync() throws ExecutionException, InterruptedException {
        int numberOfCalls = 100;
        int nestDepth = 1;
        int nestBreadth = 2;
        boolean failNest = false;
        boolean async = true;

        testMultithreadedNestedSpans(RestClientServices.REST_NESTED_MP_REST_CLIENT, numberOfCalls, nestDepth,
                nestBreadth, failNest, async);
    }

    @Test
    @RunAsClient
    private void testClientNotTraced() {
        testNotTraced(RestClientServices.REST_MP_REST_CLIENT_DISABLED_TRACING);
    }

    @Test
    @RunAsClient
    private void testMethodNotTraced() {
        testNotTraced(RestClientServices.REST_MP_REST_CLIENT_DISABLED_TRACING_METHOD);
    }

    private void testNotTraced(String service) {
        executeRemoteWebServiceRaw(RestClientServices.REST_SERVICE_PATH, service, Status.OK)
                .close();

        TestSpanTree expectedTree = new TestSpanTree(
                new TreeNode<>(
                        new TestSpan(
                                getOperationName(
                                        Tags.SPAN_KIND_SERVER,
                                        HttpMethod.GET,
                                        TestServerWebServices.class,
                                        getEndpointMethod(TestServerWebServices.class,
                                                TestServerWebServices.REST_SIMPLE_TEST)),
                                getExpectedSpanTags(
                                        Tags.SPAN_KIND_SERVER,
                                        HttpMethod.GET,
                                        TestServerWebServices.REST_TEST_SERVICE_PATH,
                                        TestServerWebServices.REST_SIMPLE_TEST,
                                        null,
                                        Status.OK.getStatusCode(),
                                        JAXRS_COMPONENT),
                                Collections.emptyList())),
                new TreeNode<>(
                        new TestSpan(
                                getOperationName(
                                        Tags.SPAN_KIND_SERVER,
                                        HttpMethod.GET,
                                        RestClientServices.class,
                                        getEndpointMethod(RestClientServices.class, service)),
                                getExpectedSpanTags(
                                        Tags.SPAN_KIND_SERVER,
                                        HttpMethod.GET,
                                        RestClientServices.REST_SERVICE_PATH,
                                        service,
                                        null,
                                        Status.OK.getStatusCode(),
                                        JAXRS_COMPONENT),
                                Collections.emptyList())));

        TestSpanTree spans = executeRemoteWebServiceTracerTree();
        assertEqualTrees(spans, expectedTree);

    }

    /**
     * Execute the nested web service.
     * 
     * @param uniqueId
     *            Some unique ID.
     * @param nestDepth
     *            How deep to nest the calls.
     * @param nestBreadth
     *            Breadth of first level of nested calls.
     * @param failNest
     *            Whether to fail the nested call.
     * @param async
     *            Whether to execute nested requests asynchronously.
     */
    @Override
    protected void executeNested(String path, int uniqueId, int nestDepth, int nestBreadth, boolean failNest,
            boolean async) {
        Map<String, Object> queryParameters = getNestedQueryParameters(uniqueId,
                nestDepth, nestBreadth, failNest, async);

        Response response = executeRemoteWebServiceRaw(
                RestClientServices.REST_SERVICE_PATH,
                path,
                queryParameters,
                Status.OK);
        response.close();
    }

    /**
     * The expected nested span layout.
     * 
     * @param spanKind
     *            Span kind
     * @param uniqueId
     *            The unique ID of the request.
     * @param nestDepth
     *            Nest depth
     * @param nestBreadth
     *            Nest breadth
     * @param failNest
     *            Whether to fail the nested call.
     * @param isFailed
     *            Whether this request is expected to fail.
     * @param async
     *            Whether to execute asynchronously.
     * @return Span for the nested call.
     */
    @Override
    protected TestSpan getExpectedNestedServerSpan(String path, String spanKind, int uniqueId,
            int nestDepth, int nestBreadth, boolean failNest,
            boolean isFailed, boolean async) {
        String operationName;
        Map<String, Object> expectedTags;

        Map<String, Object> queryParameters = new HashMap<>();
        queryParameters.put(TestServerWebServices.PARAM_UNIQUE_ID, uniqueId);
        queryParameters.put(TestServerWebServices.PARAM_NEST_DEPTH, nestDepth);
        queryParameters.put(TestServerWebServices.PARAM_NEST_BREADTH, nestBreadth);
        queryParameters.put(TestServerWebServices.PARAM_FAIL_NEST, failNest);
        queryParameters.put(TestServerWebServices.PARAM_ASYNC, async);

        operationName = getOperationName(
                spanKind,
                HttpMethod.GET,
                RestClientServices.class,
                getEndpointMethod(RestClientServices.class, path));
        expectedTags = getExpectedSpanTags(
                spanKind,
                HttpMethod.GET,
                RestClientServices.REST_SERVICE_PATH,
                path,
                queryParameters,
                Status.OK.getStatusCode(),
                JAXRS_COMPONENT);

        return new TestSpan(
                operationName,
                expectedTags,
                Collections.emptyList());
    }
}
