package io.axual.ksml.operation;

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


import io.axual.ksml.topology.BranchDefinition;
import io.axual.ksml.function.PredicateDefinition;
import io.axual.ksml.generator.TopologyBuildContext;
import io.axual.ksml.stream.KStreamWrapper;
import io.axual.ksml.stream.StreamWrapper;
import io.axual.ksml.user.UserPredicate;
import org.apache.kafka.streams.kstream.Branched;
import org.apache.kafka.streams.kstream.BranchedKStream;
import org.apache.kafka.streams.kstream.Named;
import org.apache.kafka.streams.kstream.Predicate;

import java.util.ArrayList;

public class BranchOperation extends BaseOperation<BranchOperationDefinition> {
    private static final String PREDICATE_NAME = "Predicate";

    public BranchOperation(BranchOperationDefinition definition) {
        super(definition);
    }

    @Override
    public StreamWrapper apply(KStreamWrapper input, TopologyBuildContext context) {
        final var k = input.keyType();
        final var v = input.valueType();

        // Prepare the branch predicates to pass into the KStream
        final var predicates = new ArrayList<Predicate<Object, Object>>(def.branches().size());
        for (final BranchDefinition branch : def.branches()) {
            if (branch.predicate() != null) {
                final var pred = userFunctionOf(context, PREDICATE_NAME, branch.predicate(), PredicateDefinition.EXPECTED_RESULT_TYPE, superOf(k), superOf(v));
                predicates.add(new UserPredicate(pred, tags));
            } else {
                predicates.add((key, value) -> true);
            }
        }

        // Pass the predicates to KStream and get resulting KStream branches back
        final BranchedKStream<Object, Object> splitStream = name != null
                ? input.stream.split(Named.as(name))
                : input.stream.split();
        for (var index = 0; index < predicates.size(); index++) {
            splitStream.branch(predicates.get(index), Branched.as("" + index));
        }
        final var output = splitStream.noDefaultBranch();

        // For every branch, generate a separate pipeline
        for (var index = 0; index < predicates.size(); index++) {
            StreamWrapper branchCursor = new KStreamWrapper(output.get(name + index), k, v);
            for (final var definition : def.branches().get(index).pipeline().chain()) {
                final var operation = OperationFactory.createOperation(definition);
                branchCursor = branchCursor.apply(operation, context);
            }
            if (def.branches().get(index).pipeline().sink() != null) {
                final var sinkOperation = OperationFactory.createOperation(def.branches().get(index).pipeline().sink());
                branchCursor.apply(sinkOperation, context);
            }
        }

        return null;
    }
}
