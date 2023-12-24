package io.axual.ksml.definition.parser;

import io.axual.ksml.definition.FunctionDefinition;

import java.util.List;

public record ForEachOperationDefinition(FunctionDefinition forEachAction, List<String> storeNames) {
}
