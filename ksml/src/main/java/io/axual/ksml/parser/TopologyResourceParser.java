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


import io.axual.ksml.data.schema.DataField;
import io.axual.ksml.data.schema.DataSchema;
import io.axual.ksml.data.schema.StructSchema;
import io.axual.ksml.data.schema.UnionSchema;
import io.axual.ksml.definition.TopologyResource;
import io.axual.ksml.generator.TopologyResources;
import lombok.Getter;

import java.util.List;
import java.util.function.BiFunction;

// Certain KSML resources (like streams, tables and functions) can be referenced from pipelines,
// or they can be defined inline. This parser distinguishes between the two.
public class TopologyResourceParser<T, F extends T> extends MultiFormParser<TopologyResource<T>> {
    private final String resourceType;
    private final String childName;
    private final boolean required;
    private final String doc;
    private final BiFunction<TopologyResources, String, T> lookup;
    private final StructuredParser<? extends F> inlineParser;
    private final boolean allowLookupFail;
    @Getter
    private final StructSchema schema;

    public TopologyResourceParser(String resourceType, String childName, boolean required, String doc, BiFunction<TopologyResources, String, T> lookup, StructuredParser<? extends F> inlineParser) {
        this(resourceType, childName, required, doc, lookup, inlineParser, false);
    }

    public TopologyResourceParser(String resourceType, String childName, boolean required, String doc, BiFunction<TopologyResources, String, T> lookup, StructuredParser<? extends F> inlineParser, boolean allowLookupFail) {
        this.resourceType = resourceType;
        this.childName = childName;
        this.required = required;
        this.doc = doc;
        this.lookup = lookup;
        this.inlineParser = inlineParser;
        this.allowLookupFail = allowLookupFail;
        this.schema = structSchema(resourceType + childName, doc, List.of(new DataField(childName, new UnionSchema(DataSchema.stringSchema(), inlineParser.schema()), doc, required)));
    }

    @Override
    public MultiSchemaParser<TopologyResource<T>> parser() {
        final var stringParser = stringField(childName, false, null, doc);
        return new SingleSchemaParser<>() {
            @Override
            public TopologyResource<T> parse(YamlNode node) {
                if (node == null) return null;

                // Check if the node is a text node --> parse as direct reference
                final var resourceToFind = stringParser.parse(node);
                if (resourceToFind != null) {
                    return new TopologyResource<>(resourceToFind, lookup, null);
                }

                // Parse as anonymous inline definition using the supplied inline parser
                final var childNode = node.get(childName);
                if (childNode != null) {
                    final var name = childNode.longName();
                    return new TopologyResource<>(name, null, inlineParser.parse(childNode));
                }

                final var name = node.appendName(childName).longName();
                return new TopologyResource<>(name, null, null);
            }

            @Override
            public StructSchema schema() {
                return schema;
            }
        };
    }
}
