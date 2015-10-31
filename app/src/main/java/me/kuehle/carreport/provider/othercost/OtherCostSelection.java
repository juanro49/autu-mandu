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

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;

import me.kuehle.carreport.provider.base.AbstractSelection;
import me.kuehle.carreport.provider.car.*;

/**
 * Selection for the {@code other_cost} table.
 */
public class OtherCostSelection extends AbstractSelection<OtherCostSelection> {
    @Override
    protected Uri baseUri() {
        return OtherCostColumns.CONTENT_URI;
    }

    /**
     * Query the given content resolver using this selection.
     *
     * @param contentResolver The content resolver to query.
     * @param projection A list of which columns to return. Passing null will return all columns, which is inefficient.
     * @param sortOrder How to order the rows, formatted as an SQL ORDER BY clause (excluding the ORDER BY itself). Passing null will use the default sort
     *            order, which may be unordered.
     * @return A {@code OtherCostCursor} object, which is positioned before the first entry, or null.
     */
    public OtherCostCursor query(ContentResolver contentResolver, String[] projection, String sortOrder) {
        Cursor cursor = contentResolver.query(uri(), projection, sel(), args(), sortOrder);
        if (cursor == null) return null;
        return new OtherCostCursor(cursor);
    }

    /**
     * Equivalent of calling {@code query(contentResolver, projection, null)}.
     */
    public OtherCostCursor query(ContentResolver contentResolver, String[] projection) {
        return query(contentResolver, projection, null);
    }

    /**
     * Equivalent of calling {@code query(contentResolver, projection, null, null)}.
     */
    public OtherCostCursor query(ContentResolver contentResolver) {
        return query(contentResolver, null, null);
    }


    public OtherCostSelection id(long... value) {
        addEquals("other_cost." + OtherCostColumns._ID, toObjectArray(value));
        return this;
    }

    public OtherCostSelection title(String... value) {
        addEquals(OtherCostColumns.TITLE, value);
        return this;
    }

    public OtherCostSelection titleNot(String... value) {
        addNotEquals(OtherCostColumns.TITLE, value);
        return this;
    }

    public OtherCostSelection titleLike(String... value) {
        addLike(OtherCostColumns.TITLE, value);
        return this;
    }

    public OtherCostSelection titleContains(String... value) {
        addContains(OtherCostColumns.TITLE, value);
        return this;
    }

    public OtherCostSelection titleStartsWith(String... value) {
        addStartsWith(OtherCostColumns.TITLE, value);
        return this;
    }

    public OtherCostSelection titleEndsWith(String... value) {
        addEndsWith(OtherCostColumns.TITLE, value);
        return this;
    }

    public OtherCostSelection date(Date... value) {
        addEquals(OtherCostColumns.DATE, value);
        return this;
    }

    public OtherCostSelection dateNot(Date... value) {
        addNotEquals(OtherCostColumns.DATE, value);
        return this;
    }

    public OtherCostSelection date(long... value) {
        addEquals(OtherCostColumns.DATE, toObjectArray(value));
        return this;
    }

    public OtherCostSelection dateAfter(Date value) {
        addGreaterThan(OtherCostColumns.DATE, value);
        return this;
    }

    public OtherCostSelection dateAfterEq(Date value) {
        addGreaterThanOrEquals(OtherCostColumns.DATE, value);
        return this;
    }

    public OtherCostSelection dateBefore(Date value) {
        addLessThan(OtherCostColumns.DATE, value);
        return this;
    }

    public OtherCostSelection dateBeforeEq(Date value) {
        addLessThanOrEquals(OtherCostColumns.DATE, value);
        return this;
    }

    public OtherCostSelection mileage(Integer... value) {
        addEquals(OtherCostColumns.MILEAGE, value);
        return this;
    }

    public OtherCostSelection mileageNot(Integer... value) {
        addNotEquals(OtherCostColumns.MILEAGE, value);
        return this;
    }

    public OtherCostSelection mileageGt(int value) {
        addGreaterThan(OtherCostColumns.MILEAGE, value);
        return this;
    }

    public OtherCostSelection mileageGtEq(int value) {
        addGreaterThanOrEquals(OtherCostColumns.MILEAGE, value);
        return this;
    }

    public OtherCostSelection mileageLt(int value) {
        addLessThan(OtherCostColumns.MILEAGE, value);
        return this;
    }

    public OtherCostSelection mileageLtEq(int value) {
        addLessThanOrEquals(OtherCostColumns.MILEAGE, value);
        return this;
    }

    public OtherCostSelection price(float... value) {
        addEquals(OtherCostColumns.PRICE, toObjectArray(value));
        return this;
    }

    public OtherCostSelection priceNot(float... value) {
        addNotEquals(OtherCostColumns.PRICE, toObjectArray(value));
        return this;
    }

    public OtherCostSelection priceGt(float value) {
        addGreaterThan(OtherCostColumns.PRICE, value);
        return this;
    }

    public OtherCostSelection priceGtEq(float value) {
        addGreaterThanOrEquals(OtherCostColumns.PRICE, value);
        return this;
    }

    public OtherCostSelection priceLt(float value) {
        addLessThan(OtherCostColumns.PRICE, value);
        return this;
    }

    public OtherCostSelection priceLtEq(float value) {
        addLessThanOrEquals(OtherCostColumns.PRICE, value);
        return this;
    }

    public OtherCostSelection recurrenceInterval(RecurrenceInterval... value) {
        addEquals(OtherCostColumns.RECURRENCE_INTERVAL, value);
        return this;
    }

    public OtherCostSelection recurrenceIntervalNot(RecurrenceInterval... value) {
        addNotEquals(OtherCostColumns.RECURRENCE_INTERVAL, value);
        return this;
    }


    public OtherCostSelection recurrenceMultiplier(int... value) {
        addEquals(OtherCostColumns.RECURRENCE_MULTIPLIER, toObjectArray(value));
        return this;
    }

    public OtherCostSelection recurrenceMultiplierNot(int... value) {
        addNotEquals(OtherCostColumns.RECURRENCE_MULTIPLIER, toObjectArray(value));
        return this;
    }

    public OtherCostSelection recurrenceMultiplierGt(int value) {
        addGreaterThan(OtherCostColumns.RECURRENCE_MULTIPLIER, value);
        return this;
    }

    public OtherCostSelection recurrenceMultiplierGtEq(int value) {
        addGreaterThanOrEquals(OtherCostColumns.RECURRENCE_MULTIPLIER, value);
        return this;
    }

    public OtherCostSelection recurrenceMultiplierLt(int value) {
        addLessThan(OtherCostColumns.RECURRENCE_MULTIPLIER, value);
        return this;
    }

    public OtherCostSelection recurrenceMultiplierLtEq(int value) {
        addLessThanOrEquals(OtherCostColumns.RECURRENCE_MULTIPLIER, value);
        return this;
    }

    public OtherCostSelection endDate(Date... value) {
        addEquals(OtherCostColumns.END_DATE, value);
        return this;
    }

    public OtherCostSelection endDateNot(Date... value) {
        addNotEquals(OtherCostColumns.END_DATE, value);
        return this;
    }

    public OtherCostSelection endDate(Long... value) {
        addEquals(OtherCostColumns.END_DATE, value);
        return this;
    }

    public OtherCostSelection endDateAfter(Date value) {
        addGreaterThan(OtherCostColumns.END_DATE, value);
        return this;
    }

    public OtherCostSelection endDateAfterEq(Date value) {
        addGreaterThanOrEquals(OtherCostColumns.END_DATE, value);
        return this;
    }

    public OtherCostSelection endDateBefore(Date value) {
        addLessThan(OtherCostColumns.END_DATE, value);
        return this;
    }

    public OtherCostSelection endDateBeforeEq(Date value) {
        addLessThanOrEquals(OtherCostColumns.END_DATE, value);
        return this;
    }

    public OtherCostSelection note(String... value) {
        addEquals(OtherCostColumns.NOTE, value);
        return this;
    }

    public OtherCostSelection noteNot(String... value) {
        addNotEquals(OtherCostColumns.NOTE, value);
        return this;
    }

    public OtherCostSelection noteLike(String... value) {
        addLike(OtherCostColumns.NOTE, value);
        return this;
    }

    public OtherCostSelection noteContains(String... value) {
        addContains(OtherCostColumns.NOTE, value);
        return this;
    }

    public OtherCostSelection noteStartsWith(String... value) {
        addStartsWith(OtherCostColumns.NOTE, value);
        return this;
    }

    public OtherCostSelection noteEndsWith(String... value) {
        addEndsWith(OtherCostColumns.NOTE, value);
        return this;
    }

    public OtherCostSelection carId(long... value) {
        addEquals(OtherCostColumns.CAR_ID, toObjectArray(value));
        return this;
    }

    public OtherCostSelection carIdNot(long... value) {
        addNotEquals(OtherCostColumns.CAR_ID, toObjectArray(value));
        return this;
    }

    public OtherCostSelection carIdGt(long value) {
        addGreaterThan(OtherCostColumns.CAR_ID, value);
        return this;
    }

    public OtherCostSelection carIdGtEq(long value) {
        addGreaterThanOrEquals(OtherCostColumns.CAR_ID, value);
        return this;
    }

    public OtherCostSelection carIdLt(long value) {
        addLessThan(OtherCostColumns.CAR_ID, value);
        return this;
    }

    public OtherCostSelection carIdLtEq(long value) {
        addLessThanOrEquals(OtherCostColumns.CAR_ID, value);
        return this;
    }

    public OtherCostSelection carName(String... value) {
        addEquals(CarColumns.NAME, value);
        return this;
    }

    public OtherCostSelection carNameNot(String... value) {
        addNotEquals(CarColumns.NAME, value);
        return this;
    }

    public OtherCostSelection carNameLike(String... value) {
        addLike(CarColumns.NAME, value);
        return this;
    }

    public OtherCostSelection carNameContains(String... value) {
        addContains(CarColumns.NAME, value);
        return this;
    }

    public OtherCostSelection carNameStartsWith(String... value) {
        addStartsWith(CarColumns.NAME, value);
        return this;
    }

    public OtherCostSelection carNameEndsWith(String... value) {
        addEndsWith(CarColumns.NAME, value);
        return this;
    }

    public OtherCostSelection carColor(int... value) {
        addEquals(CarColumns.COLOR, toObjectArray(value));
        return this;
    }

    public OtherCostSelection carColorNot(int... value) {
        addNotEquals(CarColumns.COLOR, toObjectArray(value));
        return this;
    }

    public OtherCostSelection carColorGt(int value) {
        addGreaterThan(CarColumns.COLOR, value);
        return this;
    }

    public OtherCostSelection carColorGtEq(int value) {
        addGreaterThanOrEquals(CarColumns.COLOR, value);
        return this;
    }

    public OtherCostSelection carColorLt(int value) {
        addLessThan(CarColumns.COLOR, value);
        return this;
    }

    public OtherCostSelection carColorLtEq(int value) {
        addLessThanOrEquals(CarColumns.COLOR, value);
        return this;
    }

    public OtherCostSelection carInitialMileage(int... value) {
        addEquals(CarColumns.INITIAL_MILEAGE, toObjectArray(value));
        return this;
    }

    public OtherCostSelection carInitialMileageNot(int... value) {
        addNotEquals(CarColumns.INITIAL_MILEAGE, toObjectArray(value));
        return this;
    }

    public OtherCostSelection carInitialMileageGt(int value) {
        addGreaterThan(CarColumns.INITIAL_MILEAGE, value);
        return this;
    }

    public OtherCostSelection carInitialMileageGtEq(int value) {
        addGreaterThanOrEquals(CarColumns.INITIAL_MILEAGE, value);
        return this;
    }

    public OtherCostSelection carInitialMileageLt(int value) {
        addLessThan(CarColumns.INITIAL_MILEAGE, value);
        return this;
    }

    public OtherCostSelection carInitialMileageLtEq(int value) {
        addLessThanOrEquals(CarColumns.INITIAL_MILEAGE, value);
        return this;
    }

    public OtherCostSelection carSuspendedSince(Date... value) {
        addEquals(CarColumns.SUSPENDED_SINCE, value);
        return this;
    }

    public OtherCostSelection carSuspendedSinceNot(Date... value) {
        addNotEquals(CarColumns.SUSPENDED_SINCE, value);
        return this;
    }

    public OtherCostSelection carSuspendedSince(Long... value) {
        addEquals(CarColumns.SUSPENDED_SINCE, value);
        return this;
    }

    public OtherCostSelection carSuspendedSinceAfter(Date value) {
        addGreaterThan(CarColumns.SUSPENDED_SINCE, value);
        return this;
    }

    public OtherCostSelection carSuspendedSinceAfterEq(Date value) {
        addGreaterThanOrEquals(CarColumns.SUSPENDED_SINCE, value);
        return this;
    }

    public OtherCostSelection carSuspendedSinceBefore(Date value) {
        addLessThan(CarColumns.SUSPENDED_SINCE, value);
        return this;
    }

    public OtherCostSelection carSuspendedSinceBeforeEq(Date value) {
        addLessThanOrEquals(CarColumns.SUSPENDED_SINCE, value);
        return this;
    }
}
