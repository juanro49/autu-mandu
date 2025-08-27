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
package org.juanro.autumandu.provider.tireusage;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;

import org.juanro.autumandu.provider.base.AbstractSelection;

import java.util.Date;

/**
 * Selection for the {@code tire_usage} table.
 */
@Deprecated
public class TireUsageSelection extends AbstractSelection<TireUsageSelection> {
    @Override
    protected Uri baseUri() {
        return TireUsageColumns.CONTENT_URI;
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
    public TireUsageCursor query(ContentResolver contentResolver, String[] projection, String sortOrder) {
        Cursor cursor = contentResolver.query(uri(), projection, sel(), args(), sortOrder);
        if (cursor == null) return null;
        return new TireUsageCursor(cursor);
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
    public TireUsageCursor query(ContentResolver contentResolver, Uri uri, String[] projection, String sortOrder) {
        Cursor cursor = contentResolver.query(uri, projection, sel(), args(), sortOrder);
        if (cursor == null) return null;
        return new TireUsageCursor(cursor);
    }

    /**
     * Equivalent of calling {@code query(contentResolver, projection, null)}.
     */
    public TireUsageCursor query(ContentResolver contentResolver, String[] projection) {
        return query(contentResolver, projection, null);
    }

    /**
     * Equivalent of calling {@code query(contentResolver, projection, null, null)}.
     */
    public TireUsageCursor query(ContentResolver contentResolver) {
        return query(contentResolver, null, null);
    }

    public TireUsageSelection id(long... value) {
        addEquals("tire_usage." + TireUsageColumns._ID, toObjectArray(value));
        return this;
    }

    public TireUsageSelection dateMount(Date... value) {
        addEquals(TireUsageColumns.DATE_MOUNT, value);
        return this;
    }

    public TireUsageSelection dateUmount(Date... value) {
        addEquals(TireUsageColumns.DATE_UMOUNT, value);
        return this;
    }

    public TireUsageSelection distanceMount(int... value) {
        addEquals(TireUsageColumns.DISTANCE_MOUNT, toObjectArray(value));
        return this;
    }

    public TireUsageSelection distanceUmount(int... value) {
        addEquals(TireUsageColumns.DISTANCE_UMOUNT, toObjectArray(value));
        return this;
    }

    public TireUsageSelection tireId(long... value) {
        addEquals(TireUsageColumns.TIRE_ID, toObjectArray(value));
        return this;
    }

    public TireUsageSelection tireIdNotUmount(long... value) {
        addEquals(TireUsageColumns.TIRE_ID, toObjectArray(value));
        and();
        addEquals(TireUsageColumns.DATE_UMOUNT, null);
        return this;
    }
}
