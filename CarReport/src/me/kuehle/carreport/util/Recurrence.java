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
		if (this.equals(RecurrenceInterval.DAY)) {
			count += Days.daysBetween(then, now).getDays() / multiplier;
		} else if (this.equals(RecurrenceInterval.MONTH)) {
			count += Months.monthsBetween(then, now).getMonths() / multiplier;
		} else if (this.equals(RecurrenceInterval.QUARTER)) {
			int quarters = Months.monthsBetween(then, now).getMonths() / 3;
			count += quarters / multiplier;
		} else if (this.equals(RecurrenceInterval.YEAR)) {
			count += Years.yearsBetween(then, now).getYears() / multiplier;
		}
		return count;
	}
}
