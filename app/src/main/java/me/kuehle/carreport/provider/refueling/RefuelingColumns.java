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
package me.kuehle.carreport.provider.refueling;

import android.net.Uri;
import android.provider.BaseColumns;

import me.kuehle.carreport.provider.DataProvider;
import me.kuehle.carreport.provider.car.CarColumns;
import me.kuehle.carreport.provider.fueltype.FuelTypeColumns;
import me.kuehle.carreport.provider.othercost.OtherCostColumns;
import me.kuehle.carreport.provider.refueling.RefuelingColumns;
import me.kuehle.carreport.provider.reminder.ReminderColumns;

/**
 * A refueling for a car.
 */
public class RefuelingColumns implements BaseColumns {
    public static final String TABLE_NAME = "refueling";
    public static final Uri CONTENT_URI = Uri.parse(DataProvider.CONTENT_URI_BASE + "/" + TABLE_NAME);

    /**
     * Primary key.
     */
    public static final String _ID = BaseColumns._ID;

    /**
     * Date on which the refueling occured.
     */
    public static final String DATE = "date";

    /**
     * Mileage on which the refueling occured.
     */
    public static final String MILEAGE = "mileage";

    /**
     * The amount of fuel, that was refilled.
     */
    public static final String VOLUME = "volume";

    /**
     * The price of the refueling.
     */
    public static final String PRICE = "price";

    /**
     * Indicates if the tank was filled completly or only partially.
     */
    public static final String PARTIAL = "partial";

    /**
     * A note for this cost. Just for display purposes.
     */
    public static final String NOTE = "note";

    public static final String FUEL_TYPE_ID = "fuel_type_id";

    public static final String CAR_ID = "car_id";


    public static final String DEFAULT_ORDER = TABLE_NAME + "." +_ID;

    // @formatter:off
    public static final String[] ALL_COLUMNS = new String[] {
            _ID,
            DATE,
            MILEAGE,
            VOLUME,
            PRICE,
            PARTIAL,
            NOTE,
            FUEL_TYPE_ID,
            CAR_ID
    };
    // @formatter:on

    public static boolean hasColumns(String[] projection) {
        if (projection == null) return true;
        for (String c : projection) {
            if (c.equals(DATE) || c.contains("." + DATE)) return true;
            if (c.equals(MILEAGE) || c.contains("." + MILEAGE)) return true;
            if (c.equals(VOLUME) || c.contains("." + VOLUME)) return true;
            if (c.equals(PRICE) || c.contains("." + PRICE)) return true;
            if (c.equals(PARTIAL) || c.contains("." + PARTIAL)) return true;
            if (c.equals(NOTE) || c.contains("." + NOTE)) return true;
            if (c.equals(FUEL_TYPE_ID) || c.contains("." + FUEL_TYPE_ID)) return true;
            if (c.equals(CAR_ID) || c.contains("." + CAR_ID)) return true;
        }
        return false;
    }

    public static final String PREFIX_FUEL_TYPE = TABLE_NAME + "__" + FuelTypeColumns.TABLE_NAME;
    public static final String PREFIX_CAR = TABLE_NAME + "__" + CarColumns.TABLE_NAME;
}
