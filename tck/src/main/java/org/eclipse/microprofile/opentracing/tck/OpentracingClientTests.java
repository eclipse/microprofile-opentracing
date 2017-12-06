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
import java.net.MalformedURLException;
import java.net.URL;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.eclipse.microprofile.opentracing.tck.application.TestServerWebServices;
import org.eclipse.microprofile.opentracing.tck.application.TestWebServicesApplication;
import org.eclipse.microprofile.opentracing.tck.application.TracerWebService;
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

/**
 * Opentracing TCK tests.
 * @author <a href="mailto:steve.m.fontes@gmail.com">Steve Fontes</a>
 */
public class OpentracingClientTests extends Arquillian {

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
     * @throws MalformedURLException Error processing the URL.
     */
    @BeforeMethod
    private void beforeEachTest() throws MalformedURLException {
        debug("beforeEachTest calling clearTracer");
        clearTracer();
        debug("beforeEachTest clearTracer completed");
    }

    /**
     * Test that server endpoint is adding standard tags
     */
    @Test
    @RunAsClient
    private void testStandardTags() throws MalformedURLException, InterruptedException {
        Response response = executeRemoteWebServiceRaw(TestServerWebServices.REST_TEST_SERVICE_PATH,
            TestServerWebServices.REST_SIMPLE_TEST);
        response.close();

        TestSpanTree spans = executeRemoteWebServiceTracer().spanTree();

        TestSpanTree expectedTree = new TestSpanTree(
            new TreeNode<>(
                new TestSpan(
                    serverOperationName(
                        HttpMethod.GET,
                        TestServerWebServices.class,
                        TestServerWebServices.REST_SIMPLE_TEST
                    ),
                    getExpectedSpanTags(
                        Tags.SPAN_KIND_SERVER,
                        HttpMethod.GET,
                        TestServerWebServices.REST_TEST_SERVICE_PATH,
                        TestServerWebServices.REST_SIMPLE_TEST,
                        Status.OK.getStatusCode()
                    )
                )
            )
        );
        assertEqualTrees(spans, expectedTree);
    }

    /**
     * Test that implementation exposes active span
     */
    @Test
    @RunAsClient
    private void testLocalSpanHasParent() throws MalformedURLException, InterruptedException {
        Response response = executeRemoteWebServiceRaw(TestServerWebServices.REST_TEST_SERVICE_PATH,
            TestServerWebServices.REST_LOCAL_SPAN);
        response.close();
        TestSpanTree spans = executeRemoteWebServiceTracer().spanTree();
        TestSpanTree expectedTree = new TestSpanTree(
            new TreeNode<>(
                new TestSpan(
                    serverOperationName(
                        HttpMethod.GET,
                        TestServerWebServices.class,
                        TestServerWebServices.REST_LOCAL_SPAN
                    ),
                    getExpectedSpanTags(
                        Tags.SPAN_KIND_SERVER,
                        HttpMethod.GET,
                        TestServerWebServices.REST_TEST_SERVICE_PATH,
                        TestServerWebServices.REST_LOCAL_SPAN,
                        Status.OK.getStatusCode()
                    )
                ),
                new TreeNode<>(new TestSpan(TestServerWebServices.REST_LOCAL_SPAN, Collections.emptyMap()))
            )
        );
        assertEqualTrees(spans, expectedTree);
    }

    /**
     * Test that async endpoint exposes active span
     */
    @Test
    @RunAsClient
    private void testAsyncLocalSpan() throws MalformedURLException, InterruptedException {
        Response response = executeRemoteWebServiceRaw(TestServerWebServices.REST_TEST_SERVICE_PATH,
            TestServerWebServices.REST_ASYNC_LOCAL_SPAN);
        response.close();
        TestSpanTree spans = executeRemoteWebServiceTracer().spanTree();
        TestSpanTree expectedTree = new TestSpanTree(
            new TreeNode<>(
                new TestSpan(
                    serverOperationName(
                        HttpMethod.GET,
                        TestServerWebServices.class,
                        TestServerWebServices.REST_ASYNC_LOCAL_SPAN
                    ),
                    getExpectedSpanTags(
                        Tags.SPAN_KIND_SERVER,
                        HttpMethod.GET,
                        TestServerWebServices.REST_TEST_SERVICE_PATH,
                        TestServerWebServices.REST_ASYNC_LOCAL_SPAN,
                        Status.OK.getStatusCode()
                    )
                ),
                new TreeNode<>(new TestSpan(TestServerWebServices.REST_LOCAL_SPAN, Collections.emptyMap()))
            )
        );
        assertEqualTrees(spans, expectedTree);
    }

    /**
     * This wrapper method allows for potential post-processing, such as
     * removing tags that we don't care to compare in {@code returnedTree}.
     * 
     * @param returnedTree The returned tree from the web service.
     * @param expectedTree The simulated tree that we expect.
     */
    private void assertEqualTrees(TestSpanTree returnedTree,
            TestSpanTree expectedTree) {
        
        // It's okay if the returnedTree has tags other than the ones we
        // want to compare, so just remove those
        returnedTree.visitSpans(span -> span.getTags().keySet()
                .removeIf(key -> !key.equals(Tags.SPAN_KIND.getKey())
                        && !key.equals(Tags.HTTP_METHOD.getKey())
                        && !key.equals(Tags.HTTP_URL.getKey())
                        && !key.equals(Tags.HTTP_STATUS.getKey())));
        
        Assert.assertEquals(returnedTree, expectedTree);
    }

    /**
     * Create a tags collection for expected span tags.
     * @param spanKind Value for {@link Tags#SPAN_KIND}
     * @param httpMethod Value for {@link Tags#HTTP_METHOD}
     * @param service First parameter to {@link #getWebServiceURL(String, String)
     * @param relativePath Second parameter to {@link #getWebServiceURL(String, String)
     * @param httpStatus Value for {@link Tags#HTTP_STATUS}
     * @return Tags collection.
     * @throws MalformedURLException Error creating web service url.
     */
    private Map<String, Object> getExpectedSpanTags(String spanKind,
            String httpMethod, String service, String relativePath,
            int httpStatus) throws MalformedURLException {
        
        // When adding items to this, also add to assertEqualTrees
        
        Map<String, Object> tags = new HashMap<>();
        tags.put(Tags.SPAN_KIND.getKey(), spanKind);
        tags.put(Tags.HTTP_METHOD.getKey(), httpMethod);
        tags.put(Tags.HTTP_URL.getKey(), getWebServiceURL(service, relativePath));
        tags.put(Tags.HTTP_STATUS.getKey(), httpStatus);
        return tags;
    }

    /**
     * Create remote URL.
     * @param service Web service path
     * @param relativePath Web service endpoint
     * @return Remote URL
     * @throws MalformedURLException Error creating URL
     */
    private String getWebServiceURL(final String service, final String relativePath)
            throws MalformedURLException {
        return new URL(deploymentURL,
                TestWebServicesApplication.TEST_WEB_SERVICES_CONTEXT_ROOT
                + "/" + service + "/" + relativePath).toString();
    }

    /**
     * Execute a remote web service and return the content.
     * @param service Web service path
     * @param relativePath Web service endpoint
     * @return Response
     * @throws MalformedURLException Bad URL
     */
    private Response executeRemoteWebServiceRaw(final String service, final String relativePath)
            throws MalformedURLException {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(getWebServiceURL(service, relativePath));
        Response response = target.request().get();
        Assert.assertEquals(response.getStatus(), Status.OK.getStatusCode());
        return response;
    }

    /**
     * Execute a remote web service and return the Tracer.
     * @return Tracer
     * @throws MalformedURLException Bad URL
     */
    private TestTracer executeRemoteWebServiceTracer()
            throws MalformedURLException {
        return executeRemoteWebServiceRaw(
                TracerWebService.REST_TRACER_SERVICE_PATH, TracerWebService.REST_GET_TRACER)
                .readEntity(TestTracer.class);
    }

    /**
     * Get server span operation name.
     * https://github.com/eclipse/microprofile-opentracing/blob/master/spec/src/main/asciidoc/microprofile-opentracing-spec.asciidoc#server-span-name
     * @param httpMethod HTTP method
     * @param clazz resource class
     * @param javaMethod method name
     * @return
     */
    private String serverOperationName(String httpMethod, Class<?> clazz, String javaMethod) {
        return String.format("%s:%s.%s", httpMethod, clazz.getName(), javaMethod);
    }

    private void clearTracer() throws MalformedURLException {
        Client client = ClientBuilder.newClient();
        String url = new URL(deploymentURL,
            TestWebServicesApplication.TEST_WEB_SERVICES_CONTEXT_ROOT
                + "/" + TracerWebService.REST_TRACER_SERVICE_PATH + "/" + TracerWebService.REST_CLEAR_TRACER).toString();
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
}
