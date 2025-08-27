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
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.juanro.autumandu.provider.base.AbstractContentValues;

import java.util.Date;

/**
 * Content values wrapper for the {@code station} table.
 */
@Deprecated
public class TireListContentValues extends AbstractContentValues {
    @Override
    public Uri uri() {
        return TireListColumns.CONTENT_URI;
    }

    /**
     * Update row(s) using the values stored by this object and the given selection.
     *
     * @param contentResolver The content resolver to use.
     * @param where The selection to use (can be {@code null}).
     */
    public int update(ContentResolver contentResolver, @Nullable TireListSelection where) {
        return contentResolver.update(uri(), values(), where == null ? null : where.sel(), where == null ? null : where.args());
    }

    public TireListContentValues putBuyDate(@NonNull Date value) {
        if (value == null) throw new IllegalArgumentException("buy_date must not be null");
        mContentValues.put(TireListColumns.BUY_DATE, value.getTime());
        return this;
    }

    public TireListContentValues putTrashDate(Date value) {
        mContentValues.put(TireListColumns.TRASH_DATE, value == null ? null : value.getTime());
        return this;
    }

    public TireListContentValues putPrice(float value) {
        mContentValues.put(TireListColumns.PRICE, value);
        return this;
    }

    public TireListContentValues putQuantity(int value) {
        mContentValues.put(TireListColumns.QUANTITY, value);
        return this;
    }

    public TireListContentValues putManufacturer(@NonNull String manufacturer) {
        if (manufacturer == null) throw new IllegalArgumentException("manufacturer must not be null");
        mContentValues.put(TireListColumns.MANUFACTURER, manufacturer);
        return this;
    }

    public TireListContentValues putModel(@NonNull String model) {
        if (model == null) throw new IllegalArgumentException("model must not be null");
        mContentValues.put(TireListColumns.MODEL, model);
        return this;
    }

    public TireListContentValues putNote(@NonNull String note) {
        if (note == null) throw new IllegalArgumentException("note must not be null");
        mContentValues.put(TireListColumns.NOTE, note);
        return this;
    }

    public TireListContentValues putCarId(long value) {
        mContentValues.put(TireListColumns.CAR_ID, value);
        return this;
    }
}
