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
            if (prefKey.equals("behavior_default_car")) {
                mViewModel.getCarById(Long.parseLong(newValue.toString()), car -> {
                    if (car != null && isAdded()) {
                        requireActivity().runOnUiThread(() -> preference.setSummary(car.getName()));
                    }
                });
            } else if (prefKey.equals("behavior_distance_entry_mode")) {
                DistanceEntryMode mode = DistanceEntryMode.valueOf(newValue.toString());
                preference.setSummary(getString(mode.getNameResourceId()));
            } else if (prefKey.equals("behavior_price_entry_mode")) {
                PriceEntryMode mode = PriceEntryMode.valueOf(newValue.toString());
                preference.setSummary(getString(mode.getNameResourceId()));
            } else if (prefKey.equals("behavior_reminder_snooze_duration")) {
                TimeSpan timeSpan = TimeSpan.fromString(newValue.toString(), null);
                if (timeSpan != null) {
                    preference.setSummary(timeSpan.toLocalizedString(requireContext()));
                }
            } else if (preference instanceof EditTextPreference) {
                preference.setSummary(newValue.toString());

                // Update fuel consumption mLabel
                FuelConsumption fuelConsumption = new FuelConsumption(requireContext());
                if (prefKey.equals("unit_distance")) {
                    fuelConsumption.setUnitDistance(newValue.toString());
                } else if (prefKey.equals("unit_volume")) {
                    fuelConsumption.setUnitVolume(newValue.toString());
                }

                updateFuelConsumptionField(fuelConsumption);
            } else if (prefKey.equals("unit_fuel_consumption")) {
                FuelConsumption fuelConsumption = new FuelConsumption(requireContext());
                fuelConsumption.setConsumptionType(Integer.parseInt(newValue.toString()));
                updateFuelConsumptionField(fuelConsumption, (ListPreference) preference);
            }

            return true;
        }

        public void updateFuelConsumptionField(FuelConsumption fuelConsumption) {
            ListPreference prefFuelConsumption = findPreference("unit_fuel_consumption");
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
        setupSimplePreference("unit_currency", prefs.getUnitCurrency());

        // Unit Volume
        setupSimplePreference("unit_volume", prefs.getUnitVolume());

        // Unit Distance
        setupSimplePreference("unit_distance", prefs.getUnitDistance());

        // Unit fuel consumption
        ListPreference fieldFuelConsumption = findPreference("unit_fuel_consumption");
        if (fieldFuelConsumption != null) {
            onPreferenceChangeListener.updateFuelConsumptionField(fuelConsumption, fieldFuelConsumption);
            fieldFuelConsumption.setOnPreferenceChangeListener(onPreferenceChangeListener);
        }
    }

    private void setupDefaultCarPreference(Preferences prefs) {
        ListPreference defaultCar = findPreference("behavior_default_car");
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

        ListPreference distanceEntryMode = findPreference("behavior_distance_entry_mode");
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

        ListPreference priceEntryMode = findPreference("behavior_price_entry_mode");
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

        ListPreference snoozeDuration = findPreference("behavior_reminder_snooze_duration");
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

        Preference reportOrder = findPreference("behavior_report_order");
        if (reportOrder != null) {
            reportOrder.setSummary(TextUtils.join(", ", reportTitles));
        }
    }
}
