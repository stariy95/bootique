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
import java.util.Objects;
import java.util.Optional;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import io.bootique.yaml.node.Mapping;
import io.bootique.yaml.node.Node;

/**
 * A helper that orchestrates configuration loading.
 *
 * @since 2.0
 */
class YamlNodeConfigurationBuilder {

    private Supplier<Stream<URL>> resourceStreamSupplier;
    private Function<URL, Optional<Node>> parser;
    private BinaryOperator<Node> merger;
    private Function<Node, Node> overrider;

    private YamlNodeConfigurationBuilder() {
    }

    public static YamlNodeConfigurationBuilder builder() {
        return new YamlNodeConfigurationBuilder();
    }

    public YamlNodeConfigurationBuilder resources(Supplier<Stream<URL>> streamSupplier) {
        this.resourceStreamSupplier = streamSupplier;
        return this;
    }

    public YamlNodeConfigurationBuilder overrider(Function<Node, Node> overrider) {
        this.overrider = overrider;
        return this;
    }

    public YamlNodeConfigurationBuilder parser(Function<URL, Optional<Node>> parser) {
        this.parser = parser;
        return this;
    }

    public YamlNodeConfigurationBuilder merger(BinaryOperator<Node> merger) {
        this.merger = merger;
        return this;
    }

    public Node build() {

        Objects.requireNonNull(resourceStreamSupplier);
        Objects.requireNonNull(parser);
        Objects.requireNonNull(merger);

        Node rootNode;

        try (Stream<URL> sources = resourceStreamSupplier.get()) {
            rootNode = sources
                    .map(parser)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .reduce(merger)
                    .orElseGet(Mapping::new);
        }

        return overrider.apply(rootNode);
    }
}
