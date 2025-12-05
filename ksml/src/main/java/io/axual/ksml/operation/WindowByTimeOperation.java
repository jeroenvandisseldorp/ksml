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


import io.axual.ksml.exception.TopologyException;
import io.axual.ksml.generator.TopologyBuildContext;
import io.axual.ksml.stream.CogroupedKStreamWrapper;
import io.axual.ksml.stream.KGroupedStreamWrapper;
import io.axual.ksml.stream.StreamWrapper;
import io.axual.ksml.stream.TimeWindowedCogroupedKStreamWrapper;
import io.axual.ksml.stream.TimeWindowedKStreamWrapper;

public class WindowByTimeOperation extends BaseOperation<WindowByTimeOperationDefinition> {
    public WindowByTimeOperation(WindowByTimeOperationDefinition definition) {
        super(definition);
    }

    @Override
    public StreamWrapper apply(KGroupedStreamWrapper input, TopologyBuildContext context) {
        final var k = input.keyType();
        final var v = input.valueType();

        if (def.slidingWindows() != null) {
            /*    Kafka Streams method signature:
             *    TimeWindowedKStream<K, V> windowedBy(
             *          final SlidingWindows windows)
             */

            return new TimeWindowedKStreamWrapper(input.groupedStream.windowedBy(def.slidingWindows()), k, v);
        }

        if (def.timeWindows() != null) {
            /*    Kafka Streams method signature:
             *    <W extends Window > TimeWindowedKStream<K, V> windowedBy(
             *          final Windows<W> windows)
             */

            return new TimeWindowedKStreamWrapper(input.groupedStream.windowedBy(def.timeWindows()), k, v);
        }

        throw new TopologyException("Operation " + name + ". Error applying WINDOW BY to " + input);
    }

    @Override
    public StreamWrapper apply(CogroupedKStreamWrapper input, TopologyBuildContext context) {
        final var k = input.keyType();
        final var v = input.valueType();

        if (def.slidingWindows() != null) {
            /*    Kafka Streams method signature:
             *    TimeWindowedCogroupedKStream<K, VOut> windowedBy(
             *          final SlidingWindows windows)
             */

            return new TimeWindowedCogroupedKStreamWrapper(input.cogroupedStream.windowedBy(def.slidingWindows()), k, v);
        }

        if (def.timeWindows() != null) {
            /*    Kafka Streams method signature:
             *    <W extends Window > TimeWindowedCogroupedKStream<K, V> windowedBy(
             *          final Windows<W> windows)
             */

            return new TimeWindowedCogroupedKStreamWrapper(input.cogroupedStream.windowedBy(def.timeWindows()), k, v);
        }

        throw new TopologyException("Operation " + name + ". Error applying WINDOW BY to " + input);
    }
}
