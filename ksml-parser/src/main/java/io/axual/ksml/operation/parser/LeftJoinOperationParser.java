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
import io.axual.ksml.function.FunctionDefinition;
import io.axual.ksml.topic.GlobalTableDefinition;
import io.axual.ksml.topic.StreamDefinition;
import io.axual.ksml.topic.TableDefinition;
import io.axual.ksml.definition.parser.ForeignKeyExtractorDefinitionParser;
import io.axual.ksml.definition.parser.GlobalTableDefinitionParser;
import io.axual.ksml.definition.parser.JoinTargetDefinitionParser;
import io.axual.ksml.definition.parser.KeyValueMapperDefinitionParser;
import io.axual.ksml.definition.parser.StreamDefinitionParser;
import io.axual.ksml.definition.parser.StreamPartitionerDefinitionParser;
import io.axual.ksml.definition.parser.TableDefinitionParser;
import io.axual.ksml.definition.parser.ValueJoinerDefinitionParser;
import io.axual.ksml.dsl.KSMLDSL;
import io.axual.ksml.exception.ParseException;
import io.axual.ksml.exception.TopologyException;
import io.axual.ksml.operation.LeftJoinWithGlobalTableOperationDefinition;
import io.axual.ksml.operation.LeftJoinWithStreamOperationDefinition;
import io.axual.ksml.operation.LeftJoinWithTableOperationDefinition;
import io.axual.ksml.operation.OperationDefinition;
import io.axual.ksml.parser.ParseContext;
import io.axual.ksml.parser.ParseNode;
import io.axual.ksml.parser.StructsParser;
import io.axual.ksml.resource.TopologyResources;
import io.axual.ksml.store.StoreType;

import java.util.ArrayList;
import java.util.List;

import static io.axual.ksml.dsl.KSMLDSL.Operations;

public class LeftJoinOperationParser extends OperationParser<OperationDefinition> {
    private final StructsParser<LeftJoinWithStreamOperationDefinition> joinStreamParser;
    private final StructsParser<LeftJoinWithTableOperationDefinition> joinTableParser;
    private final StructsParser<LeftJoinWithGlobalTableOperationDefinition> joinGlobalTableParser;
    private final List<StructSchema> schemas = new ArrayList<>();

    public LeftJoinOperationParser(ParseContext context, TopologyResources resources) {
        super(context, KSMLDSL.Operations.LEFT_JOIN, resources);
        final var valueJoinerField = functionField(Operations.Join.VALUE_JOINER, "A function that joins two values", new ValueJoinerDefinitionParser(context, false));

        joinStreamParser = createLeftJoinStreamParser(valueJoinerField);

        joinTableParser = createLeftJoinTableParser(valueJoinerField);

        joinGlobalTableParser = createLeftJoinGlobalTableParser(valueJoinerField);

        schemas.addAll(joinStreamParser.schemas());
        schemas.addAll(joinTableParser.schemas());
        schemas.addAll(joinGlobalTableParser.schemas());
    }

    public StructsParser<OperationDefinition> parser() {
        return new StructsParser<>() {
            @Override
            public OperationDefinition parse(ParseNode node) {
                if (node == null) return null;
                final var joinTopic = new JoinTargetDefinitionParser(context, resources()).parse(node);
                if (joinTopic.definition() instanceof StreamDefinition) return joinStreamParser.parse(node);
                if (joinTopic.definition() instanceof TableDefinition) return joinTableParser.parse(node);
                if (joinTopic.definition() instanceof GlobalTableDefinition) return joinGlobalTableParser.parse(node);

                final var separator = joinTopic.name() != null && joinTopic.definition() != null ? ", " : "";
                final var description = (joinTopic.name() != null ? joinTopic.name() : "") + separator + (joinTopic.definition() != null ? joinTopic.definition() : "");
                throw new ParseException(node, "LeftJoin stream not found: " + description);
            }

            @Override
            public List<StructSchema> schemas() {
                return schemas;
            }
        };
    }

    private StructsParser<LeftJoinWithStreamOperationDefinition> createLeftJoinStreamParser(StructsParser<FunctionDefinition> valueJoinerField) {
        return structsParser(
                LeftJoinWithStreamOperationDefinition.class,
                "",
                "Operation to leftJoin with a stream",
                operationNameField(),
                topicField(Operations.Join.WITH_STREAM, "A reference to the stream, or an inline definition of the stream to leftJoin with", new StreamDefinitionParser(context, resources(), true)),
                valueJoinerField,
                durationField(Operations.Join.TIME_DIFFERENCE, "The maximum time difference for a leftJoin over two streams on the same key"),
                optional(durationField(Operations.Join.GRACE, "The window grace period (the time to admit out-of-order events after the end of the window)")),
                storeField(Operations.SOURCE_STORE_ATTRIBUTE, true, "Materialized view of the source stream", StoreType.WINDOW_STORE),
                storeField(Operations.OTHER_STORE_ATTRIBUTE, true, "Materialized view of the leftJoined stream", StoreType.WINDOW_STORE),
                (name, stream, valueJoiner, timeDifference, grace, thisStore, otherStore, tags) -> {
                    if (stream instanceof StreamDefinition streamDef) {
                        return new LeftJoinWithStreamOperationDefinition(dualStoreOperationConfig(name, tags, thisStore, otherStore), streamDef, valueJoiner, timeDifference, grace);
                    }
                    throw new TopologyException("LeftJoin stream not correct, should be a defined stream");
                });
    }

    private StructsParser<LeftJoinWithTableOperationDefinition> createLeftJoinTableParser(StructsParser<FunctionDefinition> valueJoinerField) {
        return structsParser(
                LeftJoinWithTableOperationDefinition.class,
                "",
                "Operation to leftJoin with a table",
                operationNameField(),
                topicField(Operations.Join.WITH_TABLE, "A reference to the table, or an inline definition of the table to join with", new TableDefinitionParser(context, resources(), true)),
                optional(functionField(Operations.Join.FOREIGN_KEY_EXTRACTOR, "A function that can translate the join table value to a primary key", new ForeignKeyExtractorDefinitionParser(context, false))),
                valueJoinerField,
                optional(durationField(Operations.Join.GRACE, "The window grace period (the time to admit out-of-order events after the end of the window)")),
                optional(functionField(Operations.Join.PARTITIONER, "A function that partitions the records on the primary table", new StreamPartitionerDefinitionParser(context, false))),
                optional(functionField(Operations.Join.OTHER_PARTITIONER, "A function that partitions the records on the join table", new StreamPartitionerDefinitionParser(context, false))),
                storeField(false, "Materialized view of the leftJoined table (only used for Table-Table joins)", StoreType.KEYVALUE_STORE),
                (name, table, foreignKeyExtractor, valueJoiner, grace, partitioner, otherPartitioner, store, tags) -> {
                    if (table instanceof TableDefinition tableDef) {
                        return new LeftJoinWithTableOperationDefinition(storeOperationConfig(name, tags, store), tableDef, foreignKeyExtractor, valueJoiner, grace, partitioner, otherPartitioner);
                    }
                    throw new TopologyException("LeftJoin table not correct, should be a defined table");
                });
    }

    private StructsParser<LeftJoinWithGlobalTableOperationDefinition> createLeftJoinGlobalTableParser(StructsParser<FunctionDefinition> valueJoinerField) {
        return structsParser(
                LeftJoinWithGlobalTableOperationDefinition.class,
                "",
                "Operation to leftJoin with a globalTable",
                operationNameField(),
                topicField(Operations.Join.WITH_GLOBAL_TABLE, "A reference to the globalTable, or an inline definition of the globalTable to join with", new GlobalTableDefinitionParser(context, resources(), true)),
                functionField(Operations.Join.MAPPER, "A function that maps the key value from the stream with the primary key of the globalTable", new KeyValueMapperDefinitionParser(context, false)),
                valueJoinerField,
                // GlobalTable joins do not use/require a state store
                (name, globalTable, mapper, valueJoiner, tags) -> {
                    if (globalTable instanceof GlobalTableDefinition globalTableDef) {
                        return new LeftJoinWithGlobalTableOperationDefinition(operationConfig(name, tags), globalTableDef, mapper, valueJoiner);
                    }
                    throw new TopologyException("LeftJoin globalTable not correct, should be a defined globalTable");
                });
    }
}
