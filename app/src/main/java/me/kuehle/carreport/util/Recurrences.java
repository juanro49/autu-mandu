/*
 * Copyright 2012 Jan Kühle
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

package me.kuehle.carreport.util;

import java.util.Date;

import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.Months;
import org.joda.time.Years;

import me.kuehle.carreport.provider.othercost.RecurrenceInterval;

public class Recurrences {
    /**
     * Calculates the recurrences up to now.
     * @param interval The interval the event occurs.
     * @param multiplier A multiplier of the interval.
     * @param start The beginning of the recurring event.
     * @return The count of the recurrences.
     */
    public static int getRecurrencesSince(RecurrenceInterval interval, int multiplier, Date start) {
        return getRecurrencesBetween(interval, multiplier, start, new Date());
    }

    /**
     * Calculates the recurrences between a specific date and now.
     * @param interval The interval the event occurs.
     * @param multiplier A multiplier of the interval.
     * @param start The beginning of the recurring event.
     * @param from The beginning of counting the recurences.
     * @return The count of the recurrences.
     */
    public static int getRecurrencesSince(RecurrenceInterval interval, int multiplier, Date start,
                                          Date from) {
        Date now = new Date();
        return getRecurrencesBetween(interval, multiplier, start, now, from, now);
    }

    /**
     * Calculates the recurrences of an event.
     * @param interval The interval the event occurs.
     * @param multiplier A multiplier of the interval.
     * @param start The beginning of the recurring event.
     * @param end The end of the recurring event.
     * @return The count of the recurrences.
     */
    public static int getRecurrencesBetween(RecurrenceInterval interval, int multiplier, Date start,
                                            Date end) {
        return getRecurrencesBetween(interval, multiplier, start, end, start, new Date());
    }

    /**
     * Calculates the recurrences inside a timeframe.
     * @param interval The interval the event occurs.
     * @param multiplier A multiplier of the interval.
     * @param start The beginning of the recurring event.
     * @param end The end of the recurring event.
     * @param from The beginning of the timeframe.
     * @param to The end of the timeframe.
     * @return The count of the recurrences.
     */
    public static int getRecurrencesBetween(RecurrenceInterval interval, int multiplier, Date start,
                                            Date end, Date from, Date to) {
        DateTime startDateTime = new DateTime(start);
        DateTime endDateTime = new DateTime(end);
        if (startDateTime.isAfter(endDateTime) || to.getTime() < from.getTime()) {
            return 0;
        }
        if (new DateTime(to).isBefore(endDateTime)) {
            endDateTime = new DateTime(to);
        }
        DateTime fromDateTime = new DateTime(from);

        int count = (startDateTime.isAfter(fromDateTime) || startDateTime.isEqual(fromDateTime) ?
                1 : 0);
        switch (interval) {
            case ONCE:
                break;
            case DAY:
                if (fromDateTime.isAfter(startDateTime)) {
                    startDateTime = fromDateTime;
                }
                count += Days.daysBetween(startDateTime, endDateTime).getDays() / multiplier;
                break;
            case MONTH:
                if (fromDateTime.isAfter(startDateTime)) {
                    startDateTime.plusMonths(Months.monthsBetween(startDateTime, fromDateTime).
                            getMonths() + 1);
                }
                count += Months.monthsBetween(startDateTime, endDateTime).getMonths() / multiplier;
                break;
            case QUARTER:
                if (fromDateTime.isAfter(startDateTime)) {
                    startDateTime.plusMonths((Months.monthsBetween(startDateTime, fromDateTime).
                            getMonths() / 3) + 3);
                }
                int quarters = Months.monthsBetween(startDateTime, endDateTime).getMonths() / 3;
                count += quarters / multiplier;
                break;
            case YEAR:
                if (fromDateTime.isAfter(startDateTime)) {
                    startDateTime.plusYears(Years.yearsBetween(startDateTime, fromDateTime).
                            getYears() + 1);
                }
                count += Years.yearsBetween(startDateTime, endDateTime).getYears() / multiplier;
                break;
        }

        return count;
    }
}
