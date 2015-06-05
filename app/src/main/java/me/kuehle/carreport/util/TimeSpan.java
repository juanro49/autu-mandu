/*
 * Copyright 2015 Jan Kühle
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package me.kuehle.carreport.util;

import android.content.Context;

import org.joda.time.DateTime;

import java.util.Date;

import me.kuehle.carreport.R;
import me.kuehle.carreport.provider.reminder.TimeSpanUnit;

public class TimeSpan {
    private static final double MILLIS_PER_DAY = 1000 * 60 * 60 * 24;
    private static final double MILLIS_PER_MONTH = MILLIS_PER_DAY * 30;
    private static final double MILLIS_PER_YEAR = MILLIS_PER_DAY * 365;
    private static final float DEVIATION = 0.9f;

    private TimeSpanUnit mUnit;
    private int mCount;

    public TimeSpan(TimeSpanUnit unit, int count) {
        mUnit = unit;
        mCount = count;
    }

    public TimeSpanUnit getUnit() {
        return mUnit;
    }

    public int getCount() {
        return mCount;
    }

    public Date addTo(Date date) {
        DateTime dateTime = new DateTime(date);
        switch (mUnit) {
            case DAY:
                return dateTime.plusDays(mCount).toDate();
            case MONTH:
                return dateTime.plusMonths(mCount).toDate();
            case YEAR:
                return dateTime.plusYears(mCount).toDate();
        }

        return date;
    }

    public Date subtractFrom(Date date) {
        DateTime dateTime = new DateTime(date);
        switch (mUnit) {
            case DAY:
                return dateTime.minusDays(mCount).toDate();
            case MONTH:
                return dateTime.minusMonths(mCount).toDate();
            case YEAR:
                return dateTime.minusYears(mCount).toDate();
        }

        return date;
    }

    @Override
    public String toString() {
        return String.format("%d %s", mCount, mUnit.toString());
    }

    public String toLocalizedString(Context context) {
        String[] timeSpanUnits = context.getResources().getStringArray(R.array.time_units);
        return String.format("%s %s", mCount, timeSpanUnits[mUnit.ordinal()]);
    }

    public static TimeSpan fromMillis(long millis) {
        millis = Math.abs(millis);
        if (millis >= (MILLIS_PER_YEAR * DEVIATION)) {
            int count = (int) Math.round(millis / MILLIS_PER_YEAR);
            return new TimeSpan(TimeSpanUnit.YEAR, count);
        } else if (millis >= (MILLIS_PER_MONTH * DEVIATION)) {
            int count = (int) Math.round(millis / MILLIS_PER_MONTH);
            return new TimeSpan(TimeSpanUnit.MONTH, count);
        } else {
            int count = (int) Math.round(millis / MILLIS_PER_DAY);
            return new TimeSpan(TimeSpanUnit.DAY, count);
        }
    }

    public static TimeSpan fromString(String str, TimeSpan defaultValue) {
        String[] parts = str.split(" ");
        if (parts.length != 2) {
            return defaultValue;
        }

        try {
            int count = Integer.parseInt(parts[0]);
            TimeSpanUnit unit = TimeSpanUnit.valueOf(parts[1]);
            return new TimeSpan(unit, count);
        } catch (Exception e) {
            return defaultValue;
        }
    }
}
