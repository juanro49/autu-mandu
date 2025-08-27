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

import android.net.Uri;
import android.provider.BaseColumns;

import org.juanro.autumandu.provider.DataProvider;
import org.juanro.autumandu.provider.car.CarColumns;

/**
 * A station.
 */
@Deprecated
public class TireListColumns implements BaseColumns {
    public static final String TABLE_NAME = "tire_list";
    public static final Uri CONTENT_URI = Uri.parse(DataProvider.CONTENT_URI_BASE + "/" + TABLE_NAME);

    /**
     * Primary key.
     */
    public static final String _ID = BaseColumns._ID;

    public static final String BUY_DATE = "buy_date";
    public static final String TRASH_DATE = "trash_date";
    public static final String PRICE = "price";
    public static final String QUANTITY = "quantity";
    public static final String MANUFACTURER = "manufacturer";
    public static final String MODEL = "model";
    public static final String NOTE = "note";
    public static final String CAR_ID = "car_id";

    public static final String DEFAULT_ORDER = TABLE_NAME + "." +_ID;

    // @formatter:off
    public static final String[] ALL_COLUMNS = new String[] {
        _ID,
        BUY_DATE,
        TRASH_DATE,
        PRICE,
        QUANTITY,
        MANUFACTURER,
        MODEL,
        NOTE,
        CAR_ID
    };
    // @formatter:on

    public static boolean hasColumns(String[] projection) {
        if (projection == null) return true;
        for (String c : projection) {
            if (c.equals(BUY_DATE) || c.contains("." + BUY_DATE)) return true;
            if (c.equals(TRASH_DATE) || c.contains("." + TRASH_DATE)) return true;
            if (c.equals(PRICE) || c.contains("." + PRICE)) return true;
            if (c.equals(PRICE) || c.contains("." + PRICE)) return true;
            if (c.equals(QUANTITY) || c.contains("." + QUANTITY)) return true;
            if (c.equals(MANUFACTURER) || c.contains("." + MANUFACTURER)) return true;
            if (c.equals(MODEL) || c.contains("." + MODEL)) return true;
            if (c.equals(NOTE) || c.contains("." + NOTE)) return true;
            if (c.equals(CAR_ID) || c.contains("." + CAR_ID)) return true;
        }
        return false;
    }

    public static final String PREFIX_CAR = TABLE_NAME + "__" + CarColumns.TABLE_NAME;
}
