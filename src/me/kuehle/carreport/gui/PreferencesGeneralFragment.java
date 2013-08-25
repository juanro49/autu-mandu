package me.kuehle.carreport.gui;

import java.util.List;

import me.kuehle.carreport.Preferences;
import me.kuehle.carreport.R;
import me.kuehle.carreport.db.Car;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceChangeListener;

public class PreferencesGeneralFragment extends PreferenceFragment {
	private OnPreferenceChangeListener onPreferenceChangeListener = new OnPreferenceChangeListener() {
		@Override
		public boolean onPreferenceChange(Preference preference, Object newValue) {
			if (preference.getKey().equals("default_car")) {
				Car car = Car.load(Car.class,
						Long.parseLong(newValue.toString()));
				preference.setSummary(car.name);
			} else if (preference instanceof EditTextPreference) {
				preference.setSummary(newValue.toString());
			}

			return true;
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		addPreferencesFromResource(R.xml.preferences_general);
		PreferenceManager.setDefaultValues(getActivity(),
				R.xml.preferences_general, false);

		Preferences prefs = new Preferences(getActivity());

		// Default Car
		{
			List<Car> cars = Car.getAll();
			String[] defaultEntries = new String[cars.size()];
			String[] defaultEntryValues = new String[cars.size()];
			for (int i = 0; i < cars.size(); i++) {
				defaultEntries[i] = cars.get(i).name;
				defaultEntryValues[i] = String.valueOf(cars.get(i).getId());
			}

			ListPreference defaultCar = (ListPreference) findPreference("default_car");
			defaultCar.setEntries(defaultEntries);
			defaultCar.setEntryValues(defaultEntryValues);
			defaultCar
					.setOnPreferenceChangeListener(onPreferenceChangeListener);
			Car car = Car.load(Car.class, prefs.getDefaultCar());
			defaultCar.setSummary(car.name);
		}

		// Default car menu
		{
			CheckBoxPreference showCarMenu = (CheckBoxPreference) findPreference("default_show_car_menu");
			showCarMenu
					.setOnPreferenceChangeListener(onPreferenceChangeListener);
		}

		// Appearance color sections
		{
			CheckBoxPreference colorSections = (CheckBoxPreference) findPreference("appearance_color_sections");
			colorSections
					.setOnPreferenceChangeListener(onPreferenceChangeListener);
		}

		// Appearance show legend
		{
			CheckBoxPreference colorSections = (CheckBoxPreference) findPreference("appearance_show_legend");
			colorSections
					.setOnPreferenceChangeListener(onPreferenceChangeListener);
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

		// Unit Volume
		{
			EditTextPreference unitDistance = (EditTextPreference) findPreference("unit_distance");
			unitDistance
					.setOnPreferenceChangeListener(onPreferenceChangeListener);
			unitDistance.setSummary(prefs.getUnitDistance());
		}
	}
}