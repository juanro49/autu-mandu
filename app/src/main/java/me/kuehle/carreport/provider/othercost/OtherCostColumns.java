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
package me.kuehle.carreport.provider.othercost;

import android.net.Uri;
import android.provider.BaseColumns;

import me.kuehle.carreport.provider.DataProvider;
import me.kuehle.carreport.provider.car.CarColumns;
import me.kuehle.carreport.provider.fueltype.FuelTypeColumns;
import me.kuehle.carreport.provider.othercost.OtherCostColumns;
import me.kuehle.carreport.provider.refueling.RefuelingColumns;
import me.kuehle.carreport.provider.reminder.ReminderColumns;

/**
 * A cost for a car, that is not a refueling. Can also be an income, in which case the price is negative.
 */
public class OtherCostColumns implements BaseColumns {
    public static final String TABLE_NAME = "other_cost";
    public static final Uri CONTENT_URI = Uri.parse(DataProvider.CONTENT_URI_BASE + "/" + TABLE_NAME);

    /**
     * Primary key.
     */
    public static final String _ID = BaseColumns._ID;

    /**
     * Display title of the cost.
     */
    public static final String TITLE = "title";

    /**
     * Date on which the cost occured.
     */
    public static final String DATE = "date";

    /**
     * Mileage on which the cost occured.
     */
    public static final String MILEAGE = "mileage";

    /**
     * The price of the cost. If it is an income, the price it negative.
     */
    public static final String PRICE = "price";

    /**
     * Recurrence information. Together with the recurrence_multiplier this gives a recurrence like every 5 days.
     */
    public static final String RECURRENCE_INTERVAL = "recurrence_interval";

    /**
     * Recurrence information. Together with the recurrence_interval this gives a recurrence like every 5 days.
     */
    public static final String RECURRENCE_MULTIPLIER = "recurrence_multiplier";

    /**
     * Date on which the recurrence ends or null, if there is no known end date yet.
     */
    public static final String END_DATE = "end_date";

    /**
     * A note for this cost. Just for display purposes.
     */
    public static final String NOTE = "note";

    public static final String CAR_ID = "car_id";


    public static final String DEFAULT_ORDER = TABLE_NAME + "." +_ID;

    // @formatter:off
    public static final String[] ALL_COLUMNS = new String[] {
            _ID,
            TITLE,
            DATE,
            MILEAGE,
            PRICE,
            RECURRENCE_INTERVAL,
            RECURRENCE_MULTIPLIER,
            END_DATE,
            NOTE,
            CAR_ID
    };
    // @formatter:on

    public static boolean hasColumns(String[] projection) {
        if (projection == null) return true;
        for (String c : projection) {
            if (c.equals(TITLE) || c.contains("." + TITLE)) return true;
            if (c.equals(DATE) || c.contains("." + DATE)) return true;
            if (c.equals(MILEAGE) || c.contains("." + MILEAGE)) return true;
            if (c.equals(PRICE) || c.contains("." + PRICE)) return true;
            if (c.equals(RECURRENCE_INTERVAL) || c.contains("." + RECURRENCE_INTERVAL)) return true;
            if (c.equals(RECURRENCE_MULTIPLIER) || c.contains("." + RECURRENCE_MULTIPLIER)) return true;
            if (c.equals(END_DATE) || c.contains("." + END_DATE)) return true;
            if (c.equals(NOTE) || c.contains("." + NOTE)) return true;
            if (c.equals(CAR_ID) || c.contains("." + CAR_ID)) return true;
        }
        return false;
    }

    public static final String PREFIX_CAR = TABLE_NAME + "__" + CarColumns.TABLE_NAME;
}
