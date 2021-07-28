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

package org.eclipse.microprofile.opentracing.tck.application;

import org.eclipse.microprofile.opentracing.Traced;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Injectable class with the Traced annotation.
 */
@ApplicationScoped
@Traced
public class TestAnnotatedClass {

    /**
     * Method that we expect to be Traced implicitly.
     */
    public void annotatedClassMethodImplicitlyTraced() {
        System.out.println("Called annotatedClassMethodImplicitlyTraced");
    }

    /**
     * Method that we expect to not be Traced.
     */
    @Traced(value = false)
    public void annotatedClassMethodExplicitlyNotTraced() {
        System.out.println("Called annotatedClassMethodExplicitlyNotTraced");
    }

    /**
     * Method that we expect to be Traced explicitly.
     */
    @Traced(operationName = "explicitOperationName1")
    public void annotatedClassMethodExplicitlyTraced() {
        System.out.println("Called annotatedClassMethodExplicitlyTraced");
    }

    /**
     * Method that we expect to not be Traced.
     */
    @Traced(operationName = "disabledOperationName", value = false)
    public void annotatedClassMethodExplicitlyNotTracedWithOpName() {
        System.out.println("Called annotatedClassMethodExplicitlyNotTracedWithOpName");
    }

    /**
     * Method that we expect to be Traced implicitly and throws an exception.
     */
    public void annotatedClassMethodImplicitlyTracedWithException() {
        System.out.println("Called annotatedClassMethodImplicitlyTracedWithException");
        throw ApplicationUtils.createExampleRuntimeException();
    }
}
