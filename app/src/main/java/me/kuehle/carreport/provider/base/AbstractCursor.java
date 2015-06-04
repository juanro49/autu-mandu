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
package me.kuehle.carreport.provider.base;

import java.util.Date;
import java.util.HashMap;

import android.database.Cursor;
import android.database.CursorWrapper;
import android.provider.BaseColumns;

public abstract class AbstractCursor extends CursorWrapper {
    private final HashMap<String, Integer> mColumnIndexes;

    public AbstractCursor(Cursor cursor) {
        super(cursor);
        mColumnIndexes = new HashMap<String, Integer>(cursor.getColumnCount() * 4 / 3, .75f);
    }

    public abstract long getId();

    protected int getCachedColumnIndexOrThrow(String colName) {
        Integer index = mColumnIndexes.get(colName);
        if (index == null) {
            index = getColumnIndexOrThrow(colName);
            mColumnIndexes.put(colName, index);
        }
        return index;
    }

    public String getStringOrNull(String colName) {
        int index = getCachedColumnIndexOrThrow(colName);
        if (isNull(index)) return null;
        return getString(index);
    }

    public Integer getIntegerOrNull(String colName) {
        int index = getCachedColumnIndexOrThrow(colName);
        if (isNull(index)) return null;
        return getInt(index);
    }

    public Long getLongOrNull(String colName) {
        int index = getCachedColumnIndexOrThrow(colName);
        if (isNull(index)) return null;
        return getLong(index);
    }

    public Float getFloatOrNull(String colName) {
        int index = getCachedColumnIndexOrThrow(colName);
        if (isNull(index)) return null;
        return getFloat(index);
    }

    public Double getDoubleOrNull(String colName) {
        int index = getCachedColumnIndexOrThrow(colName);
        if (isNull(index)) return null;
        return getDouble(index);
    }

    public Boolean getBooleanOrNull(String colName) {
        int index = getCachedColumnIndexOrThrow(colName);
        if (isNull(index)) return null;
        return getInt(index) != 0;
    }

    public Date getDateOrNull(String colName) {
        int index = getCachedColumnIndexOrThrow(colName);
        if (isNull(index)) return null;
        return new Date(getLong(index));
    }

    public byte[] getBlobOrNull(String colName) {
        int index = getCachedColumnIndexOrThrow(colName);
        if (isNull(index)) return null;
        return getBlob(index);
    }
}
