package me.kuehle.carreport.util;

public enum RecurrenceInterval {
	ONCE(0), DAY(1), MONTH(2), QUARTER(3), YEAR(4);

	private int value;

	private RecurrenceInterval(int i) {
		value = i;
	}

	public int getValue() {
		return value;
	}

	public static RecurrenceInterval getByValue(int i) {
		switch (i) {
		case 1:
			return DAY;
		case 2:
			return MONTH;
		case 3:
			return QUARTER;
		case 4:
			return YEAR;
		default:
			return ONCE;
		}
	}
}
