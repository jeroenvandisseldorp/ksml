package io.axual.ksml.definition.parser;

/*-
 * ========================LICENSE_START=================================
 * KSML
 * %%
 * Copyright (C) 2021 - 2024 Axual B.V.
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

import io.axual.ksml.definition.ToTopicDefinition;
import io.axual.ksml.dsl.KSMLDSL;
import io.axual.ksml.generator.TopologyResources;
import io.axual.ksml.parser.TopologyResourceAwareParser;
import io.axual.ksml.parser.StructParser;

public class ToTopicDefinitionParser extends TopologyResourceAwareParser<ToTopicDefinition> {
    public ToTopicDefinitionParser(TopologyResources resources) {
        super(resources);
    }

    @Override
    protected StructParser<ToTopicDefinition> parser() {
        return structParser(
                ToTopicDefinition.class,
                "",
                "Writes out pipeline messages to a topic",
                optional(topicField(KSMLDSL.Operations.To.TOPIC, "A reference to a stream, table or globalTable, or an inline definition of the output topic", new TopicDefinitionParser(resources(), false))),
                optional(functionField(KSMLDSL.Operations.To.PARTITIONER, "A function that partitions the records in the output topic", new StreamPartitionerDefinitionParser())),
                (topic, partitioner, tags) -> topic != null ? new ToTopicDefinition(topic, partitioner) : null);
    }
}