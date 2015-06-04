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

import android.content.ContentResolver;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import me.kuehle.carreport.provider.base.AbstractContentValues;

/**
 * Content values wrapper for the {@code refueling} table.
 */
public class RefuelingContentValues extends AbstractContentValues {
    @Override
    public Uri uri() {
        return RefuelingColumns.CONTENT_URI;
    }

    /**
     * Update row(s) using the values stored by this object and the given selection.
     *
     * @param contentResolver The content resolver to use.
     * @param where The selection to use (can be {@code null}).
     */
    public int update(ContentResolver contentResolver, @Nullable RefuelingSelection where) {
        return contentResolver.update(uri(), values(), where == null ? null : where.sel(), where == null ? null : where.args());
    }

    /**
     * Date on which the refueling occured.
     */
    public RefuelingContentValues putDate(@NonNull Date value) {
        if (value == null) throw new IllegalArgumentException("date must not be null");
        mContentValues.put(RefuelingColumns.DATE, value.getTime());
        return this;
    }


    public RefuelingContentValues putDate(long value) {
        mContentValues.put(RefuelingColumns.DATE, value);
        return this;
    }

    /**
     * Mileage on which the refueling occured.
     */
    public RefuelingContentValues putMileage(int value) {
        mContentValues.put(RefuelingColumns.MILEAGE, value);
        return this;
    }


    /**
     * The amount of fuel, that was refilled.
     */
    public RefuelingContentValues putVolume(float value) {
        mContentValues.put(RefuelingColumns.VOLUME, value);
        return this;
    }


    /**
     * The price of the refueling.
     */
    public RefuelingContentValues putPrice(float value) {
        mContentValues.put(RefuelingColumns.PRICE, value);
        return this;
    }


    /**
     * Indicates if the tank was filled completly or only partially.
     */
    public RefuelingContentValues putPartial(boolean value) {
        mContentValues.put(RefuelingColumns.PARTIAL, value);
        return this;
    }


    /**
     * A note for this cost. Just for display purposes.
     */
    public RefuelingContentValues putNote(@NonNull String value) {
        if (value == null) throw new IllegalArgumentException("note must not be null");
        mContentValues.put(RefuelingColumns.NOTE, value);
        return this;
    }


    public RefuelingContentValues putFuelTypeId(long value) {
        mContentValues.put(RefuelingColumns.FUEL_TYPE_ID, value);
        return this;
    }


    public RefuelingContentValues putCarId(long value) {
        mContentValues.put(RefuelingColumns.CAR_ID, value);
        return this;
    }

}
