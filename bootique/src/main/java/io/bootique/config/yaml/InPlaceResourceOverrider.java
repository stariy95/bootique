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

import java.net.URL;
import java.util.Optional;
import java.util.function.BinaryOperator;
import java.util.function.Function;

import io.bootique.yaml.node.Node;

/**
 * Overrides JsonNode object values from one configuration resource.
 *
 * @since 2.0
 */
class InPlaceResourceOverrider implements Function<Node, Node> {

    private URL source;
    private Function<URL, Optional<Node>> parser;
    private BinaryOperator<Node> merger;

    InPlaceResourceOverrider(URL source, Function<URL, Optional<Node>> parser, BinaryOperator<Node> merger) {
        this.source = source;
        this.parser = parser;
        this.merger = merger;
    }

    @Override
    public Node apply(Node jsonNode) {
        return parser.apply(source)
                .map(configNode -> merger.apply(jsonNode, configNode))
                .orElse(jsonNode);
    }
}
