package io.axual.ksml.docs.examples.intermediate.tutorial.branching;

/*-
 * ========================LICENSE_START=================================
 * KSML
 * %%
 * Copyright (C) 2021 - 2024 Axual B.V.
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.kafka.streams.TestInputTopic;
import org.apache.kafka.streams.TestOutputTopic;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.axual.ksml.testutil.KSMLTest;
import io.axual.ksml.testutil.KSMLTestExtension;
import io.axual.ksml.testutil.KSMLTopic;
import lombok.extern.slf4j.Slf4j;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
@ExtendWith(KSMLTestExtension.class)
public class LocationRoutingTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @KSMLTopic(topic = "sensor_input")
    TestInputTopic<String, String> inputTopic;

    @KSMLTopic(topic = "datacenter_sensors")
    TestOutputTopic<String, String> datacenterOutput;

    @KSMLTopic(topic = "warehouse_sensors")
    TestOutputTopic<String, String> warehouseOutput;

    @KSMLTopic(topic = "office_sensors")
    TestOutputTopic<String, String> officeOutput;

    @KSMLTopic(topic = "unknown_sensors")
    TestOutputTopic<String, String> unknownOutput;

    @KSMLTest(topology = "docs-examples/intermediate-tutorial/branching/processor-location-routing.yaml")
    void testLocationBasedRouting() throws Exception {
        // Create test sensor data with different locations
        String datacenterSensor = createSensorDataJson("sensor1", "data_center", "TEMPERATURE", 25.5);
        String warehouseSensor = createSensorDataJson("sensor2", "warehouse", "HUMIDITY", 60.0);
        String officeSensor = createSensorDataJson("sensor3", "office", "PRESSURE", 1013.25);
        String unknownLocationSensor = createSensorDataJson("sensor4", "factory", "TEMPERATURE", 30.0);
        String anotherDatacenterSensor = createSensorDataJson("sensor5", "data_center", "HUMIDITY", 45.0);

        // Send JSON strings to input topic
        inputTopic.pipeInput("sensor1", datacenterSensor);
        inputTopic.pipeInput("sensor2", warehouseSensor);
        inputTopic.pipeInput("sensor3", officeSensor);
        inputTopic.pipeInput("sensor4", unknownLocationSensor);
        inputTopic.pipeInput("sensor5", anotherDatacenterSensor);

        // Verify routing to datacenter topic
        List<String> datacenterRecords = datacenterOutput.readValuesToList();
        assertEquals(2, datacenterRecords.size(), "Should route 2 data_center sensors to datacenter topic");
        JsonNode datacenter1 = objectMapper.readTree(datacenterRecords.get(0));
        JsonNode datacenter2 = objectMapper.readTree(datacenterRecords.get(1));
        assertEquals("data_center", datacenter1.get("location").asText(), "First datacenter record should have data_center location");
        assertEquals("data_center", datacenter2.get("location").asText(), "Second datacenter record should have data_center location");

        // Verify routing to warehouse topic
        List<String> warehouseRecords = warehouseOutput.readValuesToList();
        assertEquals(1, warehouseRecords.size(), "Should route 1 warehouse sensor to warehouse topic");
        JsonNode warehouse = objectMapper.readTree(warehouseRecords.getFirst());
        assertEquals("warehouse", warehouse.get("location").asText());
        assertEquals("sensor2", warehouse.get("name").asText());

        // Verify routing to office topic
        List<String> officeRecords = officeOutput.readValuesToList();
        assertEquals(1, officeRecords.size(), "Should route 1 office sensor to office topic");
        JsonNode office = objectMapper.readTree(officeRecords.getFirst());
        assertEquals("office", office.get("location").asText());
        assertEquals("sensor3", office.get("name").asText());

        // Verify routing to unknown topic (default branch)
        List<String> unknownRecords = unknownOutput.readValuesToList();
        assertEquals(1, unknownRecords.size(), "Should route 1 unknown location sensor to unknown topic");
        JsonNode unknown = objectMapper.readTree(unknownRecords.getFirst());
        assertEquals("factory", unknown.get("location").asText());
        assertEquals("sensor4", unknown.get("name").asText());
    }

    private String createSensorDataJson(String name, String location, String type, double value) throws Exception {
        Map<String, Object> sensorData = new HashMap<>();
        sensorData.put("name", name);
        sensorData.put("location", location);
        sensorData.put("type", type);
        sensorData.put("value", value);
        sensorData.put("timestamp", System.currentTimeMillis());
        sensorData.put("unit", "C");
        return objectMapper.writeValueAsString(sensorData);
    }
}