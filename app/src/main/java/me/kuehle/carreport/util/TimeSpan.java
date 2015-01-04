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

public class TimeSpan {
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

    public String toString(Context context) {
        String[] timeSpanUnits = context.getResources().getStringArray(R.array.time_units);
        return String.format("%s %s", mCount, timeSpanUnits[mUnit.getValue()]);
    }
}
