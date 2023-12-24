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

import io.axual.ksml.data.schema.StructSchema;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public interface SingleSchemaParser<T> extends MultiSchemaParser<T> {
    StructSchema schema();

    default List<StructSchema> schemas() {
        return List.of(schema());
    }

    static <T> SingleSchemaParser<T> of(final Function<YamlNode, T> parseFunc, StructSchema schema) {
        return of(parseFunc, () -> schema);
    }

    static <T> SingleSchemaParser<T> of(final Function<YamlNode, T> parseFunc, Supplier<StructSchema> getter) {
        return new SingleSchemaParser<>() {
            @Override
            public T parse(YamlNode node) {
                return parseFunc.apply(node);
            }

            @Override
            public StructSchema schema() {
                return getter.get();
            }
        };
    }
}
