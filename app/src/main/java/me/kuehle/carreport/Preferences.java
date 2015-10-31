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

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import me.kuehle.carreport.data.report.AbstractReport;
import me.kuehle.carreport.data.report.CostsReport;
import me.kuehle.carreport.data.report.FuelConsumptionReport;
import me.kuehle.carreport.data.report.FuelPriceReport;
import me.kuehle.carreport.data.report.MileageReport;
import me.kuehle.carreport.provider.car.CarColumns;
import me.kuehle.carreport.provider.car.CarCursor;
import me.kuehle.carreport.provider.car.CarSelection;
import me.kuehle.carreport.provider.reminder.TimeSpanUnit;
import me.kuehle.carreport.util.TimeSpan;

public class Preferences {
    private static final String TAG = "Preferences";

    private Context mContext;
    private SharedPreferences mPrefs;

    public Preferences(Context context) {
        mContext = context;
        mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public long getDefaultCar() {
        int id = Integer.parseInt(mPrefs.getString("behavior_default_car", "1"));

        CarCursor car = new CarSelection().query(mContext.getContentResolver(), new String[]{CarColumns._ID});
        if (car.getCount() == 0) {
            return 0;
        }

        while (car.moveToNext()) {
            if (car.getId() == id) {
                return id;
            }
        }

        car.moveToFirst();
        return car.getId();
    }

    public DistanceEntryMode getDistanceEntryMode() {
        String mode = mPrefs.getString("behavior_distance_entry_mode", "TOTAL");
        try {
            return DistanceEntryMode.valueOf(mode);
        } catch (IllegalArgumentException e) {
            return DistanceEntryMode.TOTAL;
        }
    }

    public PriceEntryMode getPriceEntryMode() {
        String mode = mPrefs.getString("behavior_price_entry_mode", "TOTAL_AND_VOLUME");
        try {
            return PriceEntryMode.valueOf(mode);
        } catch (IllegalArgumentException e) {
            return PriceEntryMode.TOTAL_AND_VOLUME;
        }
    }

    public String getSyncLocalFileRev() {
        return mPrefs.getString("sync_local_file_rev", null);
    }

    public void setSyncLocalFileRev(String rev) {
        Editor edit = mPrefs.edit();
        edit.putString("sync_local_file_rev", rev);
        edit.apply();
    }

    @SuppressWarnings("unchecked")
    public List<Class<? extends AbstractReport>> getReportOrder() {
        List<Class<? extends AbstractReport>> reports = new ArrayList<>();
        String reportNames = mPrefs.getString("behavior_report_order", null);
        if (reportNames == null) {
            reports.add(FuelConsumptionReport.class);
            reports.add(FuelPriceReport.class);
            reports.add(MileageReport.class);
            reports.add(CostsReport.class);
        } else {
            for (String reportName : reportNames.split(",")) {
                try {
                    Class<?> report = Class.forName(reportName);
                    if (AbstractReport.class.isAssignableFrom(report)) {
                        reports.add((Class<? extends AbstractReport>) report);
                    }
                } catch (Exception e) {
                    Log.w(TAG, String.format("Error loading report order: %s.", reportName), e);
                }
            }
        }

        return reports;
    }

    public TimeSpan getReminderSnoozeDuration() {
        String data = mPrefs.getString("behavior_reminder_snooze_duration", "7 DAY");
        return TimeSpan.fromString(data, new TimeSpan(TimeSpanUnit.DAY, 7));
    }

    public String getUnitCurrency() {
        return mPrefs.getString("unit_currency", "EUR");
    }

    public String getUnitDistance() {
        return mPrefs.getString("unit_distance", "km");
    }

    public String getUnitVolume() {
        return mPrefs.getString("unit_volume", "l");
    }

    public int getUnitFuelConsumption() {
        return Integer.parseInt(mPrefs.getString("unit_fuel_consumption", "0"));
    }

    public boolean isAutoGuessMissingDataEnabled() {
        return mPrefs.getBoolean("behavior_auto_guess_missing_data", false);
    }

    public boolean isShowCarMenu() {
        return mPrefs.getBoolean("behavior_show_car_menu", true);
    }

    public void setReportOrder(List<Class<? extends AbstractReport>> reports) {
        List<String> reportNames = new ArrayList<>();
        for (Class<? extends AbstractReport> report : reports) {
            reportNames.add(report.getName());
        }

        Editor edit = mPrefs.edit();
        edit.putString("behavior_report_order", TextUtils.join(",", reportNames));
        edit.apply();
    }

    // Deprecated Dropbox and Google Drive sync settings

    public String getDeprecatedSynchronizationProvider() {
        return mPrefs.getString("sync_current_provider", null);
    }

    public String getDeprecatedDropboxAccount() {
        return mPrefs.getString("sync_dropbox_account", null);
    }

    public String getDeprecatedDropboxAccessToken() {
        return mPrefs.getString("sync_dropbox_token", null);
    }

    public String getDeprecatedDropboxLocalRev() {
        return mPrefs.getString("sync_dropbox_rev", null);
    }

    public String getDeprecatedGoogleDriveAccount() {
        return mPrefs.getString("sync_drive_account", null);
    }

    public Date getDeprecatedGoogleDriveLocalModifiedDate() {
        try {
            long date = mPrefs.getLong("sync_drive_modified_date", -1);
            if (date == -1) {
                return null;
            } else {
                return new Date(date);
            }
        } catch (ClassCastException e) {
            return null;
        }
    }

    public void removeDeprecatedSyncSettings() {
        Editor edit = mPrefs.edit();
        edit.remove("sync_dropbox_account");
        edit.remove("sync_dropbox_token");
        edit.remove("sync_dropbox_rev");
        edit.remove("sync_drive_modified_date");
        edit.remove("sync_drive_account");
        edit.remove("sync_current_provider");
        edit.remove("sync_on_change");
        edit.remove("sync_on_start");
        edit.apply();
    }
}
