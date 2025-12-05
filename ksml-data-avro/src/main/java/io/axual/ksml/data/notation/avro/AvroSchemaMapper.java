package io.axual.ksml.data.notation.avro;

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

import io.axual.ksml.data.exception.SchemaException;
import io.axual.ksml.data.mapper.DataSchemaMapper;
import io.axual.ksml.data.mapper.DataTypeDataSchemaMapper;
import io.axual.ksml.data.object.DataNull;
import io.axual.ksml.data.object.DataObject;
import io.axual.ksml.data.schema.DataSchema;
import io.axual.ksml.data.schema.DecimalSchema;
import io.axual.ksml.data.schema.EnumSchema;
import io.axual.ksml.data.schema.FixedSchema;
import io.axual.ksml.data.schema.ListSchema;
import io.axual.ksml.data.schema.MapSchema;
import io.axual.ksml.data.schema.StructSchema;
import io.axual.ksml.data.schema.TimeSchema;
import io.axual.ksml.data.schema.TimestampSchema;
import io.axual.ksml.data.schema.UnionSchema;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.JsonProperties;
import org.apache.avro.LogicalTypes;
import org.apache.avro.Schema;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static io.axual.ksml.data.schema.DataSchemaConstants.NO_TAG;

/**
 * Maps between Avro Schema and KSML DataSchema.
 *
 * <p>Responsibilities:
 * - Avro Schema -> StructSchema/DataSchema including optionality detection via unions with null.
 * - DataSchema -> Avro Schema including default values and union construction for optional fields.</p>
 *
 * <p>See ksml-data/DEVELOPER_GUIDE.md sections on schema classes and mappers for background.</p>
 */
@Slf4j
public class AvroSchemaMapper implements DataSchemaMapper<Schema> {
    private static final AvroDataObjectMapper avroMapper = new AvroDataObjectMapper();
    private static final Schema AVRO_NULL_TYPE = Schema.create(Schema.Type.NULL);
    private static final DataTypeDataSchemaMapper TYPE_SCHEMA_MAPPER = new DataTypeDataSchemaMapper();

    /**
     * Convert an Avro record Schema into a KSML StructSchema.
     *
     * <p>The provided namespace and name parameters are ignored because the Avro Schema carries them already
     * and they take precedence.</p>
     *
     * @param namespace ignored; use schema.getNamespace()
     * @param name      ignored; use schema.getName()
     * @param schema    the Avro schema (record) to convert
     * @return a DataSchema with fields mapped from the Avro schema
     */
    @Override
    public DataSchema toDataSchema(String namespace, String name, Schema schema) {
        if (schema == null) return DataSchema.NULL_SCHEMA;
        return convertAvroSchemaToDataSchemaAndRequired(schema).schema();
    }

    private record SchemaAndRequired(DataSchema schema, boolean required) {
    }

    private SchemaAndRequired convertAvroSchemaToDataSchemaAndRequired(Schema schema) {
        // Returns a record with
        //   1. The DataSchema representation of schema parameter
        //   2. A boolean indicating whether the field is required
        return switch (schema.getType()) {
            case NULL -> new SchemaAndRequired(DataSchema.NULL_SCHEMA, false);
            case ARRAY -> new SchemaAndRequired(new ListSchema(toDataSchema(schema.getElementType())), true);
            case ENUM -> new SchemaAndRequired(
                    new EnumSchema(schema.getNamespace(), schema.getName(), schema.getDoc(), schema.getEnumSymbols().stream().map(EnumSchema.Symbol::new).toList(), schema.getEnumDefault() == null ? null : new EnumSchema.Symbol(schema.getEnumDefault())),
                    true);
            case FIXED ->
                    new SchemaAndRequired(new FixedSchema(schema.getNamespace(), schema.getName(), schema.getDoc(), schema.getFixedSize()), true);
            case MAP -> new SchemaAndRequired(new MapSchema(toDataSchema(schema.getValueType())), true);
            case RECORD ->
                    new SchemaAndRequired(new StructSchema(schema.getNamespace(), schema.getName(), schema.getDoc(), convertAvroFieldsToStructFields(schema.getFields()), false), true);
            case UNION -> convertAvroUnionMembersToToUnionSchemaAndRequired(schema.getTypes());
            default -> new SchemaAndRequired(convertSingularAvroTypeToDataSchema(schema), true);
        };
    }

    private DataSchema convertSingularAvroTypeToDataSchema(Schema schema) {
        if (schema.getLogicalType() != null) {
            return switch (schema.getLogicalType()) {
                case LogicalTypes.Decimal val -> new DecimalSchema(val.getScale(), val.getPrecision());
                case LogicalTypes.BigDecimal ignored -> new DecimalSchema(null, null);
                case LogicalTypes.Date ignored -> DataSchema.DATE_SCHEMA;
                case LogicalTypes.Duration ignored -> DataSchema.DURATION_SCHEMA;
                case LogicalTypes.LocalTimestampMicros ignored ->
                        new TimestampSchema(TimestampSchema.Unit.MICROSECONDS, true);
                case LogicalTypes.LocalTimestampMillis ignored ->
                        new TimestampSchema(TimestampSchema.Unit.MILLISECONDS, true);
                case LogicalTypes.LocalTimestampNanos ignored ->
                        new TimestampSchema(TimestampSchema.Unit.NANOSECONDS, true);
                case LogicalTypes.TimeMicros ignored -> new TimeSchema(TimeSchema.Unit.MICROSECONDS);
                case LogicalTypes.TimeMillis ignored -> new TimeSchema(TimeSchema.Unit.MILLISECONDS);
                case LogicalTypes.TimestampMicros ignored ->
                        new TimestampSchema(TimestampSchema.Unit.MICROSECONDS, false);
                case LogicalTypes.TimestampMillis ignored ->
                        new TimestampSchema(TimestampSchema.Unit.MILLISECONDS, false);
                case LogicalTypes.TimestampNanos ignored ->
                        new TimestampSchema(TimestampSchema.Unit.NANOSECONDS, false);
                case LogicalTypes.Uuid ignored -> DataSchema.UUID_SCHEMA;
                default -> throw new IllegalStateException("Unexpected logical type: " + schema.getLogicalType());
            };
        }
        return switch (schema.getType()) {
            case BOOLEAN -> DataSchema.BOOLEAN_SCHEMA;
            case INT -> DataSchema.INTEGER_SCHEMA;
            case LONG -> DataSchema.LONG_SCHEMA;
            case FLOAT -> DataSchema.FLOAT_SCHEMA;
            case DOUBLE -> DataSchema.DOUBLE_SCHEMA;
            case BYTES -> DataSchema.BYTES_SCHEMA;
            case FIXED ->
                    new FixedSchema(schema.getNamespace(), schema.getName(), schema.getDoc(), schema.getFixedSize());
            case STRING -> DataSchema.STRING_SCHEMA;
            default -> throw new SchemaException("Can not convert Avro schema to DataSchema: " + schema);
        };
    }

    private SchemaAndRequired convertAvroUnionMembersToToUnionSchemaAndRequired(List<Schema> members) {
        // Determine required based on the first member of the union: required when the first is not NULL
        final var firstIsNull = !members.isEmpty() && members.getFirst().getType() == Schema.Type.NULL;
        final var isRequired = !firstIsNull;

        // If the first schema is NULL, remove only that leading NULL from the member types; keep other NULLs intact
        final var memberSchemas = firstIsNull ? members.subList(1, members.size()) : members;

        if (memberSchemas.isEmpty()) {
            // Apparently only null was supplied, technically possible. Return optional null schema
            return new SchemaAndRequired(DataSchema.NULL_SCHEMA, isRequired);
        }

        if (memberSchemas.size() == 1) {
            // Only one member left, return that schema
            return new SchemaAndRequired(toDataSchema(memberSchemas.getFirst()), isRequired);
        }

        // Create a new union schema with the potentially adjusted member list
        return new SchemaAndRequired(new UnionSchema(convertAvroUnionMembersToUnionSchemaMembers(memberSchemas).toArray(UnionSchema.Member[]::new)), isRequired);
    }

    private List<UnionSchema.Member> convertAvroUnionMembersToUnionSchemaMembers(List<Schema> members) {
        final var result = new ArrayList<UnionSchema.Member>();
        for (var member : members) {
            result.add(new UnionSchema.Member(convertAvroSchemaToDataSchemaAndRequired(member).schema()));
        }
        return result;
    }

    private List<StructSchema.Field> convertAvroFieldsToStructFields(List<Schema.Field> fields) {
        if (fields == null) return new ArrayList<>();
        final var result = new ArrayList<StructSchema.Field>(fields.size());
        for (var field : fields) {
            final var schemaAndRequired = convertAvroSchemaToDataSchemaAndRequired(field.schema());
            final var convertedDefault = convertAvroDefaultValueToDataObject(schemaAndRequired.schema(), field.defaultVal());
            // TODO: think about how to model fixed values in AVRO and replace the "false" with logic
            result.add(new StructSchema.Field(field.name(), schemaAndRequired.schema(), field.doc(), NO_TAG, schemaAndRequired.required(), false, convertedDefault, convertAvroOrderToStructFieldOrder(field.order())));
        }
        return result;
    }

    private static StructSchema.Field.Order convertAvroOrderToStructFieldOrder(Schema.Field.Order order) {
        return switch (order) {
            case ASCENDING -> StructSchema.Field.Order.ASCENDING;
            case DESCENDING -> StructSchema.Field.Order.DESCENDING;
            default -> StructSchema.Field.Order.IGNORE;
        };
    }

    /**
     * Convert a KSML DataSchema into an Avro Schema.
     *
     * <p>Only StructSchema and other concrete schema types are supported; returns null for unsupported inputs.</p>
     *
     * @param schema the KSML schema to convert
     * @return the corresponding Avro Schema, or null when not representable
     */
    @Override
    public Schema fromDataSchema(DataSchema schema) {
        if (schema == null || schema == DataSchema.NULL_SCHEMA) return Schema.create(Schema.Type.NULL);
        if (schema == DataSchema.BOOLEAN_SCHEMA) return Schema.create(Schema.Type.BOOLEAN);
        if (schema == DataSchema.BYTE_SCHEMA || schema == DataSchema.SHORT_SCHEMA || schema == DataSchema.INTEGER_SCHEMA)
            return Schema.create(Schema.Type.INT);
        if (schema == DataSchema.LONG_SCHEMA) return Schema.create(Schema.Type.LONG);
        if (schema == DataSchema.FLOAT_SCHEMA) return Schema.create(Schema.Type.FLOAT);
        if (schema == DataSchema.DOUBLE_SCHEMA) return Schema.create(Schema.Type.DOUBLE);
        if (schema == DataSchema.BYTES_SCHEMA) return Schema.create(Schema.Type.BYTES);
        if (schema == DataSchema.STRING_SCHEMA) return Schema.create(Schema.Type.STRING);
        return switch (schema) {
            case EnumSchema es ->
                    Schema.createEnum(es.name(), es.doc(), es.namespace(), es.symbols().stream().map(EnumSchema.Symbol::name).toList(), es.defaultValue() == null ? null : es.defaultValue().name());
            case FixedSchema fs -> Schema.createFixed(fs.name(), fs.doc(), fs.namespace(), fs.size());
            case ListSchema ls -> Schema.createArray(fromDataSchema(ls.valueSchema()));
            case MapSchema ms -> Schema.createMap(fromDataSchema(ms.valueSchema()));
            case StructSchema ss ->
                    Schema.createRecord(ss.name(), ss.doc(), ss.namespace(), false, convertFieldsToAvroFields(ss.fields()));
            case UnionSchema us ->
                    Schema.createUnion(convertUnionMemberSchemasToAvro(Arrays.stream(us.members()).map(UnionSchema.Member::schema).toArray(DataSchema[]::new)));
            default -> throw new SchemaException("Can not convert schema to AVRO: " + schema);
        };
    }

    private record AvroSchemaAndDefaultValue(Schema schema, DataObject defaultValue) {
    }

    private AvroSchemaAndDefaultValue convertDataSchemaToAvroSchema(DataSchema schema, boolean required) {
        final var result = fromDataSchema(schema);

        // If the field is required, then return it
        if (required) return new AvroSchemaAndDefaultValue(result, null);

        // The field is not required, so we convert the schema to a UNION, with NULL as first possible type
        final var defaultValue = DataNull.INSTANCE;

        // If the schema is already of type UNION, then inject a NULL type at the start of array of types
        if (result.getType() == Schema.Type.UNION) {
            final var types = result.getTypes();
            // If NULL is already part of the UNION types, then return the UNION as is
            if (types.contains(AVRO_NULL_TYPE)) return new AvroSchemaAndDefaultValue(result, defaultValue);
            // Add NULL as a possible value type at the start of the array
            types.addFirst(AVRO_NULL_TYPE);
            return new AvroSchemaAndDefaultValue(Schema.createUnion(types), defaultValue);
        }

        // Create a UNION with NULL as its first type
        final var schemas = new ArrayList<Schema>();
        schemas.add(AVRO_NULL_TYPE);
        schemas.add(result);
        return new AvroSchemaAndDefaultValue(Schema.createUnion(schemas), defaultValue);
    }

    private Schema[] convertUnionMemberSchemasToAvro(DataSchema[] schemas) {
        final var result = new Schema[schemas.length];
        for (var index = 0; index < schemas.length; index++) {
            result[index] = convertDataSchemaToAvroSchema(schemas[index], true).schema();
        }
        return result;
    }

    private List<Schema.Field> convertFieldsToAvroFields(List<StructSchema.Field> fields) {
        if (fields == null) return Collections.emptyList();
        final var result = new ArrayList<Schema.Field>(fields.size());
        for (var field : fields) {
            result.add(convertStructFieldToAvroField(field));
        }
        return result;
    }

    private Schema.Field convertStructFieldToAvroField(StructSchema.Field field) {
        final var schemaAndDefault = convertDataSchemaToAvroSchema(field.schema(), field.required());
        final var defaultAvroValue = convertDataObjectToAvroDefaultValue(field.defaultValue());
        return new Schema.Field(field.name(), schemaAndDefault.schema(), field.doc(), defaultAvroValue, convertStructFieldOrderToAvroFieldOrder(field.order()));
    }

    private DataObject convertAvroDefaultValueToDataObject(DataSchema fieldSchema, Object defaultValue) {
        if (defaultValue == null) return null;
        if (defaultValue == JsonProperties.NULL_VALUE) return DataNull.INSTANCE;
        final var expectedType = TYPE_SCHEMA_MAPPER.fromDataSchema(fieldSchema);
        return avroMapper.toDataObject(expectedType, defaultValue);
    }

    private Object convertDataObjectToAvroDefaultValue(DataObject defaultValue) {
        if (defaultValue == null) return null;
        if (defaultValue == DataNull.INSTANCE) return Schema.Field.NULL_DEFAULT_VALUE;
        return avroMapper.fromDataObject(defaultValue);
    }

    private Schema.Field.Order convertStructFieldOrderToAvroFieldOrder(StructSchema.Field.Order order) {
        return switch (order) {
            case ASCENDING -> Schema.Field.Order.ASCENDING;
            case DESCENDING -> Schema.Field.Order.DESCENDING;
            default -> Schema.Field.Order.IGNORE;
        };
    }
}
