package io.axual.ksml.example;

/*-
 * ========================LICENSE_START=================================
 * KSML Examples Test
 * %%
 * Copyright (C) 2021 - 2025 Axual B.V.
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


import io.axual.ksml.operation.SensorData;
import io.axual.ksml.testutil.KSMLDriver;
import io.axual.ksml.testutil.KSMLTopic;
import io.axual.ksml.testutil.KSMLTopologyTest;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.streams.TestInputTopic;
import org.apache.kafka.streams.TestOutputTopic;
import org.apache.kafka.streams.TopologyTestDriver;
import org.junit.jupiter.api.DisplayName;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Runs {@code examples/21-example-punctuate.yaml} with data like the generator's, including STATE readings. */
class PunctuateExampleTest {

    @KSMLTopic(topic = "ksml_sensordata_avro", valueSerde = KSMLTopic.SerdeType.AVRO)
    TestInputTopic<String, GenericRecord> sensorDataInput;

    @KSMLTopic(topic = "ksml_sensordata_totals", valueSerde = KSMLTopic.SerdeType.DOUBLE)
    TestOutputTopic<String, Double> totals;

    @KSMLDriver
    TopologyTestDriver topologyTestDriver;

    private void reading(String sensor, SensorData.SensorType type, String value) {
        sensorDataInput.pipeInput(sensor, SensorData.builder()
                .name(sensor)
                .timestamp(System.currentTimeMillis())
                .value(value)
                .type(type)
                .unit("u")
                .color("blue")
                .city("Amsterdam")
                .owner("Alice")
                .build()
                .toRecord());
    }

    @KSMLTopologyTest(topologies = "examples/21-example-punctuate.yaml", schemaDirectory = "examples")
    @DisplayName("The punctuate example emits the totals per sensor every minute, skips STATE readings and clears the totals")
    void emitsTotals() {
        reading("sensor0", SensorData.SensorType.TEMPERATURE, "1.5");
        reading("sensor1", SensorData.SensorType.STATE, "on");
        reading("sensor0", SensorData.SensorType.TEMPERATURE, "2.5");
        reading("sensor2", SensorData.SensorType.HUMIDITY, "10");
        reading("sensor1", SensorData.SensorType.STATE, "off");
        assertTrue(totals.isEmpty());

        topologyTestDriver.advanceWallClockTime(Duration.ofMinutes(1));
        assertEquals(Map.of("sensor0", 4.0, "sensor2", 10.0), totals.readKeyValuesToMap());

        // The store was cleared, so an idle minute emits nothing
        topologyTestDriver.advanceWallClockTime(Duration.ofMinutes(1));
        assertTrue(totals.isEmpty());
    }
}
