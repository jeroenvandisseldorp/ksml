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

import io.stoatflow.core.state.TimestampedWindowStore;
import io.stoatflow.core.state.ValueAndTimestamp;
import org.graalvm.polyglot.HostAccess;
import org.jspecify.annotations.Nullable;

public class TimestampedWindowStoreProxy extends AbstractStateStoreProxy<TimestampedWindowStore<Object, Object>> {
    public TimestampedWindowStoreProxy(TimestampedWindowStore<Object, Object> delegate) {
        super(delegate);
    }

    @HostAccess.Export
    public long getWindowSizeMs() {
        return delegate.getWindowSizeMs();
    }

    @HostAccess.Export
    public long getRetentionMs() {
        return delegate.getRetentionMs();
    }

    @HostAccess.Export
    public void put(Object key, Object value, long timestamp, long windowStartTime) {
        delegate.put(key, ValueAndTimestamp.make(value, timestamp), windowStartTime);
    }

    @HostAccess.Export
    public void expireWindows(long currentWatermark) {
        delegate.expireWindows(currentWatermark);
    }

    @HostAccess.Export
    public long approximateNumEntries() {
        return delegate.approximateNumEntries();
    }

    @HostAccess.Export
    public @Nullable Object fetch(Object key, long windowStartTime) {
        return ProxyUtil.toPython(delegate.fetch(key, windowStartTime));
    }

    @HostAccess.Export
    public boolean containsKey(Object key, long windowStartTime) {
        return delegate.containsKey(key, windowStartTime);
    }

    @HostAccess.Export
    public Object backwardFetch(Object key, long timeFrom, long timeTo) {
        return ProxyUtil.toPython(delegate.backwardFetch(key, timeFrom, timeTo));
    }

    @HostAccess.Export
    public Object fetch(Object keyFrom, Object keyTo, long timeFrom, long timeTo) {
        return ProxyUtil.toPython(delegate.fetch(keyFrom,keyTo,timeFrom,timeTo));
    }

    @HostAccess.Export
    public Object backwardFetch(Object keyFrom, Object keyTo, long timeFrom, long timeTo) {
        return ProxyUtil.toPython(delegate.backwardFetch(keyFrom,keyTo,timeFrom,timeTo));
    }

    @HostAccess.Export
    public Object all() {
        return  ProxyUtil.toPython(delegate.all());
    }

    @HostAccess.Export
    public Object backwardAll() {
        return ProxyUtil.toPython(delegate.backwardAll());
    }

    @HostAccess.Export
    public Object fetchAll(long timeFrom, long timeTo) {
        return ProxyUtil.toPython(delegate.fetchAll(timeFrom,timeTo));
    }

    @HostAccess.Export
    public Object backwardFetchAll(long timeFrom, long timeTo) {
        return ProxyUtil.toPython(delegate.backwardFetchAll(timeFrom,timeTo));
    }
}
