package io.axual.ksml.user;

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

import io.axual.ksml.data.mapper.DataObjectFlattener;
import io.axual.ksml.data.mapper.NativeDataObjectMapper;
import io.axual.ksml.data.object.DataBoolean;
import io.axual.ksml.data.type.DataType;
import io.axual.ksml.dsl.KSMLDSL;
import io.axual.ksml.exception.ExecutionException;
import io.axual.ksml.metric.MetricTags;
import io.axual.ksml.python.Invoker;
import io.axual.ksml.store.StateStores;
import org.apache.kafka.streams.kstream.Predicate;

public class UserPredicate extends Invoker implements Predicate<Object, Object> {
    private static final DataType EXPECTED_RESULT_TYPE = DataBoolean.DATATYPE;
    private static final NativeDataObjectMapper NATIVE_MAPPER = new DataObjectFlattener();

    public UserPredicate(UserFunction function, MetricTags tags) {
        super(function, tags, KSMLDSL.Functions.TYPE_PREDICATE);
        verifyParameterCount(2);
        verifyResultType(EXPECTED_RESULT_TYPE);
    }

    @Override
    public boolean test(Object key, Object value) {
        verifyNoStoresUsed();
        return test(null, key, value);
    }

    public boolean test(StateStores stores, Object key, Object value) {
        final var result = timeExecutionOf(() -> function.call(stores, NATIVE_MAPPER.toDataObject(key), NATIVE_MAPPER.toDataObject(value)));
        if (result instanceof DataBoolean dataBoolean) {
            return dataBoolean.value();
        }
        throw new ExecutionException("Expected a boolean back from the predicate function: " + function.name);
    }
}
