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

public enum PriceEntryMode {
    PER_UNIT_AND_VOLUME(R.string.price_entry_mode_per_unit_and_volume),
    PER_UNIT_AND_TOTAL(R.string.price_entry_mode_per_unit_and_total),
    TOTAL_AND_VOLUME(R.string.price_entry_mode_total_and_volume);

    public final int nameResourceId;

    PriceEntryMode(int nameResourceId) {
        this.nameResourceId = nameResourceId;
    }
}
