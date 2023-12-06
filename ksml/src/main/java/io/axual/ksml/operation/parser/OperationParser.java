package io.axual.ksml.operation.parser;

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

import io.axual.ksml.dsl.KSMLDSL;
import io.axual.ksml.generator.TopologyResources;
import io.axual.ksml.operation.OperationConfig;
import io.axual.ksml.operation.StreamOperation;
import io.axual.ksml.parser.ContextAwareParser;
import io.axual.ksml.parser.ListParser;
import io.axual.ksml.parser.StringValueParser;
import io.axual.ksml.parser.YamlNode;

public abstract class OperationParser<T extends StreamOperation> extends ContextAwareParser<T> {
    private static final String[] TEMPLATE = new String[0];
    protected final String name;

    public OperationParser(String prefix, String name, TopologyResources resources) {
        super(prefix, resources);
        this.name = name;
    }

    protected OperationConfig operationConfig(YamlNode node) {
        return operationConfig(node, null);
    }

    protected OperationConfig operationConfig(YamlNode node, String typeName) {
        final var storeNames = new ListParser<>("state store names", new StringValueParser()).parse(node.get(KSMLDSL.Operations.STORE_NAMES_ATTRIBUTE));
        return new OperationConfig(
                prefix,
                name != null ? name : determineName(typeName),
                storeNames != null ? storeNames.toArray(TEMPLATE) : null);
    }
}
