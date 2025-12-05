package io.axual.ksml.function;

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


import io.axual.ksml.data.type.DataType;
import io.axual.ksml.dsl.KSMLDSL;

import static io.axual.ksml.function.FunctionDefinitionDSL.KEY_VALUE_METADATA_PARAMETERS;
import static io.axual.ksml.function.FunctionDefinitionDSL.PARAM_METADATA;

public class MetadataTransformerDefinition extends FunctionDefinition {
    public static final DataType EXPECTED_RESULT_TYPE = FunctionDefinitionDSL.METADATA_TYPE;

    public MetadataTransformerDefinition(FunctionDefinition definition) {
        super(definition
                .withType(KSMLDSL.Functions.TYPE_METADATATRANSFORMER)
                .withParameters(mergeParameters(KEY_VALUE_METADATA_PARAMETERS, definition.parameters()))
                .withDefaultExpression(PARAM_METADATA)
                .withDefaultResultType(EXPECTED_RESULT_TYPE));
    }
}
