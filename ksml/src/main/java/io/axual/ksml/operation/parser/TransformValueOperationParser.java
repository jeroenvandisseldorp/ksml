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


import io.axual.ksml.definition.parser.ValueTransformerDefinitionParser;
import io.axual.ksml.dsl.KSMLDSL;
import io.axual.ksml.operation.StoreOperationConfig;
import io.axual.ksml.operation.TransformValueOperation;
import io.axual.ksml.parser.MultiSchemaParser;

public class TransformValueOperationParser extends StoreOperationParser<TransformValueOperation> {
    public TransformValueOperationParser(String namespace) {
        super(namespace, "transformValue");
    }

    @Override
    protected MultiSchemaParser<TransformValueOperation> parser() {
        return structParser(
                TransformValueOperation.class,
                "Convert the value of every record in the stream to another value",
                operationTypeField(KSMLDSL.Operations.TRANSFORM_VALUE),
                nameField(),
                functionField(KSMLDSL.Operations.Transform.MAPPER, true,"A function that converts the value of every record into another value", new ValueTransformerDefinitionParser()),
                storeNamesField(),
                (type, name, mapper, storeNames) -> new TransformValueOperation(new StoreOperationConfig(namespace(), name, storeNames, null), mapper));
    }
}
