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
package me.kuehle.carreport.provider.car;

import android.net.Uri;
import android.provider.BaseColumns;

import me.kuehle.carreport.provider.DataProvider;

/**
 * A car.
 */
@Deprecated
public class CarColumns implements BaseColumns {
    public static final String TABLE_NAME = "car";
    public static final Uri CONTENT_URI = Uri.parse(DataProvider.CONTENT_URI_BASE + "/" + TABLE_NAME);

    /**
     * Primary key.
     */
    public static final String _ID = BaseColumns._ID;

    /**
     * Name of the car. Only for display purposes.
     */
    public static final String NAME = "car__name";

    /**
     * Color of the car in android color representation.
     */
    public static final String COLOR = "color";

    /**
     * Initial mileage of the car, when it starts to be used in the app.
     */
    public static final String INITIAL_MILEAGE = "initial_mileage";

    /**
     * When the car has been suspended, this contains the start date.
     */
    public static final String SUSPENDED_SINCE = "suspended_since";

    /**
     * Buying price of the car.
     */
    public static final String BUYING_PRICE = "buying_price";

    /**
     * Make of the car.
     */
    //public static final String MAKE = "make";

    /**
     * Model of the car.
     */
    //public static final String MODEL = "model";

    /**
     * Year of the car.
     */
    //public static final String YEAR = "year";

    /**
     * License plate of the car.
     */
    //public static final String LICENSE_PLATE = "license_plate";

    /**
     * Buying date of the car.
     */
    //public static final String BUYING_DATE = "buying_date";

    public static final String DEFAULT_ORDER = TABLE_NAME + "." +_ID;

    // @formatter:off
    public static final String[] ALL_COLUMNS = new String[] {
            _ID,
            NAME,
            COLOR,
            INITIAL_MILEAGE,
            SUSPENDED_SINCE,
            BUYING_PRICE,
            /*MAKE,
            MODEL,
            YEAR,
            LICENSE_PLATE,
            BUYING_DATE*/
    };
    // @formatter:on

    public static boolean hasColumns(String[] projection) {
        if (projection == null) return true;
        for (String c : projection) {
            if (c.equals(NAME) || c.contains("." + NAME)) return true;
            if (c.equals(COLOR) || c.contains("." + COLOR)) return true;
            if (c.equals(INITIAL_MILEAGE) || c.contains("." + INITIAL_MILEAGE)) return true;
            if (c.equals(SUSPENDED_SINCE) || c.contains("." + SUSPENDED_SINCE)) return true;
            if (c.equals(BUYING_PRICE) || c.contains("." + BUYING_PRICE)) return true;
            /*if (c.equals(MAKE) || c.contains("." + MAKE)) return true;
            if (c.equals(MODEL) || c.contains("." + MODEL)) return true;
            if (c.equals(YEAR) || c.contains("." + YEAR)) return true;
            if (c.equals(LICENSE_PLATE) || c.contains("." + LICENSE_PLATE)) return true;
            if (c.equals(BUYING_DATE) || c.contains("." + BUYING_DATE)) return true;*/
        }
        return false;
    }

}
