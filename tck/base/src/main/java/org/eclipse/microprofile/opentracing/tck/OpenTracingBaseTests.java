/*
 * Copyright (c) 2018 Contributors to the Eclipse Foundation
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
import java.math.BigDecimal;
import java.net.URL;
import java.util.ArrayList;
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
import java.util.concurrent.atomic.AtomicInteger;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.Path;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.eclipse.microprofile.opentracing.tck.application.ApplicationUtils;
import org.eclipse.microprofile.opentracing.tck.application.TestServerWebServices;
import org.eclipse.microprofile.opentracing.tck.application.TracerWebService;
import org.eclipse.microprofile.opentracing.tck.tracer.ConsumableTree;
import org.eclipse.microprofile.opentracing.tck.tracer.TestSpan;
import org.eclipse.microprofile.opentracing.tck.tracer.TestSpanTree;
import org.eclipse.microprofile.opentracing.tck.tracer.TestSpanTree.TreeNode;
import org.eclipse.microprofile.opentracing.tck.tracer.TestTracer;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.Reporter;
import org.testng.annotations.BeforeMethod;

import io.opentracing.tag.Tags;

/**
 * @author Pavol Loffay
 */
public abstract class OpenTracingBaseTests extends Arquillian {
    public static final String JAXRS_COMPONENT = "jaxrs";
    private final AtomicInteger idCounter = new AtomicInteger(0);

    /**
     * Server app URL for the client tests.
     */
    @ArquillianResource
    protected URL deploymentURL;

    /**
     * Deploy the apps to test.
     *
     * @return the Deployed apps
     */
    public static WebArchive createDeployment() {

        WebArchive war = ShrinkWrap.create(WebArchive.class, "opentracing.war")
            .addPackages(true, OpenTracingClientBaseTests.class.getPackage())
            .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");

        return war;
    }

    /**
     * Before each test method, clear the tracer.
     *
     * In the case that a test fails, other tests may still run, and if the clearTracer call is done
     * at the end of a test, then the next test may still have old spans in its result, which would
     * both fail that test and make debugging harder.
     */
    @BeforeMethod
    protected void beforeEachTest() {
        debug("beforeEachTest calling clearTracer");
        clearTracer();
        debug("beforeEachTest clearTracer completed");
    }

    /**
     * Create web service URL.
     * @param service Web service path
     * @param relativePath Web service endpoint
     * @return Web service URL
     */
    protected String getWebServiceURL(final String service, final String relativePath) {
        return getWebServiceURL(service, relativePath, null);
    }

    /**
     * Create web service URL.
     * @param service Web service path
     * @param relativePath Web service endpoint
     * @param queryParameters Query parameters.
     * @return Web service URL
     */
    protected String getWebServiceURL(final String service, final String relativePath, Map<String, Object> queryParameters) {
        String url = ApplicationUtils
            .getWebServiceURL(deploymentURL, service, relativePath);
        if (queryParameters != null) {
            url += ApplicationUtils.getQueryString(queryParameters);
        }
        return url;
    }

    /**
     * Execute a remote web service and return the content.
     * @param service Web service path
     * @param relativePath Web service endpoint
     * @param expectedStatus Expected HTTP status.
     * @return Response
     */
    protected Response executeRemoteWebServiceRaw(final String service, final String relativePath, Status expectedStatus) {
        return executeRemoteWebServiceRaw(service, relativePath, null, expectedStatus);
    }

    /**
     * Execute a remote web service and return the content.
     * @param service Web service path
     * @param relativePath Web service endpoint
     * @param queryParameters Query parameters.
     * @param expectedStatus Expected HTTP status.
     * @return Response
     */
    protected Response executeRemoteWebServiceRaw(final String service, final String relativePath,
        Map<String, Object> queryParameters, Status expectedStatus) {
        Client client = ClientBuilder.newClient();
        String url = getWebServiceURL(service, relativePath, queryParameters);

        debug("Executing " + url);

        WebTarget target = client.target(url);
        Response response = target.request().get();
        if (response.getStatus() != expectedStatus.getStatusCode()) {
            String unexpectedResponse = response.readEntity(String.class);
            Assert.fail("Expected HTTP response code "
                + expectedStatus.getStatusCode() + " but received "
                + response.getStatus() + "; Response: "
                + unexpectedResponse);
        }
        return response;
    }

    /**
     * Execute a remote web service and return the span tree.
     * @return TestSpanTree
     */
    protected TestSpanTree executeRemoteWebServiceTracerTree() {
        TestSpanTree testSpanTree = executeRemoteWebServiceRaw(
            TracerWebService.REST_TRACER_SERVICE_PATH, TracerWebService.REST_GET_TRACER, Status.OK)
            .readEntity(TestTracer.class).spanTree();
        debug("Tracer returned " + testSpanTree);
        return testSpanTree;
    }

    /**
     * Make a web service call to clear the server's Tracer.
     */
    private void clearTracer() {
        Client client = ClientBuilder.newClient();
        String url = getWebServiceURL(TracerWebService.REST_TRACER_SERVICE_PATH,
            TracerWebService.REST_CLEAR_TRACER);
        Response delete = client.target(url).request().delete();
        delete.close();
    }

    /**
     * This wrapper method allows for potential post-processing, such as
     * removing tags that we don't care to compare in {@code returnedTree}.
     * This method removes error related keys.
     *
     * @param returnedTree The returned tree from the web service.
     * @param expectedTree The simulated tree that we expect.
     */
    protected void assertEqualTrees(ConsumableTree<TestSpan> returnedTree,
        ConsumableTree<TestSpan> expectedTree) {

        // It's okay if the returnedTree has tags other than the ones we
        // want to compare, so just remove those
        returnedTree.visitTree(span -> {
            span.getTags().keySet()
                .removeIf(key -> !key.equals(Tags.SPAN_KIND.getKey())
                    && !key.equals(Tags.HTTP_METHOD.getKey())
                    && !key.equals(Tags.HTTP_URL.getKey())
                    && !key.equals(Tags.HTTP_STATUS.getKey())
                    && !key.equals(Tags.COMPONENT.getKey())
                    && !key.equals(TestServerWebServices.LOCAL_SPAN_TAG_KEY));
        });

        // It's okay if the returnedTree has log entries other than the ones we
        // want to compare, so just remove those
        returnedTree.visitTree(span -> span.getLogEntries()
                .removeIf(logEntry -> true));
        Assert.assertEquals(returnedTree, expectedTree);
    }


    /**
     * This wrapper method allows for potential post-processing, such as
     * removing tags that we don't care to compare in {@code returnedTree}.
     * This method keeps the error related keys.
     *
     *
     * @param returnedTree The returned tree from the web service.
     * @param expectedTree The simulated tree that we expect.
     */
    protected void assertEqualErrorTrees(ConsumableTree<TestSpan> returnedTree,
        ConsumableTree<TestSpan> expectedTree) {

        // It's okay if the returnedTree has tags other than the ones we
        // want to compare, so just remove those
        returnedTree.visitTree(span -> {
            span.getTags().keySet()
                .removeIf(key -> !key.equals(Tags.SPAN_KIND.getKey())
                    && !key.equals(Tags.HTTP_METHOD.getKey())
                    && !key.equals(Tags.HTTP_URL.getKey())
                    && !key.equals(Tags.HTTP_STATUS.getKey())
                    && !key.equals(Tags.COMPONENT.getKey())
                    && !key.equals(TestServerWebServices.LOCAL_SPAN_TAG_KEY)
                    && !key.equals(Tags.ERROR.getKey()));
        });

        // It's okay if the returnedTree has log entries other than the ones we
        // want to compare, so just remove those
        returnedTree.visitTree(span -> {
            if (!Tags.SPAN_KIND_SERVER.equals(span.getTags().get(Tags.SPAN_KIND.getKey()))) {
                span.getLogEntries().forEach(stringMap -> {
                    stringMap.keySet()
                        .removeIf(key -> !key.equals("event") && !key.equals("error.object"));
                });
            }
        });

        Assert.assertEquals(returnedTree, expectedTree);
    }

    /**
     * Print debug message to target/surefire-reports/testng-results.xml.
     *
     * @param message The debug message.
     */
    static void debug(String message) {
        Reporter.log(message);
    }

    /**
     * Get a random integer.
     *
     * @return Random integer.
     */
    protected int getRandomNumber() {
        return idCounter.incrementAndGet();
    }


    /**
     * Get REST endpoint java method based on the mapping value in {@link Path} annotation.
     *
     * @param clazz class of the endpoint.
     * @param mapping endpoint mapping.
     * @return endpoint method or null if not found.
     */
    protected Method getEndpointMethod(Class<?> clazz, String mapping) {
        for (Method method: clazz.getMethods()) {
            Path methodPath = method.getAnnotation(Path.class);
            if (methodPath != null && mapping.equals(methodPath.value())) {
                return method;
            }
        }
        return null;
    }

    /**
     * Get operation name depending on the {@code spanKind}.
     * @param spanKind The type of span.
     * @param httpMethod HTTP method
     * @param clazz resource class
     * @param method method of the REST endpoint
     * @return operation name
     */
    protected String getOperationName(String spanKind, String httpMethod, Class<?> clazz, Method method) {
        if (spanKind.equals(Tags.SPAN_KIND_SERVER)) {
            return String.format("%s:%s.%s", httpMethod, clazz.getName(), method.getName());
        }
        else if (spanKind.equals(Tags.SPAN_KIND_CLIENT)) {
            return httpMethod;
        }
        else {
            throw new RuntimeException("Span kind " + spanKind + " not implemented");
        }
    }

    /**
     * Create a tags collection for expected span tags.
     * @param spanKind Value for {@link Tags#SPAN_KIND}
     * @param httpMethod Value for {@link Tags#HTTP_METHOD}
     * @param service First parameter to {@link #getWebServiceURL(String, String)}
     * @param relativePath Second parameter to {@link #getWebServiceURL(String, String)}
     * @param queryParameters Query parameters.
     * @param httpStatus Value for {@link Tags#HTTP_STATUS}
     * @return Tags collection.
     */
    protected Map<String, Object> getExpectedSpanTags(String spanKind,
        String httpMethod, String service, String relativePath,
        Map<String, Object> queryParameters, int httpStatus, String component) {

        // When adding items to this, also add to assertEqualTrees

        Map<String, Object> tags = new HashMap<>();
        tags.put(Tags.SPAN_KIND.getKey(), spanKind);
        tags.put(Tags.HTTP_METHOD.getKey(), httpMethod);
        tags.put(Tags.HTTP_URL.getKey(), getWebServiceURL(service, relativePath, queryParameters));
        tags.put(Tags.HTTP_STATUS.getKey(), new BigDecimal(httpStatus));
        tags.put(Tags.COMPONENT.getKey(), component);
        return tags;
    }

    /**
     * Create a tags collection for expected span tags with an error.
     * @param service REST service.
     * @param spanKind Value for {@link Tags#SPAN_KIND}
     * @return Tags map.
     */
    protected Map<String, Object> getExpectedSpanTagsForError(String service, String spanKind) {
        Map<String, Object> expectedTags = getExpectedSpanTags(
            spanKind,
            HttpMethod.GET,
            TestServerWebServices.REST_TEST_SERVICE_PATH,
            service,
            null,
            Status.INTERNAL_SERVER_ERROR.getStatusCode(),
            JAXRS_COMPONENT
        );
        return expectedTags;
    }

    /**
     * Do the actual testing and assertion of a nested call.
     * @param uniqueId Some unique ID.
     * @param nestDepth How deep to nest the calls.
     * @param nestBreadth Breadth of first level of nested calls.
     * @param failNest Whether to fail the nested call.
     * @param async Whether to execute nested requests asynchronously.
     */
    protected void testNestedSpans(String path, int nestDepth, int nestBreadth,
        int uniqueId, boolean failNest, boolean async) {
        executeNested(path, uniqueId, nestDepth, nestBreadth, failNest, async);

        TestSpanTree spans = executeRemoteWebServiceTracerTree();
        TestSpanTree expectedTree = new TestSpanTree(createExpectedNestTree(path, uniqueId, nestBreadth, failNest, async));

        assertEqualTrees(spans, expectedTree);
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
    protected void testMultithreadedNestedSpans(String path, int numberOfCalls, int nestDepth,
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
                    executeNested(path, uniqueId, nestDepth, nestBreadth, failNest, async);
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

            TreeNode<TestSpan> expectedTree = createExpectedNestTree(path, uniqueId, nestBreadth, failNest, async);
            assertEqualTrees(rootSpan, expectedTree);
        }
    }

    /**
     * Execute the nested web service.
     * @param uniqueId Some unique ID.
     * @param nestDepth How deep to nest the calls.
     * @param nestBreadth Breadth of first level of nested calls.
     * @param failNest Whether to fail the nested call.
     * @param async Whether to execute nested requests asynchronously.
     */
    protected void executeNested(String path, int uniqueId, int nestDepth, int nestBreadth, boolean failNest, boolean async) {
        Map<String, Object> queryParameters = getNestedQueryParameters(uniqueId,
            nestDepth, nestBreadth, failNest, async);

        Response response = executeRemoteWebServiceRaw(
            TestServerWebServices.REST_TEST_SERVICE_PATH,
            path,
            queryParameters,
            Status.OK
        );
        response.close();
    }

    /**
     * @param uniqueId Some unique ID.
     * @param nestDepth How deep to nest the calls.
     * @param nestBreadth Breadth of first level of nested calls.
     * @param failNest Whether to fail the nested call.
     * @param async Whether to execute nested requests asynchronously.
     * @return Query parameters map.
     */
    protected Map<String, Object> getNestedQueryParameters(int uniqueId,
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
     * Create the expected span tree to assert.
     * @param uniqueId Unique ID of the request.
     * @param nestBreadth Nesting breadth.
     * @param failNest Whether to fail the nested call.
     * @param async Whether to execute nested requests asynchronously.
     * @return The expected span tree.
     */
    private TreeNode<TestSpan> createExpectedNestTree(String path, int uniqueId, int nestBreadth, boolean failNest, boolean async) {
        @SuppressWarnings("unchecked")
        TreeNode<TestSpan>[] children = (TreeNode<TestSpan>[]) new TreeNode<?>[nestBreadth];
        for (int i = 0; i < nestBreadth; i++) {
            children[i] =
                new TreeNode<>(
                    getExpectedNestedServerSpan(path, Tags.SPAN_KIND_CLIENT, uniqueId, 0, 1, false, failNest, async),
                    new TreeNode<>(
                        getExpectedNestedServerSpan(path, Tags.SPAN_KIND_SERVER, uniqueId, 0, 1, false, failNest, async)
                    )
                );
        }
        return new TreeNode<>(
            getExpectedNestedServerSpan(path, Tags.SPAN_KIND_SERVER, uniqueId, 1, nestBreadth, failNest, false, async),
            children
        );
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
    protected TestSpan getExpectedNestedServerSpan(String path, String spanKind, int uniqueId,
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
                getEndpointMethod(TestServerWebServices.class, path)
            );
            expectedTags = getExpectedSpanTags(
                spanKind,
                HttpMethod.GET,
                TestServerWebServices.REST_TEST_SERVICE_PATH,
                path,
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
}
