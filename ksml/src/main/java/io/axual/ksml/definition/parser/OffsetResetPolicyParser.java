package io.axual.ksml.definition.parser;

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

import io.axual.ksml.exception.TopologyException;
import org.apache.kafka.streams.Topology;

public class OffsetResetPolicyParser {
    public static Topology.AutoOffsetReset parseResetPolicy(String resetPolicy) {
        if (resetPolicy == null || resetPolicy.isEmpty()) return null;
        if (Topology.AutoOffsetReset.EARLIEST.name().equalsIgnoreCase(resetPolicy))
            return Topology.AutoOffsetReset.EARLIEST;
        if (Topology.AutoOffsetReset.LATEST.name().equalsIgnoreCase(resetPolicy))
            return Topology.AutoOffsetReset.LATEST;
        throw new TopologyException("Unknown offset reset policy: " + resetPolicy);
    }

    private OffsetResetPolicyParser() {
        // Prevent instantiation.
    }
}
