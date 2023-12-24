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
import io.axual.ksml.data.schema.StructSchema;
import io.axual.ksml.data.value.Pair;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// This class models parsing logic, that looks at a field value to determine which subtype needs to be parsed
public class ChoiceAttributeParser<T> extends BaseParser<T> implements MultiSchemaParser<T>, NamedObjectParser {
    private final Map<String, Pair<String, MultiSchemaParser<? extends T>>> parsers;
    @Getter
    private final List<StructSchema> schemas = new ArrayList<>();

    public ChoiceAttributeParser(String emptyName, boolean required, Map<String, Pair<String, MultiSchemaParser<? extends T>>> parsers) {
        this.parsers = ImmutableMap.copyOf(parsers);
        // Collect all possible schemas into a list of alternatives
        this.parsers.forEach((fieldValue, parser) -> {
            for (final var schema : parser.right().schemas()) {
                final var newName = parser.left();
                final var newSchema = new StructSchema(DefinitionParser.SCHEMA_NAMESPACE, newName, schema.doc(), schema.fields());
                schemas.add(newSchema);
            }
        });
        if (!required) {
            schemas.add(new StructSchema(DefinitionParser.SCHEMA_NAMESPACE, emptyName, "", new ArrayList<>()));
        }
    }

    @Override
    public T parse(YamlNode node) {
        if (node == null) return null;
        for (final var entry : parsers.entrySet()) {
            if (node.get(entry.getKey()) != null) {
                return entry.getValue().right().parse(node);
            }
        }
        return null;
    }

    @Override
    public void defaultName(String name) {
        parsers.values().forEach(p -> {
            if (p instanceof NamedObjectParser nop) nop.defaultName(name);
        });
    }
}
