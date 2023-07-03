/*
 * Copyright 2023 Juanro49
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
package org.juanro.autumandu.provider.station;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;

import org.juanro.autumandu.provider.base.AbstractSelection;

/**
 * Selection for the {@code fuel_type} table.
 */
@Deprecated
public class StationSelection extends AbstractSelection<StationSelection> {
    @Override
    protected Uri baseUri() {
        return StationColumns.CONTENT_URI;
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
    public StationCursor query(ContentResolver contentResolver, String[] projection, String sortOrder) {
        Cursor cursor = contentResolver.query(uri(), projection, sel(), args(), sortOrder);
        if (cursor == null) return null;
        return new StationCursor(cursor);
    }

    /**
     * Equivalent of calling {@code query(contentResolver, projection, null)}.
     */
    public StationCursor query(ContentResolver contentResolver, String[] projection) {
        return query(contentResolver, projection, null);
    }

    /**
     * Equivalent of calling {@code query(contentResolver, projection, null, null)}.
     */
    public StationCursor query(ContentResolver contentResolver) {
        return query(contentResolver, null, null);
    }


    public StationSelection id(long... value) {
        addEquals("station." + StationColumns._ID, toObjectArray(value));
        return this;
    }

    public StationSelection name(String... value) {
        addEquals(StationColumns.NAME, value);
        return this;
    }

    public StationSelection nameNot(String... value) {
        addNotEquals(StationColumns.NAME, value);
        return this;
    }

    public StationSelection nameLike(String... value) {
        addLike(StationColumns.NAME, value);
        return this;
    }

    public StationSelection nameContains(String... value) {
        addContains(StationColumns.NAME, value);
        return this;
    }

    public StationSelection nameStartsWith(String... value) {
        addStartsWith(StationColumns.NAME, value);
        return this;
    }

    public StationSelection nameEndsWith(String... value) {
        addEndsWith(StationColumns.NAME, value);
        return this;
    }
}
