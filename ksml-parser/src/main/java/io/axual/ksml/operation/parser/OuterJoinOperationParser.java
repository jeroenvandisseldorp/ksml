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
import io.axual.ksml.topic.StreamDefinition;
import io.axual.ksml.topic.TableDefinition;
import io.axual.ksml.definition.parser.JoinTargetDefinitionParser;
import io.axual.ksml.definition.parser.StreamDefinitionParser;
import io.axual.ksml.definition.parser.TableDefinitionParser;
import io.axual.ksml.definition.parser.ValueJoinerDefinitionParser;
import io.axual.ksml.dsl.KSMLDSL;
import io.axual.ksml.exception.ParseException;
import io.axual.ksml.exception.TopologyException;
import io.axual.ksml.operation.OperationDefinition;
import io.axual.ksml.operation.OuterJoinWithStreamOperationDefinition;
import io.axual.ksml.operation.OuterJoinWithTableOperationDefinition;
import io.axual.ksml.parser.ParseContext;
import io.axual.ksml.parser.ParseNode;
import io.axual.ksml.parser.StructsParser;
import io.axual.ksml.resource.TopologyResources;
import io.axual.ksml.store.StoreType;

import java.util.ArrayList;
import java.util.List;

public class OuterJoinOperationParser extends OperationParser<OperationDefinition> {
    private final StructsParser<OuterJoinWithStreamOperationDefinition> joinStreamParser;
    private final StructsParser<OuterJoinWithTableOperationDefinition> joinTableParser;
    private final List<StructSchema> schemas = new ArrayList<>();

    public OuterJoinOperationParser(ParseContext context, TopologyResources resources) {
        super(context, KSMLDSL.Operations.OUTER_JOIN, resources);
        joinStreamParser = createOuterJoinStreamParser();
        joinTableParser = createOuterJoinTableParser();

        schemas.addAll(joinStreamParser.schemas());
        schemas.addAll(joinTableParser.schemas());
    }

    public StructsParser<OperationDefinition> parser() {
        return new StructsParser<>() {
            @Override
            public OperationDefinition parse(ParseNode node) {
                if (node == null) return null;
                final var joinTopic = new JoinTargetDefinitionParser(context, resources()).parse(node);
                if (joinTopic.definition() instanceof StreamDefinition) return joinStreamParser.parse(node);
                if (joinTopic.definition() instanceof TableDefinition) return joinTableParser.parse(node);

                final var separator = joinTopic.name() != null && joinTopic.definition() != null ? ", " : "";
                final var description = (joinTopic.name() != null ? joinTopic.name() : "") + separator + (joinTopic.definition() != null ? joinTopic.definition() : "");
                throw new ParseException(node, "OuterJoin stream not found: " + description);
            }

            @Override
            public List<StructSchema> schemas() {
                return schemas;
            }
        };
    }

    private StructsParser<OuterJoinWithStreamOperationDefinition> createOuterJoinStreamParser() {
        return structsParser(
                OuterJoinWithStreamOperationDefinition.class,
                "",
                "Operation to outerJoin with a stream",
                operationNameField(),
                topicField(KSMLDSL.Operations.Join.WITH_STREAM, "A reference to the stream, or an inline definition of the stream to outerJoin with", new StreamDefinitionParser(context, resources(), true)),
                functionField(KSMLDSL.Operations.Join.VALUE_JOINER, "A function that joins two values", new ValueJoinerDefinitionParser(context, false)),
                durationField(KSMLDSL.Operations.Join.TIME_DIFFERENCE, "The maximum time difference for an outerJoin over two streams on the same key"),
                optional(durationField(KSMLDSL.Operations.Join.GRACE, "The window grace period (the time to admit out-of-order events after the end of the window)")),
                storeField(KSMLDSL.Operations.SOURCE_STORE_ATTRIBUTE, true, "Materialized view of the source stream", StoreType.WINDOW_STORE),
                storeField(KSMLDSL.Operations.OTHER_STORE_ATTRIBUTE, true, "Materialized view of the outerJoined stream", StoreType.WINDOW_STORE),
                (name, stream, valueJoiner, timeDifference, grace, thisStore, otherStore, tags) -> {
                    if (stream instanceof StreamDefinition streamDef) {
                        return new OuterJoinWithStreamOperationDefinition(dualStoreOperationConfig(name, tags, thisStore, otherStore), streamDef, valueJoiner, timeDifference, grace);
                    }
                    throw new TopologyException("OuterJoin stream not correct, should be a defined stream");
                });
    }

    private StructsParser<OuterJoinWithTableOperationDefinition> createOuterJoinTableParser() {
        return structsParser(
                OuterJoinWithTableOperationDefinition.class,
                "",
                "Operation to outerJoin with a table",
                operationNameField(),
                topicField(KSMLDSL.Operations.Join.WITH_TABLE, "A reference to the table, or an inline definition of the table to outerJoin with", new TableDefinitionParser(context, resources(), true)),
                functionField(KSMLDSL.Operations.Join.VALUE_JOINER, "A function that joins two values", new ValueJoinerDefinitionParser(context, false)),
                storeField(false, "Materialized view of the outerJoined table", StoreType.KEYVALUE_STORE),
                (name, table, valueJoiner, store, tags) -> {
                    if (table instanceof TableDefinition tableDef) {
                        return new OuterJoinWithTableOperationDefinition(storeOperationConfig(name, tags, store), tableDef, valueJoiner);
                    }
                    throw new TopologyException("OuterJoin table not correct, should be a defined table");
                });
    }
}
