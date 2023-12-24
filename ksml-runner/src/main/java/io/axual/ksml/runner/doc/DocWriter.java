package io.axual.ksml.runner.doc;

import io.axual.ksml.data.schema.DataSchema;
import io.axual.ksml.data.schema.ListSchema;
import io.axual.ksml.data.schema.MapSchema;
import io.axual.ksml.data.schema.StructSchema;
import io.axual.ksml.data.schema.UnionSchema;

import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.function.Function;

public class DocWriter {
    private static final String FILENAME = "operations2.md";

    public void writeTo(StructSchema schema, String directory) {
        writeTo(directory + "/" + FILENAME, operationsDoc(schema));
    }

    private void writeTo(String filename, String contents) {

    }

    private void visitSchema(DataSchema schema, Consumer<StructSchema> visitor) {
        if (schema instanceof StructSchema structSchema) {
            visitor.accept(structSchema);
            for (final var field : structSchema.fields()) {
                visitSchema(field.schema(), visitor);
            }
        }
        if (schema instanceof ListSchema listSchema) {
            visitSchema(listSchema.valueSchema(), visitor);
        }
        if (schema instanceof MapSchema mapSchema) {
            visitSchema(mapSchema.valueSchema(), visitor);
        }
        if (schema instanceof UnionSchema unionSchema) {
            for (final var possibleSchema : unionSchema.possibleSchemas()) {
                visitSchema(possibleSchema, visitor);
            }
        }
    }

    private Map<String, StructSchema> collect(StructSchema schema, Function<String, Boolean> matcher) {
        final var result = new TreeMap<String, StructSchema>();
        visitSchema(schema, struct -> {
            if (matcher.apply(struct.name())) {
                result.put(struct.name(), struct);
            }
        });
        return result;
    }

    private String operationsDoc(StructSchema schema) {
        final var result = new StringBuilder();
        result.append("[<< Back to index](index.md)\n" +
                "\n" +
                "# Operations\n" +
                "\n" +
                "### Table of Contents\n" +
                "\n" +
                "1. [Introduction](#introduction)\n" +
                "1. [Operations](#transform-operations)\n");

        final var operations = collect(schema, name -> name.contains("Definition"));
        return result.toString();
    }
}
