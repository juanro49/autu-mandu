/*
 * Copyright 2025 Juanro49
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
package org.juanro.autumandu.provider.tirelist;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;

import org.juanro.autumandu.provider.base.AbstractSelection;

import java.util.Date;

/**
 * Selection for the {@code tire_list} table.
 */
@Deprecated
public class TireListSelection extends AbstractSelection<TireListSelection> {
    @Override
    protected Uri baseUri() {
        return TireListColumns.CONTENT_URI;
    }

    /**
     * Query the given content resolver using this selection.
     *
     * @param contentResolver The content resolver to query.
     * @param projection A list of which columns to return. Passing null will return all columns, which is inefficient.
     * @param sortOrder How to order the rows, formatted as an SQL ORDER BY clause (excluding the ORDER BY itself). Passing null will use the default sort
     *            order, which may be unordered.
     * @return A {@code StationCursor} object, which is positioned before the first entry, or null.
     */
    public TireListCursor query(ContentResolver contentResolver, String[] projection, String sortOrder) {
        Cursor cursor = contentResolver.query(uri(), projection, sel(), args(), sortOrder);
        if (cursor == null) return null;
        return new TireListCursor(cursor);
    }

    /**
     * Query the given content resolver using this selection to consult uri with params.
     *
     * @param contentResolver The content resolver to query.
     * @param uri The uri with params
     * @param projection A list of which columns to return. Passing null will return all columns, which is inefficient.
     * @param sortOrder How to order the rows, formatted as an SQL ORDER BY clause (excluding the ORDER BY itself). Passing null will use the default sort
     *            order, which may be unordered.
     * @return A {@code StationCursor} object, which is positioned before the first entry, or null.
     */
    public TireListCursor query(ContentResolver contentResolver, Uri uri, String[] projection, String sortOrder) {
        Cursor cursor = contentResolver.query(uri, projection, sel(), args(), sortOrder);
        if (cursor == null) return null;
        return new TireListCursor(cursor);
    }

    /**
     * Equivalent of calling {@code query(contentResolver, projection, null)}.
     */
    public TireListCursor query(ContentResolver contentResolver, String[] projection) {
        return query(contentResolver, projection, null);
    }

    /**
     * Equivalent of calling {@code query(contentResolver, projection, null, null)}.
     */
    public TireListCursor query(ContentResolver contentResolver) {
        return query(contentResolver, null, null);
    }


    public TireListSelection id(long... value) {
        addEquals("tire_list." + TireListColumns._ID, toObjectArray(value));
        return this;
    }

    public TireListSelection buyDate(Date... value) {
        addEquals(TireListColumns.BUY_DATE, value);
        return this;
    }

    public TireListSelection trashDate(Date... value) {
        addEquals(TireListColumns.TRASH_DATE, value);
        return this;
    }

    public TireListSelection price(float... value) {
        addEquals(TireListColumns.PRICE, toObjectArray(value));
        return this;
    }

    public TireListSelection priceGt(float value) {
        addGreaterThan(TireListColumns.PRICE, value);
        return this;
    }

    public TireListSelection priceGtEq(float value) {
        addGreaterThanOrEquals(TireListColumns.PRICE, value);
        return this;
    }

    public TireListSelection priceLt(float value) {
        addLessThan(TireListColumns.PRICE, value);
        return this;
    }

    public TireListSelection priceLtEq(float value) {
        addLessThanOrEquals(TireListColumns.PRICE, value);
        return this;
    }

    public TireListSelection quantity(int... value) {
        addEquals(TireListColumns.QUANTITY, toObjectArray(value));
        return this;
    }

    public TireListSelection quantityGt(int value) {
        addGreaterThan(TireListColumns.QUANTITY, value);
        return this;
    }

    public TireListSelection quantityGtEq(int value) {
        addGreaterThanOrEquals(TireListColumns.QUANTITY, value);
        return this;
    }

    public TireListSelection quantityLt(int value) {
        addLessThan(TireListColumns.QUANTITY, value);
        return this;
    }

    public TireListSelection quantityLtEq(int value) {
        addLessThanOrEquals(TireListColumns.QUANTITY, value);
        return this;
    }

    public TireListSelection manufacturer(String... value) {
        addEquals(TireListColumns.MANUFACTURER, value);
        return this;
    }

    public TireListSelection model(String... value) {
        addEquals(TireListColumns.MODEL, value);
        return this;
    }

    public TireListSelection note(String... value) {
        addEquals(TireListColumns.NOTE, value);
        return this;
    }

    public TireListSelection carId(long... value) {
        addEquals(TireListColumns.CAR_ID, toObjectArray(value));
        return this;
    }
}
