package io.axual.ksml.operation.parser;

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

import io.axual.ksml.data.value.Pair;
import io.axual.ksml.definition.ToOperationDefinition;
import io.axual.ksml.definition.TopicDefinition;
import io.axual.ksml.definition.TopologyResource;
import io.axual.ksml.definition.parser.ShortTopicDefinitionParser;
import io.axual.ksml.definition.parser.StreamPartitionerDefinitionParser;
import io.axual.ksml.definition.parser.TopicNameExtractorDefinitionParser;
import io.axual.ksml.dsl.KSMLDSL;
import io.axual.ksml.generator.TopologyResources;
import io.axual.ksml.operation.ToOperation;
import io.axual.ksml.parser.ChoiceAttributeParser;
import io.axual.ksml.parser.MultiSchemaParser;
import lombok.Getter;

import java.util.HashMap;
import java.util.function.BiFunction;

@Getter
public class ToOperationParser extends OperationParser<ToOperation> {
    private final MultiSchemaParser<ToOperation> parser;

    public ToOperationParser(String namespace) {
        super(namespace, "to");

        final var partitionerField = functionField(
                KSMLDSL.Operations.To.PARTITIONER,
                false,
                "Controls how the output messages are partitioned",
                new StreamPartitionerDefinitionParser());
        final var toTopicParser = structParser(
                ToOperationDefinition.class,
                "Reference to a pre-defined topic, or an inline definition of a topic and an optional stream partitioner",
                topicField(
                        KSMLDSL.Operations.To.TOPIC,
                        false,
                        "Reference to a pre-defined topic, or an inline definition of a topic and an optional stream partitioner",
                        new ShortTopicDefinitionParser()),
                partitionerField,
                (topic, partitioner) -> new ToOperationDefinition(topic, null, partitioner));
        final var toTneParser = structParser(
                ToOperationDefinition.class,
                "Reference to a pre-defined topic name extractor, or an inline definition of a topic name extractor and an optional stream partitioner",
                functionField(
                        KSMLDSL.Operations.To.TOPIC_NAME_EXTRACTOR,
                        true,
                        "Reference to a pre-defined topic name extractor, or an inline definition of a topic name extractor",
                        new TopicNameExtractorDefinitionParser()),
                partitionerField,
                (tne, partitioner) -> new ToOperationDefinition(null, tne, partitioner));

        final var map = new HashMap<String, Pair<String, MultiSchemaParser<? extends ToOperationDefinition>>>();
        map.put(KSMLDSL.Operations.To.TOPIC, Pair.of("Topic", toTopicParser));
        map.put(KSMLDSL.Operations.To.TOPIC_NAME_EXTRACTOR, Pair.of("TopicNameExtractor", toTneParser));
        final var toOperationDefinitionParser = new ChoiceAttributeParser<>("NoTarget", true, map);

        final BiFunction<TopologyResources, String, TopicDefinition> lookupTopic = TopologyResources::topic;
        parser = structParser(
                ToOperation.class,
                "Ends the pipeline by sending all messages to a fixed topic, or to a topic returned by a topic name extractor function",
                resourceField(
                        KSMLDSL.Operations.TO,
                        false,
                        "Either a topic or topic name extractor that defines where to write pipeline messages to",
                        (resources, name) -> new ToOperationDefinition(new TopologyResource<>(name, lookupTopic, resources.topic(name)), null, null),
                        toOperationDefinitionParser),
                target -> new ToOperation(operationConfig("to"), target));
    }
}
