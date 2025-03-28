package io.axual.ksml.data.notation.avro;

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

import io.axual.ksml.data.exception.DataException;
import io.axual.ksml.data.exception.SchemaException;
import io.axual.ksml.data.mapper.NativeDataObjectMapper;
import io.axual.ksml.data.notation.BaseNotation;
import io.axual.ksml.data.notation.Notation;
import io.axual.ksml.data.type.DataType;
import io.axual.ksml.data.type.MapType;
import io.axual.ksml.data.type.StructType;
import io.confluent.kafka.schemaregistry.avro.AvroSchema;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import io.confluent.kafka.schemaregistry.testutil.MockSchemaRegistry;
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import lombok.Getter;
import org.apache.avro.JsonSchemaFormatter;
import org.apache.avro.Schema;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An AVRO {@link Notation} which does not need a running schema registry.
 * Internally, the schema registry URL is set to <code>mock://mock-scope</code> which will cause an
 * instance of {@link MockSchemaRegistry} to be used.
 */
public class MockAvroNotation extends BaseNotation {
    public static final String NAME = "avro";
    private static final AvroDataObjectMapper AVRO_MAPPER = new AvroDataObjectMapper();
    private final Map<String, Object> configs = new HashMap<>();
    @Getter
    private final SyncMockSchemaRegistryClient mockSchemaRegistryClient = new SyncMockSchemaRegistryClient();

    public MockAvroNotation(Map<String, ?> configs) {
        super(NAME, ".avsc", new StructType(), null, new AvroSchemaParser());
        this.configs.putAll(configs);
        this.configs.put("schema.registry.url", "mock://mock-scope");
        this.configs.put(AbstractKafkaSchemaSerDeConfig.AUTO_REGISTER_SCHEMAS, true);
    }

    public Map<String, Object> getSchemaRegistryConfigs() {
        return Collections.unmodifiableMap(configs);
    }

    public void registerSubjectSchema(String subject, Schema schema) throws RestClientException, IOException {
        synchronized (mockSchemaRegistryClient) {
            var parsedSchema = mockSchemaRegistryClient.parseSchema(AvroSchema.TYPE, new JsonSchemaFormatter(true).format(schema), List.of());
            if (parsedSchema.isPresent()) {
                mockSchemaRegistryClient.register(subject, parsedSchema.get());
            } else {
                throw new SchemaException("Could not parse schema: " + schema.toString());
            }
        }
    }

    @Override
    public Serde<Object> serde(DataType type, boolean isKey) {
        if (type instanceof MapType) {
            var result = new AvroSerde();
            result.configure(configs, isKey);
            return result;
        }
        throw new DataException("AVRO serde not found for data type " + type);
    }

    private class AvroSerde implements Serde<Object> {
        private final Serializer<Object> serializer = new KafkaAvroSerializer(mockSchemaRegistryClient);
        private final Deserializer<Object> deserializer = new KafkaAvroDeserializer(mockSchemaRegistryClient);
        private final NativeDataObjectMapper nativeMapper = new NativeDataObjectMapper();

        private final Serializer<Object> wrapSerializer =
                (topic, data) -> {
                    try {
                        return serializer.serialize(topic, AVRO_MAPPER.fromDataObject(nativeMapper.toDataObject(data)));
                    } catch (Exception e) {
                        throw new DataException("Error serializing AVRO message to topic " + topic, e);
                    }
                };

        private final Deserializer<Object> wrapDeserializer =
                (topic, data) -> {
                    try {
                        return AVRO_MAPPER.toDataObject(deserializer.deserialize(topic, data));
                    } catch (Exception e) {
                        throw new DataException("Error deserializing AVRO message from topic " + topic, e);
                    }
                };

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
