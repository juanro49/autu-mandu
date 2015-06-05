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

    public static int getRecurrencesBetween(RecurrenceInterval interval, int multiplier, Date start, Date end) {
        DateTime then = new DateTime(start);
        DateTime now = new DateTime(end);

        int count = 1;
        switch (interval) {
            case ONCE:
                break;
            case DAY:
                count += Days.daysBetween(then, now).getDays() / multiplier;
                break;
            case MONTH:
                count += Months.monthsBetween(then, now).getMonths() / multiplier;
                break;
            case QUARTER:
                int quarters = Months.monthsBetween(then, now).getMonths() / 3;
                count += quarters / multiplier;
                break;
            case YEAR:
                count += Years.yearsBetween(then, now).getYears() / multiplier;
                break;
        }

        return count;
    }
}
