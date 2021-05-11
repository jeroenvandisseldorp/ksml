package io.axual.ksml.parser;

/*-
 * ========================LICENSE_START=================================
 * KSML
 * %%
 * Copyright (C) 2021 Axual B.V.
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



import io.axual.ksml.dsl.BranchDefinition;
import io.axual.ksml.dsl.KSMLDSL;

public class BranchDefinitionParser extends ContextAwareParser<BranchDefinition> {
    public BranchDefinitionParser(ParseContext context) {
        super(context);
    }

    @Override
    public BranchDefinition parse(YamlNode node) {
        if (node == null) return null;
        return new BranchDefinition(
                parseFunction(node, KSMLDSL.BRANCH_PREDICATE_ATTRIBUTE, new PredicateDefinitionParser(), true),
                new PipelineDefinitionParser(context).parse(node, false, true));
    }
}