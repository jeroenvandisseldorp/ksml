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

import io.axual.ksml.data.type.DataType;
import io.axual.ksml.data.type.MapType;
import io.axual.ksml.data.type.StructType;
import io.axual.ksml.execution.FatalError;
import io.axual.ksml.notation.Notation;
import io.axual.ksml.notation.NotationLibrary;
import io.axual.ksml.util.DataUtil;
import io.cloudevents.CloudEvent;
import io.cloudevents.kafka.CloudEventDeserializer;
import io.cloudevents.kafka.CloudEventSerializer;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;

import java.util.HashMap;
import java.util.Map;

public class CloudEventNotation implements Notation {
    public static final String NOTATION_NAME = "CLOUDEVENT";
    public static final DataType DEFAULT_TYPE = new StructType();
    private final CloudEventDataObjectMapper mapper;
    private final Map<String, Object> configs = new HashMap<>();

    public CloudEventNotation(Map<String, Object> configs, NotationLibrary notationLibrary) {
        this.configs.putAll(configs);
        mapper = new CloudEventDataObjectMapper(notationLibrary);
    }

    @Override
    public String name() {
        return NOTATION_NAME;
    }

    @Override
    public Serde<Object> getSerde(DataType type, boolean isKey) {
        if (type instanceof MapType) {
            var result = new CloudEventSerde();
            result.configure(configs, isKey);
            return result;
        }
        throw FatalError.dataError("Avro serde not found for data type " + type);
    }

    private class CloudEventSerde implements Serde<Object> {
        private final Serializer<CloudEvent> serializer = new CloudEventSerializer();
        private final Deserializer<CloudEvent> deserializer = new CloudEventDeserializer();

        private final Serializer<Object> wrapSerializer =
                (topic, data) -> serializer.serialize(topic, mapper.fromDataObject(DataUtil.asDataObject(data)));

        private final Deserializer<Object> wrapDeserializer =
                (topic, data) -> mapper.toDataObject(deserializer.deserialize(topic, data));

        @Override
        public void configure(Map<String, ?> configs, boolean isKey) {
            serializer.configure(configs, isKey);
            deserializer.configure(configs, isKey);
        }

        @Override
        public Serializer<Object> serializer() {
            return wrapSerializer;
        }

        @Override
        public Deserializer<Object> deserializer() {
            return wrapDeserializer;
        }
    }
}
