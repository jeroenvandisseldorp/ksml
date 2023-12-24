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


import io.axual.ksml.data.value.Pair;
import io.axual.ksml.definition.parser.GlobalTableDefinitionParser;
import io.axual.ksml.definition.parser.StreamDefinitionParser;
import io.axual.ksml.definition.parser.StreamPartitionerDefinitionParser;
import io.axual.ksml.definition.parser.TableDefinitionParser;
import io.axual.ksml.definition.parser.ValueJoinerDefinitionParser;
import io.axual.ksml.dsl.KSMLDSL;
import io.axual.ksml.operation.LeftJoinOperation;
import io.axual.ksml.parser.ChoiceAttributeParser;
import io.axual.ksml.parser.MultiSchemaParser;
import lombok.Getter;

import java.util.LinkedHashMap;

import static io.axual.ksml.dsl.KSMLDSL.Operations;

@Getter
public class LeftJoinOperationParser extends StoreOperationParser<LeftJoinOperation> {
    private final MultiSchemaParser<LeftJoinOperation> parser;

    public LeftJoinOperationParser(String namespace) {
        super(namespace, "leftJoin");
        final var joinStreamParser = structParser(
                LeftJoinOperation.class,
                "Operation to join with a stream",
                operationTypeField(Operations.LEFT_JOIN),
                nameField(),
                topicField(Operations.Join.WITH_STREAM, true, "(Required for Stream joins) A reference to the Stream, or an inline definition of the Stream to join with", new StreamDefinitionParser(false)),
                functionField(Operations.Join.VALUE_JOINER, true, "(Stream joins) A function that joins two values", new ValueJoinerDefinitionParser()),
                durationField(Operations.Join.TIME_DIFFERENCE, true, "(Stream joins) The maximum time difference for a join over two streams on the same key"),
                durationField(Operations.Join.GRACE, false, "(Stream joins) The window grace period (the time to admit out-of-order events after the end of the window)"),
                storeField(false, "Materialized view of the joined streams", null),
                (type, name, stream, valueJoiner, timeDifference, grace, store) -> new LeftJoinOperation(storeOperationConfig(name, null, store), stream, valueJoiner, timeDifference, grace));

        final var joinTableParser = structParser(
                LeftJoinOperation.class,
                "Operation to join with a table",
                stringField(KSMLDSL.Operations.TYPE_ATTRIBUTE, true, "The type of the operation, fixed value \"" + Operations.JOIN + "\""),
                nameField(),
                topicField(Operations.Join.WITH_TABLE, true, "(Required for Table joins) A reference to the Table, or an inline definition of the Table to join with", new TableDefinitionParser(false)),
                functionField(Operations.Join.FOREIGN_KEY_EXTRACTOR, true, "(Table joins) A function that can translate the join table value to a primary key", new ValueJoinerDefinitionParser()),
                functionField(Operations.Join.VALUE_JOINER, true, "(Table joins) A function that joins two values", new ValueJoinerDefinitionParser()),
                durationField(Operations.Join.GRACE, false, "(Table joins) The window grace period (the time to admit out-of-order events after the end of the window)"),
                functionField(Operations.Join.PARTITIONER, false, "(Table joins) A function that partitions the records on the primary table", new StreamPartitionerDefinitionParser()),
                functionField(Operations.Join.OTHER_PARTITIONER, false, "(Table joins) A function that partitions the records on the join table", new StreamPartitionerDefinitionParser()),
                storeField(false, "Materialized view of the joined streams", null),
                (type, name, table, foreignKeyExtractor, valueJoiner, grace, partitioner, otherPartitioner, store) -> new LeftJoinOperation(storeOperationConfig(name, null, store), table, foreignKeyExtractor, valueJoiner, grace, partitioner, otherPartitioner));

        final var joinGlobalTableParser = structParser(
                LeftJoinOperation.class,
                "Operation to join with a table",
                stringField(KSMLDSL.Operations.TYPE_ATTRIBUTE, true, "The type of the operation, fixed value \"" + Operations.JOIN + "\""),
                nameField(),
                topicField(Operations.Join.WITH_GLOBAL_TABLE, true, "(Required for GlobalTable joins) A reference to the GlobalTable, or an inline definition of the GlobalTable to join with", new GlobalTableDefinitionParser(false)),
                functionField(Operations.Join.MAPPER, true, "(GlobalTable joins) A function that maps the key value from the stream with the primary key of the GlobalTable", new ValueJoinerDefinitionParser()),
                functionField(Operations.Join.VALUE_JOINER, true, "(GlobalTable joins) A function that joins two values", new ValueJoinerDefinitionParser()),
                storeField(false, "Materialized view of the joined streams", null),
                (type, name, globalTable, mapper, valueJoiner, store) -> new LeftJoinOperation(storeOperationConfig(name, null, store), globalTable, mapper, valueJoiner));

        final var map = new LinkedHashMap<String, Pair<String, MultiSchemaParser<? extends LeftJoinOperation>>>();
        map.put(Operations.Join.WITH_STREAM, Pair.of("Stream", joinStreamParser));
        map.put(Operations.Join.WITH_TABLE, Pair.of("Table", joinTableParser));
        map.put(Operations.Join.WITH_GLOBAL_TABLE, Pair.of("GlobalTable", joinGlobalTableParser));
        parser = new ChoiceAttributeParser<>("NoTarget", true, map);
    }
}
