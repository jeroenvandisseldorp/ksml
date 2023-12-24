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
import io.axual.ksml.operation.StreamOperation;
import io.axual.ksml.parser.ChoiceFieldParser;
import io.axual.ksml.parser.MultiSchemaParser;

import java.util.HashMap;
import java.util.Map;

public class PipelineOperationParser extends ChoiceFieldParser<StreamOperation> {
    public PipelineOperationParser(String namespace) {
        super(KSMLDSL.Operations.TYPE_ATTRIBUTE, null, "operation", types(namespace));
    }

    private static Map<String, MultiSchemaParser<? extends StreamOperation>> types(String namespace) {
        final var result = new HashMap<String, MultiSchemaParser<? extends StreamOperation>>();
        result.put(KSMLDSL.Operations.AGGREGATE, new AggregateOperationParser(namespace));
        result.put(KSMLDSL.Operations.COGROUP, new CogroupOperationParser(namespace));
        result.put(KSMLDSL.Operations.CONVERT_KEY, new ConvertKeyOperationParser(namespace));
        result.put(KSMLDSL.Operations.CONVERT_KEY_VALUE, new ConvertKeyValueOperationParser(namespace));
        result.put(KSMLDSL.Operations.CONVERT_VALUE, new ConvertValueOperationParser(namespace));
        result.put(KSMLDSL.Operations.COUNT, new CountOperationParser(namespace));
        result.put(KSMLDSL.Operations.FILTER, new FilterOperationParser(namespace));
        result.put(KSMLDSL.Operations.FILTER_NOT, new FilterNotOperationParser(namespace));
        result.put(KSMLDSL.Operations.FLATMAP, new TransformKeyValueToKeyValueListOperationParser(namespace));
        result.put(KSMLDSL.Operations.TRANSFORM_KEY_VALUE_TO_KEY_VALUE_LIST, new TransformKeyValueToKeyValueListOperationParser(namespace));
        result.put(KSMLDSL.Operations.FLATMAP_VALUES, new TransformKeyValueToValueListOperationParser(namespace));
        result.put(KSMLDSL.Operations.TRANSFORM_KEY_VALUE_TO_VALUE_LIST, new TransformKeyValueToValueListOperationParser(namespace));
        result.put(KSMLDSL.Operations.GROUP_BY, new GroupByOperationParser(namespace));
        result.put(KSMLDSL.Operations.GROUP_BY_KEY, new GroupByKeyOperationParser(namespace));
        result.put(KSMLDSL.Operations.JOIN, new JoinOperationParser(namespace));
        result.put(KSMLDSL.Operations.LEFT_JOIN, new LeftJoinOperationParser(namespace));
        result.put(KSMLDSL.Operations.MAP_KEY, new TransformKeyOperationParser(namespace));
        result.put(KSMLDSL.Operations.SELECT_KEY, new TransformKeyOperationParser(namespace));
        result.put(KSMLDSL.Operations.TRANSFORM_KEY, new TransformKeyOperationParser(namespace));
        result.put(KSMLDSL.Operations.MAP, new TransformKeyValueOperationParser(namespace));
        result.put(KSMLDSL.Operations.MAP_KEY_VALUE, new TransformKeyValueOperationParser(namespace));
        result.put(KSMLDSL.Operations.TRANSFORM_KEY_VALUE, new TransformKeyValueOperationParser(namespace));
        result.put(KSMLDSL.Operations.MAP_VALUE, new TransformValueOperationParser(namespace));
        result.put(KSMLDSL.Operations.MAP_VALUES, new TransformValueOperationParser(namespace));
        result.put(KSMLDSL.Operations.TRANSFORM_VALUE, new TransformValueOperationParser(namespace));
        result.put(KSMLDSL.Operations.MERGE, new MergeOperationParser(namespace));
        result.put(KSMLDSL.Operations.OUTER_JOIN, new OuterJoinOperationParser(namespace));
        result.put(KSMLDSL.Operations.PEEK, new PeekOperationParser(namespace));
        result.put(KSMLDSL.Operations.REDUCE, new ReduceOperationParser(namespace));
        result.put(KSMLDSL.Operations.REPARTITION, new RepartitionOperationParser(namespace));
        result.put(KSMLDSL.Operations.SUPPRESS, new SuppressOperationParser(namespace));
        result.put(KSMLDSL.Operations.TO_STREAM, new ToStreamOperationParser(namespace));
        result.put(KSMLDSL.Operations.TO_TABLE, new ToTableOperationParser(namespace));
        result.put(KSMLDSL.Operations.WINDOW_BY_TIME, new WindowByTimeOperationParser(namespace));
        result.put(KSMLDSL.Operations.WINDOW_BY_SESSION, new WindowBySessionOperationParser(namespace));
        return result;
    }
}
