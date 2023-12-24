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

import com.google.common.collect.ImmutableMap;
import io.axual.ksml.data.schema.DataField;
import io.axual.ksml.data.schema.DataSchema;
import io.axual.ksml.data.schema.DataValue;
import io.axual.ksml.data.schema.StructSchema;
import io.axual.ksml.execution.FatalError;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

// This class models parsing logic, that looks at a field value to determine which subtype needs to be parsed
public class ChoiceFieldParser<T> extends BaseParser<T> implements MultiSchemaParser<T>, NamedObjectParser {
    private final String childName;
    private final String parsedType;
    private final String defaultValue;
    private final Map<String, MultiSchemaParser<? extends T>> parsers;
    @Getter
    private final List<StructSchema> schemas = new ArrayList<>();

    public ChoiceFieldParser(String childName, String parsedType, String defaultValue, Map<String, MultiSchemaParser<? extends T>> parsers) {
        this.childName = childName;
        this.parsedType = parsedType;
        this.defaultValue = defaultValue;
        this.parsers = ImmutableMap.copyOf(new TreeMap<>(parsers));
        // Collect all possible schemas into a list of alternatives
        this.parsers.forEach((fieldValue, parser) -> {
            for (final var schema : parser.schemas()) {
                final var fields = new ArrayList<DataField>();
                fields.add(new DataField(childName, DataSchema.stringSchema(), "The type of " + parsedType + ", fixed value \"" + fieldValue + "\"", true, true, new DataValue(fieldValue)));
                fields.addAll(schema.fields());
                schemas.add(new StructSchema(MultiFormParser.SCHEMA_NAMESPACE, schema.name(), "", fields));
            }
        });
    }

    @Override
    public T parse(YamlNode node) {
        if (node == null) return null;
        final var child = node.get(childName);
        if (child == null) return null;
        String childValue = child.asString();
        childValue = childValue != null ? childValue : defaultValue;
        if (!parsers.containsKey(childValue)) {
            throw FatalError.parseError(child, "Unknown " + parsedType + " \"" + childName + "\", choose one of " + parsers.keySet().stream().sorted().collect(Collectors.joining(", ")));
        }
        return parsers.get(childValue).parse(node);
    }

    @Override
    public void defaultName(String name) {
        parsers.values().forEach(p -> {
            if (p instanceof NamedObjectParser nop) nop.defaultName(name);
        });
    }
}
