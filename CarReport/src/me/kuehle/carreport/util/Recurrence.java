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

public class Recurrence {
	private RecurrenceInterval interval;
	private int multiplier;

	public Recurrence(RecurrenceInterval interval) {
		this(interval, 1);
	}

	public Recurrence(RecurrenceInterval interval, int multiplier) {
		this.interval = interval;
		this.multiplier = multiplier;
	}

	public RecurrenceInterval getInterval() {
		return interval;
	}

	public int getMultiplier() {
		return multiplier;
	}

	public int getRecurrencesSince(Date date) {
		DateTime then = new DateTime(date);
		DateTime now = new DateTime();

		int count = 1;
		switch (interval) {
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
