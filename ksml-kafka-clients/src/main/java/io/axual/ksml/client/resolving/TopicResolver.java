package io.axual.ksml.client.resolving;

/*-
 * ========================LICENSE_START=================================
 * axual-common
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

import org.apache.kafka.common.TopicPartition;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * A topic resolver can translate Kafka topic names from an application's internal representation to
 * one found externally (or "physically") on Kafka clusters. The conversion from internal to
 * external representation is done through {@link #resolveTopic(String)} and similar calls. The
 * reverse is done through {link #unresolveTopic(String)} and similar calls.
 */
public interface TopicResolver extends Resolver {
    /**
     * Translates the internal representation of topic name to the external one.
     *
     * @param topic the application's internal topic name
     * @return the external representation of the topic
     */
    default String resolveTopic(final String topic) {
        return resolve(topic);
    }

    /**
     * Translates the internal representation of topic pattern to the external one.
     *
     * @param pattern the application's internal topic pattern
     * @return the external representation of the topic pattern
     */
    default Pattern resolveTopicPattern(final Pattern pattern) {
        // Wrap the pattern in brackets and resolve the resulting string as if it were a topic
        String resolvedRegex = resolve("(" + pattern.pattern() + ")");
        return Pattern.compile(resolvedRegex);
    }

    /**
     * Translates the internal representation of topic name to the external one.
     *
     * @param topicPartition the application's internal topic partition
     * @return the external representation of the topic partition
     */
    default TopicPartition resolveTopic(final TopicPartition topicPartition) {
        if (topicPartition == null) return null;
        return new TopicPartition(resolve(topicPartition.topic()), topicPartition.partition());
    }

    /**
     * Translates the internal representation of topic names to the external ones.
     *
     * @param topics the application's internal topic names
     * @return the external representation of the topics
     */
    default Set<String> resolveTopics(Collection<String> topics) {
        if (topics == null) return new HashSet<>();

        Set<String> result = new HashSet<>(topics.size());
        for (String topic : topics) {
            String resolvedTopic = resolve(topic);
            if (resolvedTopic != null) {
                result.add(resolvedTopic);
            }
        }
        return result;
    }

    /**
     * Translates the internal representation of topic partitions to the external ones.
     *
     * @param topicPartitions the application's internal topic partitions
     * @return the external representation of the topic partitions
     */
    default Set<TopicPartition> resolveTopicPartitions(Collection<TopicPartition> topicPartitions) {
        if (topicPartitions == null) return new HashSet<>();

        Set<TopicPartition> result = new HashSet<>(topicPartitions.size());
        for (TopicPartition partition : topicPartitions) {
            result.add(resolveTopic(partition));
        }
        return result;
    }

    /**
     * Translates the internal representation of a topic partition map to the external one.
     *
     * @param <V>               any type used as Value in the Map
     * @param topicPartitionMap the map containing the application's internal topic partitions as
     *                          Keys
     * @return the map containing the external representation of the topic partitions as Keys
     */
    default <V> Map<TopicPartition, V> resolveTopics(Map<TopicPartition, V> topicPartitionMap) {
        if (topicPartitionMap == null) return new HashMap<>();

        Map<TopicPartition, V> result = new HashMap<>(topicPartitionMap.size());
        for (Map.Entry<TopicPartition, V> entry : topicPartitionMap.entrySet()) {
            result.put(resolveTopic(entry.getKey()), entry.getValue());
        }
        return result;
    }

    /**
     * Translates the external representation of topic name to the internal one.
     *
     * @param topic the external topic name
     * @return the internal consumer topic name
     */
    default String unresolveTopic(final String topic) {
        return unresolve(topic);
    }

    /**
     * Translates the external representation of topic partition to the internal one.
     *
     * @param topicPartition the external topic partition
     * @return the internal consumer topic partition
     */
    default TopicPartition unresolveTopic(TopicPartition topicPartition) {
        if (topicPartition == null) return null;

        String unresolvedTopic = unresolveTopic(topicPartition.topic());
        if (unresolvedTopic == null) return null;
        return new TopicPartition(unresolvedTopic, topicPartition.partition());
    }

    /**
     * Translates the external representation of topic names to the internal ones.
     *
     * @param topics the external topic names
     * @return the internal consumer topic names
     */
    default Set<String> unresolveTopics(Collection<String> topics) {
        if (topics == null) return new HashSet<>();

        Set<String> result = new HashSet<>(topics.size());
        for (String topic : topics) {
            String unresolvedTopic = unresolveTopic(topic);
            if (unresolvedTopic != null) {
                result.add(unresolvedTopic);
            }
        }
        return result;
    }

    /**
     * Translates the external representation of topic partitions to the internal ones.
     *
     * @param topicPartitions the external topic partitions
     * @return the internal consumer topic partitions
     */
    default Set<TopicPartition> unresolveTopicPartitions(Collection<TopicPartition> topicPartitions) {
        if (topicPartitions == null) return new HashSet<>();

        Set<TopicPartition> result = new HashSet<>(topicPartitions.size());
        for (TopicPartition partition : topicPartitions) {
            TopicPartition unresolvedPartition = unresolveTopic(partition);
            if (unresolvedPartition != null) {
                result.add(unresolvedPartition);
            }
        }
        return result;
    }

    /**
     * Translates the external representation of topic partition to the internal one.
     *
     * @param <V>               any type used as Value in the Map
     * @param topicPartitionMap the map containing the application's external topic partitions as
     *                          keys
     * @return the map containing the internal representation of the topic partitions as keys
     */
    default <V> Map<TopicPartition, V> unresolveTopics(Map<TopicPartition, V> topicPartitionMap) {
        if (topicPartitionMap == null) return new HashMap<>();

        Map<TopicPartition, V> result = new HashMap<>(topicPartitionMap.size());
        for (Map.Entry<TopicPartition, V> entry : topicPartitionMap.entrySet()) {
            TopicPartition unresolvedTopic = unresolveTopic(entry.getKey());
            if (unresolvedTopic != null) {
                result.put(unresolvedTopic, entry.getValue());
            }
        }
        return result;
    }
}