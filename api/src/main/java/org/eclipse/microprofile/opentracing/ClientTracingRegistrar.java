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

package org.eclipse.microprofile.opentracing;

import java.util.ServiceLoader;
import java.util.concurrent.ExecutorService;
import javax.ws.rs.client.ClientBuilder;

/**
 * This class registers tracing components into {@link ClientBuilder}.
 * It is required to call {@link #configure(ClientBuilder)} or its variants to enable tracing in
 * {@link javax.ws.rs.client.Client}, however implementation might enable tracing globally.
 *
 * Invoking {@link ClientTracingRegistrar#configure(ClientBuilder)} returns
 * a {@link ClientBuilder} with enabled tracing integration. Note that following calls to
 * {@link ClientBuilder} which change {@link ExecutorService} might break tracing integration. If a custom
 * {@link ExecutorService} has to be used use {@link ClientTracingRegistrar#configure(ClientBuilder, ExecutorService)}.
 *
 * @author Pavol Loffay
 */
public class ClientTracingRegistrar {

    private ClientTracingRegistrar() {}

    /**
     *  Register tracing components into client builder instance.
     *
     * @param clientBuilder client builder
     * @return clientBuilder with tracing integration
     */
    public static ClientBuilder configure(ClientBuilder clientBuilder) {
        for(ClientTracingRegistrarProvider registrar: ServiceLoader.load(ClientTracingRegistrarProvider.class)) {
            return registrar.configure(clientBuilder);
        }
        return clientBuilder;
    }

    /**
     *  Register tracing components into client builder instance.
     *
     * @param clientBuilder client builder
     * @param executorService executorService which will be added to the client. Note that this overrides
     *      executor service added previously to the client.
     * @return clientBuilder with tracing integration
     */
    public static ClientBuilder configure(ClientBuilder clientBuilder, ExecutorService executorService) {
        for(ClientTracingRegistrarProvider registrar: ServiceLoader.load(ClientTracingRegistrarProvider.class)) {
            return registrar.configure(clientBuilder, executorService);
        }
        return clientBuilder;
    }
}
