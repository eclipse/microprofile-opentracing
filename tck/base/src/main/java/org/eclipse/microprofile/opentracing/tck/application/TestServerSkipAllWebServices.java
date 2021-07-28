/*
 * Copyright (c) 2018-2021 Contributors to the Eclipse Foundation
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

import org.eclipse.microprofile.opentracing.Traced;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Test JAXRS web service which should not be traced.
 */
@Path(TestServerSkipAllWebServices.REST_TEST_SKIP_SERVICE_PATH)
public class TestServerSkipAllWebServices {
    public static final String REST_TEST_SKIP_SERVICE_PATH = "skipAll";
    public static final String REST_SIMPLE_PATH = "simple";
    public static final String REST_NESTED_PATH = "simple/nested";
    public static final String REST_EXPLICITLY_TRACED = "explicitlyTraced";

    @GET
    @Path(REST_SIMPLE_PATH)
    @Produces(MediaType.TEXT_PLAIN)
    public Response simplePath() {
        return Response.ok().build();
    }

    @GET
    @Path(REST_NESTED_PATH)
    @Produces(MediaType.TEXT_PLAIN)
    public Response nestedPath() {
        return Response.ok().build();
    }

    @GET
    @Traced
    @Path(REST_EXPLICITLY_TRACED)
    @Produces(MediaType.TEXT_PLAIN)
    public Response explicitlyTraced() {
        return Response.ok().build();
    }
}
