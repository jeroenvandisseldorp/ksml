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


import io.axual.ksml.definition.StateStoreDefinition;
import lombok.Getter;

@Getter
public class StoreOperation extends BaseOperation {
    private final StateStoreDefinition store;

    public StoreOperation(StoreOperationConfig config) {
        super(config);
        this.store = config.store;
    }

    @Override
    public String toString() {
        return super.toString() + (store != null && store.name() != null ? " [storeName=\"" + store.name() + "\"]" : "");
    }
}
