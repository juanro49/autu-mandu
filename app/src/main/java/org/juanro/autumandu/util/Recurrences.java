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

package org.juanro.autumandu.util;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import org.juanro.autumandu.model.entity.helper.RecurrenceInterval;

/**
 * Utility class for calculating recurrences of events.
 */
public final class Recurrences {

    private Recurrences() {
        // Utility class
    }

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
        var now = new Date();
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
     * @param to         ...
     * @return The count of the recurrences.
     */
    public static int getRecurrencesBetween(RecurrenceInterval interval, int multiplier, Date start,
                                            Date end, Date from, Date to) {
        ZonedDateTime startDt = ZonedDateTime.ofInstant(start.toInstant(), ZoneId.systemDefault());
        ZonedDateTime endDt = ZonedDateTime.ofInstant(end.toInstant(), ZoneId.systemDefault());
        ZonedDateTime fromDt = ZonedDateTime.ofInstant(from.toInstant(), ZoneId.systemDefault());
        ZonedDateTime toDt = ZonedDateTime.ofInstant(to.toInstant(), ZoneId.systemDefault());

        if (startDt.isAfter(endDt) || toDt.isBefore(fromDt)) {
            return 0;
        }

        final ZonedDateTime finalEndDt = toDt.isBefore(endDt) ? toDt : endDt;

        return switch (interval) {
            case ONCE -> ((fromDt.isBefore(startDt) || startDt.isEqual(fromDt)) &&
                    (finalEndDt.isAfter(startDt) || finalEndDt.isEqual(startDt))) ? 1 : 0;

            case DAY -> {
                ZonedDateTime currentStart = startDt;
                if (fromDt.isAfter(currentStart)) {
                    long intervalsBetween = ChronoUnit.DAYS.between(currentStart, fromDt) / multiplier;
                    if (fromDt.isAfter(currentStart.plusDays(intervalsBetween * multiplier))) {
                        intervalsBetween++;
                    }
                    currentStart = currentStart.plusDays(intervalsBetween * multiplier);
                }
                yield (currentStart.isBefore(finalEndDt) || currentStart.isEqual(finalEndDt))
                        ? (int) (ChronoUnit.DAYS.between(currentStart, finalEndDt) / multiplier + 1) : 0;
            }

            case MONTH -> {
                ZonedDateTime currentStart = startDt;
                if (fromDt.isAfter(currentStart)) {
                    long intervalsBetween = ChronoUnit.MONTHS.between(currentStart, fromDt) / multiplier;
                    if (fromDt.isAfter(currentStart.plusMonths(intervalsBetween * multiplier))) {
                        intervalsBetween++;
                    }
                    currentStart = currentStart.plusMonths(intervalsBetween * multiplier);
                }
                yield (currentStart.isBefore(finalEndDt) || currentStart.isEqual(finalEndDt))
                        ? (int) (ChronoUnit.MONTHS.between(currentStart, finalEndDt) / multiplier + 1) : 0;
            }

            case QUARTER -> {
                ZonedDateTime currentStart = startDt;
                if (fromDt.isAfter(currentStart)) {
                    long intervalsBetween = ChronoUnit.MONTHS.between(currentStart, fromDt) / (3L * multiplier);
                    if (fromDt.isAfter(currentStart.plusMonths(intervalsBetween * 3 * multiplier))) {
                        intervalsBetween++;
                    }
                    currentStart = currentStart.plusMonths(intervalsBetween * 3 * multiplier);
                }
                yield (currentStart.isBefore(finalEndDt) || currentStart.isEqual(finalEndDt))
                        ? (int) ((ChronoUnit.MONTHS.between(currentStart, finalEndDt) / 3) / multiplier + 1) : 0;
            }

            case YEAR -> {
                ZonedDateTime currentStart = startDt;
                if (fromDt.isAfter(currentStart)) {
                    long intervalsBetween = ChronoUnit.YEARS.between(currentStart, fromDt) / multiplier;
                    if (fromDt.isAfter(currentStart.plusYears(intervalsBetween * multiplier))) {
                        intervalsBetween++;
                    }
                    currentStart = currentStart.plusYears(intervalsBetween * multiplier);
                }
                yield (currentStart.isBefore(finalEndDt) || currentStart.isEqual(finalEndDt))
                        ? (int) (ChronoUnit.YEARS.between(currentStart, finalEndDt) / multiplier + 1) : 0;
            }
        };
    }
}
