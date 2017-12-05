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
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

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
     * Test that server endpoint is adding standard tags
     */
    @Test
    @RunAsClient
    private void testStandardTags() throws MalformedURLException, InterruptedException {
        Response response = executeRemoteWebServiceRaw(TestServerWebServices.REST_TEST_SERVICE_PATH,
            TestServerWebServices.REST_SIMPLE_TEST);
        response.close();

        TestSpanTree spans = executeRemoteWebServiceTracer().spanTree();

        Map<String, Object> tags = new HashMap<>();
        tags.put(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_SERVER);
        tags.put(Tags.HTTP_METHOD.getKey(), "GET");
        tags.put(Tags.HTTP_URL.getKey(), getWebServiceURL(TestServerWebServices.REST_TEST_SERVICE_PATH,
            TestServerWebServices.REST_SIMPLE_TEST));
        tags.put(Tags.HTTP_STATUS.getKey(), 200);

        TestSpanTree expectedTree = new TestSpanTree(
            new TreeNode<>(
                new TestSpan(serverOperationName("GET", TestServerWebServices.class, "simpleTest"),
                    tags
                )
            )
        );
        Assert.assertEquals(spans, expectedTree);
        clearTracer();
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
                new TestSpan(serverOperationName("GET", TestServerWebServices.class, "localSpan"),
                    Collections.emptyMap())
            ,new TreeNode<>(new TestSpan("localSpan", Collections.emptyMap()))));
        Assert.assertEquals(spans, expectedTree);
        clearTracer();
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
                new TestSpan(serverOperationName("GET", TestServerWebServices.class, "asyncLocalSpan"),
                    Collections.emptyMap())
                ,new TreeNode<>(new TestSpan("localSpan", Collections.emptyMap()))));
        Assert.assertEquals(spans, expectedTree);
        clearTracer();
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
        Assert.assertEquals(response.getStatus(),200);
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
     * Get server span operation name
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
}
