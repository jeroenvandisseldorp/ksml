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

import io.axual.ksml.data.type.UserType;
import io.axual.ksml.definition.TableDefinition;
import io.axual.ksml.execution.FatalError;
import io.axual.ksml.parser.MultiFormParser;
import io.axual.ksml.parser.MultiSchemaParser;

import static io.axual.ksml.dsl.KSMLDSL.Streams;

public class TableDefinitionParser extends MultiFormParser<TableDefinition> {
    private final boolean requireKeyValueType;

    public TableDefinitionParser(boolean requireKeyValueType) {
        this.requireKeyValueType = requireKeyValueType;
    }

    @Override
    public MultiSchemaParser<TableDefinition> parser() {
        return structParser(
                TableDefinition.class,
                "Contains a definition of a Table, which can be referenced by producers and pipelines",
                stringField(Streams.TOPIC, true, "The name of the Kafka topic for this table"),
                userTypeField(Streams.KEY_TYPE, requireKeyValueType, "The key type of the table"),
                userTypeField(Streams.VALUE_TYPE, requireKeyValueType, "The value type of the table"),
                customField(Streams.STORE, false, "Keyvalue store associated with this table", new KeyValueStateStoreDefinitionParser(false)),
                (topic, keyType, valueType, store) -> {
                    keyType = keyType != null ? keyType : UserType.UNKNOWN;
                    valueType = valueType != null ? valueType : UserType.UNKNOWN;
                    if (store != null) {
                        if (!keyType.dataType().isAssignableFrom(store.keyType().dataType())) {
                            throw FatalError.topologyError("Incompatible key types between table \'" + topic + "\' and its corresponding state store: " + keyType.dataType() + " and " + store.keyType().dataType());
                        }
                        if (!valueType.dataType().isAssignableFrom(store.valueType().dataType())) {
                            throw FatalError.topologyError("Incompatible value types between table \'" + topic + "\' and its corresponding state store: " + valueType.dataType() + " and " + store.valueType().dataType());
                        }
                    }
                    return new TableDefinition(topic, keyType, valueType, store);
                });
    }
}
