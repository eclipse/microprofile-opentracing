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

package org.eclipse.microprofile.opentracing.tck;

import javax.inject.Inject;

import org.eclipse.microprofile.opentracing.tck.application.TestClassAnnotationApp;
import org.eclipse.microprofile.opentracing.tck.application.TestMethodAnnotationApp;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test that a @Trace on a class is processed.
 * @author <a href="mailto:steve.m.fontes@gmail.com">Steve Fontes</a>
 *
 */
@RunWith(Arquillian.class)
public class TraceTest {

    /** The application annotated at the Class level.*/
    @Inject
    private TestClassAnnotationApp testClassAnnotationApp;

    /** The application annotated at the Method level.*/
    @Inject
    private TestMethodAnnotationApp testMethodAnnotationApp;

    /**
    /**
     * Deploy the apps to test.
     * @return the Deployed apps
     */
    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class)
                .addClass(TestClassAnnotationApp.class)
                .addClass(TestMethodAnnotationApp.class)
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    /**
     * Test methods of the class are traced when class is annotated with @Trace.
     */
    @Test
    public void classAnnotatedTracingPerformed() {
        testClassAnnotationApp.serviceEndpointA();
        // Verify that the instrumentation generates the expected Span data.
        // TBD
    }

    /**
     * Test method of a class is not traced when class is annotated with @Trace,
     * and the method is annotated with @NoTrace.
     */
    @Test
    public void noTraceAnnotationHonored() {
        testClassAnnotationApp.serviceEndpointB();
        // Verify that the instrumentation generates the expected Span data.
        // TBD
    }

    /**
     * Test method annotation with trace point name specified has name honored
     * when @Trace is also specified at Class level.
     */
    @Test
    public void nameParameterHonoredWithClassAnnotation() {
        testClassAnnotationApp.serviceEndpointC();
        // Verify that the instrumentation generates the expected Span data.
        // TBD
    }

    /**
     * Test method annotation works when @Trace not specified at Class level.
     */
    @Test
    public void methodAnnotatedTracingPerformed() {
        testMethodAnnotationApp.serviceEndpointA();
        // Verify that the instrumentation generates the expected Span data.
        // TBD
    }

    /**
     * Test method not traced if no @Trace annotation on Class or method.
     */
    @Test
    public void noMethodAnnotationHonored() {
        testMethodAnnotationApp.serviceEndpointB();

    }

    /**
     * Test name parameter honored on method level @Trace.
     */
    @Test
    public void nameParameterHonoredOnMethodAnnotation() {
        testMethodAnnotationApp.serviceEndpointC();
    }

    /**
     * Test relationship parameter honored on method level @Trace.
     */
    @Test
    public void relationshipParameterHonoredOnMethodAnnotation() {
        testMethodAnnotationApp.serviceEndpointD();
    }

    /**
     * Test name and relationship parameters honored on method level @Trace.
     */
    @Test
    public void nameAndRelationshipParameterHonoredOnMethodAnnotation() {
        testMethodAnnotationApp.serviceEndpointE();
    }
}
