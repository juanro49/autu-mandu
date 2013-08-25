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

import java.util.List;

import me.kuehle.carreport.db.Car;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

public class Preferences {
	private SharedPreferences prefs;

	public Preferences(Context context) {
		this.prefs = PreferenceManager.getDefaultSharedPreferences(context);
	}

	public long getDefaultCar() {
		int id = Integer.parseInt(prefs.getString("default_car", "1"));
		List<Car> cars = Car.getAll();
		if (cars.size() == 0) {
			return 0;
		}

		for (Car car : cars) {
			if (car.getId() == id) {
				return id;
			}
		}

		return cars.get(0).getId();
	}

	public String getDropboxAccount() {
		return prefs.getString("sync_dropbox_account", null);
	}

	public String getDropboxKey() {
		return prefs.getString("sync_dropbox_key", null);
	}

	public String getDropboxLocalRev() {
		return prefs.getString("sync_dropbox_rev", null);
	}

	public String getDropboxSecret() {
		return prefs.getString("sync_dropbox_secret", null);
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

	public boolean isShowCarMenu() {
		return prefs.getBoolean("default_show_car_menu", true);
	}

	public boolean isShowLegend() {
		return prefs.getBoolean("appearance_show_legend", false);
	}

	public boolean isSyncOnChange() {
		return prefs.getBoolean("sync_on_change", true);
	}

	public boolean isSyncOnStart() {
		return prefs.getBoolean("sync_on_start", true);
	}

	public void setDropboxAccount(String account) {
		Editor edit = prefs.edit();
		edit.putString("sync_dropbox_account", account);
		edit.apply();
	}

	public void setDropboxKey(String key) {
		Editor edit = prefs.edit();
		edit.putString("sync_dropbox_key", key);
		edit.apply();
	}

	public void setDropboxLocalRev(String rev) {
		Editor edit = prefs.edit();
		edit.putString("sync_dropbox_rev", rev);
		edit.apply();
	}

	public void setDropboxSecret(String secret) {
		Editor edit = prefs.edit();
		edit.putString("sync_dropbox_secret", secret);
		edit.apply();
	}
}
