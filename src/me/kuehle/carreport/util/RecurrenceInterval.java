/*
 * Copyright 2012 Jan Kühle
 *
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
 */

package me.kuehle.carreport.util;

public enum RecurrenceInterval {
	ONCE(0), DAY(1), MONTH(2), QUARTER(3), YEAR(4);

    private int value;

    private RecurrenceInterval(int i) {
            value = i;
    }

    public int getValue() {
            return value;
    }

    public static RecurrenceInterval getByValue(int i) {
            switch (i) {
            case 1:
                    return DAY;
            case 2:
                    return MONTH;
            case 3:
                    return QUARTER;
            case 4:
                    return YEAR;
            default:
                    return ONCE;
            }
    }
}
