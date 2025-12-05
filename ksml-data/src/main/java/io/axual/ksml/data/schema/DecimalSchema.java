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

import static io.axual.ksml.data.schema.DataSchemaFlag.IGNORE_DECIMAL_SCHEMA_PRECISION;
import static io.axual.ksml.data.schema.DataSchemaFlag.IGNORE_DECIMAL_SCHEMA_SCALE;
import static io.axual.ksml.data.util.AssignableUtil.schemaMismatch;
import static io.axual.ksml.data.util.EqualUtil.fieldNotEqual;

/**
 * A schema representation for decimal values in the KSML framework.
 * <p>
 * The {@code DecimalSchema} class extends the {@link DataSchema} and is used to define
 * schemas for decimal values. It provides {@code scale} and {@code precision} attributes
 * that specify the scale and precision of the decimal value.
 * </p>
 * <p>
 * This schema is useful for scenarios where a decimal value must always conform to a
 * specific scale and precision, such as for currencies.
 * </p>
 */
@Getter
@EqualsAndHashCode
public class DecimalSchema extends DataSchema {
    /**
     * The scale of the decimal value represented by this schema.
     * <p>
     * This value is a positive integer, and it must be explicitly defined at
     * the time of schema creation. If it is not defined, or zero, then
     * infinite scale is assumed.
     * </p>
     */
    private final Integer scale;
    /**
     * The precision of the decimal value represented by this schema.
     * <p>
     * This value is a positive integer, and it must be explicitly defined at
     * the time of schema creation. If it is not defined, then infinite precision
     * is assumed.
     * </p>
     */
    private final Integer precision;

    /**
     * Constructs a {@code DecimalSchema} with the given namespace, name, documentation,
     * and size.
     *
     * @param scale     The scale of the decimal type, or null if none
     * @param precision The precision of the decimal type, or null if none
     */
    public DecimalSchema(Integer scale, Integer precision) {
        super(DataSchemaConstants.DECIMAL_TYPE);
        if (scale != null && scale < 0) {
            throw new IllegalArgumentException("Scale of DECIMAL type can not be smaller than zero. Found " + scale);
        }
        if (precision != null && precision < 0) {
            throw new IllegalArgumentException("Precision of DECIMAL type can not be smaller than zero. Found " + precision);
        }
        this.scale = scale;
        this.precision = precision;
    }

    /**
     * Determines if this schema can be assigned from another schema.
     * <p>
     * This method checks whether the provided {@code otherSchema} is compatible
     * with this {@code DecimalSchema}. Compatibility typically means that the other schema
     * has the same scale and precision.
     * </p>
     *
     * @param otherSchema The other {@link DataSchema} to be checked for compatibility.
     */
    @Override
    public Assignable isAssignableFrom(DataSchema otherSchema) {
        final var superAssignable = super.isAssignableFrom(otherSchema);
        if (superAssignable.isNotAssignable()) return superAssignable;
        if (!(otherSchema instanceof DecimalSchema otherDecimalSchema)) return schemaMismatch(this, otherSchema);
        final var scaleOK = scale == null || (otherDecimalSchema.scale != null && scale >= otherDecimalSchema.scale);
        if (!scaleOK)
            return Assignable.notAssignable("Scale of decimal schema (" + scale + ") is smaller than the other decimal schema's scale (" + (otherDecimalSchema.scale != null ? otherDecimalSchema.scale : "infinite") + ")");
        final var precisionOK = precision == null || (otherDecimalSchema.precision != null && precision >= otherDecimalSchema.precision);
        if (!precisionOK)
            return Assignable.notAssignable("Precision of decimal schema (" + precision + ") is smaller than the other decimal schema's precision (" + (otherDecimalSchema.precision != null ? otherDecimalSchema.precision : "infinite") + ")");
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

        final var that = (DecimalSchema) obj;

        // Compare precision
        if (!flags.isSet(IGNORE_DECIMAL_SCHEMA_PRECISION) && !Objects.equals(precision, that.precision))
            return fieldNotEqual("precision", this, precision, that, that.precision);

        // Compare scale
        if (!flags.isSet(IGNORE_DECIMAL_SCHEMA_SCALE) && !Objects.equals(scale, that.scale))
            return fieldNotEqual("scale", this, scale, that, that.scale);

        return Equality.equal();
    }
}
