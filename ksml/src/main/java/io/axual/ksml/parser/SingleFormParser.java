package io.axual.ksml.parser;

import io.axual.ksml.data.schema.DataField;
import io.axual.ksml.data.schema.StructSchema;
import io.axual.ksml.data.schema.UnionSchema;
import io.axual.ksml.execution.FatalError;

import java.util.List;

public abstract class SingleFormParser<T> extends MultiFormParser<T> implements SingleSchemaParser<T> {
    public SingleFormParser(String namespace) {
        super(namespace);
    }

    @Override
    public StructSchema schema() {
        final var result = super.schema();
        if (result instanceof StructSchema structSchema) return structSchema;
        if (result instanceof UnionSchema unionSchema
                && unionSchema.possibleSchemas().length == 1
                && unionSchema.possibleSchemas()[0] instanceof StructSchema unionStruct) {
            return unionStruct;
        }
        throw FatalError.executionError("Could not determine the DataSchema for this SingleFormParser. This is a programming error.");
    }

    public List<DataField> fields() {
        return schema().fields();
    }
}
