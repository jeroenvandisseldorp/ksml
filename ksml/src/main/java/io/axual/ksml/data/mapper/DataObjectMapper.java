package io.axual.ksml.data.mapper;

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

import io.axual.ksml.data.object.DataObject;
import io.axual.ksml.data.type.DataType;

public interface DataObjectMapper {
    String STRUCT_SCHEMA_FIELD = AttributeComparator.META_ATTRIBUTE_CHAR + "schema";
    String STRUCT_TYPE_FIELD = AttributeComparator.META_ATTRIBUTE_CHAR + "type";

    default DataObject toDataObject(Object value) {
        return toDataObject(null, value);
    }

    DataObject toDataObject(DataType expected, Object value);

    Object fromDataObject(DataObject value);
}