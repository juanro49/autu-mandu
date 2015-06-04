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
package me.kuehle.carreport.provider.reminder;

import android.net.Uri;
import android.provider.BaseColumns;

import me.kuehle.carreport.provider.DataProvider;
import me.kuehle.carreport.provider.car.CarColumns;
import me.kuehle.carreport.provider.fueltype.FuelTypeColumns;
import me.kuehle.carreport.provider.othercost.OtherCostColumns;
import me.kuehle.carreport.provider.refueling.RefuelingColumns;
import me.kuehle.carreport.provider.reminder.ReminderColumns;

/**
 * A reminder for a certain event of a car.
 */
public class ReminderColumns implements BaseColumns {
    public static final String TABLE_NAME = "reminder";
    public static final Uri CONTENT_URI = Uri.parse(DataProvider.CONTENT_URI_BASE + "/" + TABLE_NAME);

    /**
     * Primary key.
     */
    public static final String _ID = BaseColumns._ID;

    /**
     * Display title of the reminder.
     */
    public static final String TITLE = "title";

    /**
     * Time span after which the reminder should go off. Together with the after_time_span_count this gives a time span like 3 days.
     */
    public static final String AFTER_TIME_SPAN_UNIT = "after_time_span_unit";

    /**
     * Time span after which the reminder should go off. Together with the after_time_span_unit this gives a time span like every 3 days.
     */
    public static final String AFTER_TIME_SPAN_COUNT = "after_time_span_count";

    /**
     * Distance after which the reminder should go off.
     */
    public static final String AFTER_DISTANCE = "after_distance";

    /**
     * Date on which the reminder starts to count.
     */
    public static final String START_DATE = "start_date";

    /**
     * Mileage on which the reminder starts to count.
     */
    public static final String START_MILEAGE = "start_mileage";

    /**
     * Indicates if the reminder has gone off, but the notification has been dismissed.
     */
    public static final String NOTIFICATION_DISMISSED = "notification_dismissed";

    /**
     * When the reminder goes off, the user can snooze it. In this case the field contains the date on which the reminder will go off again.
     */
    public static final String SNOOZED_UNTIL = "snoozed_until";

    public static final String CAR_ID = "car_id";


    public static final String DEFAULT_ORDER = TABLE_NAME + "." +_ID;

    // @formatter:off
    public static final String[] ALL_COLUMNS = new String[] {
            _ID,
            TITLE,
            AFTER_TIME_SPAN_UNIT,
            AFTER_TIME_SPAN_COUNT,
            AFTER_DISTANCE,
            START_DATE,
            START_MILEAGE,
            NOTIFICATION_DISMISSED,
            SNOOZED_UNTIL,
            CAR_ID
    };
    // @formatter:on

    public static boolean hasColumns(String[] projection) {
        if (projection == null) return true;
        for (String c : projection) {
            if (c.equals(TITLE) || c.contains("." + TITLE)) return true;
            if (c.equals(AFTER_TIME_SPAN_UNIT) || c.contains("." + AFTER_TIME_SPAN_UNIT)) return true;
            if (c.equals(AFTER_TIME_SPAN_COUNT) || c.contains("." + AFTER_TIME_SPAN_COUNT)) return true;
            if (c.equals(AFTER_DISTANCE) || c.contains("." + AFTER_DISTANCE)) return true;
            if (c.equals(START_DATE) || c.contains("." + START_DATE)) return true;
            if (c.equals(START_MILEAGE) || c.contains("." + START_MILEAGE)) return true;
            if (c.equals(NOTIFICATION_DISMISSED) || c.contains("." + NOTIFICATION_DISMISSED)) return true;
            if (c.equals(SNOOZED_UNTIL) || c.contains("." + SNOOZED_UNTIL)) return true;
            if (c.equals(CAR_ID) || c.contains("." + CAR_ID)) return true;
        }
        return false;
    }

    public static final String PREFIX_CAR = TABLE_NAME + "__" + CarColumns.TABLE_NAME;
}
