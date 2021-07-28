/*
 * Copyright (c) 2019-2021 Contributors to the Eclipse Foundation
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

import static org.eclipse.microprofile.opentracing.tck.application.TestServerWebServices.PARAM_ASYNC;
import static org.eclipse.microprofile.opentracing.tck.application.TestServerWebServices.PARAM_FAIL_NEST;
import static org.eclipse.microprofile.opentracing.tck.application.TestServerWebServices.PARAM_NEST_BREADTH;
import static org.eclipse.microprofile.opentracing.tck.application.TestServerWebServices.PARAM_NEST_DEPTH;
import static org.eclipse.microprofile.opentracing.tck.application.TestServerWebServices.PARAM_UNIQUE_ID;
import static org.eclipse.microprofile.opentracing.tck.rest.client.RestClientServices.REST_SERVICE_PATH;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

import org.eclipse.microprofile.opentracing.tck.application.ApplicationUtils;
import org.eclipse.microprofile.opentracing.tck.application.TestServerWebServices;
import org.eclipse.microprofile.rest.client.RestClientBuilder;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

/**
 * @author Pavol Loffay
 */
@Path(REST_SERVICE_PATH)
public class RestClientServices {

    public static final String REST_SERVICE_PATH = "mpRestClient";

    /**
     * Web service endpoint that will call itself some number of times.
     */
    public static final String REST_NESTED_MP_REST_CLIENT = "nestedMpRestClient";

    /**
     * Web service endpoint that uses rest client with disabled tracing.
     */
    public static final String REST_MP_REST_CLIENT_DISABLED_TRACING = "restClientTracingDisabled";

    /**
     * Web service endpoint that uses rest client with disabled tracing.
     */
    public static final String REST_MP_REST_CLIENT_DISABLED_TRACING_METHOD = "restClientMethodTracingDisabled";

    /**
     * Represents the URI of the executing web service call.
     */
    @Context
    private UriInfo uri;

    @GET
    @Path(REST_MP_REST_CLIENT_DISABLED_TRACING)
    @Produces(MediaType.TEXT_PLAIN)
    public Response restClientTracingDisabled() throws MalformedURLException {
        URL webServicesUrl = new URL(getBaseURL().toString() + "rest/" + TestServerWebServices.REST_TEST_SERVICE_PATH);
        ClientServicesTracingDisabled client = RestClientBuilder.newBuilder()
                .baseUrl(webServicesUrl)
                .build(ClientServicesTracingDisabled.class);
        client.restSimpleTest();
        return Response.ok().build();
    }

    @GET
    @Path(REST_MP_REST_CLIENT_DISABLED_TRACING_METHOD)
    @Produces(MediaType.TEXT_PLAIN)
    public Response restClientMethodTracingDisabled() throws MalformedURLException {
        URL webServicesUrl = new URL(getBaseURL().toString() + "rest/" + TestServerWebServices.REST_TEST_SERVICE_PATH);
        ClientServices client = RestClientBuilder.newBuilder()
                .baseUrl(webServicesUrl)
                .build(ClientServices.class);
        client.disabledTracing();
        return Response.ok().build();
    }

    @GET
    @Path(REST_NESTED_MP_REST_CLIENT)
    @Produces(MediaType.TEXT_PLAIN)
    public Response nestedMpRestClient(@QueryParam(PARAM_NEST_DEPTH) int nestDepth,
            @QueryParam(PARAM_NEST_BREADTH) int nestBreadth,
            @QueryParam(PARAM_UNIQUE_ID) String uniqueID,
            @QueryParam(PARAM_FAIL_NEST) boolean failNest,
            @QueryParam(PARAM_ASYNC) boolean async)
            throws MalformedURLException, ExecutionException, InterruptedException {
        if (nestDepth > 0) {
            for (int i = 0; i < nestBreadth; i++) {
                executeNestedMpRestClient(nestDepth - 1, 1, uniqueID, async);
            }
        }
        return Response.ok().build();
    }

    private void executeNestedMpRestClient(int depth, int breath, String id, boolean async)
            throws MalformedURLException, InterruptedException, ExecutionException {
        URL webServicesUrl = new URL(getBaseURL().toString() + "rest/" + REST_SERVICE_PATH);
        ClientServices clientServices = RestClientBuilder.newBuilder()
                .baseUrl(webServicesUrl)
                .executorService(Executors.newFixedThreadPool(50))
                .build(ClientServices.class);
        if (async) {
            CompletionStage<Response> completionStage =
                    clientServices.executeNestedAsync(depth, breath, async, id, false);
            completionStage.toCompletableFuture()
                    .get();
        } else {
            clientServices.executeNested(depth, breath, async, id, false)
                    .close();
        }
    }

    private URL getBaseURL() {
        String incomingURLValue = uri.getAbsolutePath().toString();
        int i = incomingURLValue.indexOf(ApplicationUtils.TEST_WEB_SERVICES_CONTEXT_ROOT);
        if (i == -1) {
            throw new RuntimeException("Expecting "
                    + ApplicationUtils.TEST_WEB_SERVICES_CONTEXT_ROOT
                    + " in " + incomingURLValue);
        }
        URL incomingURL;
        try {
            incomingURL = new URL(incomingURLValue.substring(0, i));
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        return incomingURL;
    }
}
