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


import io.axual.ksml.dsl.KSMLDSL;
import io.axual.ksml.operation.OperationDefinition;
import io.axual.ksml.parser.ChoiceParser;
import io.axual.ksml.parser.ParseContext;
import io.axual.ksml.parser.StructsParser;
import io.axual.ksml.resource.TopologyResources;

import java.util.HashMap;
import java.util.Map;

public class PipelineOperationParser extends ChoiceParser<OperationDefinition> {
    public PipelineOperationParser(ParseContext context, TopologyResources resources) {
        super(KSMLDSL.Operations.TYPE_ATTRIBUTE, "OperationType", "operation", null, types(context, resources));
    }

    private static Map<String, StructsParser<? extends OperationDefinition>> types(ParseContext context, TopologyResources resources) {
        final var result = new HashMap<String, StructsParser<? extends OperationDefinition>>();
        result.put(KSMLDSL.Operations.AGGREGATE, new AggregateOperationParser(context, resources));
        result.put(KSMLDSL.Operations.COGROUP, new CogroupOperationParser(context, resources));
        result.put(KSMLDSL.Operations.CONVERT_KEY, new ConvertKeyOperationParser(context, resources));
        result.put(KSMLDSL.Operations.CONVERT_KEY_VALUE, new ConvertKeyValueOperationParser(context, resources));
        result.put(KSMLDSL.Operations.CONVERT_VALUE, new ConvertValueOperationParser(context, resources));
        result.put(KSMLDSL.Operations.COUNT, new CountOperationParser(context, resources));
        result.put(KSMLDSL.Operations.FILTER, new FilterOperationParser(context, resources));
        result.put(KSMLDSL.Operations.FILTER_NOT, new FilterNotOperationParser(context, resources));
        result.put(KSMLDSL.Operations.FLATMAP, new TransformKeyValueToKeyValueListOperationParser(context, resources));
        result.put(KSMLDSL.Operations.TRANSFORM_KEY_VALUE_TO_KEY_VALUE_LIST, new TransformKeyValueToKeyValueListOperationParser(context, resources));
        result.put(KSMLDSL.Operations.FLATMAP_VALUES, new TransformKeyValueToValueListOperationParser(context, resources));
        result.put(KSMLDSL.Operations.TRANSFORM_KEY_VALUE_TO_VALUE_LIST, new TransformKeyValueToValueListOperationParser(context, resources));
        result.put(KSMLDSL.Operations.GROUP_BY, new GroupByOperationParser(context, resources));
        result.put(KSMLDSL.Operations.GROUP_BY_KEY, new GroupByKeyOperationParser(context, resources));
        result.put(KSMLDSL.Operations.JOIN, new JoinOperationParser(context, resources));
        result.put(KSMLDSL.Operations.LEFT_JOIN, new LeftJoinOperationParser(context, resources));
        result.put(KSMLDSL.Operations.MAP_KEY, new TransformKeyOperationParser(context, resources));
        result.put(KSMLDSL.Operations.SELECT_KEY, new TransformKeyOperationParser(context, resources));
        result.put(KSMLDSL.Operations.TRANSFORM_KEY, new TransformKeyOperationParser(context, resources));
        result.put(KSMLDSL.Operations.MAP, new TransformKeyValueOperationParser(context, resources));
        result.put(KSMLDSL.Operations.TRANSFORM_KEY_VALUE, new TransformKeyValueOperationParser(context, resources));
        result.put(KSMLDSL.Operations.TRANSFORM_METADATA, new TransformMetadataOperationParser(context, resources));
        result.put(KSMLDSL.Operations.MAP_VALUE, new TransformValueOperationParser(context, resources));
        result.put(KSMLDSL.Operations.MAP_VALUES, new TransformValueOperationParser(context, resources));
        result.put(KSMLDSL.Operations.TRANSFORM_VALUE, new TransformValueOperationParser(context, resources));
        result.put(KSMLDSL.Operations.MERGE, new MergeOperationParser(context, resources));
        result.put(KSMLDSL.Operations.OUTER_JOIN, new OuterJoinOperationParser(context, resources));
        result.put(KSMLDSL.Operations.PEEK, new PeekOperationParser(context, resources));
        result.put(KSMLDSL.Operations.REDUCE, new ReduceOperationParser(context, resources));
        result.put(KSMLDSL.Operations.REPARTITION, new RepartitionOperationParser(context, resources));
        result.put(KSMLDSL.Operations.SUPPRESS, new SuppressOperationParser(context, resources));
        result.put(KSMLDSL.Operations.TO_STREAM, new ToStreamOperationParser(context, resources));
        result.put(KSMLDSL.Operations.TO_TABLE, new ToTableOperationParser(context, resources));
        result.put(KSMLDSL.Operations.WINDOW_BY_TIME, new WindowByTimeOperationParser(context, resources));
        result.put(KSMLDSL.Operations.WINDOW_BY_SESSION, new WindowBySessionOperationParser(context, resources));
        return result;
    }
}
