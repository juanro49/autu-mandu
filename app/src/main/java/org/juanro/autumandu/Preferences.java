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

package org.juanro.autumandu;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.juanro.autumandu.data.report.AbstractReport;
import org.juanro.autumandu.data.report.CostsReport;
import org.juanro.autumandu.data.report.FuelConsumptionReport;
import org.juanro.autumandu.data.report.FuelPriceReport;
import org.juanro.autumandu.data.report.MileageReport;
import org.juanro.autumandu.data.report.OverallCostsReport;
import org.juanro.autumandu.data.report.TripReport;
import org.juanro.autumandu.model.entity.helper.TimeSpanUnit;
import org.juanro.autumandu.util.TimeSpan;

/**
 * Utility class for managing application preferences.
 */
public class Preferences {
    private static final String TAG = "Preferences";

    // Preference Keys
    public static final String KEY_DEFAULT_CAR = "behavior_default_car";
    public static final String KEY_DISTANCE_ENTRY_MODE = "behavior_distance_entry_mode";
    public static final String KEY_PRICE_ENTRY_MODE = "behavior_price_entry_mode";
    public static final String KEY_SYNC_LOCAL_FILE_REV = "sync_local_file_rev";
    public static final String KEY_SYNC_LOCAL_FILE_LAST_MODIFIED = "sync_local_file_last_modified";
    public static final String KEY_SYNC_CONFLICT = "sync_conflict";
    public static final String KEY_REPORT_ORDER = "behavior_report_order";
    public static final String KEY_REMINDER_SNOOZE_DURATION = "behavior_reminder_snooze_duration";
    public static final String KEY_UNIT_CURRENCY = "unit_currency";
    public static final String KEY_UNIT_DISTANCE = "unit_distance";
    public static final String KEY_UNIT_VOLUME = "unit_volume";
    public static final String KEY_UNIT_FUEL_CONSUMPTION = "unit_fuel_consumption";
    public static final String KEY_AUTO_GUESS_MISSING_DATA = "behavior_auto_guess_missing_data";
    public static final String KEY_SHOW_CAR_MENU = "behavior_show_car_menu";
    public static final String KEY_SHOW_CARBUROID = "behavior_show_carburoid";
    public static final String KEY_BACKUP_FOLDER = "backup_folder";
    public static final String KEY_BACKUP_FOLDER_DEFAULT = "backup_folder_default";
    public static final String KEY_AUTO_BACKUP = "behavior_auto_backup";
    public static final String KEY_KEEP_BACKUPS = "behaviour_keep_backups";
    public static final String KEY_THEME = "ui_theme";
    public static final String KEY_DYNAMIC_COLOR = "ui_dynamic_color";

    private final Context context;
    private final SharedPreferences prefs;

    public Preferences(@NonNull Context context) {
        this.context = context;
        this.prefs = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public long getDefaultCar() {
        var idStr = prefs.getString(KEY_DEFAULT_CAR, "0");
        try {
            return Long.parseLong(Objects.requireNonNullElse(idStr, "0"));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public DistanceEntryMode getDistanceEntryMode() {
        var mode = prefs.getString(KEY_DISTANCE_ENTRY_MODE, "TOTAL");
        try {
            return DistanceEntryMode.valueOf(mode);
        } catch (Exception e) {
            return DistanceEntryMode.TOTAL;
        }
    }

    public PriceEntryMode getPriceEntryMode() {
        var mode = prefs.getString(KEY_PRICE_ENTRY_MODE, "TOTAL_AND_VOLUME");
        try {
            return PriceEntryMode.valueOf(mode);
        } catch (Exception e) {
            return PriceEntryMode.TOTAL_AND_VOLUME;
        }
    }

    @Nullable
    public String getSyncLocalFileRev() {
        return prefs.getString(KEY_SYNC_LOCAL_FILE_REV, null);
    }

    public void setSyncLocalFileRev(@Nullable String rev) {
        putString(KEY_SYNC_LOCAL_FILE_REV, rev);
    }

    public long getSyncLocalFileLastModified() {
        return prefs.getLong(KEY_SYNC_LOCAL_FILE_LAST_MODIFIED, 0);
    }

    public void setSyncLocalFileLastModified(long lastModified) {
        prefs.edit().putLong(KEY_SYNC_LOCAL_FILE_LAST_MODIFIED, lastModified).apply();
    }

    public boolean hasSyncConflict() {
        return prefs.getBoolean(KEY_SYNC_CONFLICT, false);
    }

    public void setSyncConflict(boolean conflict) {
        prefs.edit().putBoolean(KEY_SYNC_CONFLICT, conflict).apply();
    }

    public List<Class<? extends AbstractReport>> getReportOrder() {
        List<Class<? extends AbstractReport>> defaultOrder = List.of(
                FuelConsumptionReport.class,
                FuelPriceReport.class,
                MileageReport.class,
                TripReport.class,
                CostsReport.class,
                OverallCostsReport.class
        );

        var reportNames = prefs.getString(KEY_REPORT_ORDER, null);
        if (reportNames == null) {
            return new ArrayList<>(defaultOrder);
        }

        List<Class<? extends AbstractReport>> savedOrder = Arrays.stream(reportNames.split(","))
                .map(name -> {
                    try {
                        var cls = Class.forName(name);
                        if (AbstractReport.class.isAssignableFrom(cls)) {
                            //noinspection unchecked
                            return (Class<? extends AbstractReport>) cls;
                        }
                        return null;
                    } catch (Exception e) {
                        Log.w(TAG, "Error loading report class: " + name);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(ArrayList::new));

        // Ensure new reports are added if they were missing from the saved order
        for (Class<? extends AbstractReport> reportClass : defaultOrder) {
            if (!savedOrder.contains(reportClass)) {
                savedOrder.add(reportClass);
            }
        }

        return savedOrder;
    }

    public void setReportOrder(@NonNull List<Class<? extends AbstractReport>> reports) {
        var names = reports.stream()
                .map(Class::getName)
                .collect(Collectors.joining(","));
        putString(KEY_REPORT_ORDER, names);
    }

    public TimeSpan getReminderSnoozeDuration() {
        var data = prefs.getString(KEY_REMINDER_SNOOZE_DURATION, "7 DAY");
        return TimeSpan.fromString(data, new TimeSpan(TimeSpanUnit.DAY, 7));
    }

    public String getUnitCurrency() {
        return prefs.getString(KEY_UNIT_CURRENCY, "EUR");
    }

    public String getUnitDistance() {
        return prefs.getString(KEY_UNIT_DISTANCE, "km");
    }

    public String getUnitVolume() {
        return prefs.getString(KEY_UNIT_VOLUME, "l");
    }

    public int getUnitFuelConsumption() {
        var val = prefs.getString(KEY_UNIT_FUEL_CONSUMPTION, "0");
        return Integer.parseInt(Objects.requireNonNullElse(val, "0"));
    }

    public boolean isAutoGuessMissingDataEnabled() {
        return prefs.getBoolean(KEY_AUTO_GUESS_MISSING_DATA, false);
    }

    public boolean isShowCarMenu() {
        return prefs.getBoolean(KEY_SHOW_CAR_MENU, true);
    }

    public boolean isShowCarburoidEnabled() {
        return prefs.getBoolean(KEY_SHOW_CARBUROID, true);
    }

    public String getBackupPath() {
        return prefs.getString(KEY_BACKUP_FOLDER, getAppFilesDirPath());
    }

    public void setBackupPath(@NonNull String path) {
        putString(KEY_BACKUP_FOLDER, path);
    }

    public String getDefaultBackupPath() {
        return prefs.getString(KEY_BACKUP_FOLDER_DEFAULT, getAppFilesDirPath());
    }

    public void restoreDefaultBackupPath() {
        putString(KEY_BACKUP_FOLDER, getAppFilesDirPath());
    }

    public boolean isAutoBackupEnabled() {
        return prefs.getBoolean(KEY_AUTO_BACKUP, false);
    }

    public int getAutoBackupRetention() {
        return prefs.getInt(KEY_KEEP_BACKUPS, 12);
    }

    public String getTheme() {
        return prefs.getString(KEY_THEME, "system");
    }

    public boolean isDynamicColorEnabled() {
        return prefs.getBoolean(KEY_DYNAMIC_COLOR, true);
    }

    private void putString(String key, String value) {
        prefs.edit().putString(key, value).apply();
    }

    @NonNull
    private String getAppFilesDirPath() {
        return context.getFilesDir().getAbsolutePath();
    }
}
