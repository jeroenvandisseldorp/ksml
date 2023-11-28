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


import io.axual.ksml.definition.TopicDefinition;
import io.axual.ksml.definition.Ref;
import io.axual.ksml.generator.TopologyBuildContext;
import io.axual.ksml.stream.KStreamWrapper;
import io.axual.ksml.stream.StreamWrapper;
import org.apache.kafka.streams.kstream.Produced;

public class ToOperation extends BaseOperation {
    public final Ref<TopicDefinition> output;

    public ToOperation(OperationConfig config, Ref<TopicDefinition> output) {
        super(config);
        this.output = output;
    }

    @Override
    public StreamWrapper apply(KStreamWrapper input, TopologyBuildContext context) {
        /*    Kafka Streams method signature:
         *    void to(
         *          final String topic,
         *          final Produced<K, V> produced)
         */

        final var target = context.lookupTopic(output, "target");
        final var k = input.keyType();
        final var v = input.valueType();
        final var kr = streamDataTypeOf(firstSpecificType(target.getKeyType(), k.userType()), true);
        final var vr = streamDataTypeOf(firstSpecificType(target.getValueType(), v.userType()), false);
        // Perform a dataType check to see if the key/value data types received matches the stream definition's types
        checkType("Target topic keyType", target.getKeyType().dataType(), superOf(k));
        checkType("Target topic valueType", target.getValueType().dataType(), superOf(v));

        var produced = Produced.with(kr.getSerde(), vr.getSerde());
        if (name != null) produced = produced.withName(name);
        input.stream.to(target.getTopic(), produced);
        return null;
    }
}
