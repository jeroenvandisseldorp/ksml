package io.axual.ksml.operation;

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

import io.axual.ksml.generator.TopologyBuildContext;
import io.axual.ksml.stream.KStreamWrapper;
import io.axual.ksml.stream.StreamWrapper;
import io.axual.ksml.type.UserType;
import org.apache.kafka.streams.kstream.KeyValueMapper;
import org.apache.kafka.streams.kstream.Named;

public class ConvertKeyOperation extends BaseOperation {
    private final UserType targetKeyType;

    public ConvertKeyOperation(OperationConfig config, UserType targetKeyType) {
        super(config);
        this.targetKeyType = targetKeyType;
    }

    @Override
    public StreamWrapper apply(KStreamWrapper input, TopologyBuildContext context) {
        final var k = input.keyType().flatten();
        final var v = input.valueType();
        final var kr = streamDataTypeOf(targetKeyType, true).flatten();
        final var mapper = context.converter();

        // Set up the mapping function to convert the value
        final KeyValueMapper<Object, Object, Object> converter = (key, value) -> {
            final var keyAsData = flattenValue(key);
            return mapper.convert(k.userType().notation(), keyAsData, kr.userType());
        };

        // Inject the mapper into the topology
        final var output = name != null
                ? input.stream.selectKey(converter, Named.as(name))
                : input.stream.selectKey(converter);

        return new KStreamWrapper(output, kr, v);
    }
}
