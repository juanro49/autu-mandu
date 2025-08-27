/*
 * Copyright 2025 Juanro49
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
package org.juanro.autumandu.provider.tirelist;

import androidx.annotation.NonNull;

import org.juanro.autumandu.provider.base.BaseModel;

import java.util.Date;

@Deprecated
public interface TireListModel extends BaseModel {
    /**
     * @return buy date of tires
     */
    @NonNull
    Date getBuyDate();

    /**
     * @return trash date of tires
     */
    Date getTrashDate();

    /**
     * @return price of tires
     */
    float getPrice();

    /**
     * @return price of tires
     */
    int getQuantity();

    /**
     * @return manufacturer of tires
     */
    @NonNull
    String getManufacturer();

    /**
     * @return model of tires
     */
    @NonNull
    String getModel();

    /**
     * @return note of tires
     */
    @NonNull
    String getNote();

    /**
     * Get the {@code car_id} value.
     */
    long getCarId();
}
