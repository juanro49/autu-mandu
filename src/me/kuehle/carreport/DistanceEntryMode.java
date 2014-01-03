package me.kuehle.carreport;

public enum DistanceEntryMode {
	TRIP(R.string.distance_entry_mode_trip), TOTAL(
			R.string.distance_entry_mode_total), SHOW_SELECTOR(
			R.string.distance_entry_mode_show_selector);

	public final int nameResourceId;

	DistanceEntryMode(int nameResourceId) {
		this.nameResourceId = nameResourceId;
	}
}
