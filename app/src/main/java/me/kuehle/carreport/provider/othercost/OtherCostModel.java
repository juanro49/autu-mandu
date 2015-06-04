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
package me.kuehle.carreport.provider.othercost;

import me.kuehle.carreport.provider.base.BaseModel;

import java.util.Date;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * A cost for a car, that is not a refueling. Can also be an income, in which case the price is negative.
 */
public interface OtherCostModel extends BaseModel {

    /**
     * Display title of the cost.
     * Cannot be {@code null}.
     */
    @NonNull
    String getTitle();

    /**
     * Date on which the cost occured.
     * Cannot be {@code null}.
     */
    @NonNull
    Date getDate();

    /**
     * Mileage on which the cost occured.
     * Can be {@code null}.
     */
    @Nullable
    Integer getMileage();

    /**
     * The price of the cost. If it is an income, the price it negative.
     */
    float getPrice();

    /**
     * Recurrence information. Together with the recurrence_multiplier this gives a recurrence like every 5 days.
     * Cannot be {@code null}.
     */
    @NonNull
    RecurrenceInterval getRecurrenceInterval();

    /**
     * Recurrence information. Together with the recurrence_interval this gives a recurrence like every 5 days.
     */
    int getRecurrenceMultiplier();

    /**
     * Date on which the recurrence ends or null, if there is no known end date yet.
     * Can be {@code null}.
     */
    @Nullable
    Date getEndDate();

    /**
     * A note for this cost. Just for display purposes.
     * Cannot be {@code null}.
     */
    @NonNull
    String getNote();

    /**
     * Get the {@code car_id} value.
     */
    long getCarId();
}
