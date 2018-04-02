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

import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.Months;
import org.joda.time.Years;

import java.util.Date;

import me.kuehle.carreport.provider.othercost.RecurrenceInterval;

public class Recurrences {
    /**
     * Calculates the recurrences up to now.
     *
     * @param interval   The interval the event occurs.
     * @param multiplier A multiplier of the interval.
     * @param start      The beginning of the recurring event.
     * @return The count of the recurrences.
     */
    public static int getRecurrencesSince(RecurrenceInterval interval, int multiplier, Date start) {
        return getRecurrencesBetween(interval, multiplier, start, new Date());
    }

    /**
     * Calculates the recurrences starting at `start` but only counting from `from` up to now.
     *
     * @param interval   The interval the event occurs.
     * @param multiplier A multiplier of the interval.
     * @param start      The beginning of the recurring event.
     * @param from       The beginning of counting the recurrences.
     * @return The count of the recurrences.
     */
    public static int getRecurrencesSince(RecurrenceInterval interval, int multiplier, Date start,
                                          Date from) {
        Date now = new Date();
        return getRecurrencesBetween(interval, multiplier, start, now, from, now);
    }

    /**
     * Calculates the recurrences between `start` and `end`.
     *
     * @param interval   The interval the event occurs.
     * @param multiplier A multiplier of the interval.
     * @param start      The beginning of the recurring event.
     * @param end        The end of the recurring event.
     * @return The count of the recurrences.
     */
    public static int getRecurrencesBetween(RecurrenceInterval interval, int multiplier, Date start,
                                            Date end) {
        return getRecurrencesBetween(interval, multiplier, start, end, start, end);
    }

    /**
     * Calculates the recurrences between `start` and `end` but only counting entries in the time
     * frame from `from` to `to`.
     *
     * @param interval   The interval the event occurs.
     * @param multiplier A multiplier of the interval.
     * @param start      The beginning of the recurring event.
     * @param end        The end of the recurring event.
     * @param from       The beginning of the time frame.
     * @param to         The end of the time frame.
     * @return The count of the recurrences.
     */
    public static int getRecurrencesBetween(RecurrenceInterval interval, int multiplier, Date start,
                                            Date end, Date from, Date to) {
        DateTime startDateTime = new DateTime(start);
        DateTime endDateTime = new DateTime(end);
        DateTime fromDateTime = new DateTime(from);
        DateTime toDateTime = new DateTime(to);

        if (startDateTime.isAfter(endDateTime) || toDateTime.isBefore(fromDateTime)) {
            return 0;
        }

        if (toDateTime.isBefore(endDateTime)) {
            endDateTime = toDateTime;
        }

        int count = 0;
        switch (interval) {
            case ONCE:
                if ((fromDateTime.isBefore(startDateTime) || startDateTime.isEqual(fromDateTime)) &&
                        (endDateTime.isAfter(startDateTime) || endDateTime.isEqual(startDateTime))) {
                    count = 1;
                }

                break;
            case DAY:
                if (fromDateTime.isAfter(startDateTime)) {
                    int intervalsBetween = Days.daysBetween(startDateTime, fromDateTime).getDays() /
                            multiplier;
                    if (fromDateTime.isAfter(startDateTime.
                            plusDays(intervalsBetween * multiplier))) {
                        intervalsBetween++;
                    }

                    startDateTime = startDateTime.plusDays(intervalsBetween * multiplier);
                }

                if (startDateTime.isBefore(endDateTime) || startDateTime.equals(endDateTime)) {
                    count = Days.daysBetween(startDateTime, endDateTime).getDays() / multiplier + 1;
                }

                break;
            case MONTH:
                if (fromDateTime.isAfter(startDateTime)) {
                    int intervalsBetween = Months.monthsBetween(startDateTime, fromDateTime).
                            getMonths() / multiplier;
                    if (fromDateTime.isAfter(startDateTime.
                            plusMonths(intervalsBetween * multiplier))) {
                        intervalsBetween++;
                    }

                    startDateTime = startDateTime.plusMonths(intervalsBetween * multiplier);
                }

                if (startDateTime.isBefore(endDateTime) || startDateTime.equals(endDateTime)) {
                    count = Months.monthsBetween(startDateTime, endDateTime).getMonths() /
                            multiplier + 1;
                }

                break;
            case QUARTER:
                if (fromDateTime.isAfter(startDateTime)) {
                    int intervalsBetween = Months.monthsBetween(startDateTime, fromDateTime).
                            getMonths() / (3 * multiplier);
                    if (fromDateTime.isAfter(startDateTime.
                            plusMonths(intervalsBetween * 3 * multiplier))) {
                        intervalsBetween++;
                    }

                    startDateTime = startDateTime.plusMonths(intervalsBetween * 3 * multiplier);
                }

                if (startDateTime.isBefore(endDateTime) || startDateTime.equals(endDateTime)) {
                    int quarters = Months.monthsBetween(startDateTime, endDateTime).getMonths() / 3;
                    count = quarters / multiplier + 1;
                }

                break;
            case YEAR:
                if (fromDateTime.isAfter(startDateTime)) {
                    int intervalsBetween = Years.yearsBetween(startDateTime, fromDateTime).
                            getYears() / multiplier;
                    if (fromDateTime.isAfter(startDateTime.
                            plusYears(intervalsBetween * multiplier))) {
                        intervalsBetween++;
                    }

                    startDateTime = startDateTime.plusYears(intervalsBetween * multiplier);
                }

                if (startDateTime.isBefore(endDateTime) || startDateTime.equals(endDateTime)) {
                    count = Years.yearsBetween(startDateTime, endDateTime).getYears() / multiplier +
                            1;
                }

                break;
        }

        return count;
    }
}
