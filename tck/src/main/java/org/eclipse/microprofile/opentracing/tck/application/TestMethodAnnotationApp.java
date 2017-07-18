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

import org.eclipse.microprofile.opentracing.Trace;


/**
 * Application where @Trace annotation is added at the Method level.
 *
 * @author Steve Fontes
 *
 */
public class TestMethodAnnotationApp {

    /**
     * @return name of serviceEnpoint.
     */
    @Trace
    public String serviceEndpointA() {
        return "Invoked TestMethodAnnotationApp.serviceEndpointA";
    }

    /**
     * @return name of serviceEnpoint.
     */
    public String serviceEndpointB() {
        return "Invoked TestMethodAnnotationApp.serviceEndpointB";
    }

    /**
     * @return name of serviceEnpoint.
     */
    @Trace("MethodAnnotated.endpointC")
    public String serviceEndpointC() {
        return "Invoked TestMethodAnnotationApp.serviceEndpointC";
    }

    /**
     * @return name of serviceEnpoint.
     */
    @Trace(relationship = "follows_from")
    public String serviceEndpointD() {
        return "Invoked TestMethodAnnotationApp.serviceEndpointD";
    }

    /**
     * @return name of serviceEnpoint.
     */
    @Trace(value = "MethodAnnotated.endpointE", relationship = "follows_from")
    public String serviceEndpointE() {
        return "Invoked TestMethodAnnotationApp.serviceEndpointE";
    }
}
