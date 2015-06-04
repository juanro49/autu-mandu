/*
 * Copyright 2015 Jan Kühle
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
package me.kuehle.carreport.provider.refueling;

import me.kuehle.carreport.provider.base.BaseModel;

import java.util.Date;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * A refueling for a car.
 */
public interface RefuelingModel extends BaseModel {

    /**
     * Date on which the refueling occured.
     * Cannot be {@code null}.
     */
    @NonNull
    Date getDate();

    /**
     * Mileage on which the refueling occured.
     */
    int getMileage();

    /**
     * The amount of fuel, that was refilled.
     */
    float getVolume();

    /**
     * The price of the refueling.
     */
    float getPrice();

    /**
     * Indicates if the tank was filled completly or only partially.
     */
    boolean getPartial();

    /**
     * A note for this cost. Just for display purposes.
     * Cannot be {@code null}.
     */
    @NonNull
    String getNote();

    /**
     * Get the {@code fuel_type_id} value.
     */
    long getFuelTypeId();

    /**
     * Get the {@code car_id} value.
     */
    long getCarId();
}
