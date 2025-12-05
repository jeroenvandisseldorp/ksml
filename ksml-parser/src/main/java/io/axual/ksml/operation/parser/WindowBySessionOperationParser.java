package io.axual.ksml.operation.parser;

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


import io.axual.ksml.dsl.KSMLDSL;
import io.axual.ksml.operation.WindowBySessionOperationDefinition;
import io.axual.ksml.parser.ParseContext;
import io.axual.ksml.parser.StructsParser;
import io.axual.ksml.resource.TopologyResources;
import org.apache.kafka.streams.kstream.SessionWindows;

public class WindowBySessionOperationParser extends OperationParser<WindowBySessionOperationDefinition> {
    public WindowBySessionOperationParser(ParseContext context, TopologyResources resources) {
        super(context, KSMLDSL.Operations.WINDOW_BY_SESSION, resources);
    }

    @Override
    public StructsParser<WindowBySessionOperationDefinition> parser() {
        return structsParser(
                WindowBySessionOperationDefinition.class,
                "",
                "Operation to window messages by session, configured by an inactivity gap",
                operationNameField(),
                durationField(KSMLDSL.SessionWindows.INACTIVITY_GAP, "The inactivity gap, below which two messages are considered to be of the same session"),
                optional(durationField(KSMLDSL.SessionWindows.GRACE, "(Tumbling, Hopping) The grace period, during which out-of-order records can still be processed")),
                (name, inactivityGap, grace, tags) -> {
                    final var sessionWindows = (grace != null && grace.toMillis() > 0)
                            ? SessionWindows.ofInactivityGapAndGrace(inactivityGap, grace)
                            : SessionWindows.ofInactivityGapWithNoGrace(inactivityGap);
                    return new WindowBySessionOperationDefinition(operationConfig(name, tags), sessionWindows);
                });
    }
}
