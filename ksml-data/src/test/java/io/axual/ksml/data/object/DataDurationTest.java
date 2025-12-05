package io.axual.ksml.data.object;

/*-
 * ========================LICENSE_START=================================
 * KSML Data Library
 * %%
 * Copyright (C) 2021 - 2025 Axual B.V.
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

import org.junit.jupiter.api.Test;
import org.threeten.extra.PeriodDuration;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class DataDurationTest {
    @Test
    void testSome() {
        String[] examples = {
                "29 days 23 hours 59 minutes 20 seconds",
                "4 years 10 months 10 seconds",
                "10 days 9 minutes",
                "1 month"
        };

        for (String pds : examples) {
            // If hours, minutes or seconds are present, put a T before them
            String temp = pds.replaceFirst("(\\d+ *(?:hour|minute|second))", " T $1");
            // Abbreviate all units to 1 letter; remove spaces
            temp = temp.replaceAll("([ymwdhms])[a-z]*", "$1").replace(" ", "");
            // Prepend P
            String iso = "P" + temp;
            System.out.println(iso);
            PeriodDuration pd = PeriodDuration.parse(iso);
            assertNotNull(pd);
        }
    }
}
