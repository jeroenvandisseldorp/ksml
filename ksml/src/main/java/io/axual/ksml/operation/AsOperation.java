package io.axual.ksml.operation;

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

import io.axual.ksml.generator.TopologyBuildContext;
import io.axual.ksml.stream.*;

public class AsOperation extends BaseOperation {
    public final String targetName;

    public AsOperation(OperationConfig config, String targetName) {
        super(config);
        this.targetName = targetName;
    }

    @Override
    public StreamWrapper apply(KStreamWrapper input, TopologyBuildContext context) {
        context.registerStreamWrapper(targetName, input);
        return null;
    }

    @Override
    public StreamWrapper apply(KTableWrapper input, TopologyBuildContext context) {
        context.registerStreamWrapper(targetName, input);
        return null;
    }

    @Override
    public StreamWrapper apply(GlobalKTableWrapper input, TopologyBuildContext context) {
        context.registerStreamWrapper(targetName, input);
        return null;
    }

    @Override
    public StreamWrapper apply(KGroupedStreamWrapper input, TopologyBuildContext context) {
        context.registerStreamWrapper(targetName, input);
        return null;
    }

    @Override
    public StreamWrapper apply(KGroupedTableWrapper input, TopologyBuildContext context) {
        context.registerStreamWrapper(targetName, input);
        return null;
    }

    @Override
    public StreamWrapper apply(SessionWindowedKStreamWrapper input, TopologyBuildContext context) {
        context.registerStreamWrapper(targetName, input);
        return null;
    }

    @Override
    public StreamWrapper apply(TimeWindowedKStreamWrapper input, TopologyBuildContext context) {
        context.registerStreamWrapper(targetName, input);
        return null;
    }

    @Override
    public StreamWrapper apply(CogroupedKStreamWrapper input, TopologyBuildContext context) {
        context.registerStreamWrapper(targetName, input);
        return null;
    }

    @Override
    public StreamWrapper apply(SessionWindowedCogroupedKStreamWrapper input, TopologyBuildContext context) {
        context.registerStreamWrapper(targetName, input);
        return null;
    }

    @Override
    public StreamWrapper apply(TimeWindowedCogroupedKStreamWrapper input, TopologyBuildContext context) {
        context.registerStreamWrapper(targetName, input);
        return null;
    }
}
