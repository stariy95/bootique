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

import io.bootique.yaml.node.Node;
import io.bootique.yaml.node.Mapping;
import io.bootique.yaml.node.Scalar;
import io.bootique.yaml.node.Sequence;

/**
 * A path segment for case-insensitive path.
 *
 * @since 2.0
 */
class CiPropertySegment extends PropertyPathSegment {

    static PathSegment<?> create(Node node, String path) {
        if (path.length() == 0) {
            return new LastPathSegment(node, null, null);
        }

        if (path.charAt(0) == ARRAY_INDEX_START) {
            return new IndexPathSegment(toSequence(node), null, null, path);
        }

        return new CiPropertySegment(toMapping(node), null, null, path);
    }

    protected CiPropertySegment(Mapping node, PathSegment parent, String incomingPath, String remainingPath) {
        super(node, parent, incomingPath, remainingPath);
    }

    @Override
    protected Node readChild(String childName) {
        if(node == null) {
            return null;
        }
        Node key = getChildCiKey(node, childName);
        return node.get(key);
    }

    @Override
    protected PathSegment<Sequence> createIndexedChild(String childName, String remainingPath) {
        throw new UnsupportedOperationException("Indexed CI children are unsupported");
    }

    @Override
    protected PathSegment<Mapping> createPropertyChild(String childName, String remainingPath) {
        Mapping on = toMapping(readChild(childName));
        return new CiPropertySegment(on, this, childName, remainingPath);
    }

    private Node getChildCiKey(Mapping parent, String fieldName) {
        fieldName = fieldName.toUpperCase();
        for(Node key: parent.keys()) {
            if (fieldName.equalsIgnoreCase(key.asScalar().asString())) {
                return key;
            }
        }

        return new Scalar(fieldName);
    }

}
