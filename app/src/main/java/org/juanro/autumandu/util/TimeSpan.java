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
package org.juanro.autumandu.util;

import android.content.Context;

import androidx.annotation.NonNull;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Locale;

import org.juanro.autumandu.R;
import org.juanro.autumandu.model.entity.helper.TimeSpanUnit;

/**
 * Represents a span of time.
 */
public record TimeSpan(TimeSpanUnit unit, int count) {
    private static final double MILLIS_PER_DAY = 1000 * 60 * 60 * 24.0;
    private static final double MILLIS_PER_MONTH = MILLIS_PER_DAY * 30;
    private static final double MILLIS_PER_YEAR = MILLIS_PER_DAY * 365;
    private static final float DEVIATION = 0.9f;

    public Date addTo(Date date) {
        ZonedDateTime dateTime = ZonedDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
        ZonedDateTime result = switch (unit) {
            case DAY -> dateTime.plusDays(count);
            case MONTH -> dateTime.plusMonths(count);
            case YEAR -> dateTime.plusYears(count);
        };
        return Date.from(result.toInstant());
    }

    @NonNull
    @Override
    public String toString() {
        return String.format(Locale.US, "%d %s", count, unit.toString());
    }

    public String toLocalizedString(Context context) {
        String[] timeSpanUnits = context.getResources().getStringArray(R.array.time_units);
        return String.format(context.getResources().getConfiguration().getLocales().get(0),
                "%s %s", count, timeSpanUnits[unit.getId()]);
    }

    public static TimeSpan fromMillis(long millis) {
        long absMillis = Math.abs(millis);
        if (absMillis >= (MILLIS_PER_YEAR * DEVIATION)) {
            int count = (int) Math.round(absMillis / MILLIS_PER_YEAR);
            return new TimeSpan(TimeSpanUnit.YEAR, count);
        } else if (absMillis >= (MILLIS_PER_MONTH * DEVIATION)) {
            int count = (int) Math.round(absMillis / MILLIS_PER_MONTH);
            return new TimeSpan(TimeSpanUnit.MONTH, count);
        } else {
            int count = (int) Math.round(absMillis / MILLIS_PER_DAY);
            return new TimeSpan(TimeSpanUnit.DAY, count);
        }
    }

    public static TimeSpan fromString(String str, TimeSpan defaultValue) {
        if (str == null) return defaultValue;
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
