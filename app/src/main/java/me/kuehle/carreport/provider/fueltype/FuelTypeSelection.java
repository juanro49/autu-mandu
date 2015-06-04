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
package me.kuehle.carreport.provider.fueltype;

import java.util.Date;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;

import me.kuehle.carreport.provider.base.AbstractSelection;

/**
 * Selection for the {@code fuel_type} table.
 */
public class FuelTypeSelection extends AbstractSelection<FuelTypeSelection> {
    @Override
    protected Uri baseUri() {
        return FuelTypeColumns.CONTENT_URI;
    }

    /**
     * Query the given content resolver using this selection.
     *
     * @param contentResolver The content resolver to query.
     * @param projection A list of which columns to return. Passing null will return all columns, which is inefficient.
     * @param sortOrder How to order the rows, formatted as an SQL ORDER BY clause (excluding the ORDER BY itself). Passing null will use the default sort
     *            order, which may be unordered.
     * @return A {@code FuelTypeCursor} object, which is positioned before the first entry, or null.
     */
    public FuelTypeCursor query(ContentResolver contentResolver, String[] projection, String sortOrder) {
        Cursor cursor = contentResolver.query(uri(), projection, sel(), args(), sortOrder);
        if (cursor == null) return null;
        return new FuelTypeCursor(cursor);
    }

    /**
     * Equivalent of calling {@code query(contentResolver, projection, null)}.
     */
    public FuelTypeCursor query(ContentResolver contentResolver, String[] projection) {
        return query(contentResolver, projection, null);
    }

    /**
     * Equivalent of calling {@code query(contentResolver, projection, null, null)}.
     */
    public FuelTypeCursor query(ContentResolver contentResolver) {
        return query(contentResolver, null, null);
    }


    public FuelTypeSelection id(long... value) {
        addEquals("fuel_type." + FuelTypeColumns._ID, toObjectArray(value));
        return this;
    }

    public FuelTypeSelection name(String... value) {
        addEquals(FuelTypeColumns.NAME, value);
        return this;
    }

    public FuelTypeSelection nameNot(String... value) {
        addNotEquals(FuelTypeColumns.NAME, value);
        return this;
    }

    public FuelTypeSelection nameLike(String... value) {
        addLike(FuelTypeColumns.NAME, value);
        return this;
    }

    public FuelTypeSelection nameContains(String... value) {
        addContains(FuelTypeColumns.NAME, value);
        return this;
    }

    public FuelTypeSelection nameStartsWith(String... value) {
        addStartsWith(FuelTypeColumns.NAME, value);
        return this;
    }

    public FuelTypeSelection nameEndsWith(String... value) {
        addEndsWith(FuelTypeColumns.NAME, value);
        return this;
    }

    public FuelTypeSelection category(String... value) {
        addEquals(FuelTypeColumns.CATEGORY, value);
        return this;
    }

    public FuelTypeSelection categoryNot(String... value) {
        addNotEquals(FuelTypeColumns.CATEGORY, value);
        return this;
    }

    public FuelTypeSelection categoryLike(String... value) {
        addLike(FuelTypeColumns.CATEGORY, value);
        return this;
    }

    public FuelTypeSelection categoryContains(String... value) {
        addContains(FuelTypeColumns.CATEGORY, value);
        return this;
    }

    public FuelTypeSelection categoryStartsWith(String... value) {
        addStartsWith(FuelTypeColumns.CATEGORY, value);
        return this;
    }

    public FuelTypeSelection categoryEndsWith(String... value) {
        addEndsWith(FuelTypeColumns.CATEGORY, value);
        return this;
    }
}
