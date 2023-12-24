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

import io.axual.ksml.data.schema.StructSchema;
import io.axual.ksml.dsl.KSMLDSL;
import io.axual.ksml.operation.BaseOperation;
import io.axual.ksml.operation.OperationConfig;
import io.axual.ksml.parser.MultiSchemaParser;
import io.axual.ksml.parser.SingleFormParser;
import io.axual.ksml.parser.SingleSchemaParser;
import io.axual.ksml.parser.StringValueParser;
import io.axual.ksml.parser.YamlNode;

import java.util.List;

public abstract class OperationParser<T extends BaseOperation> extends SingleFormParser<T> {
    private final String type;

    public OperationParser(String namespace, String type) {
        super(namespace);
        this.type = type;
    }

    protected MultiSchemaParser<String> operationTypeField(String fixedType) {
        return fixedStringField(KSMLDSL.Operations.TYPE_ATTRIBUTE, true, fixedType, "The type of the operation");
    }

    protected SingleSchemaParser<String> nameField() {
        final var stringParser = stringField(KSMLDSL.Operations.NAME_ATTRIBUTE, false, null, "The name of the operation processor");
        return new SingleSchemaParser<>() {
            @Override
            public String parse(YamlNode node) {
                final var name = stringParser.parse(node);
                // To ensure every operation gets a unique name, we generate one based on the YAML node
                return name != null ? name : node.longName();
            }

            @Override
            public StructSchema schema() {
                return stringParser.schema();
            }
        };
    }

    protected MultiSchemaParser<List<String>> storeNamesField() {
        return listField(KSMLDSL.Operations.STORE_NAMES_ATTRIBUTE, "state store name", false, "The names of all state stores used by the function", new StringValueParser());
    }

    protected OperationConfig operationConfig(String name) {
        return operationConfig(name, null);
    }

    protected OperationConfig operationConfig(String name, List<String> storeNames) {
        return new OperationConfig(
                namespace(),
                name != null ? name : getClass().getSimpleName(),
                storeNames != null ? storeNames.toArray(String[]::new) : null);
    }
}
