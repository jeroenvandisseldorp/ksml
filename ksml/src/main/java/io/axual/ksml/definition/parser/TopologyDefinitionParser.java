package io.axual.ksml.definition.parser;

/*-
 * ========================LICENSE_START=================================
 * KSML
 * %%
 * Copyright (C) 2021 - 2023 Axual B.V.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =========================LICENSE_END==================================
 */

import io.axual.ksml.generator.TopologyDefinition;
import io.axual.ksml.parser.MultiFormParser;
import io.axual.ksml.parser.MultiSchemaParser;

import static io.axual.ksml.dsl.KSMLDSL.PIPELINES;
import static io.axual.ksml.dsl.KSMLDSL.PRODUCERS;

public class TopologyDefinitionParser extends MultiFormParser<TopologyDefinition> {
    private final TopologyResourcesParser resourcesParser;

    public TopologyDefinitionParser(String namespace) {
        resourcesParser = new TopologyResourcesParser(namespace);
    }

    @Override
    public MultiSchemaParser<TopologyDefinition> parser() {
        final var pipelinesParser = mapField(PIPELINES, "pipeline", false, "Collection of named pipelines", new PipelineDefinitionParser(resourcesParser.namespace()));
        final var producersParser = mapField(PRODUCERS, "producer", false, "Collection of named producers", new ProducerDefinitionParser(resourcesParser.namespace()));
        return structParser(
                TopologyDefinition.class,
                "A KSML topology description",
                resourcesParser,
                pipelinesParser,
                producersParser,
                (resources, pipelines, producers) -> {
                    final var result = new TopologyDefinition(resources.namespace());
                    // Copy the resources into the topology definition
                    resources.topics().forEach(result::register);
                    resources.stateStores().forEach(result::register);
                    resources.functions().forEach(result::register);
                    // Parse all defined pipelines, using this topology's name as operation prefix
                    pipelines.forEach(result::register);
                    // Parse all defined producers, using this topology's name as operation prefix
                    producers.forEach(result::register);
                    return result;
                });
    }
}
