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

import java.util.concurrent.ExecutionException;
import org.eclipse.microprofile.opentracing.tck.application.TestServerWebServices;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

/**
 * @author Pavol Loffay
 */
public class OpenTracingMpRestClientTests extends OpenTracingBaseTests {

    @Deployment
    public static WebArchive createDeployment() {
        return OpenTracingBaseTests.createDeployment();
    }

    /**
     * Test a web service call that makes nested calls.
     */
    @Test
    @RunAsClient
    private void testNestedSpans() {
        int nestDepth = 1;
        int nestBreadth = 2;
        int uniqueId = getRandomNumber();
        boolean failNest = false;
        boolean async = false;
        testNestedSpans(TestServerWebServices.REST_NESTED_MP_REST_CLIENT, nestDepth, nestBreadth, uniqueId, failNest, async);
    }

    /**
     * Test a web service call that makes nested calls with a client failure.
     *
     * TODO could not test with SmallRye due to https://github.com/smallrye/smallrye-rest-client/issues/11
     */
//    @Test
//    @RunAsClient
    private void testNestedSpansWithClientFailure() {
        int nestDepth = 1;
        int nestBreadth = 2;
        int uniqueId = getRandomNumber();
        boolean failNest = true;
        boolean async = false;
        testNestedSpans(TestServerWebServices.REST_NESTED_MP_REST_CLIENT, nestDepth, nestBreadth, uniqueId, failNest, async);
    }

    /**
     * Test the nested web service concurrently. A unique ID is generated
     * in the URL of each request and propagated down the nested spans.
     * We extract this out of the resulting spans and ensure the unique
     * IDs are correct.
     * @throws InterruptedException Problem executing web service.
     * @throws ExecutionException Thread pool problem.
     *
     * TODO smallrye does not support async spec yet!
     */
//    @Test
//    @RunAsClient
    private void testMultithreadedNestedSpans() throws ExecutionException, InterruptedException {
        int numberOfCalls = 100;
        int nestDepth = 1;
        int nestBreadth = 2;
        boolean failNest = false;
        boolean async = false;

        testMultithreadedNestedSpans(TestServerWebServices.REST_NESTED_MP_REST_CLIENT, numberOfCalls, nestDepth, nestBreadth, failNest, async);
    }

    /**
     * Same as testMultithreadedNestedSpans but asynchronous client and nested requests.
     * @throws InterruptedException Problem executing web service.
     * @throws ExecutionException Thread pool problem.
     *
     * TODO smallrye does not support async spec yet!
     */
//    @Test
//    @RunAsClient
    private void testMultithreadedNestedSpansAsync() throws ExecutionException, InterruptedException {
        int numberOfCalls = 100;
        int nestDepth = 1;
        int nestBreadth = 2;
        boolean failNest = false;
        boolean async = true;

        testMultithreadedNestedSpans(TestServerWebServices.REST_NESTED_MP_REST_CLIENT, numberOfCalls, nestDepth, nestBreadth, failNest, async);
    }
}
