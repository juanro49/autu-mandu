/*
 * Copyright 2017 Jan Kühle
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.Date;

import me.kuehle.carreport.provider.othercost.RecurrenceInterval;
import me.kuehle.carreport.provider.reminder.TimeSpanUnit;

public class Convert {
    private static final DateFormat dateFormat = DateFormat.getDateTimeInstance();
    private static final NumberFormat floatFormat = NumberFormat.getInstance();

    @Nullable
    public static Date toDate(String value) {
        try {
            return dateFormat.parse(value);
        } catch (Exception e) {
            return null;
        }
    }

    @Nullable
    public static Float toFloat(String value) {
        try {
            return floatFormat.parse(value).floatValue();
        } catch (Exception e) {
            return null;
        }
    }

    @Nullable
    public static Integer toInteger(String value) {
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return null;
        }
    }

    @Nullable
    public static Long toLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (Exception e) {
            return null;
        }
    }

    @Nullable
    public static Boolean toBoolean(@NonNull String value) {
        if (value.isEmpty()) {
            return null;
        } else {
            return Boolean.parseBoolean(value);
        }
    }

    @Nullable
    public static RecurrenceInterval toRecurrenceInterval(String value) {
        Integer intValue = toInteger(value);
        if (intValue != null) {
            return RecurrenceInterval.values()[intValue];
        } else {
            return null;
        }
    }

    @Nullable
    public static TimeSpanUnit toTimeSpanUnit(String value) {
        Integer intValue = toInteger(value);
        if (intValue != null) {
            return TimeSpanUnit.values()[intValue];
        } else {
            return null;
        }
    }

    @Nullable
    public static String toString(Enum value) {
        try {
            return String.valueOf(value.ordinal());
        } catch (Exception e) {
            return null;
        }
    }

    @Nullable
    public static String toString(Date value) {
        try {
            return dateFormat.format(value);
        } catch (Exception e) {
            return null;
        }
    }

    @Nullable
    public static String toString(Float value) {
        try {
            return floatFormat.format(value);
        } catch (Exception e) {
            return null;
        }
    }
}
