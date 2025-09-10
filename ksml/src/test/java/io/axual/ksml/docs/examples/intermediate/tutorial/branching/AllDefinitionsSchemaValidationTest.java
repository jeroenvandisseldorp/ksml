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

import org.everit.json.schema.Schema;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import io.axual.ksml.generator.YAMLObjectMapper;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * JSON Schema validation test for all YAML definition files
 * in the branching tutorial examples folder.
 */
public class AllDefinitionsSchemaValidationTest {

    private static Schema ksmlSchema;

    /**
     * Discovers all YAML files in the branching tutorial directory
     */
    static Stream<Path> provideYamlFiles() throws URISyntaxException, IOException {
        // Get the directory containing the YAML files
        var testResourcesUri = AllDefinitionsSchemaValidationTest.class
            .getResource("/docs-examples/intermediate-tutorial/branching/").toURI();
        Path branchingDir = Paths.get(testResourcesUri);

        // Find all .yaml files
        return Files.walk(branchingDir)
            .filter(Files::isRegularFile)
            .filter(path -> path.toString().endsWith(".yaml"))
            .sorted(); // Sort for consistent test ordering
    }

    /**
     * Loads the KSML JSON schema once for all tests
     */
    private static Schema getKsmlSchema() throws Exception {
        if (ksmlSchema == null) {
            InputStream schemaStream = AllDefinitionsSchemaValidationTest.class
                .getResourceAsStream("/ksml-language-spec.json");
            
            assertNotNull(schemaStream, "Could not find KSML JSON schema file in test resources.");
            
            JSONObject rawSchema = new JSONObject(new JSONTokener(schemaStream));
            ksmlSchema = SchemaLoader.load(rawSchema);
            schemaStream.close();
        }
        return ksmlSchema;
    }

    @ParameterizedTest(name = "Validate {0} against KSML JSON Schema")
    @MethodSource("provideYamlFiles")
    void validateYamlFileAgainstSchema(Path yamlFile) throws Exception {
        System.out.println("Validating: " + yamlFile.getFileName());
        
        // Load the KSML schema
        Schema schema = getKsmlSchema();
        
        // Read and parse the YAML file
        String yamlContent = Files.readString(yamlFile);
        JsonNode jsonContent = YAMLObjectMapper.INSTANCE.readValue(yamlContent, JsonNode.class);
        
        // Convert Jackson JsonNode to org.json JSONObject for schema validation
        JSONObject jsonObject = new JSONObject(jsonContent.toString());
        
        try {
            // Validate against schema - throws ValidationException if invalid
            schema.validate(jsonObject);
            
            // If we reach here, validation passed
            assertTrue(true, "YAML file " + yamlFile.getFileName() + " is valid against KSML schema");
            
        } catch (Exception e) {
            fail("Schema validation failed for " + yamlFile.getFileName() + ": " + e.getMessage());
        }
    }
}