package io.axual.ksml.operation;

/*-
 * ========================LICENSE_START=================================
 * KSML
 * %%
 * Copyright (C) 2021 - 2026 Axual B.V.
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

import io.axual.ksml.data.object.DataLong;
import io.axual.ksml.data.object.DataNull;
import io.axual.ksml.definition.FunctionDefinition;
import io.axual.ksml.generator.TopologyBuildContext;
import io.axual.ksml.operation.processor.OperationProcessorSupplier;
import io.axual.ksml.operation.processor.PunctuateProcessor;
import io.axual.ksml.proxy.context.ProcessorContextProxy;
import io.axual.ksml.stream.CogroupedKStreamWrapper;
import io.axual.ksml.stream.GlobalKTableWrapper;
import io.axual.ksml.stream.KGroupedStreamWrapper;
import io.axual.ksml.stream.KGroupedTableWrapper;
import io.axual.ksml.stream.KStreamWrapper;
import io.axual.ksml.stream.KTableWrapper;
import io.axual.ksml.stream.SessionWindowedCogroupedKStreamWrapper;
import io.axual.ksml.stream.SessionWindowedKStreamWrapper;
import io.axual.ksml.stream.StreamWrapper;
import io.axual.ksml.stream.TimeWindowedCogroupedKStreamWrapper;
import io.axual.ksml.stream.TimeWindowedKStreamWrapper;
import io.axual.ksml.user.UserPunctuator;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Named;
import org.apache.kafka.streams.processor.PunctuationType;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Calls a punctuator on a schedule; records pass through unchanged. The punctuator can forward records to the next
 * operation, or to the named operations listed in {@code forwardTo}: {@link OperationChain} asks the
 * {@link SidePaths} of each applied punctuate operation for a side path to each of those.
 */
public class PunctuateOperation extends BaseOperation {
    private static final String PUNCTUATOR_NAME = "Punctuator";
    private final Duration interval;
    private final PunctuationType punctuationType;
    private final FunctionDefinition punctuator;
    private final Set<String> forwardTo;

    public PunctuateOperation(OperationConfig config, Duration interval, PunctuationType punctuationType, FunctionDefinition punctuator, List<String> forwardTo) {
        super(config);
        this.interval = interval;
        this.punctuationType = punctuationType;
        this.punctuator = punctuator;
        this.forwardTo = forwardTo != null ? Collections.unmodifiableSet(new LinkedHashSet<>(forwardTo)) : Set.of();
    }

    /**
     * The result of applying a punctuate operation to a stream.
     *
     * @param next      the stream for the next operation
     * @param sidePaths the side paths to the operations in {@code forwardTo}, added while the rest of the pipeline is built
     */
    public record Applied(KStreamWrapper next, SidePaths sidePaths) {
    }

    @Override
    public StreamWrapper apply(KStreamWrapper input, TopologyBuildContext context) {
        // Only OperationChain connects the operations in forwardTo
        if (!forwardTo.isEmpty())
            throw topologyError("Punctuate with forwardTo must be applied through OperationChain");
        return applyWithSidePaths(input, context).next();
    }

    /** Applies the operation; the returned side paths connect the operations in {@code forwardTo} to it. */
    public Applied applyWithSidePaths(KStreamWrapper input, TopologyBuildContext context) {
        /*    Kafka Streams method signature:
         *    <KOut, VOut> KStream<KOut, VOut> process(
         *          final ProcessorSupplier<? super K, ? super V, KOut, VOut> processorSupplier,
         *          final Named named,
         *          final String... stateStoreNames)
         */

        checkNotNull(punctuator, PUNCTUATOR_NAME.toLowerCase());
        // The side path node names are made from the operation name
        if (name == null && !forwardTo.isEmpty())
            throw topologyError("Punctuate with forwardTo needs a valid name");
        final var k = input.keyType().flatten();
        final var v = input.valueType().flatten();
        final var userFunction = userFunctionOf(context, PUNCTUATOR_NAME, punctuator, DataNull.DATATYPE, superOf(DataLong.DATATYPE));
        final var userPunctuator = new UserPunctuator(userFunction, tags);
        final var stores = punctuator.storeNames().toArray(String[]::new);

        // With targets, the next operation gets a named pass-through node so records can be sent to it alone
        final var nextName = !forwardTo.isEmpty() ? name + "-next" : null;
        final var namedTargets = new HashMap<String, ProcessorContextProxy.Target>();
        final var schedule = new PunctuateProcessor.Schedule(interval, punctuationType,
                new ProcessorContextProxy.Target(nextName, k.userType(), v.userType()), namedTargets);
        final var supplier = new OperationProcessorSupplier<PunctuateProcessor.PunctuateAction>(
                name,
                (n, action, s) -> new PunctuateProcessor(n, schedule, action, s),
                userPunctuator::apply,
                stores,
                storeDefinitions(context, stores));
        final var named = namedOf();
        final KStream<Object, Object> output = named != null
                ? input.stream.process(supplier, named, stores)
                : input.stream.process(supplier, stores);
        final var next = nextName != null ? output.peek((key, value) -> {
        }, Named.as(nextName)) : output;
        return new Applied(new KStreamWrapper(next, k, v), new SidePaths(output, namedTargets));
    }

    /** The side paths of one applied punctuate operation to the operations in its {@code forwardTo}. */
    public final class SidePaths {
        private final KStream<Object, Object> output;
        // Shared with the processor, which reads it only after the topology is built
        private final Map<String, ProcessorContextProxy.Target> namedTargets;

        private SidePaths(KStream<Object, Object> output, Map<String, ProcessorContextProxy.Target> namedTargets) {
            this.output = output;
            this.namedTargets = namedTargets;
        }

        /** Whether {@code forwardTo} lists the operation. */
        boolean targets(BaseOperation operation) {
            return operation.definedName() != null && forwardTo.contains(operation.definedName());
        }

        /**
         * Makes the operation a target of {@code forward(..., to=...)}: adds a pass-through node on the output of the
         * punctuate operation and merges it into the stream right before the target.
         *
         * @param target the operation, which {@code forwardTo} lists
         * @param input  the stream that the target will be applied to
         * @return the stream to apply the target to
         */
        KStreamWrapper sidePathTo(BaseOperation target, KStreamWrapper input) {
            if (namedTargets.containsKey(target.definedName()))
                throw topologyError("Operation name '" + target.definedName() + "' is used twice after punctuate '" + name + "'");
            final var sideName = name + "-to-" + target.name;
            final var side = output.peek((key, value) -> {
            }, Named.as(sideName));
            namedTargets.put(target.definedName(), new ProcessorContextProxy.Target(sideName, input.keyType().flatten().userType(), input.valueType().flatten().userType()));
            return new KStreamWrapper(input.stream.merge(side, Named.as(sideName + "-merge")), input.keyType(), input.valueType());
        }

        /** Fails when an operation in {@code forwardTo} was not found. */
        void checkAllFound() {
            final var missing = forwardTo.stream().filter(target -> !namedTargets.containsKey(target)).toList();
            if (!missing.isEmpty())
                throw topologyError("Punctuate '" + name + "' can not forward to " + String.join(", ", missing)
                        + ": forwardTo can only name operations after it in the same pipeline, before any repartition, grouping, join or toTable");
        }
    }

    @Override
    public StreamWrapper apply(KTableWrapper input, TopologyBuildContext context) {
        throw needsStream(input);
    }

    @Override
    public StreamWrapper apply(GlobalKTableWrapper input, TopologyBuildContext context) {
        throw needsStream(input);
    }

    @Override
    public StreamWrapper apply(KGroupedStreamWrapper input, TopologyBuildContext context) {
        throw needsStream(input);
    }

    @Override
    public StreamWrapper apply(KGroupedTableWrapper input, TopologyBuildContext context) {
        throw needsStream(input);
    }

    @Override
    public StreamWrapper apply(SessionWindowedKStreamWrapper input, TopologyBuildContext context) {
        throw needsStream(input);
    }

    @Override
    public StreamWrapper apply(TimeWindowedKStreamWrapper input, TopologyBuildContext context) {
        throw needsStream(input);
    }

    @Override
    public StreamWrapper apply(CogroupedKStreamWrapper input, TopologyBuildContext context) {
        throw needsStream(input);
    }

    @Override
    public StreamWrapper apply(SessionWindowedCogroupedKStreamWrapper input, TopologyBuildContext context) {
        throw needsStream(input);
    }

    @Override
    public StreamWrapper apply(TimeWindowedCogroupedKStreamWrapper input, TopologyBuildContext context) {
        throw needsStream(input);
    }

    private RuntimeException needsStream(StreamWrapper input) {
        return topologyError("Punctuate needs a stream as input, but got " + input + "; use toStream first for a table");
    }
}
