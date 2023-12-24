package io.axual.ksml.definition;

import io.axual.ksml.user.UserFunction;

public record ToOperationDefinition(TopologyResource<TopicDefinition> topic,
                                    TopologyResource<FunctionDefinition> topicNameExtractor,
                                    TopologyResource<FunctionDefinition> partitioner) {
}
