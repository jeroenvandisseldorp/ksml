package io.axual.ksml.user;

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

import io.axual.ksml.data.object.DataLong;
import io.axual.ksml.dsl.KSMLDSL;
import io.axual.ksml.metric.MetricTags;
import io.axual.ksml.proxy.context.ProcessorContextProxy;
import io.axual.ksml.python.Invoker;
import io.axual.ksml.store.StateStores;

import java.util.HashMap;

public class UserPunctuator extends Invoker {
    public UserPunctuator(UserFunction function, MetricTags tags) {
        super(function, tags, KSMLDSL.Functions.TYPE_PUNCTUATOR);
        verifyParameterCount(1);
    }

    /**
     * Calls the punctuator with the timestamp, and with the stores and the context as variables. It emits records with
     * {@code context.forward}; a returned value is ignored.
     */
    public void apply(StateStores stores, long timestamp, ProcessorContextProxy context) {
        final var injections = new HashMap<String, Object>(UserFunction.withStores(stores));
        injections.put(ProcessorContextProxy.PYTHON_PROXY.name(), context);
        timeExecutionOf(() -> function.call(injections, new DataLong(timestamp)));
    }
}
