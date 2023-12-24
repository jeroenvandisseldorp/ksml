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


import io.axual.ksml.definition.HoppingWindowDefinition;
import io.axual.ksml.definition.SlidingWindowDefinition;
import io.axual.ksml.definition.TumblingWindowDefinition;
import io.axual.ksml.dsl.KSMLDSL;
import io.axual.ksml.execution.FatalError;
import io.axual.ksml.operation.WindowByTimeOperation;
import io.axual.ksml.parser.ChoiceFieldParser;
import io.axual.ksml.parser.MultiFormParser;
import io.axual.ksml.parser.MultiSchemaParser;
import org.apache.kafka.streams.kstream.SlidingWindows;
import org.apache.kafka.streams.kstream.TimeWindows;

import java.util.LinkedHashMap;

public class WindowByTimeOperationParser extends OperationParser<WindowByTimeOperation> {
    private static final String DURATION_DOC = "The duration of time windows";
    private static final String GRACE_DOC = "The grace period, during which out-of-order records can still be processed";
    private static final String ADVANCE_BY_DOC = "The amount of time to increase time windows by";
    private static final String TIME_DIFF_DOC = "The maximum amount of time difference between two records";

    public WindowByTimeOperationParser(String namespace) {
        super(namespace, "windowByTime");
    }

    private static class TumblingWindowParser extends MultiFormParser<TumblingWindowDefinition> {
        @Override
        public MultiSchemaParser<TumblingWindowDefinition> parser() {
            return structParser(
                    TumblingWindowDefinition.class,
                    "Defines a tumbling window",
                    durationField(KSMLDSL.TimeWindows.DURATION, true, DURATION_DOC),
                    durationField(KSMLDSL.TimeWindows.GRACE, false, GRACE_DOC),
                    TumblingWindowDefinition::new);
        }
    }

    private static class HoppingWindowParser extends MultiFormParser<HoppingWindowDefinition> {
        @Override
        public MultiSchemaParser<HoppingWindowDefinition> parser() {
            return structParser(
                    HoppingWindowDefinition.class,
                    "Defines a hopping window",
                    durationField(KSMLDSL.TimeWindows.DURATION, true, DURATION_DOC),
                    durationField(KSMLDSL.TimeWindows.ADVANCE_BY, false, ADVANCE_BY_DOC),
                    durationField(KSMLDSL.TimeWindows.GRACE, false, GRACE_DOC),
                    HoppingWindowDefinition::new);
        }
    }

    private static class SlidingWindowParser extends MultiFormParser<SlidingWindowDefinition> {
        @Override
        public MultiSchemaParser<SlidingWindowDefinition> parser() {
            return structParser(
                    SlidingWindowDefinition.class,
                    "Defines a sliding window",
                    durationField(KSMLDSL.TimeWindows.TIME_DIFFERENCE, false, TIME_DIFF_DOC),
                    durationField(KSMLDSL.TimeWindows.GRACE, false, GRACE_DOC),
                    SlidingWindowDefinition::new);
        }
    }

    @Override
    public MultiSchemaParser<WindowByTimeOperation> parser() {
        final var windowParsers = new LinkedHashMap<String, MultiSchemaParser<?>>();
        windowParsers.put(KSMLDSL.TimeWindows.TYPE_TUMBLING, new TumblingWindowParser());
        windowParsers.put(KSMLDSL.TimeWindows.TYPE_HOPPING, new HoppingWindowParser());
        windowParsers.put(KSMLDSL.TimeWindows.TYPE_SLIDING, new SlidingWindowParser());
        final var windowParser = new ChoiceFieldParser<>(KSMLDSL.TimeWindows.WINDOW_TYPE, "window", null, windowParsers);

        return structParser(
                WindowByTimeOperation.class,
                "Operation to reduce a series of records into a single aggregate result",
                operationTypeField(KSMLDSL.Operations.WINDOW_BY_TIME),
                nameField(),
                windowParser,
                (type, name, window) -> {
                    if (window instanceof TumblingWindowDefinition win) {
                        final var timeWindows = (win.grace() != null && win.grace().toMillis() > 0)
                                ? TimeWindows.ofSizeAndGrace(win.duration(), win.grace())
                                : TimeWindows.ofSizeWithNoGrace(win.duration());
                        return new WindowByTimeOperation(operationConfig(name), timeWindows);
                    }
                    if (window instanceof HoppingWindowDefinition win) {
                        if (win.advanceBy().toMillis() > win.duration().toMillis()) {
                            throw FatalError.topologyError("A hopping window can not advanceBy more than its duration");
                        }
                        final var timeWindows = (win.grace() != null && win.grace().toMillis() > 0)
                                ? org.apache.kafka.streams.kstream.TimeWindows.ofSizeAndGrace(win.duration(), win.grace()).advanceBy(win.advanceBy())
                                : org.apache.kafka.streams.kstream.TimeWindows.ofSizeWithNoGrace(win.duration()).advanceBy(win.advanceBy());
                        return new WindowByTimeOperation(operationConfig(name), timeWindows);
                    }
                    if (window instanceof SlidingWindowDefinition win) {
                        final var slidingWindows = (win.grace() != null && win.grace().toMillis() > 0)
                                ? SlidingWindows.ofTimeDifferenceAndGrace(win.timeDifference(), win.grace())
                                : SlidingWindows.ofTimeDifferenceWithNoGrace(win.timeDifference());
                        return new WindowByTimeOperation(operationConfig(name), slidingWindows);
                    }
                    throw FatalError.topologyError("Unknown WindowType for windowByTime operation: " + (window != null ? window.getClass().getSimpleName() : "null"));
                });
    }
}
