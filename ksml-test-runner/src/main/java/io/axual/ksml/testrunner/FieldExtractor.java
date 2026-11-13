package io.axual.ksml.testrunner;

/*-
 * ========================LICENSE_START=================================
 * KSML Test Runner
 * %%
 * Copyright (C) 2021 - 2026 Axual B.V.
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

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.yaml.YAMLFactory;

import io.axual.ksml.parser.DurationParser;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Utility for extracting typed fields from a {@link JsonNode} with rich error messages.
 * All error messages include the file path for context.
 */
class FieldExtractor {

    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());

    private final JsonNode node;
    private final Path contextFile;

    FieldExtractor(JsonNode node, Path contextFile) {
        this.node = node;
        this.contextFile = contextFile;
    }

    /**
     * Get a required string field. Throws if missing or null.
     */
    String requireString(String field) {
        var child = node.get(field);
        if (child == null || child.isNull()) {
            throw new TestDefinitionException("Missing required field '" + field + "' in " + contextFile);
        }
        return child.asString();
    }

    /**
     * Get an optional string field, returning null if absent.
     */
    String optionalString(String field) {
        var child = node.get(field);
        if (child == null || child.isNull()) {
            return null;
        }
        return child.asString();
    }

    /**
     * Get an optional string field with a default value.
     */
    String optionalString(String field, String defaultValue) {
        var result = optionalString(field);
        return result != null ? result : defaultValue;
    }

    /**
     * Get a required array field. Throws if missing or not an array.
     */
    JsonNode requireArray(String field) {
        var child = node.get(field);
        if (child == null || !child.isArray()) {
            throw new TestDefinitionException("Missing or invalid '" + field + "' array in " + contextFile);
        }
        return child;
    }

    /**
     * Get an optional long field, returning empty if absent.
     */
    Optional<Long> optionalLong(String field) {
        if (!node.has(field)) {
            return Optional.empty();
        }
        return Optional.of(node.get(field).asLong());
    }

    /**
     * Get an optional KSML duration field (a number of milliseconds or a string such as "1m"), not negative,
     * returning empty if absent.
     *
     * @param errorPrefix the start of the error message, which names the field and where it is
     */
    Optional<Duration> optionalDuration(String field, String errorPrefix) {
        var child = node.get(field);
        if (child == null || child.isNull()) {
            return Optional.empty();
        }
        if (!child.isString() && !child.isIntegralNumber()) {
            throw new TestDefinitionException(errorPrefix + "expected a duration such as '1m', got " + child);
        }
        Duration result;
        try {
            result = DurationParser.parseDuration(child.asString(), false);
        } catch (RuntimeException e) {
            throw new TestDefinitionException(errorPrefix + e.getMessage());
        }
        if (result.isNegative()) {
            throw new TestDefinitionException(errorPrefix + "must not be negative, got " + child.asString());
        }
        return Optional.of(result);
    }

    /**
     * Get an optional object field as a map, returning an empty map if absent.
     */
    @SuppressWarnings("unchecked")
    Map<String, Object> mapField(String field) {
        var child = node.get(field);
        if (child == null || !child.isObject()) {
            return Map.of();
        }
        return YAML_MAPPER.convertValue(child, LinkedHashMap.class);
    }

    /**
     * Get an optional array field as a list of strings, returning an empty list if absent.
     */
    List<String> stringListField(String field) {
        var child = node.get(field);
        if (child == null || !child.isArray()) {
            return List.of();
        }
        var result = new ArrayList<String>();
        for (var element : child) {
            result.add(element.asString());
        }
        return result;
    }

    /**
     * Convert a JsonNode value to a native Java object (String, Integer, Map, List, etc.).
     */
    static Object nodeToObject(JsonNode jsonNode) {
        if (jsonNode == null || jsonNode.isNull()) {
            return null;
        }
        if (jsonNode.isString()) {
            return jsonNode.asString();
        }
        if (jsonNode.isNumber()) {
            if (jsonNode.isInt()) return jsonNode.asInt();
            if (jsonNode.isLong()) return jsonNode.asLong();
            return jsonNode.asDouble();
        }
        if (jsonNode.isBoolean()) {
            return jsonNode.asBoolean();
        }
        if (jsonNode.isObject()) {
            return YAML_MAPPER.convertValue(jsonNode, LinkedHashMap.class);
        }
        if (jsonNode.isArray()) {
            var list = new ArrayList<>();
            for (var item : jsonNode) {
                list.add(nodeToObject(item));
            }
            return list;
        }
        return jsonNode.asString();
    }
}
