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


import io.axual.ksml.data.value.Pair;
import io.axual.ksml.definition.PipelineDefinition;
import io.axual.ksml.definition.TopicDefinition;
import io.axual.ksml.definition.TopologyResource;
import io.axual.ksml.dsl.KSMLDSL;
import io.axual.ksml.operation.AsOperation;
import io.axual.ksml.operation.BaseOperation;
import io.axual.ksml.operation.BranchOperation;
import io.axual.ksml.operation.ForEachOperation;
import io.axual.ksml.operation.OperationConfig;
import io.axual.ksml.operation.PrintOperation;
import io.axual.ksml.operation.StreamOperation;
import io.axual.ksml.operation.ToOperation;
import io.axual.ksml.operation.parser.AsOperationParser;
import io.axual.ksml.operation.parser.BranchOperationParser;
import io.axual.ksml.operation.parser.ForEachOperationParser;
import io.axual.ksml.operation.parser.PipelineOperationParser;
import io.axual.ksml.operation.parser.PrintOperationParser;
import io.axual.ksml.operation.parser.ToOperationParser;
import io.axual.ksml.parser.ChoiceAttributeParser;
import io.axual.ksml.parser.MultiFormParser;
import io.axual.ksml.parser.MultiSchemaParser;
import io.axual.ksml.parser.NamedObjectParser;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class PipelineDefinitionParser extends MultiFormParser<PipelineDefinition> implements NamedObjectParser {
    private final boolean parseSource;
    private String defaultName;

    protected PipelineDefinitionParser(String namespace) {
        this(namespace, true);
    }

    protected PipelineDefinitionParser(String namespace, boolean parseSource) {
        super(namespace);
        this.parseSource = parseSource;
    }

    @Override
    public MultiSchemaParser<PipelineDefinition> parser() {
        final var map = new LinkedHashMap<String, Pair<String, MultiSchemaParser<? extends BaseOperation>>>();
        map.put(KSMLDSL.Operations.AS, Pair.of("AsSink", new AsOperationParser(namespace())));
        map.put(KSMLDSL.Operations.BRANCH, Pair.of("BranchSink", new BranchOperationParser(namespace(), parseSource)));
        map.put(KSMLDSL.Operations.FOR_EACH, Pair.of("ForEachSink", new ForEachOperationParser(namespace())));
        map.put(KSMLDSL.Operations.PRINT, Pair.of("PrintSink", new PrintOperationParser(namespace())));
        map.put(KSMLDSL.Operations.TO, Pair.of("ToSink", new ToOperationParser(namespace())));
        final MultiSchemaParser<BaseOperation> sinkParser = new ChoiceAttributeParser<>("NoSink", false, map);

        final var doc = "Defines a pipeline through a source, a series of operations to perform on it and a sink operation to close the stream with";
        final var fromField = topicField(KSMLDSL.Pipelines.FROM, parseSource, "Pipeline source", new TopicDefinitionParser());
        final var viaField = listField(KSMLDSL.Pipelines.VIA, "step", false, "A series of operations performed on the input stream", new PipelineOperationParser(namespace()));
        if (parseSource) {
            return structParser(PipelineDefinition.class, doc, fromField, viaField, sinkParser, this::pipelineDefinition);
        }

        return structParser(PipelineDefinition.class, doc, viaField, sinkParser, (via, sink) -> pipelineDefinition(null, via, sink));
    }

    private PipelineDefinition pipelineDefinition(TopologyResource<TopicDefinition> from, List<StreamOperation> via, BaseOperation sink) {
        final var name = validateName("Pipeline", null, defaultName, false);
        via = via != null ? via : new ArrayList<>();
        if (sink instanceof AsOperation asOp) return new PipelineDefinition(name, from, via, asOp);
        if (sink instanceof BranchOperation branchOp)
            return new PipelineDefinition(name, from, via, branchOp);
        if (sink instanceof ForEachOperation forEachOp)
            return new PipelineDefinition(name, from, via, forEachOp);
        if (sink instanceof PrintOperation printOp)
            return new PipelineDefinition(name, from, via, printOp);
        if (sink instanceof ToOperation toOp) return new PipelineDefinition(name, from, via, toOp);
        // If no sink operation was specified, then we create an AS operation here with the name provided.
        // This means that pipeline results can be referred to by other pipelines using the pipeline's name
        // as identifier.
        var sinkOperation = name != null ? new AsOperation(new OperationConfig(namespace(), name, null), name) : null;
        return new PipelineDefinition(name, from, via, sinkOperation);
    }

    @Override
    public void defaultName(String name) {
        this.defaultName = name;
    }
}
