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
import android.database.Cursor;
import android.net.Uri;

import me.kuehle.carreport.provider.base.AbstractSelection;
import me.kuehle.carreport.provider.fueltype.*;
import me.kuehle.carreport.provider.car.*;

/**
 * Selection for the {@code refueling} table.
 */
public class RefuelingSelection extends AbstractSelection<RefuelingSelection> {
    @Override
    protected Uri baseUri() {
        return RefuelingColumns.CONTENT_URI;
    }

    /**
     * Query the given content resolver using this selection.
     *
     * @param contentResolver The content resolver to query.
     * @param projection A list of which columns to return. Passing null will return all columns, which is inefficient.
     * @param sortOrder How to order the rows, formatted as an SQL ORDER BY clause (excluding the ORDER BY itself). Passing null will use the default sort
     *            order, which may be unordered.
     * @return A {@code RefuelingCursor} object, which is positioned before the first entry, or null.
     */
    public RefuelingCursor query(ContentResolver contentResolver, String[] projection, String sortOrder) {
        Cursor cursor = contentResolver.query(uri(), projection, sel(), args(), sortOrder);
        if (cursor == null) return null;
        return new RefuelingCursor(cursor);
    }

    /**
     * Equivalent of calling {@code query(contentResolver, projection, null)}.
     */
    public RefuelingCursor query(ContentResolver contentResolver, String[] projection) {
        return query(contentResolver, projection, null);
    }

    /**
     * Equivalent of calling {@code query(contentResolver, projection, null, null)}.
     */
    public RefuelingCursor query(ContentResolver contentResolver) {
        return query(contentResolver, null, null);
    }


    public RefuelingSelection id(long... value) {
        addEquals("refueling." + RefuelingColumns._ID, toObjectArray(value));
        return this;
    }

    public RefuelingSelection date(Date... value) {
        addEquals(RefuelingColumns.DATE, value);
        return this;
    }

    public RefuelingSelection dateNot(Date... value) {
        addNotEquals(RefuelingColumns.DATE, value);
        return this;
    }

    public RefuelingSelection date(long... value) {
        addEquals(RefuelingColumns.DATE, toObjectArray(value));
        return this;
    }

    public RefuelingSelection dateAfter(Date value) {
        addGreaterThan(RefuelingColumns.DATE, value);
        return this;
    }

    public RefuelingSelection dateAfterEq(Date value) {
        addGreaterThanOrEquals(RefuelingColumns.DATE, value);
        return this;
    }

    public RefuelingSelection dateBefore(Date value) {
        addLessThan(RefuelingColumns.DATE, value);
        return this;
    }

    public RefuelingSelection dateBeforeEq(Date value) {
        addLessThanOrEquals(RefuelingColumns.DATE, value);
        return this;
    }

    public RefuelingSelection mileage(int... value) {
        addEquals(RefuelingColumns.MILEAGE, toObjectArray(value));
        return this;
    }

    public RefuelingSelection mileageNot(int... value) {
        addNotEquals(RefuelingColumns.MILEAGE, toObjectArray(value));
        return this;
    }

    public RefuelingSelection mileageGt(int value) {
        addGreaterThan(RefuelingColumns.MILEAGE, value);
        return this;
    }

    public RefuelingSelection mileageGtEq(int value) {
        addGreaterThanOrEquals(RefuelingColumns.MILEAGE, value);
        return this;
    }

    public RefuelingSelection mileageLt(int value) {
        addLessThan(RefuelingColumns.MILEAGE, value);
        return this;
    }

    public RefuelingSelection mileageLtEq(int value) {
        addLessThanOrEquals(RefuelingColumns.MILEAGE, value);
        return this;
    }

    public RefuelingSelection volume(float... value) {
        addEquals(RefuelingColumns.VOLUME, toObjectArray(value));
        return this;
    }

    public RefuelingSelection volumeNot(float... value) {
        addNotEquals(RefuelingColumns.VOLUME, toObjectArray(value));
        return this;
    }

    public RefuelingSelection volumeGt(float value) {
        addGreaterThan(RefuelingColumns.VOLUME, value);
        return this;
    }

    public RefuelingSelection volumeGtEq(float value) {
        addGreaterThanOrEquals(RefuelingColumns.VOLUME, value);
        return this;
    }

    public RefuelingSelection volumeLt(float value) {
        addLessThan(RefuelingColumns.VOLUME, value);
        return this;
    }

    public RefuelingSelection volumeLtEq(float value) {
        addLessThanOrEquals(RefuelingColumns.VOLUME, value);
        return this;
    }

    public RefuelingSelection price(float... value) {
        addEquals(RefuelingColumns.PRICE, toObjectArray(value));
        return this;
    }

    public RefuelingSelection priceNot(float... value) {
        addNotEquals(RefuelingColumns.PRICE, toObjectArray(value));
        return this;
    }

    public RefuelingSelection priceGt(float value) {
        addGreaterThan(RefuelingColumns.PRICE, value);
        return this;
    }

    public RefuelingSelection priceGtEq(float value) {
        addGreaterThanOrEquals(RefuelingColumns.PRICE, value);
        return this;
    }

    public RefuelingSelection priceLt(float value) {
        addLessThan(RefuelingColumns.PRICE, value);
        return this;
    }

    public RefuelingSelection priceLtEq(float value) {
        addLessThanOrEquals(RefuelingColumns.PRICE, value);
        return this;
    }

    public RefuelingSelection partial(boolean value) {
        addEquals(RefuelingColumns.PARTIAL, toObjectArray(value));
        return this;
    }

    public RefuelingSelection note(String... value) {
        addEquals(RefuelingColumns.NOTE, value);
        return this;
    }

    public RefuelingSelection noteNot(String... value) {
        addNotEquals(RefuelingColumns.NOTE, value);
        return this;
    }

    public RefuelingSelection noteLike(String... value) {
        addLike(RefuelingColumns.NOTE, value);
        return this;
    }

    public RefuelingSelection noteContains(String... value) {
        addContains(RefuelingColumns.NOTE, value);
        return this;
    }

    public RefuelingSelection noteStartsWith(String... value) {
        addStartsWith(RefuelingColumns.NOTE, value);
        return this;
    }

    public RefuelingSelection noteEndsWith(String... value) {
        addEndsWith(RefuelingColumns.NOTE, value);
        return this;
    }

    public RefuelingSelection fuelTypeId(long... value) {
        addEquals(RefuelingColumns.FUEL_TYPE_ID, toObjectArray(value));
        return this;
    }

    public RefuelingSelection fuelTypeIdNot(long... value) {
        addNotEquals(RefuelingColumns.FUEL_TYPE_ID, toObjectArray(value));
        return this;
    }

    public RefuelingSelection fuelTypeIdGt(long value) {
        addGreaterThan(RefuelingColumns.FUEL_TYPE_ID, value);
        return this;
    }

    public RefuelingSelection fuelTypeIdGtEq(long value) {
        addGreaterThanOrEquals(RefuelingColumns.FUEL_TYPE_ID, value);
        return this;
    }

    public RefuelingSelection fuelTypeIdLt(long value) {
        addLessThan(RefuelingColumns.FUEL_TYPE_ID, value);
        return this;
    }

    public RefuelingSelection fuelTypeIdLtEq(long value) {
        addLessThanOrEquals(RefuelingColumns.FUEL_TYPE_ID, value);
        return this;
    }

    public RefuelingSelection fuelTypeName(String... value) {
        addEquals(FuelTypeColumns.NAME, value);
        return this;
    }

    public RefuelingSelection fuelTypeNameNot(String... value) {
        addNotEquals(FuelTypeColumns.NAME, value);
        return this;
    }

    public RefuelingSelection fuelTypeNameLike(String... value) {
        addLike(FuelTypeColumns.NAME, value);
        return this;
    }

    public RefuelingSelection fuelTypeNameContains(String... value) {
        addContains(FuelTypeColumns.NAME, value);
        return this;
    }

    public RefuelingSelection fuelTypeNameStartsWith(String... value) {
        addStartsWith(FuelTypeColumns.NAME, value);
        return this;
    }

    public RefuelingSelection fuelTypeNameEndsWith(String... value) {
        addEndsWith(FuelTypeColumns.NAME, value);
        return this;
    }

    public RefuelingSelection fuelTypeCategory(String... value) {
        addEquals(FuelTypeColumns.CATEGORY, value);
        return this;
    }

    public RefuelingSelection fuelTypeCategoryNot(String... value) {
        addNotEquals(FuelTypeColumns.CATEGORY, value);
        return this;
    }

    public RefuelingSelection fuelTypeCategoryLike(String... value) {
        addLike(FuelTypeColumns.CATEGORY, value);
        return this;
    }

    public RefuelingSelection fuelTypeCategoryContains(String... value) {
        addContains(FuelTypeColumns.CATEGORY, value);
        return this;
    }

    public RefuelingSelection fuelTypeCategoryStartsWith(String... value) {
        addStartsWith(FuelTypeColumns.CATEGORY, value);
        return this;
    }

    public RefuelingSelection fuelTypeCategoryEndsWith(String... value) {
        addEndsWith(FuelTypeColumns.CATEGORY, value);
        return this;
    }

    public RefuelingSelection carId(long... value) {
        addEquals(RefuelingColumns.CAR_ID, toObjectArray(value));
        return this;
    }

    public RefuelingSelection carIdNot(long... value) {
        addNotEquals(RefuelingColumns.CAR_ID, toObjectArray(value));
        return this;
    }

    public RefuelingSelection carIdGt(long value) {
        addGreaterThan(RefuelingColumns.CAR_ID, value);
        return this;
    }

    public RefuelingSelection carIdGtEq(long value) {
        addGreaterThanOrEquals(RefuelingColumns.CAR_ID, value);
        return this;
    }

    public RefuelingSelection carIdLt(long value) {
        addLessThan(RefuelingColumns.CAR_ID, value);
        return this;
    }

    public RefuelingSelection carIdLtEq(long value) {
        addLessThanOrEquals(RefuelingColumns.CAR_ID, value);
        return this;
    }

    public RefuelingSelection carName(String... value) {
        addEquals(CarColumns.NAME, value);
        return this;
    }

    public RefuelingSelection carNameNot(String... value) {
        addNotEquals(CarColumns.NAME, value);
        return this;
    }

    public RefuelingSelection carNameLike(String... value) {
        addLike(CarColumns.NAME, value);
        return this;
    }

    public RefuelingSelection carNameContains(String... value) {
        addContains(CarColumns.NAME, value);
        return this;
    }

    public RefuelingSelection carNameStartsWith(String... value) {
        addStartsWith(CarColumns.NAME, value);
        return this;
    }

    public RefuelingSelection carNameEndsWith(String... value) {
        addEndsWith(CarColumns.NAME, value);
        return this;
    }

    public RefuelingSelection carColor(int... value) {
        addEquals(CarColumns.COLOR, toObjectArray(value));
        return this;
    }

    public RefuelingSelection carColorNot(int... value) {
        addNotEquals(CarColumns.COLOR, toObjectArray(value));
        return this;
    }

    public RefuelingSelection carColorGt(int value) {
        addGreaterThan(CarColumns.COLOR, value);
        return this;
    }

    public RefuelingSelection carColorGtEq(int value) {
        addGreaterThanOrEquals(CarColumns.COLOR, value);
        return this;
    }

    public RefuelingSelection carColorLt(int value) {
        addLessThan(CarColumns.COLOR, value);
        return this;
    }

    public RefuelingSelection carColorLtEq(int value) {
        addLessThanOrEquals(CarColumns.COLOR, value);
        return this;
    }

    public RefuelingSelection carInitialMileage(int... value) {
        addEquals(CarColumns.INITIAL_MILEAGE, toObjectArray(value));
        return this;
    }

    public RefuelingSelection carInitialMileageNot(int... value) {
        addNotEquals(CarColumns.INITIAL_MILEAGE, toObjectArray(value));
        return this;
    }

    public RefuelingSelection carInitialMileageGt(int value) {
        addGreaterThan(CarColumns.INITIAL_MILEAGE, value);
        return this;
    }

    public RefuelingSelection carInitialMileageGtEq(int value) {
        addGreaterThanOrEquals(CarColumns.INITIAL_MILEAGE, value);
        return this;
    }

    public RefuelingSelection carInitialMileageLt(int value) {
        addLessThan(CarColumns.INITIAL_MILEAGE, value);
        return this;
    }

    public RefuelingSelection carInitialMileageLtEq(int value) {
        addLessThanOrEquals(CarColumns.INITIAL_MILEAGE, value);
        return this;
    }

    public RefuelingSelection carSuspendedSince(Date... value) {
        addEquals(CarColumns.SUSPENDED_SINCE, value);
        return this;
    }

    public RefuelingSelection carSuspendedSinceNot(Date... value) {
        addNotEquals(CarColumns.SUSPENDED_SINCE, value);
        return this;
    }

    public RefuelingSelection carSuspendedSince(Long... value) {
        addEquals(CarColumns.SUSPENDED_SINCE, value);
        return this;
    }

    public RefuelingSelection carSuspendedSinceAfter(Date value) {
        addGreaterThan(CarColumns.SUSPENDED_SINCE, value);
        return this;
    }

    public RefuelingSelection carSuspendedSinceAfterEq(Date value) {
        addGreaterThanOrEquals(CarColumns.SUSPENDED_SINCE, value);
        return this;
    }

    public RefuelingSelection carSuspendedSinceBefore(Date value) {
        addLessThan(CarColumns.SUSPENDED_SINCE, value);
        return this;
    }

    public RefuelingSelection carSuspendedSinceBeforeEq(Date value) {
        addLessThanOrEquals(CarColumns.SUSPENDED_SINCE, value);
        return this;
    }
}
