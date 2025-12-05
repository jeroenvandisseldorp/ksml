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


import io.axual.ksml.definition.parser.PredicateDefinitionParser;
import io.axual.ksml.dsl.KSMLDSL;
import io.axual.ksml.exception.TopologyException;
import io.axual.ksml.operation.FilterOperationDefinition;
import io.axual.ksml.parser.ParseContext;
import io.axual.ksml.parser.StructsParser;
import io.axual.ksml.resource.TopologyResources;
import io.axual.ksml.store.StoreType;

public class FilterOperationParser extends OperationParser<FilterOperationDefinition> {
    public FilterOperationParser(ParseContext context, TopologyResources resources) {
        super(context, KSMLDSL.Operations.FILTER, resources);
    }

    public StructsParser<FilterOperationDefinition> parser() {
        return structsParser(
                FilterOperationDefinition.class,
                "",
                "Filter records based on a predicate function",
                operationNameField(),
                functionField(KSMLDSL.Operations.Filter.PREDICATE, "A function that returns \"true\" when records are accepted, \"false\" otherwise", new PredicateDefinitionParser(context,false)),
                storeField(false, "Materialized view of the filtered table (only applies to tables, ignored for streams)", StoreType.KEYVALUE_STORE),
                (name, pred, store, tags) -> {
                    if (pred == null) throw new TopologyException("Predicate not defined for " + type + " operation");
                    return new FilterOperationDefinition(storeOperationConfig(name, tags, store), pred);
                });
    }
}
