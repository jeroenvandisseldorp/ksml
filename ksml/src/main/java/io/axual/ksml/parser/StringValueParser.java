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

import io.axual.ksml.data.schema.DataSchema;
import io.axual.ksml.data.schema.UnionSchema;

public class StringValueParser implements StructuredParser<String> {
    private final boolean isCode;

    public interface BooleanToStringConverter {
        String interpret(boolean value);
    }

    private final BooleanToStringConverter converter;

    public StringValueParser() {
        this(false);
    }

    public StringValueParser(boolean isCode) {
        this.isCode = isCode;
        this.converter = isCode
                // Use capitals with True/False for Python code
                ? value -> value ? "True" : "False"
                : value -> value ? "true" : "false";
    }

    @Override
    public String parse(YamlNode node) {
        // This implementation catches a corner case, where Jackson parses a string as boolean, whereas it was meant
        // to be interpreted as a string literal for Python.
        if (node != null) {
            // Parse any type and convert it to String as expected
            if (node.isBoolean()) return converter.interpret(node.asBoolean());
            if (node.isFloat()) return "" + node.asFloat();
            if (node.isDouble()) return "" + node.asDouble();
            if (node.isShort()) return "" + node.asShort();
            if (node.isInt()) return "" + node.asInt();
            if (node.isLong()) return "" + node.asLong();
            if (node.isString()) return node.asString();
        }
        return null;
    }

    @Override
    public DataSchema schema() {
        if (!isCode) return DataSchema.stringSchema();
        // The string is parsed as code, so to enable proper syntax support with the generated JSON schena, we indicate
        // the code field could contain many different kinds of types (eg. used in an "expression" field as part of
        // a user function.
        return new UnionSchema(
                DataSchema.booleanSchema(),
                DataSchema.floatSchema(),
                DataSchema.doubleSchema(),
                DataSchema.shortSchema(),
                DataSchema.integerSchema(),
                DataSchema.longSchema(),
                DataSchema.stringSchema());
    }
}
