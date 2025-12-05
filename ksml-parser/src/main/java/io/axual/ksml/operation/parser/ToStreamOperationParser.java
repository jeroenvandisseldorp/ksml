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


import io.axual.ksml.definition.parser.KeyTransformerDefinitionParser;
import io.axual.ksml.dsl.KSMLDSL;
import io.axual.ksml.operation.ToStreamOperationDefinition;
import io.axual.ksml.parser.ParseContext;
import io.axual.ksml.parser.StructsParser;
import io.axual.ksml.resource.TopologyResources;

public class ToStreamOperationParser extends OperationParser<ToStreamOperationDefinition> {
    public ToStreamOperationParser(ParseContext context, TopologyResources resources) {
        super(context, KSMLDSL.Operations.TO_STREAM, resources);
    }

    @Override
    public StructsParser<ToStreamOperationDefinition> parser() {
        return structsParser(
                ToStreamOperationDefinition.class,
                "",
                "Convert a Table into a Stream, optionally through a custom key transformer",
                operationNameField(),
                optional(functionField(KSMLDSL.Operations.ToStream.MAPPER, "A function that computes the output key for every record", new KeyTransformerDefinitionParser(context, false))),
                (name, mapper, tags) -> new ToStreamOperationDefinition(operationConfig(name, tags), mapper));
    }
}
