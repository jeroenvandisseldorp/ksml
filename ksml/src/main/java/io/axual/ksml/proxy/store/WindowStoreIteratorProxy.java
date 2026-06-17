package io.axual.ksml.proxy.store;

/*-
 * ========================LICENSE_START=================================
 * KSML
 * %%
 * Copyright (C) 2021 - 2026 Axual B.V.
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

import io.stoatflow.core.state.WindowStoreIterator;
import org.graalvm.polyglot.HostAccess;
import org.jspecify.annotations.NonNull;

public class WindowStoreIteratorProxy<V> {
    private final WindowStoreIterator<V> delegate;

    public WindowStoreIteratorProxy(WindowStoreIterator<V> delegate) {
        this.delegate = delegate;
    }

    @HostAccess.Export
    public Object next() {
        if (!delegate.hasNext())
            return null;
        return ProxyUtil.toPython(delegate.next());
    }

    @HostAccess.Export
    public boolean hasNext() {
        return delegate.hasNext();
    }

    @HostAccess.Export
    public @NonNull Long peekNextKey() {
        return delegate.peekNextKey();
    }

    @HostAccess.Export
    public void close() {
        delegate.close();
    }
}
