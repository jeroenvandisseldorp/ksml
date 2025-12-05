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
import io.axual.ksml.operation.TransformKeyOperationDefinition;
import io.axual.ksml.parser.ParseContext;
import io.axual.ksml.parser.StructsParser;
import io.axual.ksml.resource.TopologyResources;

public class TransformKeyOperationParser extends OperationParser<TransformKeyOperationDefinition> {
    public TransformKeyOperationParser(ParseContext context, TopologyResources resources) {
        super(context, KSMLDSL.Operations.TRANSFORM_KEY, resources);
    }

    @Override
    protected StructsParser<TransformKeyOperationDefinition> parser() {
        return structsParser(
                TransformKeyOperationDefinition.class,
                "",
                "Convert the key of every record in the stream to another key",
                operationNameField(),
                functionField(KSMLDSL.Operations.Transform.MAPPER, "A function that computes a new key for each record", new KeyTransformerDefinitionParser(context, false)),
                (name, mapper, tags) -> new TransformKeyOperationDefinition(operationConfig(name, tags), mapper));
    }
}
