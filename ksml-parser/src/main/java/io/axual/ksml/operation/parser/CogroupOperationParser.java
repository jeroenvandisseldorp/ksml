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


import io.axual.ksml.definition.parser.AggregatorDefinitionParser;
import io.axual.ksml.dsl.KSMLDSL;
import io.axual.ksml.operation.CogroupOperationDefinition;
import io.axual.ksml.parser.ParseContext;
import io.axual.ksml.parser.StructsParser;
import io.axual.ksml.resource.TopologyResources;

import static io.axual.ksml.dsl.KSMLDSL.Operations;

public class CogroupOperationParser extends OperationParser<CogroupOperationDefinition> {
    public CogroupOperationParser(ParseContext context, TopologyResources resources) {
        super(context, KSMLDSL.Operations.COGROUP, resources);
    }

    @Override
    protected StructsParser<CogroupOperationDefinition> parser() {
        return structsParser(
                CogroupOperationDefinition.class,
                "",
                "A cogroup operation",
                operationNameField(),
                functionField(Operations.Aggregate.AGGREGATOR, "(GroupedStream, SessionWindowedStream, TimeWindowedStream) The aggregator function, which combines a value with the previous aggregation result and outputs a new aggregation result", new AggregatorDefinitionParser(context, false)),
                (name, aggr, tags) -> new CogroupOperationDefinition(operationConfig(name, tags), aggr));
    }
}
