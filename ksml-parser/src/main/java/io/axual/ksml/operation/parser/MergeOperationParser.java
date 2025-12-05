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


import io.axual.ksml.topic.StreamDefinition;
import io.axual.ksml.definition.parser.StreamDefinitionParser;
import io.axual.ksml.dsl.KSMLDSL;
import io.axual.ksml.exception.TopologyException;
import io.axual.ksml.operation.MergeOperationDefinition;
import io.axual.ksml.parser.ParseContext;
import io.axual.ksml.parser.StructsParser;
import io.axual.ksml.resource.TopologyResources;

public class MergeOperationParser extends OperationParser<MergeOperationDefinition> {
    public MergeOperationParser(ParseContext context, TopologyResources resources) {
        super(context, KSMLDSL.Operations.MERGE, resources);
    }

    @Override
    protected StructsParser<MergeOperationDefinition> parser() {
        return structsParser(
                MergeOperationDefinition.class,
                "",
                "A merge operation to join two Streams",
                operationNameField(),
                topicField(KSMLDSL.Operations.Merge.STREAM, "The stream to merge with", new StreamDefinitionParser(context, resources(), false)),
                (name, stream, tags) -> {
                    if (stream instanceof StreamDefinition streamDef) {
                        return new MergeOperationDefinition(operationConfig(name, tags), streamDef);
                    }
                    throw new TopologyException("Merge stream not correct, should be a defined Stream");
                });
    }
}
