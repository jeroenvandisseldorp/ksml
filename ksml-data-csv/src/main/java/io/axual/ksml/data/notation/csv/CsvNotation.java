package io.axual.ksml.data.notation.csv;

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

import io.axual.ksml.data.loader.SchemaLoader;
import io.axual.ksml.data.mapper.NativeDataObjectMapper;
import io.axual.ksml.data.notation.NotationConverter;
import io.axual.ksml.data.notation.string.StringNotation;
import io.axual.ksml.data.type.DataType;
import io.axual.ksml.data.type.ListType;
import io.axual.ksml.data.type.StructType;
import io.axual.ksml.data.type.UnionType;
import lombok.Getter;
import org.apache.kafka.common.serialization.Serde;

public class CsvNotation extends StringNotation {
    public static final DataType DEFAULT_TYPE = new UnionType(new StructType(), new ListType());
    @Getter
    private final NotationConverter converter = new CsvDataObjectConverter();
    @Getter
    private final SchemaLoader loader;

    public CsvNotation(NativeDataObjectMapper nativeMapper, SchemaLoader loader) {
        super(nativeMapper, new CsvDataObjectMapper());
        this.loader = loader;
    }

    @Override
    public DataType defaultType() {
        return DEFAULT_TYPE;
    }

    @Override
    public Serde<Object> serde(DataType type, boolean isKey) {
        // CSV types should ways be Lists, Structs or the union of them both
        if (type instanceof ListType || type instanceof StructType || DEFAULT_TYPE.equals(type))
            return super.serde(type, isKey);
        // Other types can not be serialized as XML
        throw noSerdeFor("CSV", type);
    }
}
