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

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.opentracing.Traced;
import org.eclipse.microprofile.opentracing.tck.tracer.TestSpan;
import org.eclipse.microprofile.opentracing.tck.tracer.TestTracer;

import io.opentracing.Tracer;

/**
 * @author Pavol Loffay
 */
@Path(TracerWebService.REST_TRACER_SERVICE_PATH)
@Traced(value = false)
public class TracerWebService {
    /**
     * Web service exposing tracer related endpoints.
     */
    public static final String REST_TRACER_SERVICE_PATH = "tracer";

    /**
     * Web service endpoint for the getTracer call.
     */
    public static final String REST_GET_TRACER = "getTracer";

    /**
     * Web service endpoint for the clearTracer call.
     */
    public static final String REST_CLEAR_TRACER = "clearTracer";

    /**
     * Mock tracer.
     */
    @Inject
    private Tracer tracer;

    /**
     * Get details about completed spans.
     * Returns a
     * {@link org.eclipse.microprofile.opentracing.tck.tracer.TestTracer}
     * which has information on the spans.
     * @return Injected tracer
     * @throws SecurityException Reflection issues
     * @throws NoSuchMethodException Reflection issues
     * @throws InvocationTargetException Reflection issues
     * @throws IllegalArgumentException Reflection issues
     * @throws IllegalAccessException Reflection issues
     */
    @SuppressWarnings("unchecked")
    @GET
    @Path(TracerWebService.REST_GET_TRACER)
    @Produces(MediaType.APPLICATION_JSON)
    public Tracer getTracer() throws NoSuchMethodException, SecurityException,
        IllegalAccessException, IllegalArgumentException,
        InvocationTargetException {
        TestTracer testTracer = new TestTracer();
        List<TestSpan> spans = new ArrayList<TestSpan>();

        try {
            Iterable<?> finishedSpans = (Iterable<?>) tracer.getClass()
                    .getMethod("finishedSpans").invoke(tracer);
            for (Object finishedSpan : finishedSpans) {
                TestSpan testSpan = new TestSpan();

                testSpan.setStartMicros((Long) finishedSpan.getClass()
                        .getMethod("startMicros").invoke(finishedSpan));

                testSpan.setFinishMicros((Long) finishedSpan.getClass()
                        .getMethod("finishMicros").invoke(finishedSpan));

                testSpan.setCachedOperationName((String) finishedSpan.getClass()
                        .getMethod("operationName").invoke(finishedSpan));

                testSpan.setParentId((Long) finishedSpan.getClass()
                        .getMethod("parentId").invoke(finishedSpan));

                Object context = finishedSpan.getClass().getMethod("context")
                        .invoke(finishedSpan);

                testSpan.setSpanId((Long) context.getClass().getMethod("spanId")
                        .invoke(context));

                testSpan.setTraceId((Long) context.getClass()
                        .getMethod("traceId").invoke(context));

                testSpan.setTags((Map<String, Object>) finishedSpan.getClass()
                        .getMethod("tags").invoke(finishedSpan));

                List<?> logEntries = (List<?>) finishedSpan.getClass()
                        .getMethod("logEntries").invoke(finishedSpan);
                for (Object logEntry : logEntries) {
                    testSpan.getLogEntries().add((Map<String, ?>) logEntry
                            .getClass().getMethod("fields").invoke(logEntry));
                }

                spans.add(testSpan);
            }
        }
        catch (NoSuchMethodException nsme) {
            // This is a likely enough exception - almost surely meaning
            // that the Tracer that's injected is not a MockTrader - that
            // we re-throw it with a more meaningful explanation.
            throw new RuntimeException(
                    "The injected Tracer is required to be an instance of io.opentracing.mock.MockTracer but is instead an instance of "
                            + tracer,
                    nsme);
        }

        testTracer.setSpans(spans);
        return testTracer;
    }

    /**
     * Clear accumulated spans in the Tracer.
     * @throws IllegalAccessException Reflection issues
     * @throws IllegalArgumentException Reflection issues
     * @throws InvocationTargetException Reflection issues
     * @throws NoSuchMethodException Reflection issues
     * @throws SecurityException Reflection issues
     */
    @DELETE
    @Path(TracerWebService.REST_CLEAR_TRACER)
    public void clearTracer() throws IllegalAccessException,
        IllegalArgumentException, InvocationTargetException,
        NoSuchMethodException, SecurityException {
        tracer.getClass().getMethod("reset").invoke(tracer);
    }
}
