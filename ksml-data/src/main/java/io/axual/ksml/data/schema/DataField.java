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

import io.axual.ksml.data.exception.DataException;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.Objects;

/**
 * Represents a field in a data schema, containing metadata about the field such as its name,
 * schema definition, documentation, and constraints. This class provides functionality
 * to define and validate fields within a schema.
 */
@Getter
@EqualsAndHashCode
public class DataField {
    /**
     * Constant value representing the absence of a tag.
     */
    public static final int NO_TAG = -1;

    /**
     * Enum representing the sorting order of the field.
     * <ul>
     *     <li>ASCENDING: Field is sorted in ascending order.</li>
     *     <li>DESCENDING: Field is sorted in descending order.</li>
     *     <li>IGNORE: Sorting is ignored.</li>
     * </ul>
     */
    public enum Order {
        ASCENDING, DESCENDING, IGNORE
    }

    /**
     * The name of the field. May be null for anonymous fields.
     */
    private final String name;
    /**
     * The schema describing the type and structure of this field. This is a
     * mandatory attribute and cannot be null.
     */
    private final DataSchema schema;
    /**
     * An optional description or documentation string for the field. This allows
     * users to provide additional context or usage details for the field.
     */
    private final String doc;
    /**
     * A boolean indicating if the field is considered mandatory.
     * If {@code true}, the field must have a value.
     */
    private final boolean required;
    /**
     * A boolean indicating if the field is constant, meaning its value cannot
     * be changed after being initialized.
     */
    private final boolean constant;
    /**
     * The tag of the field in the schema. Defaults to {@link #NO_TAG} if
     * not specified.
     */
    private final int tag;
    /**
     * The default value assigned to the field, if any. Can be null if no
     * default is defined.
     */
    private final DataValue defaultValue;
    /**
     * The sorting order for the field, which is one of the {@link Order} enum values.
     * This determines how the field should be sorted when ordering is required.
     */
    private final Order order;

    /**
     * Constructs a new DataField with specified properties.
     *
     * @param name         The name of the field. Can be null for an anonymous field.
     * @param schema       The schema of the field. Cannot be null.
     * @param doc          The documentation string for the field. Can be null.
     * @param tag          The tag of the field in the schema.
     * @param required     Whether the field is required (mandatory).
     * @param constant     Whether the field is constant and unmodifiable.
     * @param defaultValue The default value of the field. Can be null.
     * @param order        The sorting order of the field (ascending, descending, or ignored).
     * @throws DataException if the field is marked as required and the default value is null.
     */
    public DataField(String name, DataSchema schema, String doc, int tag, boolean required, boolean constant, DataValue defaultValue, Order order) {
        Objects.requireNonNull(schema);
        this.name = name;
        this.schema = schema;
        this.doc = doc;
        // Tags are always set on members of unions, not on the unions themselves
        this.tag = schema instanceof UnionSchema ? NO_TAG : tag;
        this.required = required;
        this.constant = constant;
        this.defaultValue = defaultValue;
        this.order = order;
        if (required && defaultValue != null && defaultValue.value() == null) {
            throw new DataException("Default value for field \"" + name + "\" can not be null");
        }
    }

    /**
     * Constructs a new anonymous required DataField with no default value.
     *
     * @param schema The schema of the field. Cannot be null.
     */
    public DataField(DataSchema schema) {
        this(null, schema);
    }

    /**
     * Constructs a new optional DataField with the specified name and schema.
     *
     * @param name   The name of the field.
     * @param schema The schema of the field. Cannot be null.
     */
    public DataField(String name, DataSchema schema) {
        this(name, schema, null);
    }

    /**
     * Constructs a new optional DataField with the specified name, schema, and documentation.
     *
     * @param name   The name of the field.
     * @param schema The schema of the field. Cannot be null.
     * @param doc    The documentation string for the field. Can be null.
     */
    public DataField(String name, DataSchema schema, String doc) {
        this(name, schema, doc, NO_TAG);
    }

    /**
     * Constructs a new optional DataField with the specified name, schema, documentation, and tag.
     *
     * @param name   The name of the field.
     * @param schema The schema of the field. Cannot be null.
     * @param doc    The documentation string for the field. Can be null.
     * @param tag    The tag of the field in the schema.
     */
    public DataField(String name, DataSchema schema, String doc, int tag) {
        this(name, schema, doc, tag, true, false, null);
    }

    /**
     * Constructs a new DataField with the specified name, schema, documentation, tag, and required status.
     *
     * @param name     The name of the field.
     * @param schema   The schema of the field. Cannot be null.
     * @param doc      The documentation string for the field. Can be null.
     * @param tag      The tag of the field.
     * @param required Whether the field is required.
     */
    public DataField(String name, DataSchema schema, String doc, int tag, boolean required) {
        this(name, schema, doc, tag, required, false, null);
    }

    /**
     * Constructs a new DataField with the specified properties and assigns a default ascending order.
     *
     * @param name         The name of the field.
     * @param schema       The schema of the field. Cannot be null.
     * @param doc          The documentation string for the field. Can be null.
     * @param tag          The tag of the field.
     * @param required     Whether the field is required.
     * @param constant     Whether the field is constant and unmodifiable.
     * @param defaultValue The default value of the field. Can be null.
     */
    public DataField(String name, DataSchema schema, String doc, int tag, boolean required, boolean constant, DataValue defaultValue) {
        this(name, schema, doc, tag, required, constant, defaultValue, Order.ASCENDING);
    }

    /**
     * Checks if the field has documentation defined.
     *
     * @return true if documentation is defined and not empty, false otherwise.
     */
    public boolean hasDoc() {
        return doc != null && !doc.isEmpty();
    }

    /**
     * Checks whether the schema of this field is assignable from another field's schema.
     *
     * @param otherField The other field to compare against.
     * @return true if this field's schema can be assigned from the provided field's schema, false otherwise.
     */
    public boolean isAssignableFrom(DataField otherField) {
        return otherField != null && schema.isAssignableFrom(otherField.schema);
    }

    /**
     * Returns a string representation of the DataField, including its name, schema, tag, and required status.
     *
     * @return A string summarizing the field's details.
     */
    @Override
    public String toString() {
        return (name != null ? name : "<anonymous>") + ": " + schema + " (" + tag + (required ? "" : ", optional") + ")";
    }
}
