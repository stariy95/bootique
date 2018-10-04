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

import io.bootique.yaml.node.Mapping;
import io.bootique.yaml.node.Node;
import io.bootique.yaml.node.Scalar;

/**
 * @since 2.0
 */
class PropertyPathSegment extends PathSegment<Mapping> {

    PropertyPathSegment(Mapping node, PathSegment parent, String incomingPath, String remainingPath) {
        super(node, parent, incomingPath, remainingPath);
    }

    @Override
    Node readChild(String childName) {
        return node != null ? node.get(new Scalar(childName)) : null;
    }

    void writeChild(String childName, Node childNode) {
        node.set(new Scalar(childName), childNode);
    }

    @Override
    void writeChildValue(String childName, String value) {
        Node childNode = value == null ? null : new Scalar(value);
        writeChild(childName, childNode);
    }

    @Override
    protected PathSegment parseNextNotEmpty(String path) {

        int len = path.length();

        // Start at index 1, assuming at least one leading char is the property name.
        // Look for either '.' or '['.
        for (int i = 1; i < len; i++) {
            char c = path.charAt(i);
            if (c == DOT) {
                // split ppp.ppp into "ppp" and "ppp"
                return createPropertyChild(path.substring(0, i), path.substring(i + 1));
            }

            if (c == ARRAY_INDEX_START) {
                // split ppp[nnn].ppp into "ppp" and "[nnn].ppp"
                return createIndexedChild(path.substring(0, i), path.substring(i));
            }
        }

        // no separators found ... the whole path is the property name
        return createValueChild(path);
    }

    @Override
    protected Mapping createMissingNode() {
        return new Mapping();
    }
}
