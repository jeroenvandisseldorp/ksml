package io.axual.ksml.execution;

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

import io.stoatflow.core.exception.DeserializationExceptionHandler;
import io.stoatflow.core.exception.DeserializationHandlerResponse;
import io.stoatflow.core.exception.ErrorHandlerContext;
import io.stoatflow.core.exception.ProcessingExceptionHandler;
import io.stoatflow.core.exception.ProcessingHandlerResponse;
import io.stoatflow.core.exception.ProductionExceptionHandler;
import io.stoatflow.core.exception.ProductionHandlerResponse;
import io.stoatflow.core.processor.Record;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.jspecify.annotations.NonNull;

public class ExecutionErrorHandler implements DeserializationExceptionHandler, ProcessingExceptionHandler, ProductionExceptionHandler {
    @Override
    public @NonNull DeserializationHandlerResponse handle(@NonNull ErrorHandlerContext errorHandlerContext, @NonNull ConsumerRecord<byte[], byte[]> rec, @NonNull Exception exception) {
        return ExecutionContext.INSTANCE.errorHandling().handle(rec, exception, errorHandlerContext);
    }

    @Override
    public @NonNull ProcessingHandlerResponse handle(@NonNull ErrorHandlerContext errorHandlerContext, @NonNull Record<?, ?> rec, @NonNull Exception exception) {
        return ExecutionContext.INSTANCE.errorHandling().handle(rec, exception, errorHandlerContext);
    }

    @Override
    public @NonNull ProductionHandlerResponse handle(@NonNull ErrorHandlerContext errorHandlerContext, @NonNull ProducerRecord<byte[], byte[]> rec, @NonNull Exception exception) {
        return ExecutionContext.INSTANCE.errorHandling().handle(rec, exception, errorHandlerContext);
    }
}
