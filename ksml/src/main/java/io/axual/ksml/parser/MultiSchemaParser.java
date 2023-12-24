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
import io.axual.ksml.data.schema.StructSchema;
import io.axual.ksml.data.schema.UnionSchema;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public interface MultiSchemaParser<T> extends StructuredParser<T> {
    List<StructSchema> schemas();

    default DataSchema schema() {
        final var schemas = schemas();
        return schemas.size() == 1 ? schemas.get(0) : new UnionSchema(schemas.toArray(DataSchema[]::new));
    }

    static <T> MultiSchemaParser<T> of(final Function<YamlNode, T> parseFunc, StructSchema schema) {
        return of(parseFunc, List.of(schema));
    }

    static <T> MultiSchemaParser<T> of(final Function<YamlNode, T> parseFunc, List<StructSchema> schemas) {
        return of(parseFunc, () -> schemas);
    }

    static <T> MultiSchemaParser<T> of(final Function<YamlNode, T> parseFunc, Supplier<List<StructSchema>> getter) {
        return new MultiSchemaParser<>() {
            @Override
            public T parse(YamlNode node) {
                return parseFunc.apply(node);
            }

            @Override
            public List<StructSchema> schemas() {
                return getter.get();
            }
        };
    }
}
