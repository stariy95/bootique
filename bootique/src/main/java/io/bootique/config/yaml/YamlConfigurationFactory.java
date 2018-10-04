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

import io.bootique.config.ConfigurationFactory;
import io.bootique.type.TypeRef;
import io.bootique.yaml.mapper.Mapper;
import io.bootique.yaml.node.Mapping;
import io.bootique.yaml.node.Node;

/**
 * @since 2.0
 */
public class YamlConfigurationFactory implements ConfigurationFactory {

    private Node rootNode;
    private Mapper mapper;

    public YamlConfigurationFactory(Node rootNode, Mapper mapper) {
        this.rootNode = rootNode;
        this.mapper = mapper;
    }

    @Override
    public <T> T config(Class<T> type, String prefix) {
        Node child = findChild(prefix);
        return mapper.map(child, type);
    }

    @Override
    public <T> T config(TypeRef<? extends T> type, String prefix) {
        Node child = findChild(prefix);
        return mapper.map(child, type.getType());
    }

    protected Node findChild(String path) {
        return CiPropertySegment
                .create(rootNode, path)
                .lastPathComponent().map(PathSegment::getNode)
                .orElse(new Mapping());
    }
}
