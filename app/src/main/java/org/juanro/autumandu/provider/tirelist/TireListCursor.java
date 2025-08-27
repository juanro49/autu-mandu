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

import android.database.Cursor;

import androidx.annotation.NonNull;

import org.juanro.autumandu.provider.base.AbstractCursor;

import java.util.Date;

/**
 * Cursor wrapper for the {@code station} table.
 */
@Deprecated
public class TireListCursor extends AbstractCursor implements TireListModel {
    public TireListCursor(Cursor cursor) {
        super(cursor);
    }

    /**
     * Primary key.
     */
    public long getId() {
        Long res = getLongOrNull(TireListColumns._ID);
        if (res == null)
            throw new NullPointerException("The value of '_id' in the database was null, which is not allowed according to the model definition");
        return res;
    }

    @NonNull
    public Date getBuyDate() {
        Date res = getDateOrNull(TireListColumns.BUY_DATE);
        if (res == null)
            throw new NullPointerException("The value of 'buy_date' in the database was null, which is not allowed according to the model definition");
        return res;
    }

    public Date getTrashDate() {
        return getDateOrNull(TireListColumns.TRASH_DATE);
    }

    public float getPrice() {
        Float res = getFloatOrNull(TireListColumns.PRICE);
        if (res == null)
            throw new NullPointerException("The value of 'price' in the database was null, which is not allowed according to the model definition");
        return res;
    }

    public int getQuantity() {
        Integer res = getIntegerOrNull(TireListColumns.QUANTITY);
        if (res == null)
            throw new NullPointerException("The value of 'quantity' in the database was null, which is not allowed according to the model definition");
        return res;
    }

    @NonNull
    public String getManufacturer() {
        String res = getStringOrNull(TireListColumns.MANUFACTURER);
        if (res == null)
            throw new NullPointerException("The value of 'manufacturer' in the database was null, which is not allowed according to the model definition");
        return res;
    }

    @NonNull
    public String getModel() {
        String res = getStringOrNull(TireListColumns.MODEL);
        if (res == null)
            throw new NullPointerException("The value of 'model' in the database was null, which is not allowed according to the model definition");
        return res;
    }

    @NonNull
    public String getNote() {
        String res = getStringOrNull(TireListColumns.NOTE);
        if (res == null)
            throw new NullPointerException("The value of 'note' in the database was null, which is not allowed according to the model definition");
        return res;
    }

    public long getCarId() {
        Long res = getLongOrNull(TireListColumns.CAR_ID);
        if (res == null)
            throw new NullPointerException("The value of 'car_id' in the database was null, which is not allowed according to the model definition");
        return res;
    }
}
