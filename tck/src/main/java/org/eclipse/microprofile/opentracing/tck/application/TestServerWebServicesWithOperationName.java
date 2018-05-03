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
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.opentracing.Traced;

/**
 * Test JAXRS web services.
 */
@Traced(operationName = TestServerWebServicesWithOperationName.CLASS_OPERATION_NAME)
@Path(TestServerWebServicesWithOperationName.REST_TEST_SERVICE_PATH_WITH_OP_NAME)
public class TestServerWebServicesWithOperationName {

    /**
     * The path to this set of web services.
     */
    public static final String REST_TEST_SERVICE_PATH_WITH_OP_NAME = "testServicesWithOpName";

    /**
     * Traced operationName prefix.
     */
    public static final String CLASS_OPERATION_NAME = "wsOperations";

    /**
     * Web service endpoint with an explicit operation name on the class.
     */
    public static final String REST_OPERATION_CLASS_OP_NAME = "classOperationName";

    /**
     * Web service endpoint with an explicit operation name on the class and endpoint.
     */
    public static final String REST_OPERATION_CLASS_AND_METHOD_OP_NAME = "classAndMethodOperationName";

    /**
     * Explicit endpoint operation name.
     */
    public static final String ENDPOINT_OPERATION_NAME = "endpointName";

    /**
     * Injected class with Traced annotation on the class and operation name.
     */
    @Inject
    private TestAnnotatedClassWithOperationName testAnnotatedClassWithOperationName;

    /**
     * Test class with Traced annotation and operation name.
     *
     * @return OK response
     */
    @GET
    @Path(REST_OPERATION_CLASS_OP_NAME)
    @Produces(MediaType.TEXT_PLAIN)
    public Response classOperationName() {
        testAnnotatedClassWithOperationName.annotatedClassMethodImplicitlyTraced();
        testAnnotatedClassWithOperationName.annotatedClassMethodExplicitlyTraced();
        return Response.ok().build();
    }

    /**
     * Test class and endpoint with Traced annotation and operation name.
     *
     * @return OK response
     */
    @Traced(operationName = ENDPOINT_OPERATION_NAME)
    @GET
    @Path(REST_OPERATION_CLASS_AND_METHOD_OP_NAME)
    @Produces(MediaType.TEXT_PLAIN)
    public Response classAndMethodOperationName() {
        return Response.ok().build();
    }
}
