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

import io.axual.ksml.definition.StateStoreDefinition;
import io.axual.ksml.definition.TopologyResource;
import io.axual.ksml.definition.parser.StateStoreDefinitionParser;
import io.axual.ksml.dsl.KSMLDSL;
import io.axual.ksml.generator.TopologyResources;
import io.axual.ksml.operation.StoreOperation;
import io.axual.ksml.operation.StoreOperationConfig;
import io.axual.ksml.parser.MultiSchemaParser;
import io.axual.ksml.parser.TopologyResourceParser;
import io.axual.ksml.store.StoreType;

import java.util.List;

public abstract class StoreOperationParser<T extends StoreOperation> extends OperationParser<T> {
    public StoreOperationParser(String namespace, String type) {
        super(namespace, type);
    }

    protected StoreOperationConfig storeOperationConfig(String name, List<String> storeNames, TopologyResource<StateStoreDefinition> store) {
        return new StoreOperationConfig(namespace(), name, storeNames, store);
    }

    protected MultiSchemaParser<TopologyResource<StateStoreDefinition>> storeField(boolean required, String doc, StoreType expectedStoreType) {
        final var stateStoreParser = new StateStoreDefinitionParser(expectedStoreType);
        return new TopologyResourceParser<>("state store", KSMLDSL.Operations.STORE_ATTRIBUTE, true, doc, TopologyResources::stateStore, stateStoreParser);
    }
}
