package io.axual.ksml.rest.data;

/*-
 * ========================LICENSE_START=================================
 * KSML Queryable State Store
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

import lombok.Getter;
import org.apache.kafka.streams.kstream.Window;

@Getter
public class WindowDataBean {
    public final long start;
    public final long end;
    public final String startTime;
    public final String endTime;

    public WindowDataBean(Window window) {
        this.start = window.start();
        this.end = window.end();
        this.startTime = window.startTime().toString();
        this.endTime = window.endTime().toString();
    }
}
