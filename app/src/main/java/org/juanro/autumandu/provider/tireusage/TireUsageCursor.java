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

import android.database.Cursor;

import androidx.annotation.NonNull;

import org.juanro.autumandu.provider.base.AbstractCursor;

import java.util.Date;

/**
 * Cursor wrapper for the {@code station} table.
 */
@Deprecated
public class TireUsageCursor extends AbstractCursor implements TireUsageModel {
    public TireUsageCursor(Cursor cursor) {
        super(cursor);
    }

    /**
     * Primary key.
     */
    public long getId() {
        Long res = getLongOrNull(TireUsageColumns._ID);
        if (res == null)
            throw new NullPointerException("The value of '_id' in the database was null, which is not allowed according to the model definition");
        return res;
    }

    @NonNull
    public Date getDateMount() {
        Date res = getDateOrNull(TireUsageColumns.DATE_MOUNT);
        if (res == null)
            throw new NullPointerException("The value of 'date_mount' in the database was null, which is not allowed according to the model definition");
        return res;
    }

    public Date getDateUmount() {
        return getDateOrNull(TireUsageColumns.DATE_UMOUNT);
    }

    public int getDistanceMount() {
        Integer res = getIntegerOrNull(TireUsageColumns.DISTANCE_MOUNT);
        if (res == null)
            throw new NullPointerException("The value of 'distance_mount' in the database was null, which is not allowed according to the model definition");
        return res;
    }

    public int getDistanceUmount() {
        Integer res = getIntegerOrNull(TireUsageColumns.DISTANCE_UMOUNT);
        if (res == null)
            throw new NullPointerException("The value of 'distance_umount' in the database was null, which is not allowed according to the model definition");
        return res;
    }

    public long getTireId() {
        Long res = getLongOrNull(TireUsageColumns.TIRE_ID);
        if (res == null)
            throw new NullPointerException("The value of 'tire_id' in the database was null, which is not allowed according to the model definition");
        return res;
    }
}
