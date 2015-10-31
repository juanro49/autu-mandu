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
import android.database.Cursor;
import android.net.Uri;

import me.kuehle.carreport.provider.base.AbstractSelection;

/**
 * Selection for the {@code car} table.
 */
public class CarSelection extends AbstractSelection<CarSelection> {
    @Override
    protected Uri baseUri() {
        return CarColumns.CONTENT_URI;
    }

    /**
     * Query the given content resolver using this selection.
     *
     * @param contentResolver The content resolver to query.
     * @param projection A list of which columns to return. Passing null will return all columns, which is inefficient.
     * @param sortOrder How to order the rows, formatted as an SQL ORDER BY clause (excluding the ORDER BY itself). Passing null will use the default sort
     *            order, which may be unordered.
     * @return A {@code CarCursor} object, which is positioned before the first entry, or null.
     */
    public CarCursor query(ContentResolver contentResolver, String[] projection, String sortOrder) {
        Cursor cursor = contentResolver.query(uri(), projection, sel(), args(), sortOrder);
        if (cursor == null) return null;
        return new CarCursor(cursor);
    }

    /**
     * Equivalent of calling {@code query(contentResolver, projection, null)}.
     */
    public CarCursor query(ContentResolver contentResolver, String[] projection) {
        return query(contentResolver, projection, null);
    }

    /**
     * Equivalent of calling {@code query(contentResolver, projection, null, null)}.
     */
    public CarCursor query(ContentResolver contentResolver) {
        return query(contentResolver, null, null);
    }


    public CarSelection id(long... value) {
        addEquals("car." + CarColumns._ID, toObjectArray(value));
        return this;
    }

    public CarSelection name(String... value) {
        addEquals(CarColumns.NAME, value);
        return this;
    }

    public CarSelection nameNot(String... value) {
        addNotEquals(CarColumns.NAME, value);
        return this;
    }

    public CarSelection nameLike(String... value) {
        addLike(CarColumns.NAME, value);
        return this;
    }

    public CarSelection nameContains(String... value) {
        addContains(CarColumns.NAME, value);
        return this;
    }

    public CarSelection nameStartsWith(String... value) {
        addStartsWith(CarColumns.NAME, value);
        return this;
    }

    public CarSelection nameEndsWith(String... value) {
        addEndsWith(CarColumns.NAME, value);
        return this;
    }

    public CarSelection color(int... value) {
        addEquals(CarColumns.COLOR, toObjectArray(value));
        return this;
    }

    public CarSelection colorNot(int... value) {
        addNotEquals(CarColumns.COLOR, toObjectArray(value));
        return this;
    }

    public CarSelection colorGt(int value) {
        addGreaterThan(CarColumns.COLOR, value);
        return this;
    }

    public CarSelection colorGtEq(int value) {
        addGreaterThanOrEquals(CarColumns.COLOR, value);
        return this;
    }

    public CarSelection colorLt(int value) {
        addLessThan(CarColumns.COLOR, value);
        return this;
    }

    public CarSelection colorLtEq(int value) {
        addLessThanOrEquals(CarColumns.COLOR, value);
        return this;
    }

    public CarSelection initialMileage(int... value) {
        addEquals(CarColumns.INITIAL_MILEAGE, toObjectArray(value));
        return this;
    }

    public CarSelection initialMileageNot(int... value) {
        addNotEquals(CarColumns.INITIAL_MILEAGE, toObjectArray(value));
        return this;
    }

    public CarSelection initialMileageGt(int value) {
        addGreaterThan(CarColumns.INITIAL_MILEAGE, value);
        return this;
    }

    public CarSelection initialMileageGtEq(int value) {
        addGreaterThanOrEquals(CarColumns.INITIAL_MILEAGE, value);
        return this;
    }

    public CarSelection initialMileageLt(int value) {
        addLessThan(CarColumns.INITIAL_MILEAGE, value);
        return this;
    }

    public CarSelection initialMileageLtEq(int value) {
        addLessThanOrEquals(CarColumns.INITIAL_MILEAGE, value);
        return this;
    }

    public CarSelection suspendedSince(Date... value) {
        addEquals(CarColumns.SUSPENDED_SINCE, value);
        return this;
    }

    public CarSelection suspendedSinceNot(Date... value) {
        addNotEquals(CarColumns.SUSPENDED_SINCE, value);
        return this;
    }

    public CarSelection suspendedSince(Long... value) {
        addEquals(CarColumns.SUSPENDED_SINCE, value);
        return this;
    }

    public CarSelection suspendedSinceAfter(Date value) {
        addGreaterThan(CarColumns.SUSPENDED_SINCE, value);
        return this;
    }

    public CarSelection suspendedSinceAfterEq(Date value) {
        addGreaterThanOrEquals(CarColumns.SUSPENDED_SINCE, value);
        return this;
    }

    public CarSelection suspendedSinceBefore(Date value) {
        addLessThan(CarColumns.SUSPENDED_SINCE, value);
        return this;
    }

    public CarSelection suspendedSinceBeforeEq(Date value) {
        addLessThanOrEquals(CarColumns.SUSPENDED_SINCE, value);
        return this;
    }
}
