package io.axual.ksml.data.object;

/*-
 * ========================LICENSE_START=================================
 * KSML Data Library
 * %%
 * Copyright (C) 2021 - 2025 Axual B.V.
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

import io.axual.ksml.data.schema.DataSchemaConstants;
import io.axual.ksml.data.type.DecimalType;

import java.math.BigDecimal;

/**
 * {@link DataObject} wrapper for decimal values with an associated {@link DecimalType}.
 *
 * <p>Two DataDecimal instances are equal when both type and value are equal.</p>
 */
public class DataDecimal extends DataPrimitive<BigDecimal> {
    /**
     * Represents the data type of this {@code DataDecimal}, which is {@code BigDecimal},
     * mapped to the schema definition in {@link DataSchemaConstants#DECIMAL_TYPE}.
     * <p>This constant ensures that the type metadata for a {@code BigDecimal} is
     * consistent across all usages in the framework.</p>
     */
    public DataDecimal(DecimalType type, BigDecimal value) {
        super(type, value);
    }
}
