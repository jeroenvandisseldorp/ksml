package io.axual.ksml.notation.xml;

/*-
 * ========================LICENSE_START=================================
 * KSML
 * %%
 * Copyright (C) 2021 - 2022 Axual B.V.
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

import io.axual.ksml.data.mapper.DataObjectMapper;
import io.axual.ksml.data.mapper.NativeDataObjectMapper;
import io.axual.ksml.data.object.DataObject;
import io.axual.ksml.data.type.DataType;
import io.axual.ksml.execution.FatalError;

public class XmlDataObjectMapper implements DataObjectMapper {
    private static final XmlStringMapper xmlMapper = new XmlStringMapper();
    private static final NativeDataObjectMapper nativeMapper = new NativeDataObjectMapper();

    @Override
    public DataObject toDataObject(DataType expected, Object value) {
        if (value instanceof String str) {
            var object = xmlMapper.fromString(str);
            return nativeMapper.toDataObject(object);
        }
        throw FatalError.dataError("Can not convert value to XML String: " + (value != null ? value.getClass().getSimpleName() : "null"));
    }

    @Override
    public String fromDataObject(DataObject value) {
        var object = nativeMapper.fromDataObject(value);
        return xmlMapper.toString(object);
    }
}
