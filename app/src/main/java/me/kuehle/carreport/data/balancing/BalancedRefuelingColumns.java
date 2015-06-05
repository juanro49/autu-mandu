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
package me.kuehle.carreport.data.balancing;

import me.kuehle.carreport.provider.refueling.RefuelingColumns;

public class BalancedRefuelingColumns extends RefuelingColumns {
    /**
     * Indicates if the refueling entry is valid.
     */
    public static final String VALID = "valid";

    /**
     * Indicates if the refueling entry has been guessed by the balancing engine.
     */
    public static final String GUESSED = "guessed";


    // @formatter:off
    public static final String[] ALL_COLUMNS = new String[] {
            _ID,
            DATE,
            MILEAGE,
            VOLUME,
            PRICE,
            PARTIAL,
            NOTE,
            VALID,
            GUESSED,
            FUEL_TYPE_ID,
            CAR_ID,
    };
    // @formatter:on
}
