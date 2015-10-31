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

import java.util.Date;

import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import me.kuehle.carreport.provider.base.AbstractCursor;
import me.kuehle.carreport.provider.car.*;

/**
 * Cursor wrapper for the {@code other_cost} table.
 */
public class OtherCostCursor extends AbstractCursor implements OtherCostModel {
    public OtherCostCursor(Cursor cursor) {
        super(cursor);
    }

    /**
     * Primary key.
     */
    public long getId() {
        Long res = getLongOrNull(OtherCostColumns._ID);
        if (res == null)
            throw new NullPointerException("The value of '_id' in the database was null, which is not allowed according to the model definition");
        return res;
    }

    /**
     * Display title of the cost.
     * Cannot be {@code null}.
     */
    @NonNull
    public String getTitle() {
        String res = getStringOrNull(OtherCostColumns.TITLE);
        if (res == null)
            throw new NullPointerException("The value of 'title' in the database was null, which is not allowed according to the model definition");
        return res;
    }

    /**
     * Date on which the cost occured.
     * Cannot be {@code null}.
     */
    @NonNull
    public Date getDate() {
        Date res = getDateOrNull(OtherCostColumns.DATE);
        if (res == null)
            throw new NullPointerException("The value of 'date' in the database was null, which is not allowed according to the model definition");
        return res;
    }

    /**
     * Mileage on which the cost occured.
     * Can be {@code null}.
     */
    @Nullable
    public Integer getMileage() {
        Integer res = getIntegerOrNull(OtherCostColumns.MILEAGE);
        return res;
    }

    /**
     * The price of the cost. If it is an income, the price it negative.
     */
    public float getPrice() {
        Float res = getFloatOrNull(OtherCostColumns.PRICE);
        if (res == null)
            throw new NullPointerException("The value of 'price' in the database was null, which is not allowed according to the model definition");
        return res;
    }

    /**
     * Recurrence information. Together with the recurrence_multiplier this gives a recurrence like every 5 days.
     * Cannot be {@code null}.
     */
    @NonNull
    public RecurrenceInterval getRecurrenceInterval() {
        Integer intValue = getIntegerOrNull(OtherCostColumns.RECURRENCE_INTERVAL);
        if (intValue == null)
            throw new NullPointerException("The value of 'recurrence_interval' in the database was null, which is not allowed according to the model definition");
        return RecurrenceInterval.values()[intValue];
    }

    /**
     * Recurrence information. Together with the recurrence_interval this gives a recurrence like every 5 days.
     */
    public int getRecurrenceMultiplier() {
        Integer res = getIntegerOrNull(OtherCostColumns.RECURRENCE_MULTIPLIER);
        if (res == null)
            throw new NullPointerException("The value of 'recurrence_multiplier' in the database was null, which is not allowed according to the model definition");
        return res;
    }

    /**
     * Date on which the recurrence ends or null, if there is no known end date yet.
     * Can be {@code null}.
     */
    @Nullable
    public Date getEndDate() {
        Date res = getDateOrNull(OtherCostColumns.END_DATE);
        return res;
    }

    /**
     * A note for this cost. Just for display purposes.
     * Cannot be {@code null}.
     */
    @NonNull
    public String getNote() {
        String res = getStringOrNull(OtherCostColumns.NOTE);
        if (res == null)
            throw new NullPointerException("The value of 'note' in the database was null, which is not allowed according to the model definition");
        return res;
    }

    /**
     * Get the {@code car_id} value.
     */
    public long getCarId() {
        Long res = getLongOrNull(OtherCostColumns.CAR_ID);
        if (res == null)
            throw new NullPointerException("The value of 'car_id' in the database was null, which is not allowed according to the model definition");
        return res;
    }

    /**
     * Name of the car. Only for display purposes.
     * Cannot be {@code null}.
     */
    @NonNull
    public String getCarName() {
        String res = getStringOrNull(CarColumns.NAME);
        if (res == null)
            throw new NullPointerException("The value of 'name' in the database was null, which is not allowed according to the model definition");
        return res;
    }

    /**
     * Color of the car in android color representation.
     */
    public int getCarColor() {
        Integer res = getIntegerOrNull(CarColumns.COLOR);
        if (res == null)
            throw new NullPointerException("The value of 'color' in the database was null, which is not allowed according to the model definition");
        return res;
    }

    /**
     * Initial mileage of the car, when it starts to be used in the app.
     */
    public int getCarInitialMileage() {
        Integer res = getIntegerOrNull(CarColumns.INITIAL_MILEAGE);
        if (res == null)
            throw new NullPointerException("The value of 'initial_mileage' in the database was null, which is not allowed according to the model definition");
        return res;
    }

    /**
     * When the car has been suspended, this contains the start date.
     * Can be {@code null}.
     */
    @Nullable
    public Date getCarSuspendedSince() {
        Date res = getDateOrNull(CarColumns.SUSPENDED_SINCE);
        return res;
    }
}
