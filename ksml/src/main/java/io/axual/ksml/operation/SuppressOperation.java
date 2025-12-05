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


import io.axual.ksml.data.type.WindowedType;
import io.axual.ksml.dsl.KSMLDSL;
import io.axual.ksml.exception.TopologyException;
import io.axual.ksml.generator.TopologyBuildContext;
import io.axual.ksml.stream.KTableWrapper;
import io.axual.ksml.stream.StreamWrapper;
import org.apache.kafka.streams.kstream.Suppressed;

public class SuppressOperation extends BaseOperation<SuppressOperationDefinition> {
    public SuppressOperation(SuppressOperationDefinition definition) {
        super(definition);
    }

    @Override
    public StreamWrapper apply(KTableWrapper input, TopologyBuildContext context) {
        /*    Kafka Streams method signature:
         *    KTable<K, V> suppress(
         *          final Suppressed<? super K> suppressed)
         */

        final var k = input.keyType();
        final var v = input.valueType();

        final var bufferConfig = bufferConfig(def.maxBytes(), def.maxRecords(), def.bufferFullStrategy());

        // Handle "untilTimeLimit" case
        if (def.timeToWaitForMoreEvents() != null) {
            final var suppressed = Suppressed.untilTimeLimit(def.timeToWaitForMoreEvents(), bufferConfig);
            return new KTableWrapper(input.table.suppress(suppressed), k, v);
        }

        // Because of dataType erasure, we cannot rely on Java to perform dataType checking the key
        // for us. Therefore, we check the dataType manually to ensure the user is applying the
        // "untilWindowCloses" suppression on the right KTable key dataType.

        // Validate that the key dataType is windowed or can be converted to windowed
        final var suppressedWindowed = Suppressed.untilWindowCloses(strictBufferConfig(bufferConfig));
        if (k.userType().dataType() instanceof WindowedType) {
            return new KTableWrapper(input.table.suppress((Suppressed) suppressedWindowed), k, v);
        }
        // Throw an exception if the stream key dataType is not Windowed
        throw new TopologyException("Can not apply suppress operation to a KTable with key dataType " + input.keyType().userType());
    }

    private Suppressed.EagerBufferConfig bufferConfig(String maxBytes, String maxRecords, String bufferFullStrategy) {
        Suppressed.EagerBufferConfig result = null;

        // Check for a maxBytes setting
        if (maxBytes != null) {
            result = Suppressed.BufferConfig.maxBytes(Long.parseLong(maxBytes));
        }

        // Check for a maxRecords setting
        if (maxRecords != null) {
            if (result == null) {
                result = Suppressed.BufferConfig.maxRecords(Long.parseLong(maxRecords));
            } else {
                result = result.withMaxRecords(Long.parseLong(maxRecords));
            }
        }

        // Check for a bufferFull strategy
        if (KSMLDSL.Operations.Suppress.BUFFER_FULL_STRATEGY_EMIT.equals(bufferFullStrategy)) {
            if (result == null) {
                throw new TopologyException("Can not instantiate BufferConfig without maxBytes and/or maxRecords setting");
            }
            result = result.emitEarlyWhenFull();
        }

        return result;
    }

    private Suppressed.StrictBufferConfig strictBufferConfig(Suppressed.EagerBufferConfig config) {
        // Assume the BufferFullStrategy is SHUT_DOWN from here on
        if (config == null) {
            return Suppressed.BufferConfig.unbounded();
        }

        return config.shutDownWhenFull();
    }
}
