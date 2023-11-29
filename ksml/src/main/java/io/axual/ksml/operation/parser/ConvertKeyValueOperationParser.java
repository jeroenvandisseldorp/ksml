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

import io.axual.ksml.data.type.UserTupleType;
import io.axual.ksml.data.type.UserType;
import io.axual.ksml.execution.FatalError;
import io.axual.ksml.generator.TopologyResources;
import io.axual.ksml.operation.ConvertKeyValueOperation;
import io.axual.ksml.parser.UserTypeParser;
import io.axual.ksml.parser.YamlNode;

import static io.axual.ksml.dsl.KSMLDSL.CONVERT_INTO_ATTRIBUTE;

public class ConvertKeyValueOperationParser extends OperationParser<ConvertKeyValueOperation> {
    public ConvertKeyValueOperationParser(String name, TopologyResources resources) {
        super(name, resources);
    }

    @Override
    public ConvertKeyValueOperation parse(YamlNode node) {
        if (node == null) return null;
        UserType target = UserTypeParser.parse(parseString(node, CONVERT_INTO_ATTRIBUTE));
        if (target.dataType() instanceof UserTupleType userTupleType && userTupleType.subTypeCount() == 2) {
            return new ConvertKeyValueOperation(operationConfig(node), userTupleType.getUserType(0), userTupleType.getUserType(1));
        }
        throw FatalError.parseError(node, "The type to convert to should be a tuple consisting of two subtypes. For example '(string,avro:SomeSchema)");
    }
}
