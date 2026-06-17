package io.axual.ksml.execution;

/*-
 * ========================LICENSE_START=================================
 * KSML
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

import io.stoatflow.core.exception.DeserializationContext;
import io.stoatflow.core.exception.DeserializationHandlerResponse;
import io.stoatflow.core.exception.ProcessingContext;
import io.stoatflow.core.exception.ProcessingHandlerResponse;
import io.stoatflow.core.exception.ProductionContext;
import io.stoatflow.core.exception.ProductionHandlerResponse;
import io.stoatflow.core.exception.StreamsException;
import io.stoatflow.core.exception.StreamsUncaughtExceptionHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.TopicAuthorizationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Base64;
import java.util.stream.Collectors;

@Slf4j
public class ErrorHandling {
    private static final String DATA_MASK = "*****";
    private static final String DATA_NULL = "<NULL>";
    private static final String STRING_PREFIX = "(string)";

    private ErrorHandler consumeHandler;
    private ErrorHandler processHandler;
    private ErrorHandler produceHandler;

    private Logger consumeExceptionLogger;
    private Logger produceExceptionLogger;
    private Logger processExceptionLogger;

    public void setConsumeHandler(ErrorHandler consumeHandler) {
        this.consumeHandler = consumeHandler;
        this.consumeExceptionLogger = LoggerFactory.getLogger(consumeHandler.loggerName());
    }

    public void setProcessHandler(ErrorHandler processHandler) {
        this.processHandler = processHandler;
        this.processExceptionLogger = LoggerFactory.getLogger(processHandler.loggerName());
    }

    public void setProduceHandler(ErrorHandler produceHandler) {
        this.produceHandler = produceHandler;
        this.produceExceptionLogger = LoggerFactory.getLogger(produceHandler.loggerName());
    }

    public StreamsUncaughtExceptionHandler.StreamThreadExceptionResponse uncaughtException(Throwable throwable) {
        if (throwable instanceof StreamsException streamsException) {
            if (streamsException.getCause() instanceof TopicAuthorizationException topicAuthorizationException && !topicAuthorizationException.unauthorizedTopics().isEmpty()) {
                log.error("Topic authorization exception was thrown. Please create / grant access to the following topics: \n"
                        + topicAuthorizationException.unauthorizedTopics().stream().map(t -> "  * " + t + "\n").collect(Collectors.joining()));
            }
        } else {
            processExceptionLogger.error("Caught unhandled exception, stopping this KSML instance", throwable);
        }
        // Stop only the current instance of KSML
        return StreamsUncaughtExceptionHandler.StreamThreadExceptionResponse.SHUTDOWN_CLIENT;
    }

    public String bytesToString(byte[] data) {
        return data == null ? DATA_NULL : "(base64)" + Base64.getEncoder().encodeToString(data);
    }

    public String objectToString(Object data) {
        return data == null ? DATA_NULL : STRING_PREFIX + data;
    }

    private void logError(Logger logger, String errorType, Object context, String key, String value, Exception exception) {
        switch(context) {
            case DeserializationContext ctx: {
                logger.error("Deserialization error:\n  topic={}\n  partition={}\n  offset={}\n  key={}\n  value={}\n",
                        ctx.getTopic(),
                        ctx.getPartition(),
                        ctx.getOffset(),
                        key,
                        value);
                logger.error("Caused by: {}", exception.getMessage(), exception);
                break;
            }
            case ProcessingContext ctx: {
                logger.error("Processing error:\n  sourceTopic={}\n  sourcePartition={}\n  sourceOffset={}\n  processorName={}\n  timestamp={}\n  key={}\n  value={}\n",
                        ctx.getSourceTopic(),
                        ctx.getSourcePartition(),
                        ctx.getSourceOffset(),
                        ctx.getProcessorName(),
                        ctx.getTimestamp(),
                        key,
                        value);
                logger.error("Caused by: {}", exception.getMessage(), exception);
                break;
            }
            case ProductionContext ctx: {
                logger.error("Processing error:\n  topic={}\n  failedOn={}\n  sourceTopic={}\n  sourcePartition={}\n  sourceOffset={}\n  processorNodeId={}\n  key={}\n  value={}\n",
                        ctx.getTopic(),
                        ctx.getFailedOn(),
                        ctx.getSourceTopic(),
                        ctx.getSourcePartition(),
                        ctx.getSourceOffset(),
                        ctx.getProcessorNodeId(),
                        key,
                        value);
                logger.error("Caused by: {}", exception.getMessage(), exception);
                break;
            }
            default: throw new UnsupportedOperationException("Unsupported context type: " + context.getClass().getName());
        }
    }

    public DeserializationHandlerResponse handle(ConsumerRecord<byte[], byte[]> rec, Exception exception, DeserializationContext context) {
        if (consumeHandler.log()) {
            // log record
            String key = consumeHandler.logPayload() ? bytesToString(rec.key()) : DATA_MASK;
            String value = consumeHandler.logPayload() ? bytesToString(rec.value()) : DATA_MASK;
            logError(consumeExceptionLogger, "Deserialization", context, key, value, exception);
        }
        return switch (consumeHandler.handlerType()) {
            case CONTINUE_ON_FAIL -> new DeserializationHandlerResponse(DeserializationHandlerResponse.Result.CONTINUE, new ArrayList<>());
            case STOP_ON_FAIL ->new DeserializationHandlerResponse(DeserializationHandlerResponse.Result.FAIL, new ArrayList<>());
            default ->
                    throw new UnsupportedOperationException("Unsupported deserialization error handler type. Only CONTINUE_ON_FAIL or STOP_ON_FAIL are allowed.");
        };
    }

    public ProcessingHandlerResponse handle(Object key, Object value, Exception exception, ProcessingContext context) {
        if (processHandler.log()) {
            // log record
            String keyStr = processHandler.logPayload() ? objectToString(key) : DATA_MASK;
            String valueStr = processHandler.logPayload() ? objectToString(value) : DATA_MASK;
            logError(processExceptionLogger, "Processing", context, keyStr, valueStr, exception);
        }
        return switch (processHandler.handlerType()) {
            case CONTINUE_ON_FAIL -> new ProcessingHandlerResponse(ProcessingHandlerResponse.Result.CONTINUE, new ArrayList<>());
            case STOP_ON_FAIL -> new ProcessingHandlerResponse(ProcessingHandlerResponse.Result.FAIL, new ArrayList<>());
            default ->
                    throw new UnsupportedOperationException("Unsupported processing error handler type. Only CONTINUE_ON_FAIL or STOP_ON_FAIL are allowed.");
        };
    }

    public ProductionHandlerResponse handle(ProducerRecord<byte[], byte[]> rec, Exception exception, ProductionContext context) {
        if (produceHandler.log()) {
            // log record
            String key = produceHandler.logPayload() ? bytesToString(rec.key()) : DATA_MASK;
            String value = produceHandler.logPayload() ? bytesToString(rec.value()) : DATA_MASK;
            logError(produceExceptionLogger, "Produce", context, key, value, exception);
        }
        return switch (produceHandler.handlerType()) {
            case CONTINUE_ON_FAIL -> new ProductionHandlerResponse(ProductionHandlerResponse.Result.CONTINUE, new ArrayList<>());
            case STOP_ON_FAIL -> new ProductionHandlerResponse(ProductionHandlerResponse.Result.FAIL, new ArrayList<>());
            case RETRY_ON_FAIL -> new ProductionHandlerResponse(ProductionHandlerResponse.Result.RETRY, new ArrayList<>());
        };
    }
}
