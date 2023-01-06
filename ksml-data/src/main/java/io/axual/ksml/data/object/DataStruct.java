package io.axual.ksml.data.object;

/*-
 * ========================LICENSE_START=================================
 * KSML
 * %%
 * Copyright (C) 2021 Axual B.V.
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

import java.util.HashMap;

import io.axual.ksml.data.type.StructType;
import io.axual.ksml.exception.KSMLDataException;

public class DataStruct extends HashMap<String, DataObject> implements DataObject {
    public interface DataStructApplier<T> {
        void apply(T value) throws Exception;
    }

    private final transient StructType type;

    public DataStruct() {
        this(null);
    }

    public DataStruct(StructType type) {
        this.type = type;
    }

    public void putIfNotNull(String key, DataObject value) {
        if (value != null) put(key, value);
    }

    public void getIfPresent(String key, DataStructApplier<DataObject> applier) {
        getIfPresent(key, DataObject.class, applier);
    }

    public <T> void getIfPresent(String key, Class<T> clazz, DataStructApplier<T> applier) {
        var value = get(key);
        if (value != null && clazz.isAssignableFrom(value.getClass())) {
            try {
                applier.apply((T) value);
            } catch (Exception e) {
                throw new KSMLDataException("Exception thrown while getting and applying DataStruct value for key \"" + key + "\"", e);
            }
        }
    }

    public <T> T getAs(String key, Class<T> clazz) {
        return getAs(key, clazz, null);
    }

    public <T> T getAs(String key, Class<T> clazz, T defaultValue) {
        var value = get(key);
        if (value != null && clazz.isAssignableFrom(value.getClass())) {
            return (T) value;
        }
        return defaultValue;
    }

    @Override
    public StructType type() {
        return type;
    }

    @Override
    public boolean equals(Object other) {
        if (!super.equals(other)) return false;
        if (!(other instanceof DataStruct)) return false;
        return type.equals(((DataStruct) other).type);
    }

    @Override
    public int hashCode() {
        return type.hashCode() + super.hashCode() * 31;
    }
}