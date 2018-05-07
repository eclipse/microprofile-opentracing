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

import java.util.concurrent.ExecutorService;
import javax.ws.rs.client.ClientBuilder;

/**
 * Implementation of this interface will be used to configure {@link ClientBuilder}
 * when {@link ClientTracingRegistrar#configure(ClientBuilder)} is called.
 *
 * Implementation must be registered in
 * <code>
 * META-INF/services/org.eclipse.microprofile.opentracing.{@link ClientTracingRegistrarProvider}
 * </code>
 *
 * @author Pavol Loffay
 */
public interface ClientTracingRegistrarProvider {

    /**
     * Configures {@link ClientBuilder} with tracing integration.
     *
     * @param clientBuilder Client builder to configure.
     * @return clientBuilder with tracing integration
     */
    ClientBuilder configure(ClientBuilder clientBuilder);

    /**
     * Configures {@link ClientBuilder} with tracing integration.
     *
     * @param clientBuilder Client builder to configure.
     * @param executorService Executor service which will be added to the client builder.
     * @return clientBuilder with tracing integration
     */
    ClientBuilder configure(ClientBuilder clientBuilder, ExecutorService executorService);
}
