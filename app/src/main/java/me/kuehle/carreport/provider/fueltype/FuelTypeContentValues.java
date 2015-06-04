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
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import me.kuehle.carreport.provider.base.AbstractContentValues;

/**
 * Content values wrapper for the {@code fuel_type} table.
 */
public class FuelTypeContentValues extends AbstractContentValues {
    @Override
    public Uri uri() {
        return FuelTypeColumns.CONTENT_URI;
    }

    /**
     * Update row(s) using the values stored by this object and the given selection.
     *
     * @param contentResolver The content resolver to use.
     * @param where The selection to use (can be {@code null}).
     */
    public int update(ContentResolver contentResolver, @Nullable FuelTypeSelection where) {
        return contentResolver.update(uri(), values(), where == null ? null : where.sel(), where == null ? null : where.args());
    }

    /**
     * Name of the fuel type, e.g. Diesel.
     */
    public FuelTypeContentValues putName(@NonNull String value) {
        if (value == null) throw new IllegalArgumentException("name must not be null");
        mContentValues.put(FuelTypeColumns.NAME, value);
        return this;
    }


    /**
     * An optional category like fuel or gas. Fuel types may be grouped by this category in reports.
     */
    public FuelTypeContentValues putCategory(@Nullable String value) {
        mContentValues.put(FuelTypeColumns.CATEGORY, value);
        return this;
    }

    public FuelTypeContentValues putCategoryNull() {
        mContentValues.putNull(FuelTypeColumns.CATEGORY);
        return this;
    }
}
