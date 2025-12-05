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
import io.axual.ksml.data.type.SimpleType;
import org.threeten.extra.PeriodDuration;

/**
 * Represents a wrapper for a duration as part of the {@link DataObject} framework.
 *
 * <p>The {@code DataDuration} class encapsulates a duration to integrate seamlessly
 * into the structured data model used in schema-compliant or stream-processed data.
 * It enables durations to be used as {@link DataObject} types, making them compatible
 * with the framework and allowing for standardized processing.</p>
 *
 * @see DataObject
 */
public class DataDuration extends DataPrimitive<PeriodDuration> {
    /**
     * Represents the data type of this {@code DataDuration}, which is {@code PeriodDuration},
     * mapped to the schema definition in {@link DataSchemaConstants#DURATION_TYPE}.
     * <p>This constant ensures that the type metadata for a {@code DataDuration} is
     * consistent across all usages in the framework.</p>
     */
    public static final SimpleType DATATYPE = new SimpleType(PeriodDuration.class, DataSchemaConstants.DURATION_TYPE);

    /**
     * Constructs a {@code DataDuration} instance with a null value.
     * <p>This constructor creates a {@code DataDuration} that does not hold any actual
     * {@code PeriodDuration} value, effectively representing a "null" duration in the framework.</p>
     */
    public DataDuration() {
        this(null);
    }

    /**
     * Constructs a {@code DataDuration} instance with the specified {@code PeriodDuration} value.
     *
     * <p>If the input value is {@code null}, the {@code DataDuration} will represent
     * the absence of a value (a null duration).</p>
     *
     * @param value The {@code PeriodDuration} value to encapsulate, or {@code null} to represent a null value.
     */
    public DataDuration(PeriodDuration value) {
        super(DATATYPE, value);
    }
}
