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

package org.juanro.autumandu.gui.pref;

import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import org.juanro.autumandu.DistanceEntryMode;
import org.juanro.autumandu.FuelConsumption;
import org.juanro.autumandu.Preferences;
import org.juanro.autumandu.PriceEntryMode;
import org.juanro.autumandu.R;
import org.juanro.autumandu.data.report.AbstractReport;
import org.juanro.autumandu.model.entity.Car;
import org.juanro.autumandu.util.TimeSpan;
import org.juanro.autumandu.model.entity.helper.TimeSpanUnit;
import org.juanro.autumandu.viewmodel.PreferencesGeneralViewModel;

import java.util.ArrayList;
import java.util.List;

public class PreferencesGeneralFragment extends PreferenceFragmentCompat {

    private PreferencesGeneralViewModel mViewModel;

    private class PreferenceChangeListener implements OnPreferenceChangeListener {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            String prefKey = preference.getKey();
            switch (prefKey) {
                case Preferences.KEY_DEFAULT_CAR -> handleDefaultCarChange(preference, newValue);
                case Preferences.KEY_DISTANCE_ENTRY_MODE -> handleDistanceEntryModeChange(preference, newValue);
                case Preferences.KEY_PRICE_ENTRY_MODE -> handlePriceEntryModeChange(preference, newValue);
                case Preferences.KEY_REMINDER_SNOOZE_DURATION -> handleSnoozeDurationChange(preference, newValue);
                case Preferences.KEY_UNIT_DISTANCE, Preferences.KEY_UNIT_VOLUME -> handleUnitChange(preference, newValue);
                case Preferences.KEY_UNIT_FUEL_CONSUMPTION -> handleFuelConsumptionChange((ListPreference) preference, newValue);
                case Preferences.KEY_THEME -> handleThemeChange((ListPreference) preference, newValue);
                case Preferences.KEY_DYNAMIC_COLOR -> requireActivity().recreate();
                default -> {
                    if (preference instanceof EditTextPreference) {
                        preference.setSummary(newValue.toString());
                    }
                }
            }

            return true;
        }

        private void handleDefaultCarChange(Preference preference, Object newValue) {
            mViewModel.getCarById(Long.parseLong(newValue.toString()), car -> {
                if (car != null && isAdded()) {
                    requireActivity().runOnUiThread(() -> preference.setSummary(car.getName()));
                }
            });
        }

        private void handleDistanceEntryModeChange(Preference preference, Object newValue) {
            DistanceEntryMode mode = DistanceEntryMode.valueOf(newValue.toString());
            preference.setSummary(getString(mode.getNameResourceId()));
        }

        private void handlePriceEntryModeChange(Preference preference, Object newValue) {
            PriceEntryMode mode = PriceEntryMode.valueOf(newValue.toString());
            preference.setSummary(getString(mode.getNameResourceId()));
        }

        private void handleSnoozeDurationChange(Preference preference, Object newValue) {
            TimeSpan timeSpan = TimeSpan.fromString(newValue.toString(), null);
            if (timeSpan != null) {
                preference.setSummary(timeSpan.toLocalizedString(requireContext()));
            }
        }

        private void handleUnitChange(Preference preference, Object newValue) {
            preference.setSummary(newValue.toString());
            FuelConsumption fuelConsumption = new FuelConsumption(requireContext());
            if (preference.getKey().equals(Preferences.KEY_UNIT_DISTANCE)) {
                fuelConsumption.setUnitDistance(newValue.toString());
            } else {
                fuelConsumption.setUnitVolume(newValue.toString());
            }
            updateFuelConsumptionField(fuelConsumption);
        }

        private void handleFuelConsumptionChange(ListPreference preference, Object newValue) {
            FuelConsumption fuelConsumption = new FuelConsumption(requireContext());
            fuelConsumption.setConsumptionType(Integer.parseInt(newValue.toString()));
            updateFuelConsumptionField(fuelConsumption, preference);
        }

        private void handleThemeChange(ListPreference preference, Object newValue) {
            String theme = newValue.toString();
            switch (theme) {
                case "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                case "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                default -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
            }
            updateThemeSummary(preference, theme);
        }

        private void updateThemeSummary(ListPreference preference, String value) {
            String[] values = getResources().getStringArray(R.array.theme_values);
            String[] entries = getResources().getStringArray(R.array.theme_entries);
            for (int i = 0; i < values.length; i++) {
                if (values[i].equals(value)) {
                    preference.setSummary(entries[i]);
                    break;
                }
            }
        }

        public void updateFuelConsumptionField(FuelConsumption fuelConsumption) {
            ListPreference prefFuelConsumption = findPreference(Preferences.KEY_UNIT_FUEL_CONSUMPTION);
            if (prefFuelConsumption != null) {
                updateFuelConsumptionField(fuelConsumption, prefFuelConsumption);
            }
        }

        public void updateFuelConsumptionField(FuelConsumption fuelConsumption,
                                               ListPreference prefFuelConsumption) {
            String[] entries = fuelConsumption.getUnitsEntries();
            String[] entryValues = fuelConsumption.getUnitsEntryValues();
            prefFuelConsumption.setEntries(entries);
            prefFuelConsumption.setEntryValues(entryValues);
            prefFuelConsumption.setSummary(fuelConsumption.getUnitLabel());
        }
    }

    private final PreferenceChangeListener onPreferenceChangeListener = new PreferenceChangeListener();

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.preferences_general, rootKey);
        PreferenceManager.setDefaultValues(requireContext(), R.xml.preferences_general, false);

        mViewModel = new ViewModelProvider(this).get(PreferencesGeneralViewModel.class);

        Preferences prefs = new Preferences(requireContext());
        FuelConsumption fuelConsumption = new FuelConsumption(requireContext());

        // Behavior report order
        updateReportOrderSummary();

        // Behavior default car
        setupDefaultCarPreference(prefs);

        // Behavior distance entry mode
        setupDistanceEntryModePreference(prefs);

        // Behavior price entry mode
        setupPriceEntryModePreference(prefs);

        // Behavior reminder snooze duration
        setupSnoozeDurationPreference(prefs);

        // Unit Currency
        setupSimplePreference(Preferences.KEY_UNIT_CURRENCY, prefs.getUnitCurrency());

        // Unit Volume
        setupSimplePreference(Preferences.KEY_UNIT_VOLUME, prefs.getUnitVolume());

        // Unit Distance
        setupSimplePreference(Preferences.KEY_UNIT_DISTANCE, prefs.getUnitDistance());

        // UI Theme
        ListPreference themePref = findPreference(Preferences.KEY_THEME);
        if (themePref != null) {
            onPreferenceChangeListener.updateThemeSummary(themePref, prefs.getTheme());
            themePref.setOnPreferenceChangeListener(onPreferenceChangeListener);
        }

        // UI Dynamic Color
        Preference dynamicColorPref = findPreference(Preferences.KEY_DYNAMIC_COLOR);
        if (dynamicColorPref != null) {
            dynamicColorPref.setOnPreferenceChangeListener(onPreferenceChangeListener);
        }

        // Unit fuel consumption
        ListPreference fieldFuelConsumption = findPreference(Preferences.KEY_UNIT_FUEL_CONSUMPTION);
        if (fieldFuelConsumption != null) {
            onPreferenceChangeListener.updateFuelConsumptionField(fuelConsumption, fieldFuelConsumption);
            fieldFuelConsumption.setOnPreferenceChangeListener(onPreferenceChangeListener);
        }
    }

    private void setupDefaultCarPreference(Preferences prefs) {
        ListPreference defaultCar = findPreference(Preferences.KEY_DEFAULT_CAR);
        if (defaultCar == null) return;

        long defaultCarId = prefs.getDefaultCar();
        mViewModel.getCars().observe(this, cars -> {
            String[] defaultEntries = new String[cars.size()];
            String[] defaultEntryValues = new String[cars.size()];
            String currentSummary = null;

            for (int i = 0; i < cars.size(); i++) {
                Car car = cars.get(i);
                defaultEntries[i] = car.getName();
                defaultEntryValues[i] = String.valueOf(car.getId());

                if (car.getId() == defaultCarId) {
                    currentSummary = car.getName();
                }
            }

            defaultCar.setEntries(defaultEntries);
            defaultCar.setEntryValues(defaultEntryValues);
            if (currentSummary != null) {
                defaultCar.setSummary(currentSummary);
            }
            defaultCar.setOnPreferenceChangeListener(onPreferenceChangeListener);
        });
    }

    private void setupDistanceEntryModePreference(Preferences prefs) {
        DistanceEntryMode[] modes = DistanceEntryMode.values();
        String[] entries = new String[modes.length];
        String[] entryValues = new String[modes.length];
        for (int i = 0; i < modes.length; i++) {
            entries[i] = getString(modes[i].getNameResourceId());
            entryValues[i] = modes[i].name();
        }

        ListPreference distanceEntryMode = findPreference(Preferences.KEY_DISTANCE_ENTRY_MODE);
        if (distanceEntryMode != null) {
            distanceEntryMode.setEntries(entries);
            distanceEntryMode.setEntryValues(entryValues);
            distanceEntryMode.setOnPreferenceChangeListener(onPreferenceChangeListener);
            distanceEntryMode.setSummary(getString(prefs.getDistanceEntryMode().getNameResourceId()));
        }
    }

    private void setupPriceEntryModePreference(Preferences prefs) {
        PriceEntryMode[] modes = PriceEntryMode.values();
        String[] entries = new String[modes.length];
        String[] entryValues = new String[modes.length];
        for (int i = 0; i < modes.length; i++) {
            entries[i] = getString(modes[i].getNameResourceId());
            entryValues[i] = modes[i].name();
        }

        ListPreference priceEntryMode = findPreference(Preferences.KEY_PRICE_ENTRY_MODE);
        if (priceEntryMode != null) {
            priceEntryMode.setEntries(entries);
            priceEntryMode.setEntryValues(entryValues);
            priceEntryMode.setOnPreferenceChangeListener(onPreferenceChangeListener);
            priceEntryMode.setSummary(getString(prefs.getPriceEntryMode().getNameResourceId()));
        }
    }

    private void setupSnoozeDurationPreference(Preferences prefs) {
        TimeSpan[] availableTimeSpans = {
                new TimeSpan(TimeSpanUnit.DAY, 1),
                new TimeSpan(TimeSpanUnit.DAY, 2),
                new TimeSpan(TimeSpanUnit.DAY, 7),
                new TimeSpan(TimeSpanUnit.DAY, 14),
                new TimeSpan(TimeSpanUnit.MONTH, 1),
                new TimeSpan(TimeSpanUnit.MONTH, 2),
                new TimeSpan(TimeSpanUnit.MONTH, 3),
                new TimeSpan(TimeSpanUnit.MONTH, 6)
        };
        String[] entries = new String[availableTimeSpans.length];
        String[] entryValues = new String[availableTimeSpans.length];
        for (int i = 0; i < availableTimeSpans.length; i++) {
            entries[i] = availableTimeSpans[i].toLocalizedString(requireContext());
            entryValues[i] = availableTimeSpans[i].toString();
        }

        ListPreference snoozeDuration = findPreference(Preferences.KEY_REMINDER_SNOOZE_DURATION);
        if (snoozeDuration != null) {
            snoozeDuration.setEntries(entries);
            snoozeDuration.setEntryValues(entryValues);
            snoozeDuration.setOnPreferenceChangeListener(onPreferenceChangeListener);
            snoozeDuration.setSummary(prefs.getReminderSnoozeDuration().toLocalizedString(requireContext()));
        }
    }

    private void setupSimplePreference(String key, String summary) {
        Preference preference = findPreference(key);
        if (preference != null) {
            preference.setOnPreferenceChangeListener(onPreferenceChangeListener);
            preference.setSummary(summary);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateReportOrderSummary();
    }

    private void updateReportOrderSummary() {
        List<Class<? extends AbstractReport>> reportClasses = new Preferences(
                requireContext()).getReportOrder();
        List<String> reportTitles = new ArrayList<>();
        for (Class<? extends AbstractReport> reportClass : reportClasses) {
            AbstractReport report = AbstractReport.newInstance(reportClass, requireContext());
            if (report != null) {
                reportTitles.add(report.getTitle());
            }
        }

        Preference reportOrder = findPreference(Preferences.KEY_REPORT_ORDER);
        if (reportOrder != null) {
            reportOrder.setSummary(TextUtils.join(", ", reportTitles));
        }
    }
}
