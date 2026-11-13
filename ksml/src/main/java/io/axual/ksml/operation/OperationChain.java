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

import io.axual.ksml.generator.TopologyBuildContext;
import io.axual.ksml.stream.KStreamWrapper;
import io.axual.ksml.stream.StreamWrapper;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/** Applies the operations of a pipeline in order, and connects punctuate operations to the operations in their forwardTo. */
public final class OperationChain {
    private OperationChain() {
    }

    /** Applies the operations to the cursor. */
    public static StreamWrapper apply(StreamWrapper cursor, List<StreamOperation> operations, TopologyBuildContext context) {
        return apply(cursor, operations, context, (operation, result) -> {
        });
    }

    /**
     * Applies the operations to the cursor.
     *
     * @param cursor     the stream to start with
     * @param operations the operations of the pipeline, in order
     * @param context    the build context
     * @param applied    called with each operation and its result
     * @return the result of the last operation
     */
    public static StreamWrapper apply(StreamWrapper cursor, List<StreamOperation> operations, TopologyBuildContext context, BiConsumer<StreamOperation, StreamWrapper> applied) {
        final var punctuates = new ArrayList<PunctuateOperation.SidePaths>();
        for (final var operation : operations) {
            if (operation instanceof BaseOperation target) cursor = addSidePaths(punctuates, target, cursor);
            if (operation instanceof PunctuateOperation punctuate && cursor instanceof KStreamWrapper stream) {
                final var result = punctuate.applyWithSidePaths(stream, context);
                punctuates.add(result.sidePaths());
                cursor = result.next();
            } else {
                cursor = cursor.apply(operation, context);
            }
            // Records from before a repartition topic must not be merged with records after it
            if (operation instanceof RepartitionOperation) {
                punctuates.forEach(PunctuateOperation.SidePaths::checkAllFound);
                punctuates.clear();
            }
            applied.accept(operation, cursor);
        }
        punctuates.forEach(PunctuateOperation.SidePaths::checkAllFound);
        return cursor;
    }

    /** Merges a side path from each punctuate operation that forwards to the target into its input. */
    private static StreamWrapper addSidePaths(List<PunctuateOperation.SidePaths> punctuates, BaseOperation target, StreamWrapper input) {
        if (!(input instanceof KStreamWrapper stream)) {
            for (final var punctuate : punctuates) punctuate.checkStreamInput(target, input);
            return input;
        }
        var merged = stream;
        for (final var punctuate : punctuates) {
            if (punctuate.targets(target)) merged = punctuate.sidePathTo(target, merged);
        }
        return merged;
    }
}
