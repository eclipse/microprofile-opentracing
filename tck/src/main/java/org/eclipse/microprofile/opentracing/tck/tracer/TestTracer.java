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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.inject.Alternative;

import org.eclipse.microprofile.opentracing.tck.tracer.TestSpanTree.TreeNode;

import io.opentracing.ActiveSpan;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;

/**
 * Test Tracer.
 * The {@link javax.enterprise.inject.Alternative} annotation
 * is needed so that this doesn't get injected instead of the
 * container's {@link io.opentracing.Tracer}.
 */
@Alternative
public class TestTracer implements Tracer {

    /**
     * List of accumulated spans.
     */
    private List<TestSpan> spans = new ArrayList<TestSpan>();

    /**
     * Get a list of accumulated spans.
     * @return List of spans.
     */
    public List<TestSpan> getSpans() {
        return spans;
    }

    /**
     * Set the list of spans.
     * @param newSpans List of spans.
     */
    public void setSpans(final List<TestSpan> newSpans) {
        this.spans = newSpans;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ActiveSpan activeSpan() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ActiveSpan makeActive(final Span span) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SpanBuilder buildSpan(final String operationName) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <C> void inject(final SpanContext spanContext,
            final Format<C> format, final C carrier) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <C> SpanContext extract(final Format<C> format, final C carrier) {
        throw new UnsupportedOperationException();
    }

    /**
     * Convert the list of spans into a tree.
     * @return Tree of spans.
     */
    public TestSpanTree spanTree() {
        TestSpanTree tree = new TestSpanTree();
        Map<Long, TreeNode<TestSpan>> map = new HashMap<>();
        for (TestSpan span : spans) {
            if (span.getParentId() == 0) {
                TreeNode<TestSpan> node = tree.addRootSpan(span);
                map.put(span.getSpanId(), node);
            }
            else {
                TreeNode<TestSpan> parent = map.get(span.getParentId());
                if (parent != null) {
                    TreeNode<TestSpan> node = parent.addChild(span);
                    map.put(span.getSpanId(), node);
                }
                else {
                    throw new IllegalStateException(
                            "Could not find parent span with ID "
                            + span.getParentId() + " for span " + span);
                }
            }
        }
        return tree;
    }
}
