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

import io.axual.ksml.definition.parser.ForEachActionDefinitionParser;
import io.axual.ksml.dsl.KSMLDSL;
import io.axual.ksml.operation.ForEachOperation;
import io.axual.ksml.operation.StoreOperationConfig;
import io.axual.ksml.parser.MultiSchemaParser;

public class ForEachOperationParser extends OperationParser<ForEachOperation> {
    public ForEachOperationParser(String namespace) {
        super(namespace, "forEach");
    }

    public MultiSchemaParser<ForEachOperation> parser() {
        return structParser(
                ForEachOperation.class,
                "Operation to call a function for every record",
                functionField(
                        KSMLDSL.Operations.FOR_EACH,
                        false,
                        "A function that gets called for every message",
                        new ForEachActionDefinitionParser()),
                action -> new ForEachOperation(new StoreOperationConfig(namespace(), "forEach", null, null), action));
    }
}
