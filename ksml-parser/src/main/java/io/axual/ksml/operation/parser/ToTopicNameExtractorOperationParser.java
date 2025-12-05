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

import io.axual.ksml.function.TopicNameExtractorDefinition;
import io.axual.ksml.definition.parser.ToTopicNameExtractorDefinitionParser;
import io.axual.ksml.dsl.KSMLDSL;
import io.axual.ksml.exception.TopologyException;
import io.axual.ksml.operation.ToTopicNameExtractorOperationDefinition;
import io.axual.ksml.parser.DefinitionParser;
import io.axual.ksml.parser.ParseContext;
import io.axual.ksml.parser.StructsParser;
import io.axual.ksml.resource.TopologyResources;

public class ToTopicNameExtractorOperationParser extends OperationParser<ToTopicNameExtractorOperationDefinition> {
    public ToTopicNameExtractorOperationParser(ParseContext context, TopologyResources resources) {
        super(context, KSMLDSL.Operations.TO_TOPIC_NAME_EXTRACTOR, resources);
    }

    private class ToOperationDefinitionParser extends DefinitionParser<ToTopicNameExtractorOperationDefinition> {
        private ToOperationDefinitionParser(ParseContext context) {
            super(context);
        }

        @Override
        protected StructsParser<ToTopicNameExtractorOperationDefinition> parser() {
            return structsParser(
                    ToTopicNameExtractorOperationDefinition.class,
                    "",
                    "Inline defined topic name extractor to send output messages to",
                    new ToTopicNameExtractorDefinitionParser(context, resources()),
                    (inlineTneAndPartitioner, tags) -> {
                        if (inlineTneAndPartitioner != null && inlineTneAndPartitioner.topicNameExtractor() != null) {
                            return new ToTopicNameExtractorOperationDefinition(operationConfig(null, tags), inlineTneAndPartitioner.topicNameExtractor(), inlineTneAndPartitioner.partitioner());
                        }
                        throw new TopologyException("Unknown target for pipeline \"to\" operation");
                    });
        }
    }

    @Override
    public StructsParser<ToTopicNameExtractorOperationDefinition> parser() {
        return lookupField(
                "target topic",
                KSMLDSL.Operations.TO_TOPIC_NAME_EXTRACTOR,
                "Ends the pipeline by sending all messages to a topic provided by a pre-defined topic name extractor function, or to a topic provided by an inline defined topic name extractor and optional partitioner",
                (name, tags) -> {
                    // Try to find a corresponding topic definition
                    final var tne = resources().function(name);
                    return tne != null ? new ToTopicNameExtractorOperationDefinition(operationConfig(null, tags), new TopicNameExtractorDefinition(tne), null) : null;
                },
                new ToOperationDefinitionParser(context));
    }
}
