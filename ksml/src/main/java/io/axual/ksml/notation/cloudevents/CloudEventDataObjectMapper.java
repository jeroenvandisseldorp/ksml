package io.axual.ksml.notation.cloudevents;

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

import io.axual.ksml.data.mapper.DataObjectMapper;
import io.axual.ksml.data.object.DataBytes;
import io.axual.ksml.data.object.DataNull;
import io.axual.ksml.data.object.DataObject;
import io.axual.ksml.data.object.DataString;
import io.axual.ksml.data.object.DataStruct;
import io.axual.ksml.data.type.DataType;
import io.axual.ksml.execution.FatalError;
import io.axual.ksml.notation.Notation;
import io.axual.ksml.notation.NotationLibrary;
import io.axual.ksml.notation.binary.BinaryNotation;
import io.axual.ksml.notation.binary.NativeDataObjectMapper;
import io.axual.ksml.notation.json.JsonNotation;
import io.cloudevents.CloudEvent;
import io.cloudevents.CloudEventData;
import io.cloudevents.SpecVersion;
import io.cloudevents.core.data.BytesCloudEventData;
import io.cloudevents.core.data.PojoCloudEventData;
import io.cloudevents.core.v03.CloudEventV03;
import io.cloudevents.core.v1.CloudEventV1;

public class CloudEventDataObjectMapper implements DataObjectMapper<CloudEvent> {
    private static final NativeDataObjectMapper MAPPER = new NativeDataObjectMapper();
    private static final String DATA = "data";
    private static final String EXTENSIONS = "extensions";
    private final NotationLibrary notationLibrary;

    public CloudEventDataObjectMapper(NotationLibrary notationLibrary) {
        this.notationLibrary = notationLibrary;
    }

    @Override
    public DataObject toDataObject(DataType expected, CloudEvent value) {
        DataStruct result = new DataStruct();
        if (value.getSpecVersion() == SpecVersion.V03) {
            result.putIfNotNull(CloudEventV03.SPECVERSION, DataString.from(value.getSpecVersion()));
            result.putIfNotNull(CloudEventV03.ID, DataString.from(value.getId()));
            result.putIfNotNull(CloudEventV03.TYPE, DataString.from(value.getType()));
            result.putIfNotNull(CloudEventV03.SOURCE, DataString.from(value.getSource()));
            result.putIfNotNull(CloudEventV03.DATACONTENTTYPE, DataString.from(value.getDataContentType()));
            result.putIfNotNull(CloudEventV03.DATACONTENTENCODING, DataString.from(value.getAttribute(CloudEventV03.DATACONTENTENCODING)));
            result.putIfNotNull(CloudEventV03.SCHEMAURL, DataString.from(value.getDataSchema()));
            result.putIfNotNull(CloudEventV03.SUBJECT, DataString.from(value.getSubject()));
            result.putIfNotNull(CloudEventV03.TIME, DataString.from(value.getTime() != null ? value.getTime().toString() : null));
        } else if (value.getSpecVersion() == SpecVersion.V1) {
            result.putIfNotNull(CloudEventV1.SPECVERSION, DataString.from(value.getSpecVersion()));
            result.putIfNotNull(CloudEventV1.ID, DataString.from(value.getId()));
            result.putIfNotNull(CloudEventV1.TYPE, DataString.from(value.getType()));
            result.putIfNotNull(CloudEventV1.SOURCE, DataString.from(value.getSource()));
            result.putIfNotNull(CloudEventV1.DATACONTENTTYPE, DataString.from(value.getDataContentType()));
            result.putIfNotNull(CloudEventV1.DATASCHEMA, DataString.from(value.getDataSchema()));
            result.putIfNotNull(CloudEventV1.SUBJECT, DataString.from(value.getSubject()));
            result.putIfNotNull(CloudEventV1.TIME, DataString.from(value.getTime() != null ? value.getTime().toString() : null));
        } else {
            throw FatalError.dataError("Unknown CloudEvent spec version: " + value.getSpecVersion());
        }
        if (value.getData() instanceof PojoCloudEventData<?> data) {
            result.put(DATA, MAPPER.toDataObject(data.getValue()));
        } else if (value.getData() != null) {
            result.put(DATA, new DataBytes(value.getData().toBytes()));
        } else {
            result.put(DATA, DataNull.INSTANCE);
        }
        return result;
    }

    @Override
    public CloudEvent fromDataObject(DataObject value) {
//        if (!(value instanceof DataStruct cloudEvent)) {
//            throw FatalError.dataError("Can not convert data object to CloudEvent: " + (value != null ? value.type().toString() : "null"));
//        }
//        JsonFormat.CONTENT_TYPE
//        EventFormatProvider.getInstance().resolveFormat()
//        var version = cloudEvent.get(CloudEventV1.SPECVERSION);
//        var specVersion = SpecVersion.valueOf(version != null ? version.toString() : "unknown");
//        if (specVersion == SpecVersion.V03) {
//            var id = cloudEvent.get(CloudEventV03.ID) != null ? cloudEvent.get(CloudEventV03.ID).toString() : null;
//            var source = cloudEvent.get(CloudEventV03.SOURCE) != null ? URI.create(cloudEvent.get(CloudEventV03.ID).toString()) : null;
//            var type = cloudEvent.get(CloudEventV03.TYPE) != null ? cloudEvent.get(CloudEventV03.TYPE).toString() : null;
//            var time = cloudEvent.get(CloudEventV03.TYPE) != null ? OffsetDateTime.parse(cloudEvent.get(CloudEventV03.TIME).toString()) : null;
//            var schemaUrl = cloudEvent.get(CloudEventV03.SCHEMAURL) != null ? URI.create(cloudEvent.get(CloudEventV03.SCHEMAURL).toString()) : null;
//            var dataContentType = cloudEvent.get(CloudEventV03.DATACONTENTTYPE) != null ? cloudEvent.get(CloudEventV03.DATACONTENTTYPE).toString() : null;
//            var subject = cloudEvent.get(CloudEventV03.SUBJECT) != null ? cloudEvent.get(CloudEventV03.SUBJECT).toString() : null;
//            var data = getDataFrom(cloudEvent.get(DATA), dataContentType);
//            var extensions = new HashMap<String, Object>();
//            return new CloudEventV03(
//                    id,
//                    source,
//                    type,
//                    time,
//                    schemaUrl,
//                    dataContentType,
//                    subject,
//                    data,
//                    extensions);
//        }
//        if (specVersion == SpecVersion.V1) {
//            var id = cloudEvent.get(CloudEventV1.ID) != null ? cloudEvent.get(CloudEventV1.ID).toString() : null;
//            var source = cloudEvent.get(CloudEventV1.SOURCE) != null ? URI.create(cloudEvent.get(CloudEventV1.ID).toString()) : null;
//            var type = cloudEvent.get(CloudEventV1.TYPE) != null ? cloudEvent.get(CloudEventV1.TYPE).toString() : null;
//            var dataContentType = cloudEvent.get(CloudEventV1.DATACONTENTTYPE) != null ? cloudEvent.get(CloudEventV1.DATACONTENTTYPE).toString() : null;
//            var schemaUrl = cloudEvent.get(CloudEventV1.DATASCHEMA) != null ? URI.create(cloudEvent.get(CloudEventV1.DATASCHEMA).toString()) : null;
//            var subject = cloudEvent.get(CloudEventV1.SUBJECT) != null ? cloudEvent.get(CloudEventV1.SUBJECT).toString() : null;
//            var time = cloudEvent.get(CloudEventV1.TYPE) != null ? OffsetDateTime.parse(cloudEvent.get(CloudEventV1.TIME).toString()) : null;
//            var data = getDataFrom(cloudEvent.get(DATA), dataContentType);
//            var extensions = new HashMap<String, Object>();
//            return new CloudEventV1(
//                    id,
//                    source,
//                    type,
//                    dataContentType,
//                    schemaUrl,
//                    subject,
//                    time,
//                    data,
//                    extensions);
//        }
        var version='x';
        throw FatalError.dataError("Unknown CloudEvent spec version: " + version);
    }

    private CloudEventData getDataFrom(final DataObject data, String contentType) {
        if (data == null || data instanceof DataNull) return null;
        if (data instanceof DataBytes value) return BytesCloudEventData.wrap(value.value());
        Notation notation = getNotation(contentType);
        var serializer = notation.getSerde(data.type(), false).serializer();
        return PojoCloudEventData.wrap(data, o -> serializer.serialize("dummy", o));
    }

    private Notation getNotation(String contentType) {
        if ("application/json".equalsIgnoreCase(contentType)) return notationLibrary.get(JsonNotation.NOTATION_NAME);
        return notationLibrary.get(BinaryNotation.NOTATION_NAME);
    }
}
