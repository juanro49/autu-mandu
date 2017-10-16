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
    public static int getRecurrencesSince(RecurrenceInterval interval, int multiplier, Date start) {
        return getRecurrencesBetween(interval, multiplier, start, new Date());
    }

    public static int getRecurrencesBetween(RecurrenceInterval interval, int multiplier, Date start,
                                            Date end) {
        return getRecurrencesBetween(interval, multiplier, start, end, start, new Date());
    }

    /**
     * Calculates the recurrences inside a timeframe.
     * @param interval The interval the event occurs.
     * @param multiplier A multiplier.
     * @param start The beginning of the recurring event.
     * @param end The end of the recurring event.
     * @param from The beginning of the timeframe.
     * @param to The end of the timeframe.
     * @return The count of the recurrences.
     */
    public static int getRecurrencesBetween(RecurrenceInterval interval, int multiplier, Date start,
                                            Date end, Date from, Date to) {
        DateTime then = new DateTime(start);
        DateTime now = new DateTime(end);
        if (then.isAfter(now) || to.getTime() < from.getTime()) {
            return 0;
        }
        if (new DateTime(to).isBefore(now)) {
            now = new DateTime(to);
        }
        DateTime fromDateTime = new DateTime(from);

        int count = (then.isAfter(fromDateTime) || then.isEqual(fromDateTime) ? 1 : 0);
        switch (interval) {
            case ONCE:
                break;
            case DAY:
                if (fromDateTime.isAfter(then)) {
                    then = fromDateTime;
                }
                count += Days.daysBetween(then, now).getDays() / multiplier;
                break;
            case MONTH:
                if (fromDateTime.isAfter(then)) {
                    then.plusMonths(Months.monthsBetween(then, new DateTime(from)).getMonths() + 1);
                }
                count += Months.monthsBetween(then, now).getMonths() / multiplier;
                break;
            case QUARTER:
                if (fromDateTime.isAfter(then)) {
                    then.plusMonths((Months.monthsBetween(then, new DateTime(from)).getMonths() / 3)
                            + 3);
                }
                int quarters = Months.monthsBetween(then, now).getMonths() / 3;
                count += quarters / multiplier;
                break;
            case YEAR:
                if (fromDateTime.isAfter(then)) {
                    then.plusYears(Years.yearsBetween(then, new DateTime(from)).getYears() + 1);
                }
                count += Years.yearsBetween(then, now).getYears() / multiplier;
                break;
        }

        return count;
    }
}
