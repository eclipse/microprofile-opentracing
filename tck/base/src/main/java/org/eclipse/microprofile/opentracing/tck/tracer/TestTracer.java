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
package org.eclipse.microprofile.opentracing.tck.tracer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.opentracing.tck.tracer.TestSpanTree.TreeNode;

import io.opentracing.Scope;
import io.opentracing.ScopeManager;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import jakarta.enterprise.inject.Alternative;

/**
 * Test Tracer. The {@link jakarta.enterprise.inject.Alternative} annotation is needed so that this doesn't get injected
 * instead of the container's {@link io.opentracing.Tracer}.
 */
@Alternative
public class TestTracer implements Tracer {

    /**
     * List of accumulated spans.
     */
    private List<TestSpan> spans = new ArrayList<TestSpan>();

    /**
     * Get a list of accumulated spans.
     * 
     * @return List of spans.
     */
    public List<TestSpan> getSpans() {
        return spans;
    }

    /**
     * Set the list of spans.
     * 
     * @param newSpans
     *            List of spans.
     */
    public void setSpans(final List<TestSpan> newSpans) {
        this.spans = newSpans;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ScopeManager scopeManager() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Span activeSpan() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Scope activateSpan(Span span) {
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

    @Override
    public void close() {
    }

    /**
     * Convert the list of spans into a tree.
     * 
     * @return Tree of spans.
     */
    public TestSpanTree spanTree() {
        TestSpanTree tree = new TestSpanTree();
        Map<Long, TreeNode<TestSpan>> map = new HashMap<>();
        for (TestSpan span : spans) {
            map.put(span.getSpanId(), new TreeNode<>(span));
        }

        for (TestSpan span : spans) {
            TreeNode<TestSpan> spanTreeNode = map.get(span.getSpanId());
            if (span.getParentId() == 0 || map.get(span.getParentId()) == null) {
                tree.addRootNode(spanTreeNode);
            } else {
                TreeNode<TestSpan> parentNode = map.get(span.getParentId());
                if (parentNode != null) {
                    parentNode.addChild(spanTreeNode);
                }
            }
        }
        return tree;
    }
}
