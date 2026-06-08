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

import io.stoatflow.core.exception.DeserializationContext;
import io.stoatflow.core.exception.DeserializationExceptionHandler;
import io.stoatflow.core.exception.DeserializationHandlerResponse;
import io.stoatflow.core.exception.ProcessingContext;
import io.stoatflow.core.exception.ProcessingExceptionHandler;
import io.stoatflow.core.exception.ProcessingHandlerResponse;
import io.stoatflow.core.exception.ProductionContext;
import io.stoatflow.core.exception.ProductionExceptionHandler;
import io.stoatflow.core.exception.ProductionHandlerResponse;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;

import javax.annotation.Nonnull;

public class ExecutionErrorHandler implements DeserializationExceptionHandler, ProcessingExceptionHandler, ProductionExceptionHandler {
    @Override
    @Nonnull
    public DeserializationHandlerResponse handle(@Nonnull ConsumerRecord<byte[], byte[]> rec, @Nonnull Exception exception, @Nonnull DeserializationContext context) {
        return ExecutionContext.INSTANCE.errorHandling().handle(rec, exception, context);
    }

    @Override
    @Nonnull
    public ProcessingHandlerResponse handle(Object key, Object value, @Nonnull Exception exception, @Nonnull ProcessingContext context) {
        return ExecutionContext.INSTANCE.errorHandling().handle(key, value, exception, context);
    }

    @Override
    @Nonnull
    public ProductionHandlerResponse handle(@Nonnull ProducerRecord<byte[], byte[]> rec, @Nonnull Exception exception, @Nonnull ProductionContext context) {
        return ExecutionContext.INSTANCE.errorHandling().handle(rec, exception, context);
    }
}
