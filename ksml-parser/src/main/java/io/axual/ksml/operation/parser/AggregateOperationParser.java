package io.axual.ksml.operation.parser;

/*-
 * ========================LICENSE_START=================================
 * KSML
 * %%
 * Copyright (C) 2021 - 2025 Axual B.V.
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

import io.axual.ksml.definition.parser.AggregatorDefinitionParser;
import io.axual.ksml.definition.parser.InitializerDefinitionParser;
import io.axual.ksml.definition.parser.MergerDefinitionParser;
import io.axual.ksml.dsl.KSMLDSL;
import io.axual.ksml.operation.AggregateOperationDefinition;
import io.axual.ksml.parser.ParseContext;
import io.axual.ksml.parser.StructsParser;
import io.axual.ksml.resource.TopologyResources;

public class AggregateOperationParser extends OperationParser<AggregateOperationDefinition> {
    public AggregateOperationParser(ParseContext context, TopologyResources resources) {
        super(context, KSMLDSL.Operations.AGGREGATE, resources);
    }

    @Override
    protected StructsParser<AggregateOperationDefinition> parser() {
        return structsParser(
                AggregateOperationDefinition.class,
                "",
                "An aggregate operation",
                operationNameField(),
                functionField(KSMLDSL.Operations.Aggregate.INITIALIZER, "The initializer function, which generates an initial value for every set of aggregated records", new InitializerDefinitionParser(context, false)),
                optional(functionField(KSMLDSL.Operations.Aggregate.AGGREGATOR, "(GroupedStream, SessionWindowedStream, TimeWindowedStream) The aggregator function, which combines a value with the previous aggregation result and outputs a new aggregation result", new AggregatorDefinitionParser(context, false))),
                optional(functionField(KSMLDSL.Operations.Aggregate.MERGER, "(SessionWindowedStream, SessionWindowedCogroupedStream) A function that combines two aggregation results", new MergerDefinitionParser(context, false))),
                optional(functionField(KSMLDSL.Operations.Aggregate.ADDER, "(GroupedTable) A function that adds a record to the aggregation result", new AggregatorDefinitionParser(context, false))),
                optional(functionField(KSMLDSL.Operations.Aggregate.SUBTRACTOR, "(GroupedTable) A function that removes a record from the aggregation result", new AggregatorDefinitionParser(context, false))),
                storeField(true, "Materialized view of the result aggregation", null),
                (name, init, aggr, merg, add, sub, store, tags) -> new AggregateOperationDefinition(storeOperationConfig(name, tags, store), init, aggr, merg, add, sub));
    }
}
