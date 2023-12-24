package io.axual.ksml.parser;

import io.axual.ksml.data.schema.DataField;
import io.axual.ksml.data.schema.DataSchema;
import io.axual.ksml.data.schema.StructSchema;
import io.axual.ksml.data.schema.UnionSchema;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

import static io.axual.ksml.parser.MultiFormParser.SCHEMA_NAMESPACE;

@Slf4j
public class Utils {
    public static List<StructSchema> possibleSchemas(DataSchema schema) {
        final var result = new ArrayList<StructSchema>();
        if (schema instanceof UnionSchema unionSchema) {
            for (final var possibleSchema : unionSchema.possibleSchemas()) {
                if (possibleSchema instanceof StructSchema structSchema) {
                    result.add(structSchema);
                } else {
                    log.warn("Could not convert union subtype: " + possibleSchema.type());
                }
            }
        } else {
            if (schema instanceof StructSchema structSchema) {
                result.add(structSchema);
            } else {
                log.warn("Could not convert subtype: " + schema.type());
            }
        }
        return result;
    }

    public static void addToSchemas(List<StructSchema> schemas, String name, String doc, MultiSchemaParser<?> subParser) {
        if (schemas.isEmpty()) {
            schemas.addAll(subParser.schemas());
        } else {
            // Here we combine the schemas from the subParser with the known list of schemas. Effectively this
            // multiplies the number of schemas. So if we had 3 schemas in our schemas variable already, and the
            // parser also parses 4 schema alternatives, then we end up with 12 new schemas.
            final var newSchemas = new ArrayList<StructSchema>();
            for (final var schema : schemas) {
                final var subSchemas = subParser.schemas();
                final var usePostfix = subSchemas.size() > 1;
                for (final var subSchema : subParser.schemas()) {
                    final var fields = new ArrayList<DataField>();
                    fields.addAll(schema.fields());
                    fields.addAll(subSchema.fields());
                    final var newName = usePostfix ? name + "With" + subSchema.name() : name;
                    final var newSchema = new StructSchema(SCHEMA_NAMESPACE, newName, doc, fields);
                    newSchemas.add(newSchema);
                }
            }

            // Clear the old schema array and replace with new schemas
            schemas.clear();
            schemas.addAll(newSchemas);
        }
    }
}
