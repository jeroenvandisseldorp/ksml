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

import io.axual.ksml.definition.ProducerDefinition;
import io.axual.ksml.generator.TopologyResources;
import io.axual.ksml.generator.TopologySpecification;
import io.axual.ksml.parser.BaseParser;
import io.axual.ksml.parser.MapParser;
import io.axual.ksml.parser.YamlNode;

import static io.axual.ksml.dsl.KSMLDSL.*;
import static io.axual.ksml.dsl.KSMLDSL.PIPELINES_DEFINITION;

public class TopologySpecificationParser extends BaseParser<TopologySpecification> {
    @Override
    public TopologySpecification parse(YamlNode node) {
        // If there is nothing to parse, return immediately
        if (node == null) return null;

        // Parse the underlying resources first
        final var resources = new TopologyResourcesParser().parse(node);

        // Set up an index for the topology specification
        final var result = new TopologySpecification(resources);

        // Parse all defined pipelines
        new MapParser<>("pipeline definition", new PipelineDefinitionParser(resources)).parse(node.get(PIPELINES_DEFINITION)).forEach(result::register);
        // Parse all defined producers
        new MapParser<>("producer definition", new ProducerDefinitionParser(resources)).parse(node.get(PIPELINES_DEFINITION)).forEach(result::register);

        return result;
    }
}
