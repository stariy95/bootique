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

import java.util.Map;
import java.util.function.Function;

import io.bootique.yaml.node.Node;


/**
 * Overrides JsonNode object values from a map of properties.
 *
 * @since 2.0
 */
class InPlaceMapOverrider implements Function<Node, Node> {

    private Map<String, String> properties;

    InPlaceMapOverrider(Map<String, String> properties) {
        this.properties = properties;
    }

    @Override
    public Node apply(Node t) {
        for(Map.Entry<String, String> entry: properties.entrySet()) {
            PathSegment target = lastPathComponent(t, entry.getKey());
            target.fillMissingParents();
            if (target.getParent() == null) {
                throw new IllegalArgumentException("No parent node");
            }
            target.getParent().writeChildValue(target.getIncomingPath(), entry.getValue());
        }
        return t;
    }

    private PathSegment<?> lastPathComponent(Node t, String path) {
        return PathSegment.create(t, path).lastPathComponent().get();
    }
}
