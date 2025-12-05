package io.axual.ksml.data.schema;

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

import io.axual.ksml.data.compare.Assignable;
import io.axual.ksml.data.compare.Equality;
import io.axual.ksml.data.compare.EqualityFlags;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.Objects;

import static io.axual.ksml.data.schema.DataSchemaFlag.IGNORE_TIME_SCHEMA_UNIT;
import static io.axual.ksml.data.util.AssignableUtil.schemaMismatch;
import static io.axual.ksml.data.util.EqualUtil.fieldNotEqual;

/**
 * A schema representation for timestamp values in the KSML framework.
 * <p>
 * The {@code TimestampSchema} class extends the {@link DataSchema} and is used to define
 * schemas for timestamp values. It provides a {@code unit} attribute that specifies
 * the unit of the timestamp value.
 * </p>
 */
@Getter
@EqualsAndHashCode
public class TimestampSchema extends DataSchema {
    public enum Unit {
        NANOSECONDS, MICROSECONDS, MILLISECONDS
    }

    /**
     * The unit of the timestamp value represented by this schema.
     */
    private final boolean local;
    private final Unit unit;

    /**
     * Constructs a {@code TimestampSchema} with the given unit.
     *
     * @param unit The unit of the time
     */
    public TimestampSchema(Unit unit, boolean local) {
        super(DataSchemaConstants.TIME_TYPE);
        this.unit = unit;
        this.local = local;
    }

    /**
     * Determines if this schema can be assigned from another schema.
     * <p>
     * This method checks whether the provided {@code otherSchema} is compatible with
     * this {@code TimestampSchema}. Compatibility typically means that the other schema has
     * the same schema type. Units are expected to be handled correctly during conversion.
     * </p>
     *
     * @param otherSchema The other {@link DataSchema} to be checked for compatibility.
     */
    @Override
    public Assignable isAssignableFrom(DataSchema otherSchema) {
        final var superAssignable = super.isAssignableFrom(otherSchema);
        if (superAssignable.isNotAssignable()) return superAssignable;
        if (!(otherSchema instanceof TimestampSchema)) return schemaMismatch(this, otherSchema);
        return Assignable.assignable();
    }

    /**
     * Checks if this schema type is equal to another schema. Equality checks are parameterized by flags passed in.
     *
     * @param obj   The other schema to compare.
     * @param flags The flags that indicate what to compare.
     */
    @Override
    public Equality equals(Object obj, EqualityFlags flags) {
        final var superEqual = super.equals(obj, flags);
        if (superEqual.isNotEqual()) return superEqual;

        final var that = (TimestampSchema) obj;

        // Compare unit
        if (!flags.isSet(IGNORE_TIME_SCHEMA_UNIT) && !Objects.equals(unit, that.unit))
            return fieldNotEqual("unit", this, unit, that, that.unit);

        return Equality.equal();
    }
}
