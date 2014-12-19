/*
 * Copyright 2013 Jan Kühle
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

package me.kuehle.carreport.gui;

import java.util.ArrayList;
import java.util.List;

import me.kuehle.carreport.DistanceEntryMode;
import me.kuehle.carreport.FuelConsumption;
import me.kuehle.carreport.Preferences;
import me.kuehle.carreport.R;
import me.kuehle.carreport.data.report.AbstractReport;
import me.kuehle.carreport.db.Car;
import me.kuehle.carreport.util.Strings;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;

public class PreferencesGeneralFragment extends PreferenceFragment {
	private class PreferenceChangeListener implements
			OnPreferenceChangeListener {
		@Override
		public boolean onPreferenceChange(Preference preference, Object newValue) {
			FuelConsumption fuelConsumption = new FuelConsumption(getActivity());
			String prefKey = preference.getKey();
			if (prefKey.equals("behavior_car")) {
				Car car = Car.load(Car.class,
						Long.parseLong(newValue.toString()));
				preference.setSummary(car.name);
			} else if (prefKey.equals("behavior_distance_entry_mode")) {
				DistanceEntryMode mode = DistanceEntryMode.valueOf(newValue
						.toString());
				preference.setSummary(getString(mode.nameResourceId));
			} else if (preference instanceof EditTextPreference) {
				preference.setSummary(newValue.toString());

				// Update fuel consumption label
				if (prefKey.equals("unit_distance")) {
					fuelConsumption.setUnitDistance(newValue.toString());
				} else if (prefKey.equals("unit_volume")) {
					fuelConsumption.setUnitVolume(newValue.toString());
				}

				updateFuelConsumptionField(fuelConsumption);
			} else if (prefKey.equals("unit_fuel_consumption")) {
				fuelConsumption.setConsumptionType(Integer.parseInt(newValue
						.toString()));
				updateFuelConsumptionField(fuelConsumption,
						(ListPreference) preference);
			}
			
			return true;
		}

		public ListPreference updateFuelConsumptionField(
				FuelConsumption fuelConsumption) {
			ListPreference prefFuelConsumption = (ListPreference) findPreference("unit_fuel_consumption");
			return this.updateFuelConsumptionField(fuelConsumption,
					prefFuelConsumption);
		}

		public ListPreference updateFuelConsumptionField(
				FuelConsumption fuelConsumption,
				ListPreference prefFuelConsumption) {
			String[] entries = fuelConsumption.getUnitsEntries();
			String[] entryValues = fuelConsumption.getUnitsEntryValues();
			prefFuelConsumption.setEntries(entries);
			prefFuelConsumption.setEntryValues(entryValues);
			prefFuelConsumption.setSummary(fuelConsumption.getUnitLabel());
			return prefFuelConsumption;
		}
	}

	private PreferenceChangeListener onPreferenceChangeListener = new PreferenceChangeListener();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		addPreferencesFromResource(R.xml.preferences_general);
		PreferenceManager.setDefaultValues(getActivity(),
				R.xml.preferences_general, false);

		Preferences prefs = new Preferences(getActivity());
		FuelConsumption fuelConsumption = new FuelConsumption(getActivity());

		// Behavior report order
		{
			updateReportOrderSummary();
		}

		// Behavior default car
		{
			List<Car> cars = Car.getAll();
			String[] defaultEntries = new String[cars.size()];
			String[] defaultEntryValues = new String[cars.size()];
			for (int i = 0; i < cars.size(); i++) {
				defaultEntries[i] = cars.get(i).name;
				defaultEntryValues[i] = String.valueOf(cars.get(i).id);
			}

			ListPreference defaultCar = (ListPreference) findPreference("behavior_default_car");
			defaultCar.setEntries(defaultEntries);
			defaultCar.setEntryValues(defaultEntryValues);
			defaultCar
					.setOnPreferenceChangeListener(onPreferenceChangeListener);
			Car car = Car.load(Car.class, prefs.getDefaultCar());
			defaultCar.setSummary(car.name);
		}

		// Behavior distance entry mode
		{
			DistanceEntryMode[] modes = DistanceEntryMode.values();
			String[] entries = new String[modes.length];
			String[] entryValues = new String[modes.length];
			for (int i = 0; i < modes.length; i++) {
				entries[i] = getString(modes[i].nameResourceId);
				entryValues[i] = modes[i].name();
			}

			ListPreference distanceEntryMode = (ListPreference) findPreference("behavior_distance_entry_mode");
			distanceEntryMode.setEntries(entries);
			distanceEntryMode.setEntryValues(entryValues);
			distanceEntryMode
					.setOnPreferenceChangeListener(onPreferenceChangeListener);
			distanceEntryMode
					.setSummary(getString(prefs.getDistanceEntryMode().nameResourceId));
		}

		// Unit Currency
		{
			EditTextPreference unitCurrency = (EditTextPreference) findPreference("unit_currency");
			unitCurrency
					.setOnPreferenceChangeListener(onPreferenceChangeListener);
			unitCurrency.setSummary(prefs.getUnitCurrency());
		}

		// Unit Volume
		{
			EditTextPreference unitVolume = (EditTextPreference) findPreference("unit_volume");
			unitVolume
					.setOnPreferenceChangeListener(onPreferenceChangeListener);
			unitVolume.setSummary(prefs.getUnitVolume());
		}

		// Unit Distance
		{
			EditTextPreference unitDistance = (EditTextPreference) findPreference("unit_distance");
			unitDistance
					.setOnPreferenceChangeListener(onPreferenceChangeListener);
			unitDistance.setSummary(prefs.getUnitDistance());
		}

		// Unit fuel consumption
		{
			ListPreference fieldFuelConsumption = onPreferenceChangeListener
					.updateFuelConsumptionField(fuelConsumption);
			fieldFuelConsumption
					.setOnPreferenceChangeListener(onPreferenceChangeListener);
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		updateReportOrderSummary();
	}

	private void updateReportOrderSummary() {
		List<Class<? extends AbstractReport>> reportClasses = new Preferences(
				getActivity()).getReportOrder();
		List<String> reportTitles = new ArrayList<String>();
		for (Class<? extends AbstractReport> reportClass : reportClasses) {
			AbstractReport report = AbstractReport.newInstance(reportClass,
					getActivity());
			reportTitles.add(report.getTitle());
		}

		PreferenceScreen reportOrder = (PreferenceScreen) findPreference("behavior_report_order");
		reportOrder.setSummary(Strings.join(", ", reportTitles));
	}
}