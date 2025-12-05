package io.axual.ksml.operation;

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

import io.axual.ksml.store.StateStoreDefinition;

public class DualStoreOperation<DEF extends DualStoreOperationDefinition> extends BaseOperation<DEF> {
    public DualStoreOperation(DEF definition) {
        super(definition);
    }

    public StateStoreDefinition thisStore() {
        return def.dualStoreOperationConfig().thisStore();
    }

    public StateStoreDefinition otherStore() {
        return def.dualStoreOperationConfig().otherStore();
    }

    @Override
    public String toString() {
        final var thisStore = def.dualStoreOperationConfig().thisStore();
        final var otherStore = def.dualStoreOperationConfig().otherStore();
        return super.toString()
                + (thisStore != null && thisStore.name() != null ? " [thisStore=\"" + thisStore.name() + "\"]" : "")
                + (otherStore != null && otherStore.name() != null ? " [otherStore=\"" + otherStore.name() + "\"]" : "");
    }
}
