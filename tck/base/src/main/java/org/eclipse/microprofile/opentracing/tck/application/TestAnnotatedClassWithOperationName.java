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
 * Injectable class with the Traced annotation and operation name.
 */
@ApplicationScoped
@Traced(operationName = TestAnnotatedClassWithOperationName.OPERATION_NAME)
public class TestAnnotatedClassWithOperationName {

    /**
     * Operation name for this class.
     */
    public static final String OPERATION_NAME = "operationPrefix";

    /**
     * Method that we expect to be Traced implicitly.
     */
    public void annotatedClassMethodImplicitlyTraced() {
        System.out.println("Called annotatedClassMethodImplicitlyTraced");
    }

    /**
     * Method that we expect to be Traced explicitly.
     */
    @Traced(operationName = "explicitOperationName4")
    public void annotatedClassMethodExplicitlyTraced() {
        System.out.println("Called annotatedClassMethodExplicitlyTraced");
    }
}
