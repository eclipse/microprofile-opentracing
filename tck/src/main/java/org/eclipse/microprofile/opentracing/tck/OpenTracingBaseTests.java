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

import io.opentracing.tag.Tags;
import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
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
import org.eclipse.microprofile.opentracing.tck.tracer.TestTracer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.testng.Assert;
import org.testng.Reporter;
import org.testng.annotations.BeforeMethod;

/**
 * @author Pavol Loffay
 */
abstract class OpenTracingBaseTests extends Arquillian {
    static final String JAXRS_COMPONENT = "jaxrs";
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
    @Deployment
    public static WebArchive createDeployment() {

        File[] files = Maven.resolver()
            .resolve(
                "io.opentracing:opentracing-api:0.31.0",
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
        String url = TestWebServicesApplication
            .getWebServiceURL(deploymentURL, service, relativePath);
        if (queryParameters != null) {
            url += TestWebServicesApplication.getQueryString(queryParameters);
        }
        return url;
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
     * Assert the response status equals the expected status.
     * @param expectedStatus Expected status code.
     * @param response The response object.
     */
    protected void assertResponseStatus(Status expectedStatus,
        Response response) {
        Assert.assertEquals(response.getStatus(), expectedStatus.getStatusCode());
    }

    /**
     * Execute a remote web service asynchronously and return a wrapper around a Future to the Response
     * @param service Web service path
     * @param relativePath Web service endpoint
     * @param queryParameters Query parameters.
     * @param expectedStatus Expected HTTP status.
     * @return Future for a Response
     */
    protected Future<Response> executeRemoteWebServiceRawAsync(final String service, final String relativePath,
        Map<String, Object> queryParameters, Status expectedStatus) {
        Client client = ClientBuilder.newClient();
        String url = getWebServiceURL(service, relativePath, queryParameters);

        debug("Executing " + url);

        WebTarget target = client.target(url);
        return target.request().async().get();
    }

    /**
     * Execute a remote web service and return the Tracer.
     * @return Tracer
     */
    protected TestTracer executeRemoteWebServiceTracer() {
        return executeRemoteWebServiceRaw(
            TracerWebService.REST_TRACER_SERVICE_PATH, TracerWebService.REST_GET_TRACER, Status.OK)
            .readEntity(TestTracer.class);
    }

    /**
     * This wrapper method allows for potential post-processing, such as
     * removing tags that we don't care to compare in {@code returnedTree}.
     *
     * @param returnedTree The returned tree from the web service.
     * @param expectedTree The simulated tree that we expect.
     */
    protected void assertEqualTrees(ConsumableTree<TestSpan> returnedTree,
        ConsumableTree<TestSpan> expectedTree) {

        // It's okay if the returnedTree has tags other than the ones we
        // want to compare, so just remove those
        returnedTree.visitTree(span -> span.getTags().keySet()
            .removeIf(key -> !key.equals(Tags.SPAN_KIND.getKey())
                && !key.equals(Tags.HTTP_METHOD.getKey())
                && !key.equals(Tags.HTTP_URL.getKey())
                && !key.equals(Tags.HTTP_STATUS.getKey())
                && !key.equals(Tags.COMPONENT.getKey())
                && !key.equals(TestServerWebServices.LOCAL_SPAN_TAG_KEY)));

        // It's okay if the returnedTree has log entries other than the ones we
        // want to compare, so just remove those
        returnedTree.visitTree(span -> span.getLogEntries()
            .removeIf(logEntry -> true));

        Assert.assertEquals(returnedTree, expectedTree);
    }

    /**
     * Execute a remote web service and return the span tree.
     * @return TestSpanTree
     */
    protected TestSpanTree executeRemoteWebServiceTracerTree() {
        TestSpanTree result = executeRemoteWebServiceTracer().spanTree();

        debug("Tracer returned " + result);

        return result;
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
     * Get operation name depending on the {@code spanKind}.
     * @param spanKind The type of span.
     * @param httpMethod HTTP method
     * @param clazz resource class
     * @param javaMethod method name
     * @return operation name
     */
    protected String getOperationName(String spanKind, String httpMethod, Class<?> clazz, String javaMethod) {
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
     * Create a tags collection for expected span tags.
     * @param spanKind Value for {@link Tags#SPAN_KIND}
     * @param httpMethod Value for {@link Tags#HTTP_METHOD}
     * @param service First parameter to {@link #getWebServiceURL(String, String)
     * @param relativePath Second parameter to {@link #getWebServiceURL(String, String)
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
        tags.put(Tags.HTTP_STATUS.getKey(), httpStatus);
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
}
