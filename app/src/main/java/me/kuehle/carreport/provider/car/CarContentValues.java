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

import android.content.ContentResolver;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import me.kuehle.carreport.provider.base.AbstractContentValues;

/**
 * Content values wrapper for the {@code car} table.
 */
@Deprecated
public class CarContentValues extends AbstractContentValues {
    @Override
    public Uri uri() {
        return CarColumns.CONTENT_URI;
    }

    /**
     * Update row(s) using the values stored by this object and the given selection.
     *
     * @param contentResolver The content resolver to use.
     * @param where The selection to use (can be {@code null}).
     */
    public int update(ContentResolver contentResolver, @Nullable CarSelection where) {
        return contentResolver.update(uri(), values(), where == null ? null : where.sel(), where == null ? null : where.args());
    }

    /**
     * Name of the car. Only for display purposes.
     */
    public CarContentValues putName(@NonNull String value) {
        if (value == null) throw new IllegalArgumentException("name must not be null");
        mContentValues.put(CarColumns.NAME, value);
        return this;
    }


    /**
     * Color of the car in android color representation.
     */
    public CarContentValues putColor(int value) {
        mContentValues.put(CarColumns.COLOR, value);
        return this;
    }


    /**
     * Initial mileage of the car, when it starts to be used in the app.
     */
    public CarContentValues putInitialMileage(int value) {
        mContentValues.put(CarColumns.INITIAL_MILEAGE, value);
        return this;
    }


    /**
     * When the car has been suspended, this contains the start date.
     */
    public CarContentValues putSuspendedSince(@Nullable Date value) {
        mContentValues.put(CarColumns.SUSPENDED_SINCE, value == null ? null : value.getTime());
        return this;
    }

    public CarContentValues putSuspendedSinceNull() {
        mContentValues.putNull(CarColumns.SUSPENDED_SINCE);
        return this;
    }

    public CarContentValues putSuspendedSince(@Nullable Long value) {
        mContentValues.put(CarColumns.SUSPENDED_SINCE, value);
        return this;
    }

    /**
     * Buying price of the car.
     */
    public CarContentValues putBuyingPrice(double value) {
        mContentValues.put(CarColumns.BUYING_PRICE, value);
        return this;
    }

    /**
     * Make of the car.
     */
    /*public CarContentValues putMake(String value) {
        mContentValues.put(CarColumns.MAKE, value);
        return this;
    }*/

    /**
     * Model of the car.
     */
    /*public CarContentValues putModel(String value) {
        mContentValues.put(CarColumns.MODEL, value);
        return this;
    }*/

    /**
     * Year of the car.
     */
    /*public CarContentValues putYear(int value) {
        mContentValues.put(CarColumns.YEAR, value);
        return this;
    }*/

    /**
     * License plate of the car.
     */
    /*public CarContentValues putLicensePlate(String value) {
        mContentValues.put(CarColumns.LICENSE_PLATE, value);
        return this;
    }*/

    /**
     * Buying date of the car.
     */
    /*public CarContentValues putBuyingDate(@Nullable Date value) {
        mContentValues.put(CarColumns.BUYING_DATE, value == null ? null : value.getTime());
        return this;
    }*/
}
