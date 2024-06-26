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
import io.axual.ksml.generator.TopologyResources;
import io.axual.ksml.operation.AsOperation;
import io.axual.ksml.parser.StructsParser;

public class AsOperationParser extends OperationParser<AsOperation> {
    public AsOperationParser(TopologyResources resources) {
        super(KSMLDSL.Operations.AS, resources);
    }

    public StructsParser<AsOperation> parser() {
        return structsParser(
                AsOperation.class,
                "",
                "An operation to close the pipeline and save the result under a given name",
                stringField(KSMLDSL.Operations.AS, "The name to register the pipeline result under, which can be used as source by follow-up pipelines"),
                (name, tags) -> name != null ? new AsOperation(operationConfig(name, tags), name) : null);
    }
}
