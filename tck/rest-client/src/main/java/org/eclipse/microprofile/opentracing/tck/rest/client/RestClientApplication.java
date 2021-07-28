/*
 * Copyright (c) 2017-2021 Contributors to the Eclipse Foundation
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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.microprofile.opentracing.tck.application.ApplicationUtils;
import org.eclipse.microprofile.opentracing.tck.application.TestClientRegistrarWebServices;
import org.eclipse.microprofile.opentracing.tck.application.TestServerSkipAllWebServices;
import org.eclipse.microprofile.opentracing.tck.application.TestServerWebServices;
import org.eclipse.microprofile.opentracing.tck.application.TestServerWebServicesWithOperationName;
import org.eclipse.microprofile.opentracing.tck.application.TracerWebService;
import org.eclipse.microprofile.opentracing.tck.application.WildcardClassService;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

/**
 * @author Pavol Loffay
 *
 *         Note that we cannot extend TestWebServicesApplication. We remove it from the deployment and map a this JAX-RS
 *         map to its URL.
 *
 */
@ApplicationPath(ApplicationUtils.TEST_WEB_SERVICES_CONTEXT_ROOT)
public class RestClientApplication extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        return new HashSet<>(Arrays.asList(
                TracerWebService.class,
                TestServerWebServices.class,
                TestServerSkipAllWebServices.class,
                TestServerWebServicesWithOperationName.class,
                TestClientRegistrarWebServices.class,
                WildcardClassService.class,
                RestClientServices.class));
    }
}
