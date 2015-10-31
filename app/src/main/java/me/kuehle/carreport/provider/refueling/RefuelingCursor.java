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

import java.util.Date;

import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import me.kuehle.carreport.provider.base.AbstractCursor;
import me.kuehle.carreport.provider.fueltype.*;
import me.kuehle.carreport.provider.car.*;

/**
 * Cursor wrapper for the {@code refueling} table.
 */
public class RefuelingCursor extends AbstractCursor implements RefuelingModel {
    public RefuelingCursor(Cursor cursor) {
        super(cursor);
    }

    /**
     * Primary key.
     */
    public long getId() {
        Long res = getLongOrNull(RefuelingColumns._ID);
        if (res == null)
            throw new NullPointerException("The value of '_id' in the database was null, which is not allowed according to the model definition");
        return res;
    }

    /**
     * Date on which the refueling occured.
     * Cannot be {@code null}.
     */
    @NonNull
    public Date getDate() {
        Date res = getDateOrNull(RefuelingColumns.DATE);
        if (res == null)
            throw new NullPointerException("The value of 'date' in the database was null, which is not allowed according to the model definition");
        return res;
    }

    /**
     * Mileage on which the refueling occured.
     */
    public int getMileage() {
        Integer res = getIntegerOrNull(RefuelingColumns.MILEAGE);
        if (res == null)
            throw new NullPointerException("The value of 'mileage' in the database was null, which is not allowed according to the model definition");
        return res;
    }

    /**
     * The amount of fuel, that was refilled.
     */
    public float getVolume() {
        Float res = getFloatOrNull(RefuelingColumns.VOLUME);
        if (res == null)
            throw new NullPointerException("The value of 'volume' in the database was null, which is not allowed according to the model definition");
        return res;
    }

    /**
     * The price of the refueling.
     */
    public float getPrice() {
        Float res = getFloatOrNull(RefuelingColumns.PRICE);
        if (res == null)
            throw new NullPointerException("The value of 'price' in the database was null, which is not allowed according to the model definition");
        return res;
    }

    /**
     * Indicates if the tank was filled completly or only partially.
     */
    public boolean getPartial() {
        Boolean res = getBooleanOrNull(RefuelingColumns.PARTIAL);
        if (res == null)
            throw new NullPointerException("The value of 'partial' in the database was null, which is not allowed according to the model definition");
        return res;
    }

    /**
     * A note for this cost. Just for display purposes.
     * Cannot be {@code null}.
     */
    @NonNull
    public String getNote() {
        String res = getStringOrNull(RefuelingColumns.NOTE);
        if (res == null)
            throw new NullPointerException("The value of 'note' in the database was null, which is not allowed according to the model definition");
        return res;
    }

    /**
     * Get the {@code fuel_type_id} value.
     */
    public long getFuelTypeId() {
        Long res = getLongOrNull(RefuelingColumns.FUEL_TYPE_ID);
        if (res == null)
            throw new NullPointerException("The value of 'fuel_type_id' in the database was null, which is not allowed according to the model definition");
        return res;
    }

    /**
     * Name of the fuel type, e.g. Diesel.
     * Cannot be {@code null}.
     */
    @NonNull
    public String getFuelTypeName() {
        String res = getStringOrNull(FuelTypeColumns.NAME);
        if (res == null)
            throw new NullPointerException("The value of 'name' in the database was null, which is not allowed according to the model definition");
        return res;
    }

    /**
     * An optional category like fuel or gas. Fuel types may be grouped by this category in reports.
     * Can be {@code null}.
     */
    @Nullable
    public String getFuelTypeCategory() {
        String res = getStringOrNull(FuelTypeColumns.CATEGORY);
        return res;
    }

    /**
     * Get the {@code car_id} value.
     */
    public long getCarId() {
        Long res = getLongOrNull(RefuelingColumns.CAR_ID);
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
