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


import io.axual.ksml.definition.BranchDefinition;
import io.axual.ksml.dsl.KSMLDSL;
import io.axual.ksml.parser.MultiFormParser;
import io.axual.ksml.parser.MultiSchemaParser;
import io.axual.ksml.parser.Utils;

import java.util.ArrayList;

public class BranchDefinitionParser extends MultiFormParser<BranchDefinition> {
    private final boolean includePipelineSchema;

    public BranchDefinitionParser(String namespace, boolean includePipelineSchema) {
        super(namespace);
        this.includePipelineSchema = includePipelineSchema;
    }

    @Override
    public MultiSchemaParser<BranchDefinition> parser() {
        // This parser uses the PipelineDefinitionParser recursively, hence requires a special implementation to not
        // make the associated DataSchema recurse infinitely.
        final var predParser = functionField(KSMLDSL.Operations.Branch.PREDICATE, false, "Defines the condition under which messages get sent down this branch", new PredicateDefinitionParser());
        final var pipelineParser = new PipelineDefinitionParser(namespace(), false);
        final var schemas = new ArrayList<>(predParser.schemas());
        if (includePipelineSchema) Utils.addToSchemas(schemas, "name", "doc", pipelineParser);
        return MultiSchemaParser.of(node -> new BranchDefinition(predParser.parse(node), pipelineParser.parse(node)), schemas);
    }
}
