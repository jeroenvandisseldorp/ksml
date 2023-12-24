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


import io.axual.ksml.definition.parser.KeyValueToKeyValueListTransformerDefinitionParser;
import io.axual.ksml.dsl.KSMLDSL;
import io.axual.ksml.generator.TopologyResources;
import io.axual.ksml.operation.StoreOperationConfig;
import io.axual.ksml.operation.TransformKeyValueToKeyValueListOperation;
import io.axual.ksml.parser.MultiSchemaParser;

public class TransformKeyValueToKeyValueListOperationParser extends OperationParser<TransformKeyValueToKeyValueListOperation> {
    public TransformKeyValueToKeyValueListOperationParser(String namespace) {
        super(namespace, "transformKeyValueToKeyValueList");
    }

    @Override
    protected MultiSchemaParser<TransformKeyValueToKeyValueListOperation> parser() {
        return structParser(
                TransformKeyValueToKeyValueListOperation.class,
                "Convert a stream by transforming every record into a list of derived records",
                operationTypeField(KSMLDSL.Operations.TRANSFORM_KEY_VALUE_TO_KEY_VALUE_LIST),
                nameField(),
                functionField(KSMLDSL.Operations.Transform.MAPPER, true, "A function that converts every record of a stream to a list of output records.", new KeyValueToKeyValueListTransformerDefinitionParser()),
                storeNamesField(),
                (type, name, mapper, storeNames) -> new TransformKeyValueToKeyValueListOperation(new StoreOperationConfig(namespace(), name, storeNames, null), mapper));
    }
}
