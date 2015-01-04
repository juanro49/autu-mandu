/*
 * Copyright 2015 Jan Kühle
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package me.kuehle.carreport.util;

public enum TimeSpanUnit {
    DAY(0), MONTH(1), YEAR(2);

    private int value;

    private TimeSpanUnit(int i) {
        value = i;
    }

    public int getValue() {
        return value;
    }

    public static TimeSpanUnit getByValue(int i) {
        switch (i) {
            case 0:
                return DAY;
            case 1:
                return MONTH;
            case 2:
                return YEAR;
            default:
                return DAY;
        }
    }
}
