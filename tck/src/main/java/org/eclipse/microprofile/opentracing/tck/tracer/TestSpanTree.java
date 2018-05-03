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
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * Represents a tree of spans.
 */
public class TestSpanTree implements ConsumableTree<TestSpan> {

    /**
     * List of all root spans.
     */
    private final List<TreeNode<TestSpan>> rootSpans = new ArrayList<>();

    /**
     * JSON display name of node.
     */
    private String nodeName = "span";

    /**
     * Create a tree with the default JSON display node name.
     */
    public TestSpanTree() {
    }

    /**
     * Create a tree with a particular JSON display node name.
     * @param jsonNodeName JSON node name.
     */
    public TestSpanTree(final String jsonNodeName) {
        this.nodeName = jsonNodeName;
    }

    /**
     * Create a tree with a particular JSON display node name.
     * @param newRootSpans Root spans
     */
    @SafeVarargs
    public TestSpanTree(final TreeNode<TestSpan>... newRootSpans) {
        for (TreeNode<TestSpan> rootSpan : newRootSpans) {
            rootSpans.add(rootSpan);
        }
    }

    /**
     * Return a list of this tree's root spans.
     * @return List of root spans.
     */
    public List<TreeNode<TestSpan>> getRootSpans() {
        return rootSpans;
    }

    /**
     * Add a root span.
     * @param node The node to add.
     * @return The added node.
     */
    public TreeNode<TestSpan> addRootNode(final TreeNode<TestSpan> node) {
        rootSpans.add(node);
        return node;
    }

    /**
     * Generic tree structure.
     * @param <T> Type of data to hold in the node.
     */
    public static class TreeNode<T> implements ConsumableTree<T> {

        /**
         * List of child nodes.
         */
        private final List<TreeNode<T>> children = new ArrayList<>();

        /**
         * Data held in the node.
         */
        private final T data;

        /**
         * Create a new node.
         * @param nodeData Data held in the node.
         * @param newChildren Child nodes
         */
        @SafeVarargs
        public TreeNode(final T nodeData, final TreeNode<T>... newChildren) {
            this.data = nodeData;
            for (TreeNode<T> child : newChildren) {
                children.add(child);
            }
        }

        /**
         * Return the data in the node.
         * @return Data
         */
        public T getData() {
            return data;
        }

        /**
         * Return the child nodes.
         * @return Child nodes.
         */
        public List<TreeNode<T>> getChildren() {
            return children;
        }

        /**
         * Add new child to this node.
         * @param child Child to add.
         * @return Newly created node.
         */
        public TreeNode<T> addChild(final TreeNode<T> child) {
            children.add(child);
            return child;
        }

        /**
         * Return JSON representation.
         * @param nodeName Node name to display in JSON.
         * @param indentationLevel How many instances of indentationCharacters
         * @param indentationCharacters The indentation characters to use
         * @return JSON
         */
        public String toJSON(final String nodeName, final int indentationLevel,
                final String indentationCharacters) {
            StringBuilder sb = new StringBuilder();
            String indent = String.join("", Collections
                    .nCopies(indentationLevel, indentationCharacters));
            String indent2 = indent + indentationCharacters;
            sb.append(indent);
            sb.append('{');
            sb.append(System.lineSeparator());
            sb.append(indent2);
            sb.append(nodeName);
            sb.append(": \"");
            sb.append(data);
            if (children.size() > 0) {
                sb.append("\",");
                sb.append(System.lineSeparator());
                sb.append(indent2);
                sb.append("children: [");
                sb.append(System.lineSeparator());

                for (int i = 0; i < children.size(); i++) {
                    TreeNode<T> child = children.get(i);
                    sb.append(child.toJSON(nodeName, indentationLevel + 2,
                            indentationCharacters));
                    if (i < children.size() - 1) {
                        sb.append(',');
                    }
                    sb.append(System.lineSeparator());
                }

                sb.append(indent2);
                sb.append(']');
            }
            else {
                sb.append('\"');
            }
            sb.append(System.lineSeparator());
            sb.append(indent);
            sb.append('}');
            return sb.toString();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals(final Object obj) {
            @SuppressWarnings("unchecked")
            TreeNode<T> otherNode = (TreeNode<T>) obj;
            if (otherNode != null) {
                if (!data.equals(otherNode.data)) {
                    return false;
                }
                if (otherNode.children.size() != children.size()) {
                    System.err
                            .println("MISMATCH: Child span counts don't match: "
                                    + children.size() + " ; "
                                    + otherNode.children.size());
                    return false;
                }
                for (int i = 0; i < children.size(); i++) {
                    TreeNode<T> x = children.get(i);
                    TreeNode<T> y = otherNode.children.get(i);
                    if (!x.equals(y)) {
                        return false;
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

        /**
         * Recursively visit all nodes with the lambda.
         * @param visitor Lambda
         */
        @Override
        public void visitTree(final Consumer<? super T> visitor) {
            visitor.accept(this.data);
            for (TreeNode<T> child : this.children) {
                child.visitTree(visitor);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return toJSON(data.getClass().getSimpleName(), 1, "  ");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        sb.append(System.lineSeparator());

        for (int i = 0; i < rootSpans.size(); i++) {
            TreeNode<TestSpan> node = rootSpans.get(i);
            sb.append(node.toJSON(nodeName, 1, "  "));
            if (i < rootSpans.size() - 1) {
                sb.append(',');
            }
            sb.append(System.lineSeparator());
        }

        sb.append(']');
        sb.append(System.lineSeparator());
        return sb.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        TestSpanTree otherTree = (TestSpanTree) obj;
        if (otherTree != null) {
            if (otherTree.rootSpans.size() != rootSpans.size()) {
                System.err.println("MISMATCH: Root span counts don't match: "
                        + rootSpans.size() + " ; "
                        + otherTree.rootSpans.size());
                return false;
            }
            for (int i = 0; i < rootSpans.size(); i++) {
                TreeNode<TestSpan> x = rootSpans.get(i);
                TreeNode<TestSpan> y = otherTree.rootSpans.get(i);
                if (!x.equals(y)) {
                    return false;
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

    /**
     * Visit every {@link TestSpan} in this tree with the {@code visitor}
     * lambda.
     * @param visitor Lambda
     */
    @Override
    public void visitTree(final Consumer<? super TestSpan> visitor) {
        for (TreeNode<TestSpan> node : this.rootSpans) {
            node.visitTree(visitor);
        }
    }
}
