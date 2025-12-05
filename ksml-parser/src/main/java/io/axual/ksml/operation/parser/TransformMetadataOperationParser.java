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

import io.axual.ksml.definition.parser.MetadataTransformerDefinitionParser;
import io.axual.ksml.dsl.KSMLDSL;
import io.axual.ksml.operation.TransformMetadataOperationDefinition;
import io.axual.ksml.parser.ParseContext;
import io.axual.ksml.parser.StructsParser;
import io.axual.ksml.resource.TopologyResources;

public class TransformMetadataOperationParser extends OperationParser<TransformMetadataOperationDefinition> {
    public TransformMetadataOperationParser(ParseContext context, TopologyResources resources) {
        super(context, KSMLDSL.Operations.TRANSFORM_METADATA, resources);
    }

    @Override
    protected StructsParser<TransformMetadataOperationDefinition> parser() {
        return structsParser(
                TransformMetadataOperationDefinition.class,
                "",
                "Convert the metadata of every record in the stream",
                operationNameField(),
                functionField(KSMLDSL.Operations.Transform.MAPPER, "A function that converts the metadata (Kafka headers, timestamp) of every record in the stream", new MetadataTransformerDefinitionParser(context, false)),
                (name, mapper, tags) -> new TransformMetadataOperationDefinition(operationConfig(name, tags), mapper));
    }
}
