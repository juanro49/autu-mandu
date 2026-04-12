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
package org.juanro.autumandu;

import androidx.annotation.NonNull;

/**
 * Modes for entering refueling prices.
 */
public enum PriceEntryMode {
    PER_UNIT_AND_VOLUME(0, R.string.price_entry_mode_per_unit_and_volume),
    PER_UNIT_AND_TOTAL(1, R.string.price_entry_mode_per_unit_and_total),
    TOTAL_AND_VOLUME(2, R.string.price_entry_mode_total_and_volume);

    private final int id;
    private final int nameResourceId;

    PriceEntryMode(int id, int nameResourceId) {
        this.id = id;
        this.nameResourceId = nameResourceId;
    }

    public int getId() {
        return id;
    }

    public int getNameResourceId() {
        return nameResourceId;
    }

    /**
     * Resolves the mode from its ID.
     * Uses Java 21 Switch Expression for cleaner logic.
     */
    @NonNull
    public static PriceEntryMode fromId(int id) {
        return switch (id) {
            case 1 -> PER_UNIT_AND_TOTAL;
            case 2 -> TOTAL_AND_VOLUME;
            default -> PER_UNIT_AND_VOLUME;
        };
    }
}
