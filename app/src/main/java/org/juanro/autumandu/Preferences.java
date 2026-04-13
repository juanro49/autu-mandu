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
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.juanro.autumandu.data.report.AbstractReport;
import org.juanro.autumandu.data.report.CostsReport;
import org.juanro.autumandu.data.report.FuelConsumptionReport;
import org.juanro.autumandu.data.report.FuelPriceReport;
import org.juanro.autumandu.data.report.MileageReport;
import org.juanro.autumandu.data.report.OverallCostsReport;
import org.juanro.autumandu.model.entity.helper.TimeSpanUnit;
import org.juanro.autumandu.util.TimeSpan;

/**
 * Utility class for managing application preferences.
 */
public class Preferences {
    private static final String TAG = "Preferences";

    // Preference Keys
    private static final String KEY_DEFAULT_CAR = "behavior_default_car";
    private static final String KEY_DISTANCE_ENTRY_MODE = "behavior_distance_entry_mode";
    private static final String KEY_PRICE_ENTRY_MODE = "behavior_price_entry_mode";
    private static final String KEY_SYNC_LOCAL_FILE_REV = "sync_local_file_rev";
    private static final String KEY_REPORT_ORDER = "behavior_report_order";
    private static final String KEY_REMINDER_SNOOZE_DURATION = "behavior_reminder_snooze_duration";
    private static final String KEY_UNIT_CURRENCY = "unit_currency";
    private static final String KEY_UNIT_DISTANCE = "unit_distance";
    private static final String KEY_UNIT_VOLUME = "unit_volume";
    private static final String KEY_UNIT_FUEL_CONSUMPTION = "unit_fuel_consumption";
    private static final String KEY_AUTO_GUESS_MISSING_DATA = "behavior_auto_guess_missing_data";
    private static final String KEY_SHOW_CAR_MENU = "behavior_show_car_menu";
    private static final String KEY_BACKUP_FOLDER = "backup_folder";
    private static final String KEY_BACKUP_FOLDER_DEFAULT = "backup_folder_default";
    private static final String KEY_AUTO_BACKUP = "behavior_auto_backup";
    private static final String KEY_KEEP_BACKUPS = "behaviour_keep_backups";

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

    @SuppressWarnings("unchecked")
    public List<Class<? extends AbstractReport>> getReportOrder() {
        var reportNames = prefs.getString(KEY_REPORT_ORDER, null);
        if (reportNames == null) {
            return new ArrayList<>(List.of(
                    FuelConsumptionReport.class,
                    FuelPriceReport.class,
                    MileageReport.class,
                    CostsReport.class,
                    OverallCostsReport.class
            ));
        }

        return Arrays.stream(reportNames.split(","))
                .map(name -> {
                    try {
                        var cls = Class.forName(name);
                        return AbstractReport.class.isAssignableFrom(cls) ? cls : null;
                    } catch (Exception e) {
                        Log.w(TAG, "Error loading report class: " + name);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .map(cls -> (Class<? extends AbstractReport>) cls)
                .collect(Collectors.toCollection(ArrayList::new));
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

    public String getBackupPath() {
        var savedValue = prefs.getString(KEY_BACKUP_FOLDER, "");
        if (TextUtils.isEmpty(savedValue)) {
            savedValue = getExternalFilesDirPath();
            putString(KEY_BACKUP_FOLDER, savedValue);
        }
        return savedValue;
    }

    public void setBackupPath(@NonNull String path) {
        putString(KEY_BACKUP_FOLDER, path);
    }

    public String getDefaultBackupPath() {
        var savedValue = prefs.getString(KEY_BACKUP_FOLDER_DEFAULT, "");
        if (TextUtils.isEmpty(savedValue)) {
            savedValue = getExternalFilesDirPath();
            putString(KEY_BACKUP_FOLDER_DEFAULT, savedValue);
        }
        return savedValue;
    }

    public void restoreDefaultBackupPath() {
        putString(KEY_BACKUP_FOLDER, getExternalFilesDirPath());
    }

    public boolean getAutoBackupEnabled() {
        return prefs.getBoolean(KEY_AUTO_BACKUP, false);
    }

    public int getAutoBackupRetention() {
        return prefs.getInt(KEY_KEEP_BACKUPS, 12);
    }

    private void putString(String key, String value) {
        prefs.edit().putString(key, value).apply();
    }

    @NonNull
    private String getExternalFilesDirPath() {
        var externalDir = context.getExternalFilesDir(null);
        return externalDir != null ? externalDir.getAbsolutePath() : "";
    }

    // Deprecated Sync Settings Logic

    @Deprecated
    @Nullable
    public String getDeprecatedSynchronizationProvider() {
        return prefs.getString("sync_current_provider", null);
    }

    @Deprecated
    public void removeDeprecatedSyncSettings() {
        prefs.edit()
                .remove("sync_dropbox_account")
                .remove("sync_dropbox_token")
                .remove("sync_dropbox_rev")
                .remove("sync_drive_modified_date")
                .remove("sync_drive_account")
                .remove("sync_current_provider")
                .remove("sync_on_change")
                .remove("sync_on_start")
                .apply();
    }

    @Deprecated
    @Nullable
    public Date getDeprecatedGoogleDriveLocalModifiedDate() {
        long date = prefs.getLong("sync_drive_modified_date", -1);
        return (date == -1) ? null : new Date(date);
    }
}
