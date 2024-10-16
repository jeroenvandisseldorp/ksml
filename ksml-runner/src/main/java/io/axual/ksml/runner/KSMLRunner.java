package io.axual.ksml.runner;

/*-
 * ========================LICENSE_START=================================
 * KSML Runner
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


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.utils.Utils;
import org.apache.kafka.streams.KeyQueryMetadata;
import org.apache.kafka.streams.StoreQueryParameters;
import org.apache.kafka.streams.StreamsMetadata;
import org.apache.kafka.streams.state.HostInfo;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import io.axual.ksml.client.serde.ResolvingDeserializer;
import io.axual.ksml.client.serde.ResolvingSerializer;
import io.axual.ksml.data.mapper.DataObjectFlattener;
import io.axual.ksml.data.notation.NotationLibrary;
import io.axual.ksml.data.notation.avro.AvroNotation;
import io.axual.ksml.data.notation.avro.AvroSchemaLoader;
import io.axual.ksml.data.notation.binary.BinaryNotation;
import io.axual.ksml.data.notation.csv.CsvDataObjectConverter;
import io.axual.ksml.data.notation.csv.CsvNotation;
import io.axual.ksml.data.notation.csv.CsvSchemaLoader;
import io.axual.ksml.data.notation.json.JsonDataObjectConverter;
import io.axual.ksml.data.notation.json.JsonNotation;
import io.axual.ksml.data.notation.json.JsonSchemaLoader;
import io.axual.ksml.data.notation.json.JsonSchemaMapper;
import io.axual.ksml.data.notation.soap.SOAPDataObjectConverter;
import io.axual.ksml.data.notation.soap.SOAPNotation;
import io.axual.ksml.data.notation.xml.XmlDataObjectConverter;
import io.axual.ksml.data.notation.xml.XmlNotation;
import io.axual.ksml.data.notation.xml.XmlSchemaLoader;
import io.axual.ksml.data.parser.ParseNode;
import io.axual.ksml.data.schema.SchemaLibrary;
import io.axual.ksml.definition.parser.TopologyDefinitionParser;
import io.axual.ksml.execution.ErrorHandler;
import io.axual.ksml.execution.ExecutionContext;
import io.axual.ksml.execution.FatalError;
import io.axual.ksml.generator.TopologyDefinition;
import io.axual.ksml.rest.server.ComponentState;
import io.axual.ksml.rest.server.KsmlQuerier;
import io.axual.ksml.rest.server.RestServer;
import io.axual.ksml.runner.backend.KafkaProducerRunner;
import io.axual.ksml.runner.backend.KafkaStreamsRunner;
import io.axual.ksml.runner.backend.Runner;
import io.axual.ksml.runner.config.KSMLErrorHandlingConfig;
import io.axual.ksml.runner.config.KSMLRunnerConfig;
import io.axual.ksml.runner.exception.ConfigException;
import io.axual.ksml.runner.prometheus.PrometheusExport;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class KSMLRunner {
    private static final String DEFAULT_CONFIG_FILE_SHORT = "ksml-runner.yaml";
    private static final String WRITE_KSML_SCHEMA_ARGUMENT = "--schema";

    public static void main(String[] args) {
        try {
            // Load name and version from manifest
            var ksmlTitle = determineTitle();

            // Check if we need to output the schema and then exit
            checkForSchemaOutput(args);

            log.info("Starting {}", ksmlTitle);

            // Begin loading config file
            final var configFile = new File(args.length == 0 ? DEFAULT_CONFIG_FILE_SHORT : args[0]);
            if (!configFile.exists()) {
                log.error("Configuration file '{}' not found", configFile);
                System.exit(1);
            }

            final var config = readConfiguration(configFile);
            final var ksmlConfig = config.getKsmlConfig();
            log.info("Using directories: config: {}, schema: {}, storage: {}", ksmlConfig.getConfigDirectory(), ksmlConfig.getSchemaDirectory(), ksmlConfig.getStorageDirectory());
            final var definitions = ksmlConfig.getDefinitions();
            if (definitions == null || definitions.isEmpty()) {
                throw new ConfigException("definitions", definitions, "No KSML definitions found in configuration");
            }

            KsmlInfo.registerKsmlAppInfo(config.getApplicationId());

            // Start the appserver if needed
            final var appServer = ksmlConfig.getApplicationServerConfig();
            RestServer restServer = null;
            // Start rest server to provide service endpoints
            if (appServer.isEnabled()) {
                HostInfo hostInfo = new HostInfo(appServer.getHost(), appServer.getPort());
                restServer = new RestServer(hostInfo);
                restServer.start();
            }

            // Set up the notation library with all known notations and type override classes
            final var nativeMapper = new DataObjectFlattener(true);
            final var jsonNotation = new JsonNotation(nativeMapper);
            NotationLibrary.register(AvroNotation.NOTATION_NAME, new AvroNotation(nativeMapper, config.getKafkaConfig()), null);
            NotationLibrary.register(BinaryNotation.NOTATION_NAME, new BinaryNotation(nativeMapper, jsonNotation::serde), null);
            NotationLibrary.register(CsvNotation.NOTATION_NAME, new CsvNotation(nativeMapper), new CsvDataObjectConverter());
            NotationLibrary.register(JsonNotation.NOTATION_NAME, jsonNotation, new JsonDataObjectConverter());
            NotationLibrary.register(SOAPNotation.NOTATION_NAME, new SOAPNotation(nativeMapper), new SOAPDataObjectConverter());
            NotationLibrary.register(XmlNotation.NOTATION_NAME, new XmlNotation(nativeMapper), new XmlDataObjectConverter());

            // Register schema loaders
            final var schemaDirectory = ksmlConfig.getSchemaDirectory();
            SchemaLibrary.registerLoader(AvroNotation.NOTATION_NAME, new AvroSchemaLoader(schemaDirectory));
            SchemaLibrary.registerLoader(CsvNotation.NOTATION_NAME, new CsvSchemaLoader(schemaDirectory));
            SchemaLibrary.registerLoader(JsonNotation.NOTATION_NAME, new JsonSchemaLoader(schemaDirectory));
            SchemaLibrary.registerLoader(XmlNotation.NOTATION_NAME, new XmlSchemaLoader(schemaDirectory));

            final var errorHandling = ksmlConfig.getErrorHandlingConfig();
            if (errorHandling != null) {
                ExecutionContext.INSTANCE.setConsumeHandler(getErrorHandler(errorHandling.getConsumerErrorHandlingConfig()));
                ExecutionContext.INSTANCE.setProduceHandler(getErrorHandler(errorHandling.getProducerErrorHandlingConfig()));
                ExecutionContext.INSTANCE.setProcessHandler(getErrorHandler(errorHandling.getProcessErrorHandlingConfig()));
            }
            ExecutionContext.INSTANCE.serdeWrapper(
                    serde -> new Serdes.WrapperSerde<>(
                            new ResolvingSerializer<>(serde.serializer(), config.getKafkaConfig()),
                            new ResolvingDeserializer<>(serde.deserializer(), config.getKafkaConfig())));

            final Map<String, TopologyDefinition> producerSpecs = new HashMap<>();
            final Map<String, TopologyDefinition> pipelineSpecs = new HashMap<>();
            definitions.forEach((name, definition) -> {
                final var parser = new TopologyDefinitionParser(name);
                final var specification = parser.parse(ParseNode.fromRoot(definition, name));
                if (!specification.producers().isEmpty()) producerSpecs.put(name, specification);
                if (!specification.pipelines().isEmpty()) pipelineSpecs.put(name, specification);
            });


            if (!ksmlConfig.isEnableProducers() && !producerSpecs.isEmpty()) {
                log.warn("Producers are disabled for this runner. The supplied producer specifications will be ignored.");
                producerSpecs.clear();
            }
            if (!ksmlConfig.isEnablePipelines() && !pipelineSpecs.isEmpty()) {
                log.warn("Pipelines are disabled for this runner. The supplied pipeline specifications will be ignored.");
                pipelineSpecs.clear();
            }

            final var producer = producerSpecs.isEmpty() ? null : new KafkaProducerRunner(KafkaProducerRunner.Config.builder()
                    .definitions(producerSpecs)
                    .kafkaConfig(config.getKafkaConfig())
                    .build());
            final var streams = pipelineSpecs.isEmpty() ? null : new KafkaStreamsRunner(KafkaStreamsRunner.Config.builder()
                    .storageDirectory(ksmlConfig.getStorageDirectory())
                    .appServer(ksmlConfig.getApplicationServerConfig())
                    .definitions(pipelineSpecs)
                    .kafkaConfig(config.getKafkaConfig())
                    .build());

            if (producer != null || streams != null) {
                var shutdownHook = new Thread(() -> {
                    try {
                        log.debug("In KSML shutdown hook");
                        if (producer != null) producer.stop();
                        if (streams != null) streams.stop();
                    } catch (Exception e) {
                        log.error("Could not properly shut down KSML", e);
                    }
                });

                Runtime.getRuntime().addShutdownHook(shutdownHook);

                try (var prometheusExport = new PrometheusExport(config.getKsmlConfig().getPrometheusConfig())) {
                    prometheusExport.start();
                    final var executorService = Executors.newFixedThreadPool(2);

                    final var producerFuture = producer == null ? CompletableFuture.completedFuture(null) : CompletableFuture.runAsync(producer, executorService);
                    final var streamsFuture = streams == null ? CompletableFuture.completedFuture(null) : CompletableFuture.runAsync(streams, executorService);

                    try {
                        // Allow the runner(s) to start
                        Utils.sleep(2000);

                        if (restServer != null) {
                            // Run with the REST server
                            restServer.initGlobalQuerier(getQuerier(streams, producer));
                        }

                        producerFuture.whenComplete((result, exc) -> {
                            if (exc != null) {
                                log.info("Producer failed", exc);
                                // Exception, always stop streams too
                                if (streams != null) {
                                    streams.stop();
                                }
                            }
                        });
                        streamsFuture.whenComplete((result, exc) -> {
                            if (exc != null) {
                                log.info("Stream processing failed", exc);
                                // Exception, always stop producer too
                                if (producer != null) {
                                    producer.stop();
                                }
                            }
                        });

                        final var allFutures = CompletableFuture.allOf(producerFuture, streamsFuture);
                        // wait for all futures to finish
                        allFutures.join();

                        closeExecutorService(executorService);
                    } finally {
                        if (restServer != null) restServer.close();
                    }
                } finally {
                    Runtime.getRuntime().removeShutdownHook(shutdownHook);
                }
            }
        } catch (Throwable t) {
            log.error("KSML Stopping because of unhandled exception");
            throw FatalError.reportAndExit(t);
        }
        // Explicit exit, need to find out which threads are actually stopping us
        System.exit(0);
    }

    private static void closeExecutorService(final ExecutorService executorService) throws ExecutionException {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(2000, TimeUnit.MILLISECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            throw new ExecutionException("Exception caught while shutting down", e);
        }
    }

    protected static KsmlQuerier getQuerier(KafkaStreamsRunner streamsRunner, KafkaProducerRunner producerRunner) {
        return new KsmlQuerier() {
            @Override
            public Collection<StreamsMetadata> allMetadataForStore(String storeName) {
                if (streamsRunner == null) {
                    return List.of();
                }
                return streamsRunner.getKafkaStreams().streamsMetadataForStore(storeName);
            }

            @Override
            public <K> KeyQueryMetadata queryMetadataForKey(String storeName, K key, Serializer<K> keySerializer) {
                if (streamsRunner == null) {
                    return null;
                }
                return streamsRunner.getKafkaStreams().queryMetadataForKey(storeName, key, keySerializer);
            }

            @Override
            public <T> T store(StoreQueryParameters<T> storeQueryParameters) {
                if (streamsRunner == null) {
                    return null;
                }
                return streamsRunner.getKafkaStreams().store(storeQueryParameters);
            }

            @Override
            public ComponentState getStreamRunnerState() {
                if (streamsRunner == null) {
                    return ComponentState.NOT_APPLICABLE;
                }
                return stateConverter(streamsRunner.getState());
            }

            @Override
            public ComponentState getProducerState() {
                if (producerRunner == null) {
                    return ComponentState.NOT_APPLICABLE;
                }
                return stateConverter(producerRunner.getState());
            }

            ComponentState stateConverter(Runner.State state) {
                return switch (state) {
                    case CREATED -> ComponentState.CREATED;
                    case STARTING -> ComponentState.STARTING;
                    case STARTED -> ComponentState.STARTED;
                    case STOPPING -> ComponentState.STOPPING;
                    case STOPPED -> ComponentState.STOPPED;
                    case FAILED -> ComponentState.FAILED;
                };
            }
        };
    }

    private static String determineTitle() {
        var titleBuilder = new StringBuilder()
                .append(KsmlInfo.APP_NAME);
        if (!KsmlInfo.APP_VERSION.isBlank()) {
            titleBuilder.append(" ").append(KsmlInfo.APP_VERSION);
        }
        if (!KsmlInfo.BUILD_TIME.isBlank()) {
            titleBuilder.append(" (")
                    .append(KsmlInfo.BUILD_TIME)
                    .append(")");
        }
        return titleBuilder.toString();
    }

    private static void checkForSchemaOutput(String[] args) {
        // Check if the runner was started with "--schema". If so, then we output the JSON schema to validate the
        // KSML definitions with on stdout and exit
        if (args.length >= 1 && WRITE_KSML_SCHEMA_ARGUMENT.equals(args[0])) {
            final var parser = new TopologyDefinitionParser("dummy");
            final var schema = new JsonSchemaMapper().fromDataSchema(parser.schema());

            final var filename = args.length >= 2 ? args[1] : null;
            if (filename != null) {
                try {
                    final var writer = new PrintWriter(filename);
                    writer.println(schema);
                    writer.close();
                    log.info("KSML JSON schema written to file: {}", filename);
                } catch (Exception e) {
                    // Ignore
                    log.atError()
                            .setMessage("""
                                    Error writing KSML JSON schema to file: {}
                                    Error: {}
                                    """)
                            .addArgument(filename)
                            .addArgument(e::getMessage)
                            .log();
                }
            } else {
                System.out.println(schema);
            }
            System.exit(0);
        }
    }

    private static KSMLRunnerConfig readConfiguration(File configFile) {
        final var mapper = new ObjectMapper(new YAMLFactory());
        try {
            final var config = mapper.readValue(configFile, KSMLRunnerConfig.class);
            if (config != null) {
                if (config.getKsmlConfig() == null) {
                    throw new ConfigException("Section \"ksml\" is missing in configuration");
                }
                if (config.getKafkaConfig() == null) {
                    throw new ConfigException("Section \"kafka\" is missing in configuration");
                }
                return config;
            }
        } catch (IOException e) {
            log.error("Configuration exception", e);
        }
        throw new ConfigException("No configuration found");
    }


    private static ErrorHandler getErrorHandler(KSMLErrorHandlingConfig.ErrorHandlingConfig config) {
        final var handlerType = switch (config.getHandler()) {
            case CONTINUE -> ErrorHandler.HandlerType.CONTINUE_ON_FAIL;
            case STOP -> ErrorHandler.HandlerType.STOP_ON_FAIL;
        };
        return new ErrorHandler(
                config.isLog(),
                config.isLogPayload(),
                config.getLoggerName(),
                handlerType);
    }
}
