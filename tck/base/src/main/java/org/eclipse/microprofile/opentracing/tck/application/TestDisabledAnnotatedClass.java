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

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.opentracing.Traced;

/**
 * Injectable class with the Traced annotation disabled on the class but not on methods.
 */
@ApplicationScoped
@Traced(value = false)
public class TestDisabledAnnotatedClass {

    /**
     * Method that we expect to be Traced.
     */
    @Traced
    public void annotatedClassMethodExplicitlyTraced() {
        System.out.println("Called annotatedClassMethodExplicitlyTraced");
    }

    /**
     * Method that we expect to be Traced with an operation name.
     */
    @Traced(operationName = "explicitOperationName3")
    public void annotatedClassMethodExplicitlyTracedWithOperationName() {
        System.out.println("Called annotatedClassMethodExplicitlyTracedWithOperationName");
    }

    /**
     * Method that we expect not to be Traced.
     */
    public void annotatedClassMethodImplicitlyNotTraced() {
        System.out.println("Called annotatedClassMethodImplicitlyNotTraced");
    }
}
