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

import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

import me.kuehle.carreport.DistanceEntryMode;
import me.kuehle.carreport.FuelConsumption;
import me.kuehle.carreport.Preferences;
import me.kuehle.carreport.PriceEntryMode;
import me.kuehle.carreport.R;
import me.kuehle.carreport.data.report.AbstractReport;
import me.kuehle.carreport.provider.car.CarColumns;
import me.kuehle.carreport.provider.car.CarCursor;
import me.kuehle.carreport.provider.car.CarSelection;
import me.kuehle.carreport.provider.reminder.TimeSpanUnit;
import me.kuehle.carreport.util.TimeSpan;

public class PreferencesGeneralFragment extends PreferenceFragment {
    private class PreferenceChangeListener implements OnPreferenceChangeListener {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            FuelConsumption fuelConsumption = new FuelConsumption(getActivity());
            String prefKey = preference.getKey();
            if (prefKey.equals("behavior_car")) {
                CarCursor car = new CarSelection().id(Long.parseLong(newValue.toString())).query(getActivity().getContentResolver(), CarColumns.ALL_COLUMNS);
                car.moveToFirst();
                preference.setSummary(car.getName());
            } else if (prefKey.equals("behavior_distance_entry_mode")) {
                DistanceEntryMode mode = DistanceEntryMode.valueOf(newValue.toString());
                preference.setSummary(getString(mode.nameResourceId));
            } else if (prefKey.equals("behavior_price_entry_mode")) {
                PriceEntryMode mode = PriceEntryMode.valueOf(newValue.toString());
                preference.setSummary(getString(mode.nameResourceId));
            } else if (prefKey.equals("behavior_reminder_snooze_duration")) {
                TimeSpan timeSpan = TimeSpan.fromString(newValue.toString(), null);
                preference.setSummary(timeSpan.toLocalizedString(getActivity()));
            } else if (preference instanceof EditTextPreference) {
                preference.setSummary(newValue.toString());

                // Update fuel consumption mLabel
                if (prefKey.equals("unit_distance")) {
                    fuelConsumption.setUnitDistance(newValue.toString());
                } else if (prefKey.equals("unit_volume")) {
                    fuelConsumption.setUnitVolume(newValue.toString());
                }

                updateFuelConsumptionField(fuelConsumption);
            } else if (prefKey.equals("unit_fuel_consumption")) {
                fuelConsumption.setConsumptionType(Integer.parseInt(newValue.toString()));
                updateFuelConsumptionField(fuelConsumption, (ListPreference) preference);
            }

            return true;
        }

        public ListPreference updateFuelConsumptionField(FuelConsumption fuelConsumption) {
            ListPreference prefFuelConsumption = (ListPreference) findPreference("unit_fuel_consumption");
            return updateFuelConsumptionField(fuelConsumption, prefFuelConsumption);
        }

        public ListPreference updateFuelConsumptionField(FuelConsumption fuelConsumption,
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
            ListPreference defaultCar = (ListPreference) findPreference("behavior_default_car");
            long defaultCarId = prefs.getDefaultCar();

            CarCursor car = new CarSelection().query(getActivity().getContentResolver(), CarColumns.ALL_COLUMNS, CarColumns.NAME + " COLLATE UNICODE");
            String[] defaultEntries = new String[car.getCount()];
            String[] defaultEntryValues = new String[car.getCount()];
            for (int i = 0; i < car.getCount(); i++) {
                car.moveToPosition(i);
                defaultEntries[i] = car.getName();
                defaultEntryValues[i] = String.valueOf(car.getId());

                if (car.getId() == defaultCarId) {
                    defaultCar.setSummary(car.getName());
                }
            }

            defaultCar.setEntries(defaultEntries);
            defaultCar.setEntryValues(defaultEntryValues);
            defaultCar.setOnPreferenceChangeListener(onPreferenceChangeListener);
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
            distanceEntryMode.setOnPreferenceChangeListener(onPreferenceChangeListener);
            distanceEntryMode.setSummary(getString(prefs.getDistanceEntryMode().nameResourceId));
        }

        // Behavior price entry mode
        {
            PriceEntryMode[] modes = PriceEntryMode.values();
            String[] entries = new String[modes.length];
            String[] entryValues = new String[modes.length];
            for (int i = 0; i < modes.length; i++) {
                entries[i] = getString(modes[i].nameResourceId);
                entryValues[i] = modes[i].name();
            }

            ListPreference priceEntryMode = (ListPreference) findPreference("behavior_price_entry_mode");
            priceEntryMode.setEntries(entries);
            priceEntryMode.setEntryValues(entryValues);
            priceEntryMode.setOnPreferenceChangeListener(onPreferenceChangeListener);
            priceEntryMode.setSummary(getString(prefs.getPriceEntryMode().nameResourceId));
        }

        // Behavior reminder snooze duration
        {
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
                entries[i] = availableTimeSpans[i].toLocalizedString(getActivity());
                entryValues[i] = availableTimeSpans[i].toString();
            }

            ListPreference snoozeDuration = (ListPreference) findPreference("behavior_reminder_snooze_duration");
            snoozeDuration.setEntries(entries);
            snoozeDuration.setEntryValues(entryValues);
            snoozeDuration.setOnPreferenceChangeListener(onPreferenceChangeListener);
            snoozeDuration.setSummary(prefs.getReminderSnoozeDuration().toLocalizedString(getActivity()));
        }

        // Unit Currency
        {
            EditTextPreference unitCurrency = (EditTextPreference) findPreference("unit_currency");
            unitCurrency.setOnPreferenceChangeListener(onPreferenceChangeListener);
            unitCurrency.setSummary(prefs.getUnitCurrency());
        }

        // Unit Volume
        {
            EditTextPreference unitVolume = (EditTextPreference) findPreference("unit_volume");
            unitVolume.setOnPreferenceChangeListener(onPreferenceChangeListener);
            unitVolume.setSummary(prefs.getUnitVolume());
        }

        // Unit Distance
        {
            EditTextPreference unitDistance = (EditTextPreference) findPreference("unit_distance");
            unitDistance.setOnPreferenceChangeListener(onPreferenceChangeListener);
            unitDistance.setSummary(prefs.getUnitDistance());
        }

        // Unit fuel consumption
        {
            ListPreference fieldFuelConsumption = onPreferenceChangeListener
                    .updateFuelConsumptionField(fuelConsumption);
            fieldFuelConsumption.setOnPreferenceChangeListener(onPreferenceChangeListener);
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
        List<String> reportTitles = new ArrayList<>();
        for (Class<? extends AbstractReport> reportClass : reportClasses) {
            AbstractReport report = AbstractReport.newInstance(reportClass, getActivity());
            if (report != null) {
                reportTitles.add(report.getTitle());
            }
        }

        PreferenceScreen reportOrder = (PreferenceScreen) findPreference("behavior_report_order");
        reportOrder.setSummary(TextUtils.join(", ", reportTitles));
    }
}