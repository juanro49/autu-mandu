package me.kuehle.carreport;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class Preferences {
	private SharedPreferences prefs;

	public Preferences(Context context) {
		this.prefs = PreferenceManager.getDefaultSharedPreferences(context);
	}

	public int getDefaultCar() {
		return Integer.parseInt(prefs.getString("default_car", "1"));
	}

	public int getDefaultReport() {
		return Integer.parseInt(prefs.getString("default_report", "0"));
	}

	public String getUnitCurrency() {
		return prefs.getString("unit_currency", "EUR");
	}

	public String getUnitDistance() {
		return prefs.getString("unit_distance", "km");
	}

	public String getUnitVolume() {
		return prefs.getString("unit_volume", "l");
	}
}
