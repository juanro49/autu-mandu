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
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.juanro.autumandu.provider.base.AbstractContentValues;

import java.util.Date;

/**
 * Content values wrapper for the {@code station} table.
 */
@Deprecated
public class TireUsageContentValues extends AbstractContentValues {
    @Override
    public Uri uri() {
        return TireUsageColumns.CONTENT_URI;
    }

    /**
     * Update row(s) using the values stored by this object and the given selection.
     *
     * @param contentResolver The content resolver to use.
     * @param where The selection to use (can be {@code null}).
     */
    public int update(ContentResolver contentResolver, @Nullable TireUsageSelection where) {
        return contentResolver.update(uri(), values(), where == null ? null : where.sel(), where == null ? null : where.args());
    }

    public TireUsageContentValues putDistanceMount(int value) {
        mContentValues.put(TireUsageColumns.DISTANCE_MOUNT, value);
        return this;
    }
    public TireUsageContentValues putDateMount(@NonNull Date value) {
        if (value == null) throw new IllegalArgumentException("date_mount must not be null");
        mContentValues.put(TireUsageColumns.DATE_MOUNT, value.getTime());
        return this;
    }

    public TireUsageContentValues putDistanceUmount(int value) {
        mContentValues.put(TireUsageColumns.DISTANCE_UMOUNT, value);
        return this;
    }

    public TireUsageContentValues putDateUmount(Date value) {
        mContentValues.put(TireUsageColumns.DATE_UMOUNT, value == null ? null : value.getTime());
        return this;
    }

    public TireUsageContentValues putTireId(long value) {
        mContentValues.put(TireUsageColumns.TIRE_ID, value);
        return this;
    }
}
