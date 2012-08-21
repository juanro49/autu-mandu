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

package me.kuehle.carreport;

import me.kuehle.carreport.db.Car;
import android.app.backup.BackupManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class Preferences {
	private Context context;
	private SharedPreferences prefs;

	public Preferences(Context context) {
		this.context = context;
		this.prefs = PreferenceManager.getDefaultSharedPreferences(context);
	}

	public int getDefaultCar() {
		int id = Integer.parseInt(prefs.getString("default_car", "1"));
		Car[] cars = Car.getAll();
		for (Car car : cars) {
			if (car.getId() == id) {
				return id;
			}
		}
		return cars[0].getId();
	}

	public int getDefaultReport() {
		return Integer.parseInt(prefs.getString("default_report", "0"));
	}

	public int getOverallSectionPos() {
		return Integer.parseInt(prefs.getString(
				"appearance_overall_section_pos", "0"));
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

	public boolean isColorSections() {
		return prefs.getBoolean("appearance_color_sections", true);
	}

	public boolean isShowLegend() {
		return prefs.getBoolean("appearance_show_legend", false);
	}
	
	public boolean isValidateDontAskAgain(int field) {
		return prefs.getBoolean(String.format("validate_dont_ask_again_f_%d", field), false);
	}
	
	public void setValidateDontAskAgain(int field, boolean value) {
		SharedPreferences.Editor editor = prefs.edit();
		editor.putBoolean(String.format("validate_dont_ask_again_f_%d", field), value);
		if(editor.commit()) {
			BackupManager backupManager = new BackupManager(context);
			backupManager.dataChanged();
		}
	}
}
