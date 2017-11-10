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
import java.net.MalformedURLException;
import java.net.URL;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.opentracing.tck.application.TestWebServices;
import org.eclipse.microprofile.opentracing.tck.application.TestWebServicesApplication;
import org.eclipse.microprofile.opentracing.tck.tracer.SpanKind;
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
     * Test web service calls.
     * @throws MalformedURLException URL errors
     */
    @Test
    @RunAsClient
    public void simpleTest() throws MalformedURLException {
        // Execute the Hello World web service multiple times to check
        // that clearing the Tracer works.
        executeHelloWorld();
        executeHelloWorld();
    }

    /**
     * Execute the Hello World web service, verify spans, and clear tracer.
     * @throws MalformedURLException Error building URL
     */
    private void executeHelloWorld() throws MalformedURLException {
        String responseText = executeRemoteWebServiceString(
                TestWebServicesApplication.REST_SIMPLE_TEST);
        Assert.assertEquals(responseText, TestWebServices.SIMPLE_TEST_CONTENT);

        TestSpanTree spans = executeRemoteWebServiceTracer().spanTree();

        TestSpanTree expectedTree = new TestSpanTree(
            new TreeNode<>(
                new TestSpan(
                    SpanKind.SERVER,
                    getWebServiceURL(
                        TestWebServicesApplication.REST_SIMPLE_TEST
                    )
                )
            )
        );

        Assert.assertEquals(spans, expectedTree);

        executeRemoteWebServiceString(
                TestWebServicesApplication.REST_CLEAR_TRACER);
    }

    /**
     * Create remote URL.
     * @param relativePath Path at the end of the URL
     * @return Remote URL
     * @throws MalformedURLException Error creating URL
     */
    private String getWebServiceURL(final String relativePath)
            throws MalformedURLException {
        return new URL(deploymentURL,
                TestWebServicesApplication.TEST_WEB_SERVICES_CONTEXT_ROOT
                + "/" + TestWebServices.TEST_WS_PATH + "/"
                        + relativePath).toString();
    }

    /**
     * Execute a remote web service and return the content.
     * @param relativePath Web service URL
     * @return Response
     * @throws MalformedURLException Bad URL
     */
    private Response executeRemoteWebServiceRaw(final String relativePath)
            throws MalformedURLException {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(getWebServiceURL(relativePath));
        return target.request().get();
    }

    /**
     * Execute a remote web service and return the content.
     * @param relativePath Web service URL
     * @return HTML response
     * @throws MalformedURLException Bad URL
     */
    private String executeRemoteWebServiceString(final String relativePath)
            throws MalformedURLException {
        return executeRemoteWebServiceRaw(relativePath)
                .readEntity(String.class);
    }

    /**
     * Execute a remote web service and return the Tracer.
     * @return Tracer
     * @throws MalformedURLException Bad URL
     */
    private TestTracer executeRemoteWebServiceTracer()
            throws MalformedURLException {
        return executeRemoteWebServiceRaw(
                TestWebServicesApplication.REST_GET_TRACER)
                .readEntity(TestTracer.class);
    }
}
