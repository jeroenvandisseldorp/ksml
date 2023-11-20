package io.axual.ksml.client.consumer;

/*-
 * ========================LICENSE_START=================================
 * axual-client-proxy
 * %%
 * Copyright (C) 2020 Axual B.V.
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

import io.axual.ksml.client.generic.ResolvingClientConfig;
import io.axual.ksml.client.resolving.GroupResolver;
import org.apache.kafka.clients.consumer.ConsumerConfig;

import java.util.Map;

public class ResolvingConsumerConfig extends ResolvingClientConfig {
    ResolvingConsumerConfig(Map<String, Object> configs) {
        super(configs);

        // Apply resolved group id to downstream consumer
        var configuredGroupId = configs.get(ConsumerConfig.GROUP_ID_CONFIG);
        if (configuredGroupId instanceof String groupId) {
            downstreamConfigs.put(ConsumerConfig.GROUP_ID_CONFIG, groupResolver.resolve(groupId));
        }

        // Apply resolving partition assignment strategy to downstream consumer
        if (configs.containsKey(ConsumerConfig.PARTITION_ASSIGNMENT_STRATEGY_CONFIG)) {
            downstreamConfigs.put(ResolvingConsumerPartitionAssignorConfig.BACKING_ASSIGNOR_CONFIG, configs.get(ConsumerConfig.PARTITION_ASSIGNMENT_STRATEGY_CONFIG));
            downstreamConfigs.put(ResolvingConsumerPartitionAssignorConfig.TOPIC_RESOLVER_CONFIG, topicResolver);
            downstreamConfigs.put(ConsumerConfig.PARTITION_ASSIGNMENT_STRATEGY_CONFIG, ResolvingConsumerPartitionAssignor.class.getName());
        }
    }

    public GroupResolver getGroupResolver() {
        return groupResolver;
    }
}
