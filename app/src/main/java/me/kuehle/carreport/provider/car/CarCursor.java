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
package me.kuehle.carreport.provider.car;

import java.util.Date;

import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import me.kuehle.carreport.provider.base.AbstractCursor;

/**
 * Cursor wrapper for the {@code car} table.
 */
public class CarCursor extends AbstractCursor implements CarModel {
    public CarCursor(Cursor cursor) {
        super(cursor);
    }

    /**
     * Primary key.
     */
    public long getId() {
        Long res = getLongOrNull(CarColumns._ID);
        if (res == null)
            throw new NullPointerException("The value of '_id' in the database was null, which is not allowed according to the model definition");
        return res;
    }

    /**
     * Name of the car. Only for display purposes.
     * Cannot be {@code null}.
     */
    @NonNull
    public String getName() {
        String res = getStringOrNull(CarColumns.NAME);
        if (res == null)
            throw new NullPointerException("The value of 'name' in the database was null, which is not allowed according to the model definition");
        return res;
    }

    /**
     * Color of the car in android color representation.
     */
    public int getColor() {
        Integer res = getIntegerOrNull(CarColumns.COLOR);
        if (res == null)
            throw new NullPointerException("The value of 'color' in the database was null, which is not allowed according to the model definition");
        return res;
    }

    /**
     * Initial mileage of the car, when it starts to be used in the app.
     */
    public int getInitialMileage() {
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
    public Date getSuspendedSince() {
        Date res = getDateOrNull(CarColumns.SUSPENDED_SINCE);
        return res;
    }
}
