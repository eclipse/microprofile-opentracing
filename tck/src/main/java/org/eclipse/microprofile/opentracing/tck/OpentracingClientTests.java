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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.eclipse.microprofile.opentracing.tck.application.TestServerWebServices;
import org.eclipse.microprofile.opentracing.tck.application.TestWebServicesApplication;
import org.eclipse.microprofile.opentracing.tck.application.TracerWebService;
import org.eclipse.microprofile.opentracing.tck.tracer.ConsumableTree;
import org.eclipse.microprofile.opentracing.tck.tracer.TestSpan;
import org.eclipse.microprofile.opentracing.tck.tracer.TestSpanTree;
import org.eclipse.microprofile.opentracing.tck.tracer.TestSpanTree.TreeNode;
import org.eclipse.microprofile.opentracing.tck.tracer.TestTracer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.testng.Assert;
import org.testng.Reporter;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.opentracing.tag.Tags;

/**
 * Opentracing TCK tests.
 * @author <a href="mailto:steve.m.fontes@gmail.com">Steve Fontes</a>
 */
public class OpentracingClientTests extends Arquillian {
    
    /**
     * "A stable identifier for some notable moment in the lifetime of a Span.
     * For instance, a mutex lock acquisition or release or the sorts of
     * lifetime events in a browser page load described in the
     * Performance.timing specification. E.g., from Zipkin, "cs", "sr", "ss", or
     * "cr". Or, more generally, "initialized" or "timed out". For errors,
     * "error""
     * https://github.com/opentracing/specification/blob/master/semantic_conventions.md#log-fields-table
     */
    public static final String LOG_ENTRY_NAME_EVENT = "event";
    
    /**
     * "For languages that support such a thing (e.g., Java, Python), the actual
     * Throwable/Exception/Error object instance itself. E.g., A
     * java.lang.UnsupportedOperationException instance, a python
     * exceptions.NameError instance"
     * https://github.com/opentracing/specification/blob/master/semantic_conventions.md#log-fields-table
     */
    public static final String LOG_ENTRY_NAME_ERROR_OBJECT = "error.object";

    /** Server app URL for the client tests. */
    @ArquillianResource
    private URL deploymentURL;

    /**
     * Deploy the apps to test.
     * @return the Deployed apps
     */
    @Deployment
    public static WebArchive createDeployment() {

        File[] files = Maven.resolver()
                .resolve(
                "io.opentracing:opentracing-api:0.30.0",
                "com.fasterxml.jackson.jaxrs:jackson-jaxrs-json-provider:2.9.0"
                )
                .withTransitivity().asFile();

        WebArchive war = ShrinkWrap.create(WebArchive.class, "opentracing.war")
                .addPackages(true, OpentracingClientTests.class.getPackage())
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsLibraries(files);

        return war;
    }
   
    /**
     * Before each test method, clear the tracer.
     * 
     * In the case that a test fails, other tests may still run, and if the
     * clearTracer call is done at the end of a test, then the next test may
     * still have old spans in its result, which would both fail that test
     * and make debugging harder.
     */
    @BeforeMethod
    private void beforeEachTest() {
        debug("beforeEachTest calling clearTracer");
        clearTracer();
        debug("beforeEachTest clearTracer completed");
    }

    /**
     * Test that server endpoint is adding standard tags
     */
    @Test
    @RunAsClient
    private void testStandardTags() throws InterruptedException {
        Response response = executeRemoteWebServiceRaw(TestServerWebServices.REST_TEST_SERVICE_PATH,
            TestServerWebServices.REST_SIMPLE_TEST, Status.OK);
        response.close();

        TestSpanTree spans = executeRemoteWebServiceTracer().spanTree();

        TestSpanTree expectedTree = new TestSpanTree(
            new TreeNode<>(
                new TestSpan(
                    getOperationName(
                        Tags.SPAN_KIND_SERVER,
                        HttpMethod.GET,
                        TestServerWebServices.class,
                        TestServerWebServices.REST_SIMPLE_TEST
                    ),
                    getExpectedSpanTags(
                        Tags.SPAN_KIND_SERVER,
                        HttpMethod.GET,
                        TestServerWebServices.REST_TEST_SERVICE_PATH,
                        TestServerWebServices.REST_SIMPLE_TEST,
                        null,
                        Status.OK.getStatusCode()
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
        assertErrorTest(TestServerWebServices.REST_ERROR, false);
    }

    /**
     * Test exception web service.
     */
    @Test
    @RunAsClient
    private void testException() throws InterruptedException {
        assertErrorTest(TestServerWebServices.REST_EXCEPTION, true);
    }

    /**
     * Common code for handling error and exception tests.
     * @param service The REST service path.
     * @param checkLogEntries True if an unhandled exception is thrown by the web service.
     */
    private void assertErrorTest(String service, boolean checkLogEntries) {
        Response response = executeRemoteWebServiceRaw(TestServerWebServices.REST_TEST_SERVICE_PATH,
            service, Status.INTERNAL_SERVER_ERROR);
        response.close();

        TestSpanTree spans = executeRemoteWebServiceTracer().spanTree();
        
        Map<String, Object> expectedTags = getExpectedSpanTagsForError(service, Tags.SPAN_KIND_SERVER);
        
        List<Map<String, ?>> expectedLogEntries = new ArrayList<>();

        if (checkLogEntries) {
            // The following are only added if there is an exception object:
            // https://github.com/eclipse/microprofile-opentracing/blob/master/spec/src/main/asciidoc/microprofile-opentracing-spec.asciidoc
            // https://github.com/opentracing/specification/blob/master/semantic_conventions.md#log-fields-table

            Map<String, Object> expectedLogEntry = new HashMap<>();

            expectedLogEntries.add(expectedLogEntry);

            expectedLogEntry.put(LOG_ENTRY_NAME_EVENT, "error");

            // Jackson will serialize over the Throwable in a
            // Map<String, Object>, so to create a matching one to compare to,
            // we first serialize the same exception the web service threw
            // into a String, and then deserialize that back into a
            // HashMap<String, Object>.
            // Of course, the full stacks also won't match, so we remove
            // everything but the top stack frame for both.
            
            List<TreeNode<TestSpan>> rootSpans = spans.getRootSpans();

            Assert.assertEquals(rootSpans.size(), 1);

            TreeNode<TestSpan> firstSpan = rootSpans.get(0);

            List<Map<String, ?>> firstLogEntries = firstSpan.getData()
                    .getLogEntries();

            Assert.assertEquals(firstLogEntries.size(), 1);

            Map<String, ?> firstLogEntry = firstLogEntries.get(0);

            @SuppressWarnings("unchecked")
            LinkedHashMap<String, Object> serverException = (LinkedHashMap<String, Object>) firstLogEntry
                    .get(LOG_ENTRY_NAME_ERROR_OBJECT);

            prepareDeserializedExceptionForComparison(serverException);

            ObjectMapper objectMapper = new ObjectMapper();
            LinkedHashMap<String, Object> exceptionMap;
            try {
                exceptionMap = objectMapper.readValue(
                        objectMapper
                                .writeValueAsString(TestWebServicesApplication
                                        .createExampleRuntimeException()),
                        objectMapper.getTypeFactory().constructParametricType(
                                LinkedHashMap.class, String.class,
                                Object.class));
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }

            prepareDeserializedExceptionForComparison(exceptionMap);

            expectedLogEntry.put(LOG_ENTRY_NAME_ERROR_OBJECT, exceptionMap);
        }

        TestSpanTree expectedTree = new TestSpanTree(
            new TreeNode<>(
                new TestSpan(
                    getOperationName(
                        Tags.SPAN_KIND_SERVER,
                        HttpMethod.GET,
                        TestServerWebServices.class,
                        service
                    ),
                    expectedTags,
                    expectedLogEntries
                )
            )
        );
        assertEqualTrees(spans, expectedTree);
    }

    /**
     * Exception stack traces won't match between the client and runtime.
     * Clean up the stack trace for comparison.
     * 
     * @param serverException The deserialized exception.
     */
    private void prepareDeserializedExceptionForComparison(
            LinkedHashMap<String, Object> serverException) {
        @SuppressWarnings("unchecked")
        ArrayList<Map<String, String>> stackFrames = (ArrayList<Map<String, String>>) serverException
                .get("stackTrace");

        while (stackFrames.size() > 1) {
            stackFrames.remove(stackFrames.size() - 1);
        }
    }

    /**
     * Create a tags collection for expected span tags with an error.
     * @param service REST service.
     * @param spanKind Value for {@link Tags#SPAN_KIND}
     * @return Tags map.
     */
    private Map<String, Object> getExpectedSpanTagsForError(String service, String spanKind) {
        Map<String, Object> expectedTags = getExpectedSpanTags(
            spanKind,
            HttpMethod.GET,
            TestServerWebServices.REST_TEST_SERVICE_PATH,
            service,
            null,
            Status.INTERNAL_SERVER_ERROR.getStatusCode()
        );
        
        // https://github.com/opentracing/specification/blob/master/semantic_conventions.md#span-tags-table
        expectedTags.put(Tags.ERROR.getKey(), true);
        return expectedTags;
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
        
        executeNestedSpans(nestDepth, nestBreadth, uniqueId, failNest);
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
        
        executeNestedSpans(nestDepth, nestBreadth, uniqueId, failNest);
    }

    /**
     * Do the actual testing and assertion of a nested call.
     * @param uniqueId Some unique ID.
     * @param nestDepth How deep to nest the calls.
     * @param nestBreadth Breadth of first level of nested calls.
     * @param failNest Whether to fail the nested call.
     */
    private void executeNestedSpans(int nestDepth, int nestBreadth,
            int uniqueId, boolean failNest) {
        executeNested(uniqueId, nestDepth, nestBreadth, failNest);
        
        TestSpanTree spans = executeRemoteWebServiceTracer().spanTree();
        TestSpanTree expectedTree = new TestSpanTree(createExpectedNestTree(uniqueId, nestBreadth, failNest));
        
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
        
        ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        List<Future<?>> futures = new ArrayList<>(numberOfCalls);
        Set<Integer> uniqueIds = ConcurrentHashMap.newKeySet();
        for (int i = 0; i < numberOfCalls; i++) {
            futures.add(executorService.submit(new Runnable() {
                @Override
                public void run() {
                    int uniqueId = getRandomNumber();
                    uniqueIds.add(uniqueId);
                    executeNested(uniqueId, nestDepth, nestBreadth, failNest);
                }
            }));
        }

        // wait to finish all calls
        for (Future<?> future: futures) {
            future.get();
        }

        executorService.awaitTermination(1, TimeUnit.SECONDS);
        executorService.shutdown();
        
        TestSpanTree spans = executeRemoteWebServiceTracer().spanTree();
        
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
            
            TreeNode<TestSpan> expectedTree = createExpectedNestTree(uniqueId, nestBreadth, failNest);
            assertEqualTrees(rootSpan, expectedTree);
        }
    }

    /**
     * Create the expected span tree to assert.
     * @param uniqueId Unique ID of the request.
     * @param nestBreadth Nesting breadth.
     * @param failNest Whether to fail the nested call.
     * @return The expected span tree.
     */
    private TreeNode<TestSpan> createExpectedNestTree(int uniqueId, int nestBreadth, boolean failNest) {
        @SuppressWarnings("unchecked")
        TreeNode<TestSpan>[] children = (TreeNode<TestSpan>[]) new TreeNode<?>[nestBreadth];
        for (int i = 0; i < nestBreadth; i++) {
            children[i] =
                new TreeNode<>(
                    getExpectedNestedServerSpan(Tags.SPAN_KIND_CLIENT, uniqueId, 0, 1, false, failNest),
                    new TreeNode<>(
                        getExpectedNestedServerSpan(Tags.SPAN_KIND_SERVER, uniqueId, 0, 1, false, failNest)
                    )
                );
        }
        return new TreeNode<>(
            getExpectedNestedServerSpan(Tags.SPAN_KIND_SERVER, uniqueId, 1, nestBreadth, failNest, false),
            children
        );
    }

    /**
     * Execute the nested web service.
     * @param uniqueId Some unique ID.
     * @param nestDepth How deep to nest the calls.
     * @param nestBreadth Breadth of first level of nested calls.
     * @param failNest Whether to fail the nested call.
     */
    private void executeNested(int uniqueId, int nestDepth, int nestBreadth, boolean failNest) {
        Map<String, Object> queryParameters = new HashMap<>();
        queryParameters.put(TestServerWebServices.PARAM_UNIQUE_ID, uniqueId);
        queryParameters.put(TestServerWebServices.PARAM_NEST_DEPTH, nestDepth);
        queryParameters.put(TestServerWebServices.PARAM_NEST_BREADTH, nestBreadth);
        queryParameters.put(TestServerWebServices.PARAM_FAIL_NEST, failNest);
        
        Response response = executeRemoteWebServiceRaw(
            TestServerWebServices.REST_TEST_SERVICE_PATH,
            TestServerWebServices.REST_NESTED,
            queryParameters,
            Status.OK
        );
        response.close();
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
        TestSpanTree spans = executeRemoteWebServiceTracer().spanTree();
        TestSpanTree expectedTree = new TestSpanTree(
            new TreeNode<>(
                new TestSpan(
                    getOperationName(
                        Tags.SPAN_KIND_SERVER,
                        HttpMethod.GET,
                        TestServerWebServices.class,
                        TestServerWebServices.REST_LOCAL_SPAN
                    ),
                    getExpectedSpanTags(
                        Tags.SPAN_KIND_SERVER,
                        HttpMethod.GET,
                        TestServerWebServices.REST_TEST_SERVICE_PATH,
                        TestServerWebServices.REST_LOCAL_SPAN,
                        null,
                        Status.OK.getStatusCode()
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
        TestSpanTree spans = executeRemoteWebServiceTracer().spanTree();
        TestSpanTree expectedTree = new TestSpanTree(
            new TreeNode<>(
                new TestSpan(
                    getOperationName(
                        Tags.SPAN_KIND_SERVER,
                        HttpMethod.GET,
                        TestServerWebServices.class,
                        TestServerWebServices.REST_ASYNC_LOCAL_SPAN
                    ),
                    getExpectedSpanTags(
                        Tags.SPAN_KIND_SERVER,
                        HttpMethod.GET,
                        TestServerWebServices.REST_TEST_SERVICE_PATH,
                        TestServerWebServices.REST_ASYNC_LOCAL_SPAN,
                        null,
                        Status.OK.getStatusCode()
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
     * @return Span for the nested call.
     */
    private TestSpan getExpectedNestedServerSpan(String spanKind, int uniqueId,
            int nestDepth, int nestBreadth, boolean failNest,
            boolean isFailed) {
        String operationName;
        Map<String, Object> expectedTags;
        
        if (isFailed) {
            operationName = getOperationName(
                spanKind,
                HttpMethod.GET,
                TestServerWebServices.class,
                TestServerWebServices.REST_ERROR
            );
            expectedTags = getExpectedSpanTagsForError(TestServerWebServices.REST_ERROR, spanKind);
        }
        else {
            Map<String, Object> queryParameters = new HashMap<>();
            queryParameters.put(TestServerWebServices.PARAM_UNIQUE_ID, uniqueId);
            queryParameters.put(TestServerWebServices.PARAM_NEST_DEPTH, nestDepth);
            queryParameters.put(TestServerWebServices.PARAM_NEST_BREADTH, nestBreadth);
            queryParameters.put(TestServerWebServices.PARAM_FAIL_NEST, failNest);

            operationName = getOperationName(
                spanKind,
                HttpMethod.GET,
                TestServerWebServices.class,
                TestServerWebServices.REST_NESTED
            );
            expectedTags = getExpectedSpanTags(
                spanKind,
                HttpMethod.GET,
                TestServerWebServices.REST_TEST_SERVICE_PATH,
                TestServerWebServices.REST_NESTED,
                queryParameters,
                Status.OK.getStatusCode()
            );
        }
        
        return new TestSpan(
            operationName,
            expectedTags,
            Collections.emptyList()
        );
    }

    /**
     * This wrapper method allows for potential post-processing, such as
     * removing tags that we don't care to compare in {@code returnedTree}.
     * 
     * @param returnedTree The returned tree from the web service.
     * @param expectedTree The simulated tree that we expect.
     */
    private void assertEqualTrees(ConsumableTree<TestSpan> returnedTree,
            ConsumableTree<TestSpan> expectedTree) {
        
        // It's okay if the returnedTree has tags other than the ones we
        // want to compare, so just remove those
        returnedTree.visitTree(span -> span.getTags().keySet()
                .removeIf(key -> !key.equals(Tags.SPAN_KIND.getKey())
                        && !key.equals(Tags.HTTP_METHOD.getKey())
                        && !key.equals(Tags.HTTP_URL.getKey())
                        && !key.equals(Tags.HTTP_STATUS.getKey())
                        && !key.equals(Tags.ERROR.getKey())
                        && !key.equals(TestServerWebServices.LOCAL_SPAN_TAG_KEY)));
        
        // It's okay if the returnedTree has log entries other than the ones we
        // want to compare, so just remove those
        returnedTree.visitTree(span -> span.getLogEntries()
                .removeIf(logEntry -> !logEntry.containsKey(LOG_ENTRY_NAME_EVENT)));
        
        Assert.assertEquals(returnedTree, expectedTree);
    }

    /**
     * Create a tags collection for expected span tags.
     * @param spanKind Value for {@link Tags#SPAN_KIND}
     * @param httpMethod Value for {@link Tags#HTTP_METHOD}
     * @param service First parameter to {@link #getWebServiceURL(String, String)
     * @param relativePath Second parameter to {@link #getWebServiceURL(String, String)
     * @param queryParameters Query parameters.
     * @param httpStatus Value for {@link Tags#HTTP_STATUS}
     * @return Tags collection.
     */
    private Map<String, Object> getExpectedSpanTags(String spanKind,
            String httpMethod, String service, String relativePath,
            Map<String, Object> queryParameters, int httpStatus) {
        
        // When adding items to this, also add to assertEqualTrees
        
        Map<String, Object> tags = new HashMap<>();
        tags.put(Tags.SPAN_KIND.getKey(), spanKind);
        tags.put(Tags.HTTP_METHOD.getKey(), httpMethod);
        tags.put(Tags.HTTP_URL.getKey(), getWebServiceURL(service, relativePath, queryParameters));
        tags.put(Tags.HTTP_STATUS.getKey(), httpStatus);
        return tags;
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
    
    /**
     * Create web service URL.
     * @param service Web service path
     * @param relativePath Web service endpoint
     * @return Web service URL
     */
    private String getWebServiceURL(final String service, final String relativePath) {
        return getWebServiceURL(service, relativePath, null);
    }
    
    /**
     * Create web service URL.
     * @param service Web service path
     * @param relativePath Web service endpoint
     * @param queryParameters Query parameters.
     * @return Web service URL
     */
    private String getWebServiceURL(final String service, final String relativePath, Map<String, Object> queryParameters) {
        String url = TestWebServicesApplication.getWebServiceURL(deploymentURL, service, relativePath);
        if (queryParameters != null) {
            url += TestWebServicesApplication.getQueryString(queryParameters);
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
    private Response executeRemoteWebServiceRaw(final String service, final String relativePath, Status expectedStatus) {
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
    private Response executeRemoteWebServiceRaw(final String service, final String relativePath,
            Map<String, Object> queryParameters, Status expectedStatus) {
        Client client = ClientBuilder.newClient();
        String url = getWebServiceURL(service, relativePath, queryParameters);
        
        debug("Executing " + url);
        
        WebTarget target = client.target(url);
        Response response = target.request().get();
        Assert.assertEquals(response.getStatus(), expectedStatus.getStatusCode());
        return response;
    }

    /**
     * Execute a remote web service and return the Tracer.
     * @return Tracer
     */
    private TestTracer executeRemoteWebServiceTracer() {
        return executeRemoteWebServiceRaw(
                TracerWebService.REST_TRACER_SERVICE_PATH, TracerWebService.REST_GET_TRACER, Status.OK)
                .readEntity(TestTracer.class);
    }

    /**
     * Get operation name depending on the {@code spanKind}.
     * https://github.com/eclipse/microprofile-opentracing/blob/master/spec/src/main/asciidoc/microprofile-opentracing-spec.asciidoc
     * @param spanKind The type of span.
     * @param httpMethod HTTP method
     * @param clazz resource class
     * @param javaMethod method name
     * @return operation name
     */
    private String getOperationName(String spanKind, String httpMethod, Class<?> clazz, String javaMethod) {
        if (spanKind.equals(Tags.SPAN_KIND_SERVER)) {
            return String.format("%s:%s.%s", httpMethod, clazz.getName(), javaMethod);
        }
        else if (spanKind.equals(Tags.SPAN_KIND_CLIENT)) {
            return httpMethod;
        }
        else {
            throw new RuntimeException("Span kind " + spanKind + " not implemented");
        }
    }

    /**
     * Make a web service call to clear the server's Tracer.
     */
    private void clearTracer() {
        Client client = ClientBuilder.newClient();
        String url = getWebServiceURL(TracerWebService.REST_TRACER_SERVICE_PATH, TracerWebService.REST_CLEAR_TRACER);
        Response delete = client.target(url).request().delete();
        delete.close();
    }
    
    /**
     * Print debug message to target/surefire-reports/testng-results.xml.
     * @param message The debug message.
     */
    private static void debug(String message) {
        Reporter.log(message);
    }

    /**
     * Get a random integer.
     * @return Random integer.
     */
    private int getRandomNumber() {
        int uniqueId = ThreadLocalRandom.current().nextInt(1, 999999);
        return uniqueId;
    }
}
