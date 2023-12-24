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


import io.axual.ksml.data.object.DataInteger;
import io.axual.ksml.data.object.DataString;
import io.axual.ksml.data.type.StructType;
import io.axual.ksml.data.type.UserType;
import io.axual.ksml.definition.ToOperationDefinition;
import io.axual.ksml.definition.TopologyResource;
import io.axual.ksml.execution.FatalError;
import io.axual.ksml.generator.TopologyBuildContext;
import io.axual.ksml.stream.KStreamWrapper;
import io.axual.ksml.stream.StreamWrapper;
import io.axual.ksml.user.UserStreamPartitioner;
import io.axual.ksml.user.UserTopicNameExtractor;

import static io.axual.ksml.dsl.RecordContextSchema.RECORD_CONTEXT_SCHEMA;

public class ToOperation extends BaseOperation {
    private static final String PARTITIONER_NAME = "Partitioner";
    private static final String TOPICNAMEEXTRACTOR_NAME = "TopicNameExtractor";
    public final TopologyResource<ToOperationDefinition> target;

    public ToOperation(OperationConfig config, TopologyResource<ToOperationDefinition> target) {
        super(config);
        this.target = target;
    }

    @Override
    public StreamWrapper apply(KStreamWrapper input, TopologyBuildContext context) {
        final var to = context.get(target);
        final var topic = context.get(to.topic());
        if (topic != null) {
            /*    Kafka Streams method signature:
             *    void to(
             *          final String topic,
             *          final Produced<K, V> produced)
             */

            final var k = input.keyType();
            final var v = input.valueType();
            final var kt = streamDataTypeOf(firstSpecificType(topic.keyType(), k.userType()), true);
            final var vt = streamDataTypeOf(firstSpecificType(topic.valueType(), v.userType()), false);
            // Perform a dataType check to see if the key/value data types received matches the stream definition's types
            checkType("Target topic keyType", topic.keyType().dataType(), superOf(k));
            checkType("Target topic valueType", topic.valueType().dataType(), superOf(v));
            final var part = userFunctionOf(context, PARTITIONER_NAME, context.get(to.partitioner()), equalTo(DataInteger.DATATYPE), equalTo(DataString.DATATYPE), superOf(k), superOf(v), equalTo(DataInteger.DATATYPE));
            final var userPart = part != null ? new UserStreamPartitioner(part) : null;
            final var produced = producedOf(kt, vt, userPart);
            if (produced != null)
                input.stream.to(topic.topic(), produced);
            else
                input.stream.to(topic.topic());
            return null;
        }

        final var tne = context.get(to.topicNameExtractor());
        if (tne != null) {
            /*    Kafka Streams method signature:
             *    void to(
             *          final TopicNameExtractor<K, V> topicExtractor,
             *          final Produced<K, V> produced)
             */

            final var k = input.keyType();
            final var v = input.valueType();
            final var topicNameType = new UserType(DataString.DATATYPE);
            final var recordContextType = new UserType(new StructType(RECORD_CONTEXT_SCHEMA));
            final var extract = userFunctionOf(context, TOPICNAMEEXTRACTOR_NAME, tne, topicNameType, superOf(k), superOf(v), superOf(recordContextType));
            final var userExtract = new UserTopicNameExtractor(extract);
            final var part = userFunctionOf(context, PARTITIONER_NAME, context.get(to.partitioner()), equalTo(DataInteger.DATATYPE), equalTo(DataString.DATATYPE), superOf(k), superOf(v), equalTo(DataInteger.DATATYPE));
            final var userPart = part != null ? new UserStreamPartitioner(part) : null;
            final var produced = producedOf(k, v, userPart);
            if (produced != null)
                input.stream.to(userExtract, produced);
            else
                input.stream.to(userExtract);
            return null;
        }

        throw FatalError.topologyError("To operation should send data to topic or topic name extractor, but neither were specified");
    }
}
