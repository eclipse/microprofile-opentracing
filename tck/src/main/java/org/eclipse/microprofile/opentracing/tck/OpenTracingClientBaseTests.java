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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.eclipse.microprofile.opentracing.tck.application.TestAnnotatedClass;
import org.eclipse.microprofile.opentracing.tck.application.TestAnnotatedClassWithOperationName;
import org.eclipse.microprofile.opentracing.tck.application.TestAnnotatedMethods;
import org.eclipse.microprofile.opentracing.tck.application.TestDisabledAnnotatedClass;
import org.eclipse.microprofile.opentracing.tck.application.TestServerWebServices;
import org.eclipse.microprofile.opentracing.tck.application.TestServerWebServicesWithOperationName;
import org.eclipse.microprofile.opentracing.tck.tracer.TestSpan;
import org.eclipse.microprofile.opentracing.tck.tracer.TestSpanTree;
import org.eclipse.microprofile.opentracing.tck.tracer.TestSpanTree.TreeNode;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.testng.Assert;
import org.testng.annotations.Test;

import io.opentracing.tag.Tags;

/**
 * Opentracing TCK tests.
 * @author <a href="mailto:steve.m.fontes@gmail.com">Steve Fontes</a>
 */
public abstract class OpenTracingClientBaseTests extends OpenTracingBaseTests {

    /**
     * Test that server endpoint is adding standard tags
     */
    @Test
    @RunAsClient
    private void testStandardTags() throws InterruptedException {
        Response response = executeRemoteWebServiceRaw(TestServerWebServices.REST_TEST_SERVICE_PATH,
            TestServerWebServices.REST_SIMPLE_TEST, Status.OK);
        response.close();

        TestSpanTree spans = executeRemoteWebServiceTracerTree();

        TestSpanTree expectedTree = new TestSpanTree(
            new TreeNode<>(
                new TestSpan(
                    getOperationName(
                        Tags.SPAN_KIND_SERVER,
                        HttpMethod.GET,
                        TestServerWebServices.class,
                        getEndpointMethod(TestServerWebServices.class, TestServerWebServices.REST_SIMPLE_TEST)
                    ),
                    getExpectedSpanTags(
                        Tags.SPAN_KIND_SERVER,
                        HttpMethod.GET,
                        TestServerWebServices.REST_TEST_SERVICE_PATH,
                        TestServerWebServices.REST_SIMPLE_TEST,
                        null,
                        Status.OK.getStatusCode(),
                        JAXRS_COMPONENT
                    ),
                    Collections.emptyList()
                )
            )
        );
        assertEqualTrees(spans, expectedTree);
    }

    /**
     * Test various Traced annotations.
     * @throws InterruptedException Error executing web service.
     */
    @Test
    @RunAsClient
    private void testAnnotations() throws InterruptedException {
        Response response = executeRemoteWebServiceRaw(TestServerWebServices.REST_TEST_SERVICE_PATH,
            TestServerWebServices.REST_ANNOTATIONS, Status.OK);
        response.close();

        TestSpanTree spans = executeRemoteWebServiceTracerTree();

        TestSpanTree expectedTree = new TestSpanTree(
            new TreeNode<>(
                new TestSpan(
                    getOperationName(
                        Tags.SPAN_KIND_SERVER,
                        HttpMethod.GET,
                        TestServerWebServices.class,
                        getEndpointMethod(TestServerWebServices.class, TestServerWebServices.REST_ANNOTATIONS)
                    ),
                    getExpectedSpanTags(
                        Tags.SPAN_KIND_SERVER,
                        HttpMethod.GET,
                        TestServerWebServices.REST_TEST_SERVICE_PATH,
                        TestServerWebServices.REST_ANNOTATIONS,
                        null,
                        Status.OK.getStatusCode(),
                        JAXRS_COMPONENT
                    ),
                    Collections.emptyList()
                ),
                new TreeNode<>(
                    new TestSpan(
                        TestAnnotatedClass.class.getName() + ".annotatedClassMethodImplicitlyTraced",
                        Collections.emptyMap(),
                        Collections.emptyList()
                    )
                ),
                new TreeNode<>(
                    new TestSpan(
                        "explicitOperationName1",
                        Collections.emptyMap(),
                        Collections.emptyList()
                    )
                ),
                new TreeNode<>(
                    new TestSpan(
                        TestAnnotatedMethods.class.getName() + ".annotatedMethodExplicitlyTraced",
                        Collections.emptyMap(),
                        Collections.emptyList()
                    )
                ),
                new TreeNode<>(
                    new TestSpan(
                        "explicitOperationName2",
                        Collections.emptyMap(),
                        Collections.emptyList()
                    )
                ),
                new TreeNode<>(
                    new TestSpan(
                        TestDisabledAnnotatedClass.class.getName() + ".annotatedClassMethodExplicitlyTraced",
                        Collections.emptyMap(),
                        Collections.emptyList()
                    )
                ),
                new TreeNode<>(
                    new TestSpan(
                        "explicitOperationName3",
                        Collections.emptyMap(),
                        Collections.emptyList()
                    )
                )
            )
        );
        assertEqualTrees(spans, expectedTree);
    }

    /**
     * Test a web service endpoint that shouldn't create a span.
     * @throws InterruptedException Error executing web service.
     */
    @Test
    @RunAsClient
    private void testNotTraced() throws InterruptedException {
        Response response = executeRemoteWebServiceRaw(TestServerWebServices.REST_TEST_SERVICE_PATH,
            TestServerWebServices.REST_NOT_TRACED, Status.OK);
        response.close();

        TestSpanTree spans = executeRemoteWebServiceTracerTree();

        TestSpanTree expectedTree = new TestSpanTree();

        assertEqualTrees(spans, expectedTree);
    }

    /**
     * Test web service with an explicit operation name.
     * @throws InterruptedException Error executing web service.
     */
    @Test
    @RunAsClient
    private void testOperationName() throws InterruptedException {
        Response response = executeRemoteWebServiceRaw(TestServerWebServices.REST_TEST_SERVICE_PATH,
            TestServerWebServices.REST_OPERATION_NAME, Status.OK);
        response.close();

        TestSpanTree spans = executeRemoteWebServiceTracerTree();

        TestSpanTree expectedTree = new TestSpanTree(
            new TreeNode<>(
                new TestSpan(
                    TestServerWebServices.REST_OPERATION_NAME,
                    getExpectedSpanTags(
                        Tags.SPAN_KIND_SERVER,
                        HttpMethod.GET,
                        TestServerWebServices.REST_TEST_SERVICE_PATH,
                        TestServerWebServices.REST_OPERATION_NAME,
                        null,
                        Status.OK.getStatusCode(),
                        JAXRS_COMPONENT
                    ),
                    Collections.emptyList()
                )
            )
        );
        assertEqualTrees(spans, expectedTree);
    }

    /**
     * Test class annotated with an operationName.
     * @throws InterruptedException Error executing web service.
     */
    @Test
    @RunAsClient
    private void testClassOperationName() throws InterruptedException {
        Response response = executeRemoteWebServiceRaw(TestServerWebServicesWithOperationName.REST_TEST_SERVICE_PATH_WITH_OP_NAME,
                TestServerWebServicesWithOperationName.REST_OPERATION_CLASS_OP_NAME, Status.OK);
        response.close();

        TestSpanTree spans = executeRemoteWebServiceTracerTree();

        TestSpanTree expectedTree = new TestSpanTree(
            new TreeNode<>(
                new TestSpan(
                    TestServerWebServicesWithOperationName.CLASS_OPERATION_NAME,
                    getExpectedSpanTags(
                        Tags.SPAN_KIND_SERVER,
                        HttpMethod.GET,
                        TestServerWebServicesWithOperationName.REST_TEST_SERVICE_PATH_WITH_OP_NAME,
                        TestServerWebServicesWithOperationName.REST_OPERATION_CLASS_OP_NAME,
                        null,
                        Status.OK.getStatusCode(),
                        JAXRS_COMPONENT
                    ),
                    Collections.emptyList()
                ),
                new TreeNode<>(
                    new TestSpan(
                        TestAnnotatedClassWithOperationName.OPERATION_NAME,
                        Collections.emptyMap(),
                        Collections.emptyList()
                    )
                ),
                new TreeNode<>(
                    new TestSpan(
                        "explicitOperationName4",
                        Collections.emptyMap(),
                        Collections.emptyList()
                    )
                )
            )
        );
        assertEqualTrees(spans, expectedTree);
    }

    /**
     * Test class and endpoint annotated with an operationName.
     * @throws InterruptedException Error executing web service.
     */
    @Test
    @RunAsClient
    private void testClassAndMethodOperationName() throws InterruptedException {
        Response response = executeRemoteWebServiceRaw(TestServerWebServicesWithOperationName.REST_TEST_SERVICE_PATH_WITH_OP_NAME,
                TestServerWebServicesWithOperationName.REST_OPERATION_CLASS_AND_METHOD_OP_NAME, Status.OK);
        response.close();

        TestSpanTree spans = executeRemoteWebServiceTracerTree();

        TestSpanTree expectedTree = new TestSpanTree(
            new TreeNode<>(
                new TestSpan(
                    TestServerWebServicesWithOperationName.ENDPOINT_OPERATION_NAME,
                    getExpectedSpanTags(
                        Tags.SPAN_KIND_SERVER,
                        HttpMethod.GET,
                        TestServerWebServicesWithOperationName.REST_TEST_SERVICE_PATH_WITH_OP_NAME,
                        TestServerWebServicesWithOperationName.REST_OPERATION_CLASS_AND_METHOD_OP_NAME,
                        null,
                        Status.OK.getStatusCode(),
                        JAXRS_COMPONENT
                    ),
                    Collections.emptyList()
                )
            )
        );
        assertEqualTrees(spans, expectedTree);
    }

    /**
     * Test error web service.
     */
    @Test
    @RunAsClient
    private void testError() throws InterruptedException {
        assertErrorTest(getEndpointMethod(TestServerWebServices.class, TestServerWebServices.REST_ERROR));
    }

    /**
     * Test exception web service.
     */
    @Test
    @RunAsClient
    private void testException() throws InterruptedException {
        assertErrorTest(getEndpointMethod(TestServerWebServices.class, TestServerWebServices.REST_EXCEPTION));
    }

    /**
     * Common code for handling error and exception tests.
     * @param method method of the REST endpoint.
     */
    private void assertErrorTest(Method method) {
        String path = method.getAnnotation(Path.class).value();
        Response response = executeRemoteWebServiceRaw(TestServerWebServices.REST_TEST_SERVICE_PATH,
            path, Status.INTERNAL_SERVER_ERROR);
        response.close();

        TestSpanTree spans = executeRemoteWebServiceTracerTree();

        Map<String, Object> expectedTags = getExpectedSpanTagsForError(path, Tags.SPAN_KIND_SERVER);

        TestSpanTree expectedTree = new TestSpanTree(
            new TreeNode<>(
                new TestSpan(
                    getOperationName(
                        Tags.SPAN_KIND_SERVER,
                        HttpMethod.GET,
                        TestServerWebServices.class,
                        method
                    ),
                    expectedTags,
                    Collections.emptyList()
                )
            )
        );
        assertEqualTrees(spans, expectedTree);
    }

    /**
     * Test annotation exception web service.
     */
    @Test
    @RunAsClient
    private void testAnnotationException() throws InterruptedException {
        Response response = executeRemoteWebServiceRaw(TestServerWebServices.REST_TEST_SERVICE_PATH,
                TestServerWebServices.REST_ANNOTATION_EXCEPTION, Status.OK);
        response.close();

        TestSpanTree spans = executeRemoteWebServiceTracerTree();

        Map<String, Object> tags = new HashMap<>();
        tags.put(Tags.ERROR.getKey(), true);
        Map<String, Object> logs = new HashMap<>();
        logs.put("event", Tags.ERROR.getKey());
        logs.put("error.object", new RuntimeException());

        TestSpanTree expectedTree = new TestSpanTree(
            new TreeNode<>(
                new TestSpan(
                    getOperationName(
                        Tags.SPAN_KIND_SERVER,
                        HttpMethod.GET,
                        TestServerWebServices.class,
                        getEndpointMethod(TestServerWebServices.class, TestServerWebServices.REST_ANNOTATION_EXCEPTION)
                    ),
                    getExpectedSpanTags(
                        Tags.SPAN_KIND_SERVER,
                        HttpMethod.GET,
                        TestServerWebServices.REST_TEST_SERVICE_PATH,
                        TestServerWebServices.REST_ANNOTATION_EXCEPTION,
                        null,
                        Status.OK.getStatusCode(),
                        JAXRS_COMPONENT
                    ),
                    Collections.emptyList()
                ),
                new TreeNode<>(
                    new TestSpan(
                        TestAnnotatedClass.class.getName() + ".annotatedClassMethodImplicitlyTracedWithException",
                        tags,
                        Arrays.asList(logs)
                    )
                )
            )
        );
        assertEqualTrees(spans, expectedTree);
    }

    /**
     * Test a web service call that makes nested calls.
     */
    @Test
    @RunAsClient
    private void testNestedSpans() throws InterruptedException {

        int nestDepth = 1;
        int nestBreadth = 2;
        int uniqueId = getRandomNumber();
        boolean failNest = false;
        boolean async = false;

        executeNestedSpans(nestDepth, nestBreadth, uniqueId, failNest, async);
    }

    /**
     * Test a web service call that makes nested calls with a client failure.
     */
    @Test
    @RunAsClient
    private void testNestedSpansWithClientFailure() throws InterruptedException {

        int nestDepth = 1;
        int nestBreadth = 2;
        int uniqueId = getRandomNumber();
        boolean failNest = true;
        boolean async = false;

        executeNestedSpans(nestDepth, nestBreadth, uniqueId, failNest, async);
    }

    /**
     * Do the actual testing and assertion of a nested call.
     * @param uniqueId Some unique ID.
     * @param nestDepth How deep to nest the calls.
     * @param nestBreadth Breadth of first level of nested calls.
     * @param failNest Whether to fail the nested call.
     * @param async Whether to execute nested requests asynchronously.
     */
    private void executeNestedSpans(int nestDepth, int nestBreadth,
            int uniqueId, boolean failNest, boolean async) {
        executeNested(uniqueId, nestDepth, nestBreadth, failNest, async);

        TestSpanTree spans = executeRemoteWebServiceTracerTree();
        TestSpanTree expectedTree = new TestSpanTree(createExpectedNestTree(uniqueId, nestBreadth, failNest, async));

        assertEqualTrees(spans, expectedTree);
    }

    /**
     * Test the nested web service concurrently. A unique ID is generated
     * in the URL of each request and propagated down the nested spans.
     * We extract this out of the resulting spans and ensure the unique
     * IDs are correct.
     * @throws InterruptedException Problem executing web service.
     * @throws ExecutionException Thread pool problem.
     */
    @Test
    @RunAsClient
    private void testMultithreadedNestedSpans() throws InterruptedException, ExecutionException {
        int numberOfCalls = 100;
        int nestDepth = 1;
        int nestBreadth = 2;
        boolean failNest = false;
        boolean async = false;

        runNestedTests(numberOfCalls, nestDepth, nestBreadth, failNest, async);
    }

    /**
     * Same as testMultithreadedNestedSpans but asynchronous client and nested requests.
     * @throws InterruptedException Problem executing web service.
     * @throws ExecutionException Thread pool problem.
     */
    @Test
    @RunAsClient
    private void testMultithreadedNestedSpansAsync() throws InterruptedException, ExecutionException {
        int numberOfCalls = 100;
        int nestDepth = 1;
        int nestBreadth = 2;
        boolean failNest = false;
        boolean async = true;

        runNestedTests(numberOfCalls, nestDepth, nestBreadth, failNest, async);
    }

    /**
     * @param numberOfCalls Number of total web requests.
     * @param nestDepth How deep to nest the calls.
     * @param nestBreadth Breadth of first level of nested calls.
     * @param failNest Whether to fail the nested call.
     * @param async Whether to execute nested requests asynchronously.
     * @throws InterruptedException Problem executing web service.
     * @throws ExecutionException Thread pool problem.
     */
    private void runNestedTests(int numberOfCalls, int nestDepth,
            int nestBreadth, boolean failNest, boolean async)
            throws InterruptedException, ExecutionException {
        int processors = Runtime.getRuntime().availableProcessors();
        ExecutorService executorService = Executors.newFixedThreadPool(processors);
        List<Future<?>> futures = new ArrayList<>(numberOfCalls);
        Set<Integer> uniqueIds = ConcurrentHashMap.newKeySet();
        for (int i = 0; i < numberOfCalls; i++) {
            futures.add(executorService.submit(new Runnable() {
                @Override
                public void run() {
                    int uniqueId = getRandomNumber();
                    uniqueIds.add(uniqueId);
                    if (async) {
                        Response response;
                        try {
                            response = executeNestedAsync(uniqueId, nestDepth, nestBreadth, failNest, async).get();
                        }
                        catch (InterruptedException|ExecutionException e) {
                            Assert.fail();
                            throw new RuntimeException(e);
                        }
                        assertResponseStatus(Status.OK, response);
                        response.close();
                    }
                    else {
                        executeNested(uniqueId, nestDepth, nestBreadth, failNest, async);
                    }
                }
            }));
        }

        // wait to finish all calls
        for (Future<?> future: futures) {
            future.get();
        }

        executorService.awaitTermination(1, TimeUnit.SECONDS);
        executorService.shutdown();

        TestSpanTree spans = executeRemoteWebServiceTracerTree();

        List<TreeNode<TestSpan>> rootSpans = spans.getRootSpans();

        // If this assertion fails, it means that the number of returned
        // root spans doesn't equal the number of web service calls.
        Assert.assertEquals(rootSpans.size(), numberOfCalls);

        for (TreeNode<TestSpan> rootSpan: rootSpans) {

            // Extract the unique ID from the root span's URL:
            String url = (String) rootSpan.getData().getTags().get(Tags.HTTP_URL.getKey());
            int i = url.indexOf(TestServerWebServices.PARAM_UNIQUE_ID);
            Assert.assertNotEquals(i, -1);
            String uniqueIdStr = url.substring(i + TestServerWebServices.PARAM_UNIQUE_ID.length() + 1);
            i = uniqueIdStr.indexOf('&');
            if (i != -1) {
                uniqueIdStr = uniqueIdStr.substring(0, i);
            }
            int uniqueId = Integer.parseInt(uniqueIdStr);

            // If this assertion fails, it means that the unique ID
            // in the root span URL doesn't match any of the
            // unique IDs that we sent in the requests above.
            boolean removeResult = uniqueIds.remove(uniqueId);

            if (!removeResult) {
                debug("Unique ID " + uniqueId + " not found in request list. Span: " + rootSpan);
            }

            Assert.assertTrue(removeResult);

            TreeNode<TestSpan> expectedTree = createExpectedNestTree(uniqueId, nestBreadth, failNest, async);
            assertEqualTrees(rootSpan, expectedTree);
        }
    }

    /**
     * Create the expected span tree to assert.
     * @param uniqueId Unique ID of the request.
     * @param nestBreadth Nesting breadth.
     * @param failNest Whether to fail the nested call.
     * @param async Whether to execute nested requests asynchronously.
     * @return The expected span tree.
     */
    private TreeNode<TestSpan> createExpectedNestTree(int uniqueId, int nestBreadth, boolean failNest, boolean async) {
        @SuppressWarnings("unchecked")
        TreeNode<TestSpan>[] children = (TreeNode<TestSpan>[]) new TreeNode<?>[nestBreadth];
        for (int i = 0; i < nestBreadth; i++) {
            children[i] =
                new TreeNode<>(
                    getExpectedNestedServerSpan(Tags.SPAN_KIND_CLIENT, uniqueId, 0, 1, false, failNest, false),
                    new TreeNode<>(
                        getExpectedNestedServerSpan(Tags.SPAN_KIND_SERVER, uniqueId, 0, 1, false, failNest, false)
                    )
                );
        }
        return new TreeNode<>(
            getExpectedNestedServerSpan(Tags.SPAN_KIND_SERVER, uniqueId, 1, nestBreadth, failNest, false, async),
            children
        );
    }

    /**
     * Execute the nested web service.
     * @param uniqueId Some unique ID.
     * @param nestDepth How deep to nest the calls.
     * @param nestBreadth Breadth of first level of nested calls.
     * @param failNest Whether to fail the nested call.
     * @param async Whether to execute nested requests asynchronously.
     */
    private void executeNested(int uniqueId, int nestDepth, int nestBreadth, boolean failNest, boolean async) {
        Map<String, Object> queryParameters = getNestedQueryParameters(uniqueId,
                nestDepth, nestBreadth, failNest, async);

        Response response = executeRemoteWebServiceRaw(
            TestServerWebServices.REST_TEST_SERVICE_PATH,
            TestServerWebServices.REST_NESTED,
            queryParameters,
            Status.OK
        );
        response.close();
    }


    /**
     * Execute the nested web service.
     * @param uniqueId Some unique ID.
     * @param nestDepth How deep to nest the calls.
     * @param nestBreadth Breadth of first level of nested calls.
     * @param failNest Whether to fail the nested call.
     * @param async Whether to execute nested requests asynchronously.
     * @return A future for the Response
     */
    private Future<Response> executeNestedAsync(int uniqueId, int nestDepth, int nestBreadth, boolean failNest, boolean async) {
        Map<String, Object> queryParameters = getNestedQueryParameters(uniqueId,
                nestDepth, nestBreadth, failNest, async);

        return executeRemoteWebServiceRawAsync(
            TestServerWebServices.REST_TEST_SERVICE_PATH,
            TestServerWebServices.REST_NESTED,
            queryParameters,
            Status.OK
        );
    }

    /**
     * @param uniqueId Some unique ID.
     * @param nestDepth How deep to nest the calls.
     * @param nestBreadth Breadth of first level of nested calls.
     * @param failNest Whether to fail the nested call.
     * @param async Whether to execute nested requests asynchronously.
     * @return Query parameters map.
     */
    private Map<String, Object> getNestedQueryParameters(int uniqueId,
            int nestDepth, int nestBreadth, boolean failNest, boolean async) {
        Map<String, Object> queryParameters = new HashMap<>();
        queryParameters.put(TestServerWebServices.PARAM_UNIQUE_ID, uniqueId);
        queryParameters.put(TestServerWebServices.PARAM_NEST_DEPTH, nestDepth);
        queryParameters.put(TestServerWebServices.PARAM_NEST_BREADTH, nestBreadth);
        queryParameters.put(TestServerWebServices.PARAM_FAIL_NEST, failNest);
        queryParameters.put(TestServerWebServices.PARAM_ASYNC, async);
        return queryParameters;
    }

    /**
     * Test that implementation exposes active span
     */
    @Test
    @RunAsClient
    private void testLocalSpanHasParent() throws InterruptedException {
        Response response = executeRemoteWebServiceRaw(TestServerWebServices.REST_TEST_SERVICE_PATH,
            TestServerWebServices.REST_LOCAL_SPAN, Status.OK);
        response.close();
        TestSpanTree spans = executeRemoteWebServiceTracerTree();
        TestSpanTree expectedTree = new TestSpanTree(
            new TreeNode<>(
                new TestSpan(
                    getOperationName(
                        Tags.SPAN_KIND_SERVER,
                        HttpMethod.GET,
                        TestServerWebServices.class,
                        getEndpointMethod(TestServerWebServices.class, TestServerWebServices.REST_LOCAL_SPAN)
                    ),
                    getExpectedSpanTags(
                        Tags.SPAN_KIND_SERVER,
                        HttpMethod.GET,
                        TestServerWebServices.REST_TEST_SERVICE_PATH,
                        TestServerWebServices.REST_LOCAL_SPAN,
                        null,
                        Status.OK.getStatusCode(),
                        JAXRS_COMPONENT
                    ),
                    Collections.emptyList()
                ),
                new TreeNode<>(
                    new TestSpan(
                        TestServerWebServices.REST_LOCAL_SPAN,
                        getExpectedLocalSpanTags(),
                        Collections.emptyList()
                    )
                )
            )
        );
        assertEqualTrees(spans, expectedTree);
    }

    /**
     * Test that async endpoint exposes active span
     */
    @Test
    @RunAsClient
    private void testAsyncLocalSpan() throws InterruptedException {
        Response response = executeRemoteWebServiceRaw(TestServerWebServices.REST_TEST_SERVICE_PATH,
            TestServerWebServices.REST_ASYNC_LOCAL_SPAN, Status.OK);
        response.close();
        TestSpanTree spans = executeRemoteWebServiceTracerTree();
        TestSpanTree expectedTree = new TestSpanTree(
            new TreeNode<>(
                new TestSpan(
                    getOperationName(
                        Tags.SPAN_KIND_SERVER,
                        HttpMethod.GET,
                        TestServerWebServices.class,
                        getEndpointMethod(TestServerWebServices.class, TestServerWebServices.REST_ASYNC_LOCAL_SPAN)
                    ),
                    getExpectedSpanTags(
                        Tags.SPAN_KIND_SERVER,
                        HttpMethod.GET,
                        TestServerWebServices.REST_TEST_SERVICE_PATH,
                        TestServerWebServices.REST_ASYNC_LOCAL_SPAN,
                        null,
                        Status.OK.getStatusCode(),
                        JAXRS_COMPONENT
                    ),
                    Collections.emptyList()
                ),
                new TreeNode<>(
                    new TestSpan(
                        TestServerWebServices.REST_LOCAL_SPAN,
                        getExpectedLocalSpanTags(),
                        Collections.emptyList()
                    )
                )
            )
        );
        assertEqualTrees(spans, expectedTree);
    }

    /**
     * The expected nested span layout.
     * @param spanKind Span kind
     * @param uniqueId The unique ID of the request.
     * @param nestDepth Nest depth
     * @param nestBreadth Nest breadth
     * @param failNest Whether to fail the nested call.
     * @param isFailed Whether this request is expected to fail.
     * @param async Whether to execute asynchronously.
     * @return Span for the nested call.
     */
    private TestSpan getExpectedNestedServerSpan(String spanKind, int uniqueId,
            int nestDepth, int nestBreadth, boolean failNest,
            boolean isFailed, boolean async) {
        String operationName;
        Map<String, Object> expectedTags;

        if (isFailed) {
            operationName = getOperationName(
                spanKind,
                HttpMethod.GET,
                TestServerWebServices.class,
                getEndpointMethod(TestServerWebServices.class, TestServerWebServices.REST_ERROR)
            );
            expectedTags = getExpectedSpanTagsForError(TestServerWebServices.REST_ERROR, spanKind);
        }
        else {
            Map<String, Object> queryParameters = new HashMap<>();
            queryParameters.put(TestServerWebServices.PARAM_UNIQUE_ID, uniqueId);
            queryParameters.put(TestServerWebServices.PARAM_NEST_DEPTH, nestDepth);
            queryParameters.put(TestServerWebServices.PARAM_NEST_BREADTH, nestBreadth);
            queryParameters.put(TestServerWebServices.PARAM_FAIL_NEST, failNest);
            queryParameters.put(TestServerWebServices.PARAM_ASYNC, async);

            operationName = getOperationName(
                spanKind,
                HttpMethod.GET,
                TestServerWebServices.class,
                getEndpointMethod(TestServerWebServices.class, TestServerWebServices.REST_NESTED)
            );
            expectedTags = getExpectedSpanTags(
                spanKind,
                HttpMethod.GET,
                TestServerWebServices.REST_TEST_SERVICE_PATH,
                TestServerWebServices.REST_NESTED,
                queryParameters,
                Status.OK.getStatusCode(),
                JAXRS_COMPONENT
            );
        }

        return new TestSpan(
            operationName,
            expectedTags,
            Collections.emptyList()
        );
    }


    /**
     * Create a tags collection for expected span tags of a local span.
     * @return Tags collection.
     */
    private Map<String, Object> getExpectedLocalSpanTags() {

        // When adding items to this, also add to assertEqualTrees

        Map<String, Object> tags = new HashMap<>();
        tags.put(TestServerWebServices.LOCAL_SPAN_TAG_KEY, TestServerWebServices.LOCAL_SPAN_TAG_VALUE);
        return tags;
    }

}
