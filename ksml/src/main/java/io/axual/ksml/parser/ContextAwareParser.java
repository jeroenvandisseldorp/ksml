package io.axual.ksml.parser;

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


import io.axual.ksml.execution.FatalError;
import io.axual.ksml.generator.TopologyResources;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class ContextAwareParser<T> extends SingleFormParser<T> {
    private static final Map<String, AtomicInteger> typeInstanceCounters = new HashMap<>();
    // The set of streams, functions and stores that producers and pipelines can reference
    private final TopologyResources resources;

    protected ContextAwareParser(TopologyResources resources) {
        super(resources != null ? resources.namespace() : null);
        this.resources = resources;
    }

    protected TopologyResources resources() {
        if (resources != null) return resources;
        throw FatalError.topologyError("Topology resources not properly initialized. This is a programming error.");
    }

    protected String determineName(String name, String type) {
        if (name == null || name.trim().isEmpty()) {
            return determineName(type);
        }
        return name.trim();
    }

    protected String determineName(String name) {
        final var basename = namespace() + "_" + name;
        return String.format("%s_%03d", basename, typeInstanceCounters.computeIfAbsent(basename, t -> new AtomicInteger(1)).getAndIncrement());
    }

//    protected <F extends FunctionDefinition> MultiSchemaParser<FunctionDefinition> functionField(String childName, boolean required, String doc, MultiSchemaParser<? extends FunctionDefinition> parser) {
//        final var resourceParser = new TopologyResourceParser<>("function", childName, required, doc, resources::function, parser);
//        return MultiSchemaParser.of(resourceParser::parseDefinition, resourceParser.schema());
//    }

//    protected <T> SingleSchemaParser<T> lookupField(String resourceType, String childName, boolean required, String doc, Function<String, T> lookup, StructuredParser<? extends T> parser) {
//        final var resourceParser = new TopologyResourceParser<>("stream", childName, required, doc, lookup, parser);
//        return new SingleSchemaParser<>() {
//            @Override
//            public T parse(YamlNode node) {
//                if (node == null) return null;
//                final var resource = resourceParser.parse(node);
//                if (resource != null && (resource.definition() != null || !required)) return resource.definition();
//                if (!required) return null;
//                throw FatalError.parseError(node, resourceType + " not defined");
//            }
//
//            @Override
//            public StructSchema schema() {
//                return resourceParser.schema();
//            }
//        };
//    }

//    public SingleSchemaParser<TopicDefinition> topicField(String childName, boolean required, String doc, StructuredParser<? extends TopicDefinition> parser) {
//        return lookupField("topic", childName, required, doc, resources::topic, parser);
//    }

//    protected <T> SingleSchemaParser<TopologyResource<T>> topologyResourceField(String resourceType, String childName, boolean required, String doc, Function<String, T> lookup, StructuredParser<T> parser) {
//        final var resourceParser = new TopologyResourceParser<>(resourceType, childName, required, doc, lookup, parser, true);
//        return SingleSchemaParser.of(resourceParser::parse, resourceParser.schema());
//    }
}
