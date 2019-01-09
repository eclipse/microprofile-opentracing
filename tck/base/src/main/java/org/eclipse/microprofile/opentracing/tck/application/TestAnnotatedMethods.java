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
 * Injectable class without the Traced annotation on the class.
 */
@ApplicationScoped
public class TestAnnotatedMethods {

    /**
     * Method that we expect to be Traced.
     */
    @Traced
    public void annotatedMethodExplicitlyTraced() {
        System.out.println("Called annotatedMethodExplicitlyTraced");
    }

    /**
     * Method that we expect to not be Traced.
     */
    @Traced(value = false)
    public void annotatedMethodExplicitlyNotTraced() {
        System.out.println("Called annotatedMethodExplicitlyNotTraced");
    }

    /**
     * Method that we expect to be Traced with operation name.
     */
    @Traced(operationName = "explicitOperationName2")
    public void annotatedMethodExplicitlyTracedWithOpName() {
        System.out.println("Called annotatedMethodExplicitlyTracedWithOpName");
    }

    /**
     * Method that we expect to not be Traced with operation name.
     */
    @Traced(value = false, operationName = "disabledOperationName")
    public void annotatedMethodExplicitlyNotTracedWithOpName() {
        System.out.println("Called annotatedMethodExplicitlyNotTracedWithOpName");
    }
}
