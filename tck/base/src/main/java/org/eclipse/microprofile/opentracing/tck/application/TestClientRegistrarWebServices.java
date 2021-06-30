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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.eclipse.microprofile.opentracing.ClientTracingRegistrar;

/**
 * @author Pavol Loffay
 * @author Patrik Dudits
 */
@Path(TestClientRegistrarWebServices.REST_SERVICE_PATH)
public class TestClientRegistrarWebServices {
    public static final String REST_SERVICE_PATH = "testRegistrarServices";
    public static final String REST_OK = "ok";
    public static final String REST_CLIENT_BUILDER = "clientBuilder";
    public static final String REST_CLIENT_BUILDER_EXECUTOR = "clientBuilderExecutor";

    @Context
    private UriInfo uri;

    @GET
    @Path(REST_OK)
    @Produces(MediaType.TEXT_PLAIN)
    public Response ok() {
        return Response.ok().build();
    }

    /**
     * Endpoint which uses {@link ClientTracingRegistrar#configure(ClientBuilder)} to create an outbound request
     * to instrument a client for an outbound request.
     */
    @GET
    @Path(REST_CLIENT_BUILDER)
    @Produces(MediaType.TEXT_PLAIN)
    public Response clientRegistrar(@QueryParam("async") boolean async)
        throws ExecutionException, InterruptedException {
        return executeSimpleEndpoint(instrumentedClient(), async);
    }

    /**
     * Endpoint which uses {@link ClientTracingRegistrar#configure(ClientBuilder, java.util.concurrent.ExecutorService)}
     * to instrument a client for an outbound request.
     */
    @GET
    @Path(REST_CLIENT_BUILDER_EXECUTOR)
    @Produces(MediaType.TEXT_PLAIN)
    public Response clientRegistrarExecutor(@QueryParam("async") boolean async)
        throws ExecutionException, InterruptedException {
        return executeSimpleEndpoint(instrumentedClientExecutor(), async);
    }

    private Response executeSimpleEndpoint(Client client, boolean async)
        throws ExecutionException, InterruptedException {
        Builder requestBuilder = client.target(uri.getBaseUri())
            .path(REST_SERVICE_PATH)
            .path(REST_OK)
            .request();

        Response response = async ? requestBuilder.async().get().get() : requestBuilder.get();
        response.close();
        client.close();
        return Response.status(response.getStatus()).build();
    }

    private Client instrumentedClient() {
        ClientBuilder clientBuilder = ClientBuilder.newBuilder();
        clientBuilder = ClientTracingRegistrar.configure(clientBuilder);
        return clientBuilder.build();
    }

    private Client instrumentedClientExecutor() {
        ClientBuilder clientBuilder = ClientBuilder.newBuilder();
        clientBuilder = ClientTracingRegistrar.configure(clientBuilder, Executors.newFixedThreadPool(10));
        return clientBuilder.build();
    }
}
