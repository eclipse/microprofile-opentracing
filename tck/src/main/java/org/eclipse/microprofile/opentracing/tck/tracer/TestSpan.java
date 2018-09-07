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
package org.eclipse.microprofile.opentracing.tck.tracer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import io.opentracing.Span;
import io.opentracing.SpanContext;

/**
 * Test Tracer.
 */
public class TestSpan implements Span {

    /**
     * Start time of the span.
     */
    private long startMicros;

    /**
     * End time of the span.
     */
    private long finishMicros;

    /**
     * Trace ID of the span.
     */
    private long traceId;

    /**
     * Parent ID of the span.
     */
    private long parentId;

    /**
     * Span ID.
     */
    private long spanId;

    /**
     * Operation name of the span.
     */
    private String cachedOperationName;

    /**
     * Tags.
     */
    private Map<String, Object> tags = new HashMap<>();

    /**
     * Log entries.
     */
    private List<Map<String, ?>> logEntries = new ArrayList<>();

    /**
     * Simulated by the test harness for comparison to the real span.
     */
    @SuppressWarnings("unused")
    private boolean simulated;

    /**
     * Create blank span.
     */
    public TestSpan() {
    }

    /**
     * Create span with a particular kind and operation name.
     * @param operationName Operation name
     * @param tags Tags associated with span
     * @param logEntries Log entries
     */
    public TestSpan(final String operationName, Map<String, Object> tags, List<Map<String, ?>> logEntries) {
        this.simulated = true;
        this.cachedOperationName = operationName;
        this.tags = new HashMap<>(tags);
        this.logEntries = logEntries;
    }

    /**
     * Start time of the span.
     * @return the startMicros
     */
    public long getStartMicros() {
        return startMicros;
    }

    /**
     * Set the start time of the span.
     * @param newSartMicros the startMicros to set
     */
    public void setStartMicros(final long newSartMicros) {
        this.startMicros = newSartMicros;
    }

    /**
     * End time of the span.
     * @return the finishMicros
     */
    public long getFinishMicros() {
        return finishMicros;
    }

    /**
     * Set the finish time of the span.
     * @param newFinishMicros the finishMicros to set
     */
    public void setFinishMicros(final long newFinishMicros) {
        this.finishMicros = newFinishMicros;
    }

    /**
     * Get the trace ID of the span.
     * @return the traceId
     */
    public long getTraceId() {
        return traceId;
    }

    /**
     * Set the trace ID of the span.
     * @param newTraceId the traceId to set
     */
    public void setTraceId(final long newTraceId) {
        this.traceId = newTraceId;
    }

    /**
     * Get the Parent ID of the span.
     * @return the parentId
     */
    public long getParentId() {
        return parentId;
    }

    /**
     * Set the parent ID of the span.
     * @param newParentId the parentId to set
     */
    public void setParentId(final long newParentId) {
        this.parentId = newParentId;
    }

    /**
     * Get the Span ID.
     * @return the spanId
     */
    public long getSpanId() {
        return spanId;
    }

    /**
     * Set the span ID.
     * @param newSpanId the spanId to set
     */
    public void setSpanId(final long newSpanId) {
        this.spanId = newSpanId;
    }

    /**
     * Get the operation name of the span.
     * @return the operationName
     */
    public String getCachedOperationName() {
        return cachedOperationName;
    }

    /**
     * Set the operation name of the span.
     * @param newCachedOperationName the operationName to set
     */
    public void setCachedOperationName(final String newCachedOperationName) {
        this.cachedOperationName = newCachedOperationName;
    }

    /**
     * Return a map of tags.
     * @return the tags
     */
    public Map<String, Object> getTags() {
        return tags;
    }

    /**
     * Set the map of tags.
     * @param newTags the tags to set
     */
    public void setTags(final Map<String, Object> newTags) {
        this.tags = newTags;
    }

    /**
     * Return a list of log entries.
     * @return the log entries
     */
    public List<Map<String, ?>> getLogEntries() {
        return logEntries;
    }

    /**
     * Set the list of log entries
     * @param newLogEntries the log entries to set
     */
    public void setLogEntries(final List<Map<String, ?>> newLogEntries) {
        this.logEntries = newLogEntries;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SpanContext context() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Span setTag(final String key, final String value) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Span setTag(final String key, final boolean value) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Span setTag(final String key, final Number value) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Span log(final Map<String, ?> fields) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Span log(final long timestampMicroseconds,
            final Map<String, ?> fields) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Span log(final String event) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Span log(final long timestampMicroseconds, final String event) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Span setBaggageItem(final String key, final String value) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getBaggageItem(final String key) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Span setOperationName(final String operationName) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void finish() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void finish(final long newFinishMicros) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        // Only print the parts that are checked in equals so that an
        // assertion failure is easy to understand.

        // return "{ " + "startMicros: " + startMicros
        //         + ", finishMicros: " + finishMicros + ", traceId: "
        //         + traceId + ", parentId: " + parentId + ", spanId: "
        //         + spanId + ", operationName: " + cachedOperationName
        //         + ", tags: " + tags + " }";

        // Sort the tags to make it easier to visually compare object outputs.
        List<Entry<String, Object>> tagsList = new ArrayList<>();
        tagsList.addAll(tags.entrySet());
        tagsList.sort(new Comparator<Entry<String, Object>>() {
            @Override
            public int compare(Entry<String, Object> x,
                    Entry<String, Object> y) {
                return x.getKey().compareTo(y.getKey());
            }
        });

        return "{ " + "operationName: " + cachedOperationName + ", tags: "
                + tagsList + ", logEntries: " + logEntries + "}";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        TestSpan otherSpan = (TestSpan) obj;
        if (otherSpan != null) {
            if (!cachedOperationName.equals(otherSpan.cachedOperationName)) {
                System.err.println("MISMATCH: Operation names don't match: "
                        + cachedOperationName + " ; "
                        + otherSpan.cachedOperationName);
                return false;
            }

            if (tags.size() != otherSpan.tags.size()) {
                System.err.println("MISMATCH: Number of tags doesn't match");
                return false;
            }

            if (!tags.equals(otherSpan.tags)) {
                return false;
            }

            if (logEntries.size() != otherSpan.logEntries.size()) {
                System.err.println(
                        "MISMATCH: Number of log entries doesn't match ("
                                + logEntries.size() + ", "
                                + otherSpan.logEntries.size() + ")");
                return false;
            }

            for (int i = 0; i < logEntries.size(); i++) {
                Map<String, ?> logFieldsX = logEntries.get(i);
                Map<String, ?> logFieldsY = otherSpan.logEntries.get(i);
                if (logFieldsX.size() != logFieldsY.size()) {
                    System.err.printf("MISMATCH: Number of log fields doesn't match (%d, %d)\n", logFieldsX.size(), logFieldsY.size());
                    return false;
                }

                for (Map.Entry<String, ?> logEntry: logFieldsX.entrySet()) {
                    Object valY = logFieldsY.get(logEntry.getKey());
                    if (valY == null) {
                        System.err.printf("Log field %s not present in the logs\n", logEntry.getKey());
                        return false;
                    }

                    if (!logEntry.getKey().equals("error.object")) {
                        if (!valY.equals(logEntry.getValue())) {
                            System.err.printf("Log values do not match key=%s, %s, %s\n\n\n\n", logEntry.getKey(), logEntry.getValue(), valY);
                            return false;
                        }
                    }
                }
            }

            return true;
        }
        return super.equals(obj);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
