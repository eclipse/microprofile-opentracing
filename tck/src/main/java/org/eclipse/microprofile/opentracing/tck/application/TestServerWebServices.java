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
package org.eclipse.microprofile.opentracing.tck.application;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import io.opentracing.ActiveSpan;
import io.opentracing.Span;
import io.opentracing.Tracer;

/**
 * Test JAXRS web services.
 */
@Path(TestServerWebServices.REST_TEST_SERVICE_PATH)
public class TestServerWebServices {

    /**
     * The path to this set of web services.
     */
    public static final String REST_TEST_SERVICE_PATH = "testServices";
    
    /**
     * Web service endpoint for the simpleTest call.
     */
    public static final String REST_SIMPLE_TEST = "simpleTest";
    
    /**
     * Web service endpoint that creates local span.
     */
    public static final String REST_LOCAL_SPAN = "localSpan";
    
    /**
     * Async web service endpoint that creates local span.
     */
    public static final String REST_ASYNC_LOCAL_SPAN = "asyncLocalSpan";
    
    /**
     * Web service endpoint that will return HTTP 500.
     */
    public static final String REST_ERROR = "error";
    
    /**
     * Web service that throws an unhandled exception and return HTTP 500.
     */
    public static final String REST_EXCEPTION = "exception";

    /**
     * Query parameter that's a unique ID propagated down nested calls.
     */
    public static final String PARAM_UNIQUE_ID = "data";

    /**
     * Query parameter whether to fail the nested call.
     */
    public static final String PARAM_FAIL_NEST = "failNest";

    /**
     * Web service endpoint that will call itself some number of times.
     */
    public static final String REST_NESTED = "nested";
    
    /**
     * Query parameter for the number of nested calls.
     */
    public static final String PARAM_NEST_DEPTH = "nestDepth";
    
    /**
     * Query parameter for the breadth of nesting.
     */
    public static final String PARAM_NEST_BREADTH = "nestBreadth";

    /**
     * The key of a simulated span tag.
     */
    public static final String LOCAL_SPAN_TAG_KEY = "localSpanKey";

    /**
     * The value of a simulated span tag.
     */
    public static final String LOCAL_SPAN_TAG_VALUE = "localSpanValue";
    
    /**
     * Injected tracer.
     */
    @Inject
    private Tracer tracer;
    
    /**
     * Represents the URI of the executing web service call.
     */
    @Context
    private UriInfo uri;

    /**
     * Simple JAXRS endpoint.
     * @return OK response
     */
    @GET
    @Path(REST_SIMPLE_TEST)
    @Produces(MediaType.TEXT_PLAIN)
    public Response simpleTest() {
        return Response.ok().build();
    }

    /**
     * Endpoint which creates local span.
     * @return OK response
     */
    @GET
    @Path(REST_LOCAL_SPAN)
    @Produces(MediaType.TEXT_PLAIN)
    public Response localSpan() {
        finishChildSpan(startChildSpan(REST_LOCAL_SPAN));
        return Response.ok().build();
    }

    /**
     * Async endpoint which creates local span.
     * @param asyncResponse holds state of the asynchronous processing
     */
    @GET
    @Path(REST_ASYNC_LOCAL_SPAN)
    @Produces(MediaType.TEXT_PLAIN)
    public void asyncLocalSpan(@Suspended final AsyncResponse asyncResponse) {
        finishChildSpan(startChildSpan(REST_LOCAL_SPAN));
        asyncResponse.resume(Response.ok().build());
    }

    /**
     * Returns HTTP 500 error.
     * @return error response
     */
    @GET
    @Path(REST_ERROR)
    @Produces(MediaType.TEXT_PLAIN)
    public Response error() {
        return Response.serverError().build();
    }

    /**
     * Returns HTTP 500 error.
     * @return Response Never returned because an exception is thrown.
     */
    @GET
    @Path(REST_EXCEPTION)
    @Produces(MediaType.TEXT_PLAIN)
    public Response exception() {
        throw TestWebServicesApplication.createExampleRuntimeException();
    }

    /**
     * Web service call that calls itself {@code nestDepth} - 1 times.
     *
     * A nesting depth of zero (0) causes an immediate return with the specified
     * response text.
     *
     * A nesting depth greater than zero causes a call to the nesting service
     * with the depth reduced by one (1).
     * 
     * The {@code nestBreadth} controls how many concurrent, nested calls are
     * made on the first level of nesting.
     *
     * @param nestDepth The depth of nesting to use when implementing the request.
     * @param nestBreadth The breadth of nested calls.
     * @param uniqueID Unique ID propagated down nested calls.
     * @param failNest True if nested response should be an error
     * @return OK response
     * @throws ExecutionException Error executing nested web service.
     * @throws InterruptedException Error executing nested web service.
     */
    @GET
    @Path(REST_NESTED)
    @Produces(MediaType.TEXT_PLAIN)
    public Response nested(@QueryParam(PARAM_NEST_DEPTH) int nestDepth,
            @QueryParam(PARAM_NEST_BREADTH) int nestBreadth,
            @QueryParam(PARAM_UNIQUE_ID) String uniqueID,
            @QueryParam(PARAM_FAIL_NEST) boolean failNest)
            throws InterruptedException, ExecutionException {
        
        if (nestDepth > 0) {
            Map<String, Object> nestParameters = new HashMap<String, Object>();

            String target;
            if (failNest) {
                target = REST_ERROR;
            }
            else {
                target = REST_NESTED;
                nestParameters.put(PARAM_NEST_DEPTH, nestDepth - 1);
                nestParameters.put(PARAM_NEST_BREADTH, 1);
                nestParameters.put(PARAM_UNIQUE_ID, uniqueID);
                nestParameters.put(PARAM_FAIL_NEST, false);
            }
            
            String requestUrl = getRequestPath(
                    REST_TEST_SERVICE_PATH, target, nestParameters);
            
            for (int i = 0; i < nestBreadth; i++) {
                executeNested(requestUrl);
            }
        }

        return Response.ok().build();
    }

    /**
     * Execute a nested web service call.
     * @param requestUrl The request URL.
     */
    private void executeNested(String requestUrl) {
        Response nestedResponse = TestWebServicesApplication.invoke(requestUrl);
        nestedResponse.close();
    }

    /**
     * Create the full URL for the web service request.
     * @param servicePath Service path component.
     * @param endpointPath The final component of the path.
     * @param requestParameters Query parameters.
     * @return Full URL for the web service request.
     */
    public String getRequestPath(
            String servicePath, String endpointPath,
            Map<String, Object> requestParameters) {
        
        String incomingUrl = uri.getAbsolutePath().toString();
        int i = incomingUrl.indexOf(TestWebServicesApplication.TEST_WEB_SERVICES_CONTEXT_ROOT);
        if (i == -1) {
            throw new RuntimeException("Expecting "
                    + TestWebServicesApplication.TEST_WEB_SERVICES_CONTEXT_ROOT
                    + " in " + incomingUrl);
        }
        URL incomingURL;
        try {
            incomingURL = new URL(incomingUrl.substring(0, i));
        }
        catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        String result = TestWebServicesApplication.getWebServiceURL(incomingURL, servicePath, endpointPath);

        if ((requestParameters != null) && !requestParameters.isEmpty()) {
            result += TestWebServicesApplication.getQueryString(requestParameters);
        }

        return result;
    }
    
    /**
     * Potentially print a debug message.
     * @param message Debug message.
     */
    @SuppressWarnings("unused")
    private void debug(String message) {
        System.out.println(message);
    }

    /**
     * Start a new child span of the active span.
     *
     * The child span must be finished using {@link #finishChildSpan} before
     * completing the service request which created the span.
     * 
     * If there is no active span, the newly created span is made the active
     * span.
     *
     * @param operationName The operation name to give the new child span.
     *
     * @return The new child span.
     */
    private Span startChildSpan(String operationName) {
        ActiveSpan activeSpan = tracer.activeSpan();
        Tracer.SpanBuilder spanBuilder = tracer.buildSpan(operationName);
        if (activeSpan != null) {
            spanBuilder.asChildOf(activeSpan.context());
        }
        Span childSpan = spanBuilder.startManual();
        if (activeSpan == null) {
            tracer.makeActive(childSpan);
        }
        childSpan.setTag(LOCAL_SPAN_TAG_KEY, LOCAL_SPAN_TAG_VALUE);
        return childSpan;
    }

    /**
     * Finish a child span of the active span.
     *
     * @param span The child span.
     */
    private void finishChildSpan(Span span) {
        span.finish();
    }
}
