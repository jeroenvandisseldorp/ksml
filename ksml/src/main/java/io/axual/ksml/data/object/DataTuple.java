package io.axual.ksml.data.object;

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

import io.axual.ksml.data.value.Tuple;
import io.axual.ksml.data.type.DataType;
import io.axual.ksml.data.type.TupleType;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode
public class DataTuple extends Tuple<DataObject> implements DataObject {
    private final DataType type;

    public DataTuple(DataObject... elements) {
        super(elements);
        this.type = new TupleType(convertElements(elements));
    }

    private static DataType[] convertElements(DataObject... elements) {
        DataType[] result = new DataType[elements.length];
        for (int index = 0; index < elements.length; index++) {
            result[index] = elements[index].type();
        }
        return result;
    }

    @Override
    public String toString() {
        return type.toString() + ": " + super.toString();
    }
}
