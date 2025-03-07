package io.axual.ksml.client.admin;

/*-
 * ========================LICENSE_START=================================
 * Extended Kafka clients for KSML
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

import io.axual.ksml.client.resolving.GroupResolver;
import io.axual.ksml.client.resolving.TopicResolver;
import org.apache.kafka.clients.admin.ExtendableListConsumerGroupOffsetsResult;
import org.apache.kafka.clients.admin.internals.CoordinatorKey;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.internals.KafkaFutureImpl;

import java.util.HashMap;
import java.util.Map;

public class ResolvingListConsumerGroupOffsetsResult extends ExtendableListConsumerGroupOffsetsResult {
    public ResolvingListConsumerGroupOffsetsResult(final Map<CoordinatorKey, KafkaFuture<Map<TopicPartition, OffsetAndMetadata>>> futures, TopicResolver topicResolver, GroupResolver groupResolver) {
        super(convertResult(futures, topicResolver, groupResolver));
    }

    private static Map<CoordinatorKey, KafkaFuture<Map<TopicPartition, OffsetAndMetadata>>> convertResult(final Map<CoordinatorKey, KafkaFuture<Map<TopicPartition, OffsetAndMetadata>>> futures, TopicResolver topicResolver, GroupResolver groupResolver) {
        final var result = HashMap.<CoordinatorKey, KafkaFuture<Map<TopicPartition, OffsetAndMetadata>>>newHashMap(futures.size());
        futures.forEach((coordinatorKey, future) -> {
            final var newKey = CoordinatorKey.byGroupId(groupResolver.unresolve(coordinatorKey.idValue));

            final KafkaFutureImpl<Map<TopicPartition, OffsetAndMetadata>> wrappingFuture = new KafkaFutureImpl<>();
            future.whenComplete((offsets, throwable) -> {
                if (offsets != null) {
                    final var newOffsets = HashMap.<TopicPartition, OffsetAndMetadata>newHashMap(offsets.size());
                    offsets.forEach(((topicPartition, offsetAndMetadata) -> newOffsets.put(topicResolver.unresolve(topicPartition), offsetAndMetadata)));
                    wrappingFuture.complete(newOffsets);
                } else {
                    wrappingFuture.completeExceptionally(throwable);
                }
            });

            result.put(newKey, wrappingFuture);
        });
        return result;
    }
}
