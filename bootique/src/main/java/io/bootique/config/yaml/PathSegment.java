/*
 * Licensed to ObjectStyle LLC under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ObjectStyle LLC licenses
 * this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.bootique.config.yaml;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.StreamSupport;

import io.bootique.yaml.node.Mapping;
import io.bootique.yaml.node.Node;
import io.bootique.yaml.node.Sequence;

/**
 * A helper class to navigate {@link Node} objects.
 *
 * @since 2.0
 */
abstract class PathSegment<T extends Node> implements Iterable<PathSegment<?>> {

    static final char DOT = '.';
    static final char ARRAY_INDEX_START = '[';
    static final char ARRAY_INDEX_END = ']';

    protected T node;
    protected String incomingPath;
    protected String path;
    protected PathSegment parent;

    protected PathSegment(T node, PathSegment parent, String incomingPath, String path) {
        this.node = node;
        this.parent = parent;
        this.incomingPath = incomingPath;
        this.path = path;
    }

    static PathSegment<?> create(Node node, String path) {

        if (path.length() == 0) {
            return new LastPathSegment(node, null, null);
        }

        if (path.charAt(0) == ARRAY_INDEX_START) {
            return new IndexPathSegment(toSequence(node), null, null, path);
        }

        return new PropertyPathSegment(toMapping(node), null, null, path);
    }

    protected static Sequence toSequence(Node node) {
        if (node == null) {
            return null;
        }

        if (!node.isSequence()) {
            throw new IllegalArgumentException(
                    "Expected ARRAY node. Instead got " + node);
        }

        return node.asSequence();
    }

    protected static Mapping toMapping(Node node) {
        if (node == null) {
            return null;
        }

        if (!node.isObjectMapping()) {
            throw new IllegalArgumentException(
                        "Expected OBJECT node. Instead got " + node);
        }

        return node.asMapping();
    }

    Optional<PathSegment<?>> lastPathComponent() {
        return StreamSupport.stream(spliterator(), false).reduce((a, b) -> b);
    }

    Node getNode() {
        return node;
    }

    PathSegment<?> getParent() {
        return parent;
    }

    PathSegment<?> getRoot() {
        if(this.parent == null) {
            return this;
        }
        return parent.getRoot();
    }

    String getIncomingPath() {
        return incomingPath;
    }

    protected PathSegment<?> parseNext() {
        if (path == null) {
            return null;
        }

        int len = path.length();
        if (len == 0) {
            return null;
        }

        return parseNextNotEmpty(path);
    }

    protected abstract PathSegment<?> parseNextNotEmpty(String path);

    abstract Node readChild(String childName);

    abstract void writeChild(String childName, Node childNode);

    abstract void writeChildValue(String childName, String value);

    protected PathSegment<Node> createValueChild(String childName) {
        Node child = readChild(childName);
        return new LastPathSegment(child, this, childName);
    }

    protected PathSegment<Mapping> createPropertyChild(String childName, String remainingPath) {
        Mapping mapping = toMapping(readChild(childName));
        return new PropertyPathSegment(mapping, this, childName, remainingPath);
    }

    protected PathSegment<Sequence> createIndexedChild(String childName, String remainingPath) {
        Sequence an = toSequence(readChild(childName));
        return new IndexPathSegment(an, this, childName, remainingPath);
    }

    void fillMissingParents() {
        parent.fillMissingNodes(incomingPath, node);
    }

    protected abstract T createMissingNode();

    protected final void fillMissingNodes(String field, Node child) {
        if (node == null) {
            node = createMissingNode();
            parent.fillMissingNodes(incomingPath, node);
        }

        if (child != null) {
            writeChild(field, child);
        }
    }

    @Override
    public Iterator<PathSegment<?>> iterator() {
        return new Iterator<PathSegment<?>>() {

            private PathSegment current = PathSegment.this;
            private PathSegment next = current.parseNext();

            @Override
            public boolean hasNext() {
                return current != null;
            }

            @Override
            public PathSegment<?> next() {

                if (!hasNext()) {
                    throw new NoSuchElementException("Past iterator end");
                }

                PathSegment<?> r = current;
                current = next;
                next = current != null ? current.parseNext() : null;
                return r;
            }
        };
    }
}