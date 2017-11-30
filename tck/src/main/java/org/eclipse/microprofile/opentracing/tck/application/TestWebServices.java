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

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.opentracing.Tracer;
import javax.ws.rs.core.Response;

/**
 * Test JAXRS web services.
 */
@Path(TestWebServices.REST_TEST_SERVICE_PATH)
public class TestWebServices {

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

    @Inject
    private Tracer tracer;

    /**
     * Hello world service.
     * @return Hello World string
     */
    @GET
    @Path(REST_SIMPLE_TEST)
    @Produces(MediaType.TEXT_PLAIN)
    public Response simpleTest() {
        return Response.ok().build();
    }

    /**
     * Hello world service.
     * @return Hello World string
     */
    @GET
    @Path(REST_LOCAL_SPAN)
    @Produces(MediaType.TEXT_PLAIN)
    public Response localSpan() {
        tracer.buildSpan("localSpan").start().finish();
        return Response.ok().build();
    }

}
