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

package org.eclipse.microprofile.opentracing.tck.rest.client;

import static org.eclipse.microprofile.opentracing.tck.application.TestServerWebServices.PARAM_ASYNC;
import static org.eclipse.microprofile.opentracing.tck.application.TestServerWebServices.PARAM_FAIL_NEST;
import static org.eclipse.microprofile.opentracing.tck.application.TestServerWebServices.PARAM_NEST_BREADTH;
import static org.eclipse.microprofile.opentracing.tck.application.TestServerWebServices.PARAM_NEST_DEPTH;
import static org.eclipse.microprofile.opentracing.tck.application.TestServerWebServices.PARAM_UNIQUE_ID;
import static org.eclipse.microprofile.opentracing.tck.rest.client.RestClientServices.REST_NESTED_MP_REST_CLIENT;

import java.util.concurrent.CompletionStage;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import org.eclipse.microprofile.opentracing.Traced;
import org.eclipse.microprofile.opentracing.tck.application.TestServerWebServices;

/**
 * @author Pavol Loffay
 */
@Path("/")
public interface ClientServices {

    @GET
    @Path(REST_NESTED_MP_REST_CLIENT)
    Response executeNested(@QueryParam(PARAM_NEST_DEPTH) int nestDepth,
        @QueryParam(PARAM_NEST_BREADTH) int nestBreadth,
        @QueryParam(PARAM_ASYNC) boolean async,
        @QueryParam(PARAM_UNIQUE_ID) String uniqueID,
        @QueryParam(PARAM_FAIL_NEST) boolean failNest);

    @GET
    @Path(REST_NESTED_MP_REST_CLIENT)
    CompletionStage<Response> executeNestedAsync(@QueryParam(PARAM_NEST_DEPTH) int nestDepth,
        @QueryParam(PARAM_NEST_BREADTH) int nestBreadth,
        @QueryParam(PARAM_ASYNC) boolean async,
        @QueryParam(PARAM_UNIQUE_ID) String uniqueID,
        @QueryParam(PARAM_FAIL_NEST) boolean failNest);


    @GET
    @Traced(false)
    @Path(TestServerWebServices.REST_SIMPLE_TEST)
    Response disabledTracing();
}
