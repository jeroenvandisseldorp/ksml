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


import io.axual.ksml.data.mapper.NativeDataObjectMapper;
import io.axual.ksml.data.object.DataObject;
import io.axual.ksml.data.tag.ContextTags;
import io.axual.ksml.dsl.KSMLDSL;
import io.axual.ksml.python.Invoker;
import org.apache.kafka.streams.kstream.Aggregator;

public class UserAggregator extends Invoker implements Aggregator<Object, Object, Object> {
    private final NativeDataObjectMapper nativeMapper = NativeDataObjectMapper.SUPPLIER().create();

    public UserAggregator(UserFunction function, ContextTags tags) {
        super(function, tags, KSMLDSL.Functions.TYPE_AGGREGATOR);
        verifyParameterCount(3);
    }

    @Override
    public DataObject apply(Object key, Object value, Object aggregatedValue) {
        return time(() -> function.call(nativeMapper.toDataObject(key), nativeMapper.toDataObject(value), nativeMapper.toDataObject(aggregatedValue)));
    }
}
