package io.axual.ksml.definition;

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

/**
 * A variable that a function's caller sets for the function's code, such as {@code context} for a punctuator. It is
 * not a definition in the KSML language: the function type declares it, not the user.
 *
 * @param name           the Python global the code reads the variable from
 * @param proxyClassName the Python class that wraps the injected object, or {@code null} to pass it as it is
 * @param proxyClassCode the Python code that defines {@code proxyClassName}, or {@code null}
 */
public record InjectedParameter(String name, String proxyClassName, String proxyClassCode) {
    public InjectedParameter(String name) {
        this(name, null, null);
    }
}
