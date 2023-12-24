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
import io.axual.ksml.definition.FunctionDefinition;
import io.axual.ksml.definition.TopicDefinition;
import io.axual.ksml.definition.TopologyResource;
import io.axual.ksml.execution.FatalError;
import io.axual.ksml.generator.TopologyResources;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

public abstract class MultiFormParser<T> extends DefinitionParser<T> implements MultiSchemaParser<T> {
    // To ensure that code is parsed correctly, we need to take into account that "expression" statements can take
    // many forms, eg. "True", or "1234" or "variable". Therefore, when Jackson parses code, it can potentially be
    // of many different types. These situations are handled by the codeParser.
    private MultiSchemaParser<T> parser;

    public MultiFormParser() {
        this(null);
    }

    public MultiFormParser(String namespace) {
        super(namespace);
    }

    protected abstract MultiSchemaParser<T> parser();

    public final T parse(YamlNode node) {
        if (parser == null) parser = parser();
        return parser.parse(node);
    }

    public final List<StructSchema> schemas() {
        if (parser == null) parser = parser();
        return parser.schemas();
    }

    //    protected static <V> StructParser<V> optional(StructParser<V> parser) {
//        final var newSchemas = new ArrayList<DataSchema>();
//        for (final var schema : parser.schemas()) {
//            final var newFields = schema.fields().stream()
//                    .map(field -> new DataField(field.name(), field.schema(), "(Optional) " + field.doc(), false, false, new DataValue(DataNull.INSTANCE)))
//                    .toList();
//            newSchemas.add(new StructSchema(schema.namespace(), schema.name(), schema.doc(), newFields));
//        }
//        return StructParser.of(node -> node != null ? parser.parse(node) : null, newSchemas);
//    }
//


    protected String validateName(String objectType, String parsedName, String defaultName) {
        return validateName(objectType, parsedName, defaultName, true);
    }

    protected String validateName(String objectType, String parsedName, String defaultName, boolean required) {
        if (parsedName != null) return parsedName;
        if (defaultName != null) return defaultName;
        if (!required) return null;
        return parseError(objectType + " name not defined");
    }

    protected <V> V parseError(String message) {
        throw FatalError.topologyError(message);
    }


    private List<StructSchema> subSchemas(DataSchema schema) {
        final var result = new ArrayList<StructSchema>();
        if (schema instanceof StructSchema structSchema) {
            result.add(structSchema);
            return result;
        }
        if (schema instanceof UnionSchema unionSchema) {
            for (final var possibleSchema : unionSchema.possibleSchemas()) {
                if (possibleSchema instanceof StructSchema structSchema) {
                    result.add(structSchema);
                }
            }
        }
        return result;
    }

    private List<StructSchema> schemas(DataSchema schema) {
        final var results = new ArrayList<StructSchema>();
        if (schema instanceof StructSchema structSchema) {
            results.add(structSchema);
            final var schemasPerField = new ArrayList<List<StructSchema>>();
            for (final var field : structSchema.fields())
                schemasPerField.add(subSchemas(field.schema()));
            for (final var fieldSchemas : schemasPerField)
                if (fieldSchemas.size() > 1) {
                    final var newResults = new ArrayList<StructSchema>();
                    for (final var result : results) {
                        for (final var fieldSchema : fieldSchemas) {
                            // Combine resultSchema and fieldSchemas into a new set of
                            final var newFields = new ArrayList<DataField>();
                            newFields.addAll(result.fields());
                            newFields.addAll(fieldSchema.fields());
                            final var newStruct = structSchema(result.name() + "With" + fieldSchema.name(), "Combined " + result.doc() + " and " + fieldSchema.doc(), newFields);
                            newResults.add(newStruct);
                        }
                    }
                    results.clear();
                    results.addAll(newResults);
                }
        }
        return results;
    }

    protected <T> MultiSchemaParser<TopologyResource<T>> resourceField(String childName, boolean required, String doc, BiFunction<TopologyResources, String, T> lookup, MultiSchemaParser<? extends T> parser) {
        return new TopologyResourceParser<>("stream", childName, required, doc, lookup, parser);
    }

    protected MultiSchemaParser<TopologyResource<FunctionDefinition>> functionField(String childName, boolean required, String doc, MultiSchemaParser<? extends FunctionDefinition> parser) {
        return resourceField(childName, required, doc, TopologyResources::function, parser);
    }

    protected MultiSchemaParser<TopologyResource<TopicDefinition>> topicField(String childName, boolean required, String doc, MultiSchemaParser<? extends TopicDefinition> parser) {
        return resourceField(childName, required, doc, TopologyResources::topic, parser);
    }
}
