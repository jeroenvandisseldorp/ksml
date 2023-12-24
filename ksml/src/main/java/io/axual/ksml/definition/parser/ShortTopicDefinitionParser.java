package io.axual.ksml.definition.parser;

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


import io.axual.ksml.definition.ShortTopicDefinition;
import io.axual.ksml.dsl.KSMLDSL;
import io.axual.ksml.parser.MultiFormParser;
import io.axual.ksml.parser.MultiSchemaParser;

public class ShortTopicDefinitionParser extends MultiFormParser<ShortTopicDefinition> {
    @Override
    public MultiSchemaParser<ShortTopicDefinition> parser() {
        return structParser(
                ShortTopicDefinition.class,
                "Contains a definition of a Topic, which can be referenced by producers and pipelines",
                stringField(KSMLDSL.Streams.TOPIC, true, "The name of the Kafka topic"),
                userTypeField(KSMLDSL.Streams.KEY_TYPE, false, "The key type of the topic"),
                userTypeField(KSMLDSL.Streams.VALUE_TYPE, false, "The value type of the topic"),
                (topic, keyType, valueType) -> new ShortTopicDefinition("Topic", topic, keyType, valueType));
    }
}
