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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import javax.inject.Inject;

import org.eclipse.microprofile.opentracing.tck.annotationimpls.TraceInterceptor;
import org.eclipse.microprofile.opentracing.tck.application.TestClassAnnotationApp;
import org.eclipse.microprofile.opentracing.tck.application.TestMethodAnnotationApp;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
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

    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private PrintStream outSave;

    @Before
    public void setUpStreams() {
        outSave = System.out;
        System.setOut(new PrintStream(outContent));
    }

    @After
    public void cleanUpStreams() {
        System.setOut(outSave);
        System.out.print(outContent.toString());
        System.out.flush();
    }

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
                .addClass(TraceInterceptor.class)
                .addAsManifestResource(
                new StringAsset(
                  "<beans>\n"
                  + "  <interceptors>\n"
                  + "    <class>"
                  + "org.eclipse.microprofile.opentracing.tck.annotationimpls."
                  + "TraceInterceptor"
                  + "    </class>\n"
                  + "  </interceptors>\n"
                  + "</beans>"),
                  "beans.xml");
    }

    /**
     * Make sure all methods of the class a traced.
     */
    @Test
    public void classAnnotatedTracingPerformed() {
        String name = "org.eclipse.microprofile.opentracing.tck.application.TestClassAnnotationApp"
                + ".serviceEndpointA";
        String relationship = "child_of";
        System.out.println(testClassAnnotationApp.serviceEndpointA());
        Assert.assertTrue(outContent.toString().contains(
                "Create new Span ["
                        + name
                        + "] with relationship ["
                        + relationship
                        + "]"));
        Assert.assertTrue(outContent.toString().contains(
                "Invoked"));
    }

    @Test
    public void noTraceAnnotationHonored() {
        System.out.println(testClassAnnotationApp.serviceEndpointB());
        Assert.assertTrue(!outContent.toString().contains(
                "Create new Span ["));
        Assert.assertTrue(outContent.toString().contains(
                "Invoked"));
    }

    @Test
    public void nameParameterHonoredWithClassAnnotation() {
        String name = "ClassAnnotated.endpointC";
        String relationship = "child_of";
        System.out.println(testClassAnnotationApp.serviceEndpointC());
        Assert.assertTrue(outContent.toString().contains(
                "Create new Span ["
                        + name
                        + "] with relationship ["
                        + relationship
                        + "]"));
        Assert.assertTrue(outContent.toString().contains(
                "Invoked"));
    }

    @Test
    public void methodAnnotatedTracingPerformed() {
        String name = "org.eclipse.microprofile.opentracing.tck.application.TestMethodAnnotationApp"
                + ".serviceEndpointA";
        String relationship = "child_of";
        System.out.println(testMethodAnnotationApp.serviceEndpointA());
        Assert.assertTrue(outContent.toString().contains(
                "Create new Span ["
                        + name
                        + "] with relationship ["
                        + relationship
                        + "]"));
        Assert.assertTrue(outContent.toString().contains(
                "Invoked"));
    }

    @Test
    public void noMethodAnnotationHonored() { 
        System.out.println(testMethodAnnotationApp.serviceEndpointB());
        Assert.assertTrue(!outContent.toString().contains(
                "Create new Span ["));
        Assert.assertTrue(outContent.toString().contains(
                "Invoked"));
    }

    @Test
    public void nameParameterHonoredOnMethodAnnotation() {
        String name = "MethodAnnotated.endpointC";
        String relationship = "child_of";
        System.out.println(testMethodAnnotationApp.serviceEndpointC());
        Assert.assertTrue(outContent.toString().contains(
                "Create new Span ["
                        + name
                        + "] with relationship ["
                        + relationship
                        + "]"));
        Assert.assertTrue(outContent.toString().contains(
                "Invoked"));
    }

    @Test
    public void relationshipParameterHonoredOnMethodAnnotation() {
        String name = "org.eclipse.microprofile.opentracing.tck.application.TestMethodAnnotationApp"
                + ".serviceEndpointD";
        String relationship = "follows_from";
        System.out.println(testMethodAnnotationApp.serviceEndpointD());
        Assert.assertTrue(outContent.toString().contains(
                "Create new Span ["
                        + name
                        + "] with relationship ["
                        + relationship
                        + "]"));
        Assert.assertTrue(outContent.toString().contains(
                "Invoked"));
    }

    @Test
    public void nameAndRelationshipParameterHonoredOnMethodAnnotation() {
        String name = "MethodAnnotated.endpointE";
        String relationship = "follows_from";
        System.out.println(testMethodAnnotationApp.serviceEndpointE());
        Assert.assertTrue(outContent.toString().contains(
                "Create new Span ["
                        + name
                        + "] with relationship ["
                        + relationship
                        + "]"));
        Assert.assertTrue(outContent.toString().contains(
                "Invoked"));
    }
}
