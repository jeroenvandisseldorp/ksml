package io.axual.ksml.proxy.store;

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

import io.axual.ksml.data.mapper.DataObjectFlattener;
import io.axual.ksml.data.type.DataType;
import io.axual.ksml.data.type.WindowedType;
import io.stoatflow.core.state.SessionStore;
import io.stoatflow.core.topology.Windowed;
import org.graalvm.polyglot.HostAccess;

/**
 * A proxy for accessing Kafka Streams SessionStore in Python code. This proxy mediates between Python and Java data
 * types and delegates all operations to the underlying store.
 */
public class SessionStoreProxy extends AbstractStateStoreProxy<SessionStore<Object, Object>> {
    private static final DataObjectFlattener FLATTENER = new DataObjectFlattener();

    public SessionStoreProxy(SessionStore<Object, Object> delegate) {
        super(delegate);
    }

    // ==================== ReadOnlySessionStore methods ====================


    // ==================== SessionStore methods ====================

    @HostAccess.Export
    public void put(final Object sessionKey, final Object aggregate) {
        final var key = FLATTENER.toDataObject(NATIVE_MAPPER.fromPython(sessionKey));
        final var windowedKey = FLATTENER.unflatten(new WindowedType(DataType.UNKNOWN), key);
        if (windowedKey instanceof Windowed<?> windowed) {
            delegate.put((Windowed<Object>) windowed, NATIVE_MAPPER.fromPython(aggregate));
        }
    }

    @HostAccess.Export
    public void remove(final Object sessionKey) {
        final var key = FLATTENER.toDataObject(NATIVE_MAPPER.fromPython(sessionKey));
        final var windowedKey = FLATTENER.unflatten(new WindowedType(DataType.UNKNOWN), key);
        if (windowedKey instanceof Windowed<?> windowed) {
            delegate.remove((Windowed<Object>) windowed);
        }
    }
}
