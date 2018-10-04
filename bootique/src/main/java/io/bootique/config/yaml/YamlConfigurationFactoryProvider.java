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

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.function.Function;

import com.google.inject.Inject;
import com.google.inject.Provider;
import io.bootique.BootiqueException;
import io.bootique.cli.Cli;
import io.bootique.config.ConfigurationFactory;
import io.bootique.config.ConfigurationSource;
import io.bootique.config.OptionRefWithConfig;
import io.bootique.env.Environment;
import io.bootique.meta.application.OptionMetadata;
import io.bootique.yaml.YamlException;
import io.bootique.yaml.mapper.Mapper;
import io.bootique.yaml.mapper.MapperBuilder;
import io.bootique.yaml.node.Node;
import io.bootique.yaml.parser.DefaultParser;
import io.bootique.yaml.parser.Parser;
import joptsimple.OptionSpec;

import static java.util.function.Function.identity;

/**
 * @since 2.0
 */
public class YamlConfigurationFactoryProvider implements Provider<ConfigurationFactory> {

    @Inject
    private ConfigurationSource configurationSource;

    @Inject
    private Environment environment;

    @Inject
    private Set<OptionMetadata> optionMetadata;

    @Inject
    private Set<OptionRefWithConfig> optionDecorators;

    @Inject
    private Cli cli;

    protected Node loadConfiguration(Map<String, String> properties) {
        Parser yamlParser = new DefaultParser();

        Function<URL, Optional<Node>> parser = url -> {
            try {
                Node node = yamlParser.parse(url.openStream()).getNode();
                return Optional.ofNullable(node);
            } catch (IOException e) {
                throw new BootiqueException(1, "Config resource is not found or is inaccessible: " + url, e);
            } catch (YamlException e) {
                throw new BootiqueException(1, "Unable to parse config resource: " + url, e);
            }
        };

        BinaryOperator<Node> singleConfigMerger = new InPlaceLeftHandMerger();
        Function<Node, Node> overrider = andCliOptionOverrider(identity(), parser, singleConfigMerger);
        if (!properties.isEmpty()) {
            overrider = overrider.andThen(new InPlaceMapOverrider(properties));
        }

        return YamlNodeConfigurationBuilder.builder()
                .parser(parser)
                .merger(singleConfigMerger)
                .resources(configurationSource)
                .overrider(overrider)
                .build();
    }

    private Function<Node, Node> andCliOptionOverrider (
            Function<Node, Node> overrider,
            Function<URL, Optional<Node>> parser,
            BinaryOperator<Node> singleConfigMerger) {

        if (optionMetadata.isEmpty()) {
            return overrider;
        }

        List<OptionSpec<?>> detectedOptions = cli.detectedOptions();
        if (detectedOptions.isEmpty()) {
            return overrider;
        }

        for (OptionSpec<?> cliOpt : detectedOptions) {

            OptionMetadata omd = findMetadata(cliOpt);

            if (omd == null) {
                continue;
            }

            // config decorators are loaded first, and then can be overridden from options...
            for (OptionRefWithConfig decorator : optionDecorators) {
                if (decorator.getOptionName().equals(omd.getName())) {
                    overrider = overrider
                            .andThen(new InPlaceResourceOverrider(decorator.getConfigResource().getUrl(), parser, singleConfigMerger));
                }
            }

            if (omd.getConfigPath() != null) {
                String cliValue = cli.optionString(omd.getName());
                if (cliValue == null) {
                    cliValue = omd.getDefaultValue();
                }

                String finalCliValue = cliValue;
                overrider = overrider
                        .andThen(new InPlaceMapOverrider(Collections.singletonMap(omd.getConfigPath(), finalCliValue)));
            }
        }

        return overrider;
    }

    private OptionMetadata findMetadata(OptionSpec<?> option) {

        List<String> optionNames = option.options();

        // TODO: allow lookup of option metadata by name to avoid linear scans...
        // Though we are dealing with small collection, so shouldn't be too horrible.

        for (OptionMetadata omd : optionMetadata) {
            if (optionNames.contains(omd.getName())) {
                return omd;
            }
        }

        // this was likely a command, not an option.
        return null;
    }

    @Override
    public ConfigurationFactory get() {
        Map<String, String> properties = environment.frameworkProperties();
        Node rootNode = loadConfiguration(properties);
        Mapper mapper = MapperBuilder.builder().build();
        return new YamlConfigurationFactory(rootNode, mapper);
    }
}
