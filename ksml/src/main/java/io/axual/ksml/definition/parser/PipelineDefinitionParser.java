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


import io.axual.ksml.definition.PipelineDefinition;
import io.axual.ksml.dsl.KSMLDSL;
import io.axual.ksml.generator.TopologyResources;
import io.axual.ksml.operation.parser.PipelineOperationParser;
import io.axual.ksml.operation.parser.PipelineSinkOperationParser;
import io.axual.ksml.parser.ContextAwareParser;
import io.axual.ksml.parser.ListParser;
import io.axual.ksml.parser.TopologyResourceParser;
import io.axual.ksml.parser.YamlNode;

public class PipelineDefinitionParser extends ContextAwareParser<PipelineDefinition> {
    protected PipelineDefinitionParser(String prefix, TopologyResources context) {
        super(prefix, context);
    }

    @Override
    public PipelineDefinition parse(YamlNode node) {
        return parse(node, true, true);
    }

    public PipelineDefinition parse(YamlNode node, boolean parseSource, boolean parseSink) {
        if (node == null) return null;
        final var source = parseSource
                ? new TopologyResourceParser<>("source", KSMLDSL.Pipelines.FROM, resources.topics()::get, new TopicDefinitionParser()).parseDefinition(node)
                : null;
        final var operations = new ListParser<>("pipeline operation", new PipelineOperationParser(prefix, resources)).parse(node.get(KSMLDSL.Pipelines.VIA, "step"));
        final var sink = parseSink ? new PipelineSinkOperationParser(prefix, null, resources).parse(node) : null;
        return new PipelineDefinition(source, operations, sink);
    }
}
