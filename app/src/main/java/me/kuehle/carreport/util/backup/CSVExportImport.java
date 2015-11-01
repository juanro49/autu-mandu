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

package me.kuehle.carreport.util.backup;

import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.OperationApplicationException;
import android.os.Environment;
import android.os.RemoteException;

import java.io.File;
import java.util.ArrayList;

import me.kuehle.carreport.data.query.CarQueries;
import me.kuehle.carreport.provider.DataProvider;
import me.kuehle.carreport.provider.car.CarColumns;
import me.kuehle.carreport.provider.car.CarContentValues;
import me.kuehle.carreport.provider.car.CarCursor;
import me.kuehle.carreport.provider.car.CarSelection;
import me.kuehle.carreport.provider.fueltype.FuelTypeColumns;
import me.kuehle.carreport.provider.fueltype.FuelTypeContentValues;
import me.kuehle.carreport.provider.fueltype.FuelTypeCursor;
import me.kuehle.carreport.provider.fueltype.FuelTypeSelection;
import me.kuehle.carreport.provider.othercost.OtherCostColumns;
import me.kuehle.carreport.provider.othercost.OtherCostContentValues;
import me.kuehle.carreport.provider.othercost.OtherCostCursor;
import me.kuehle.carreport.provider.othercost.OtherCostSelection;
import me.kuehle.carreport.provider.refueling.RefuelingColumns;
import me.kuehle.carreport.provider.refueling.RefuelingContentValues;
import me.kuehle.carreport.provider.refueling.RefuelingCursor;
import me.kuehle.carreport.provider.refueling.RefuelingSelection;
import me.kuehle.carreport.provider.reminder.ReminderColumns;
import me.kuehle.carreport.provider.reminder.ReminderContentValues;
import me.kuehle.carreport.provider.reminder.ReminderCursor;
import me.kuehle.carreport.provider.reminder.ReminderSelection;
import me.kuehle.carreport.util.CSVReader;
import me.kuehle.carreport.util.CSVWriter;

public class CSVExportImport {
    public static final String DIRECTORY = "Car Report CSV";

    public Context mContext;

    private File mExportDir;

    private static String[] allTables = {
            CarColumns.TABLE_NAME,
            FuelTypeColumns.TABLE_NAME,
            OtherCostColumns.TABLE_NAME,
            RefuelingColumns.TABLE_NAME,
            ReminderColumns.TABLE_NAME
    };

    public CSVExportImport(Context context) {
        mContext = context;
        File mExternalStorageDir = Environment.getExternalStorageDirectory();
        mExportDir = new File(mExternalStorageDir, DIRECTORY);
    }

    public boolean export() {
        if (!mExportDir.isDirectory()) {
            if (!mExportDir.mkdir()) {
                return false;
            }
        }

        exportCars();
        exportFuelTypes();
        exportOtherCosts();
        exportRefuelings();
        exportReminders();

        return true;
    }

    private void exportCars() {
        CSVWriter csv = new CSVWriter();
        csv.writeLine((Object[]) CarColumns.ALL_COLUMNS);

        CarCursor car = new CarSelection().query(mContext.getContentResolver());
        while (car.moveToNext()) {
            csv.writeLine(
                    car.getId(),
                    car.getName(),
                    car.getColor(),
                    car.getInitialMileage(),
                    car.getSuspendedSince());
        }

        csv.toFile(new File(mExportDir, CarColumns.TABLE_NAME + ".csv"));
    }

    private void exportFuelTypes() {
        CSVWriter csv = new CSVWriter();
        csv.writeLine((Object[]) FuelTypeColumns.ALL_COLUMNS);

        FuelTypeCursor fuelType = new FuelTypeSelection().query(mContext.getContentResolver());
        while (fuelType.moveToNext()) {
            csv.writeLine(
                    fuelType.getId(),
                    fuelType.getName(),
                    fuelType.getCategory());
        }

        csv.toFile(new File(mExportDir, FuelTypeColumns.TABLE_NAME + ".csv"));
    }

    private void exportOtherCosts() {
        CSVWriter csv = new CSVWriter();
        csv.writeLine((Object[]) OtherCostColumns.ALL_COLUMNS);

        OtherCostCursor otherCost = new OtherCostSelection().query(mContext.getContentResolver());
        while (otherCost.moveToNext()) {
            csv.writeLine(
                    otherCost.getId(),
                    otherCost.getTitle(),
                    otherCost.getDate(),
                    otherCost.getMileage(),
                    otherCost.getPrice(),
                    otherCost.getRecurrenceInterval(),
                    otherCost.getRecurrenceMultiplier(),
                    otherCost.getEndDate(),
                    otherCost.getNote(),
                    otherCost.getCarId());
        }

        csv.toFile(new File(mExportDir, OtherCostColumns.TABLE_NAME + ".csv"));
    }

    private void exportRefuelings() {
        CSVWriter csv = new CSVWriter();
        csv.writeLine((Object[]) RefuelingColumns.ALL_COLUMNS);

        RefuelingCursor refueling = new RefuelingSelection().query(mContext.getContentResolver());
        while (refueling.moveToNext()) {
            csv.writeLine(
                    refueling.getId(),
                    refueling.getDate(),
                    refueling.getMileage(),
                    refueling.getVolume(),
                    refueling.getPrice(),
                    refueling.getPartial(),
                    refueling.getNote(),
                    refueling.getFuelTypeId(),
                    refueling.getCarId());
        }

        csv.toFile(new File(mExportDir, RefuelingColumns.TABLE_NAME + ".csv"));
    }

    private void exportReminders() {
        CSVWriter csv = new CSVWriter();
        csv.writeLine((Object[]) ReminderColumns.ALL_COLUMNS);

        ReminderCursor reminder = new ReminderSelection().query(mContext.getContentResolver());
        while (reminder.moveToNext()) {
            csv.writeLine(
                    reminder.getId(),
                    reminder.getTitle(),
                    reminder.getAfterTimeSpanUnit(),
                    reminder.getAfterTimeSpanCount(),
                    reminder.getAfterDistance(),
                    reminder.getStartDate(),
                    reminder.getStartMileage(),
                    reminder.getNotificationDismissed(),
                    reminder.getSnoozedUntil(),
                    reminder.getCarId());
        }

        csv.toFile(new File(mExportDir, ReminderColumns.TABLE_NAME + ".csv"));
    }

    public boolean allExportFilesExist() {
        for (String table : allTables) {
            File file = new File(mExportDir, table + ".csv");
            if (!file.isFile()) {
                return false;
            }
        }

        return true;
    }

    public boolean anyExportFileExist() {
        for (String table : allTables) {
            File file = new File(mExportDir, table + ".csv");
            if (file.isFile()) {
                return true;
            }
        }

        return false;
    }

    public boolean import_() {
        if (!allExportFilesExist()) {
            return false;
        }

        ArrayList<ContentProviderOperation> operations = new ArrayList<>();
        operations.addAll(importCars());
        operations.addAll(importFuelTypes());
        operations.addAll(importOtherCosts());
        operations.addAll(importRefuelings());
        operations.addAll(importReminders());

        try {
            mContext.getContentResolver().applyBatch(DataProvider.AUTHORITY, operations);
            return true;
        } catch (RemoteException | OperationApplicationException e) {
            return false;
        }
    }

    private ArrayList<ContentProviderOperation> importCars() {
        ArrayList<ContentProviderOperation> operations = new ArrayList<>();
        CSVReader csv = CSVReader.fromFile(new File(mExportDir, CarColumns.TABLE_NAME + ".csv"), true);
        for (int i = 0; i < csv.getRowCount(); i++) {
            Long id = csv.getLong(i, CarColumns._ID);

            CarContentValues values = new CarContentValues();
            values.putName(csv.getString(i, CarColumns.NAME));
            values.putColor(csv.getInteger(i, CarColumns.COLOR));
            values.putInitialMileage(csv.getInteger(i, CarColumns.INITIAL_MILEAGE));
            values.putSuspendedSince(csv.getDate(i, CarColumns.SUSPENDED_SINCE));

            boolean updated = false;
            if (id != null) {
                CarSelection selection = new CarSelection().id(id);
                if (CarQueries.getCount(mContext) > 0) {
                    operations.add(ContentProviderOperation.newUpdate(values.uri())
                            .withSelection(selection.sel(), selection.args())
                            .withValues(values.values())
                            .build());
                    updated = true;
                }
            }

            if (!updated) {
                if (id != null) {
                    values.values().put(CarColumns._ID, id);
                }

                operations.add(ContentProviderOperation.newInsert(values.uri())
                        .withValues(values.values())
                        .build());
            }
        }

        return operations;
    }

    private ArrayList<ContentProviderOperation> importFuelTypes() {
        ArrayList<ContentProviderOperation> operations = new ArrayList<>();
        CSVReader csv = CSVReader.fromFile(new File(mExportDir, FuelTypeColumns.TABLE_NAME + ".csv"), true);
        for (int i = 0; i < csv.getRowCount(); i++) {
            Long id = csv.getLong(i, FuelTypeColumns._ID);

            FuelTypeContentValues values = new FuelTypeContentValues();
            values.putName(csv.getString(i, FuelTypeColumns.NAME));
            values.putCategory(csv.getString(i, FuelTypeColumns.CATEGORY));

            boolean updated = false;
            if (id != null) {
                FuelTypeSelection selection = new FuelTypeSelection().id(id);
                FuelTypeCursor fuelType = selection.query(mContext.getContentResolver());
                if (fuelType.getCount() > 0) {
                    operations.add(ContentProviderOperation.newUpdate(values.uri())
                            .withSelection(selection.sel(), selection.args())
                            .withValues(values.values())
                            .build());
                    updated = true;
                }
            }

            if (!updated) {
                if (id != null) {
                    values.values().put(FuelTypeColumns._ID, id);
                }

                operations.add(ContentProviderOperation.newInsert(values.uri())
                        .withValues(values.values())
                        .build());
            }
        }

        return operations;
    }

    private ArrayList<ContentProviderOperation> importOtherCosts() {
        ArrayList<ContentProviderOperation> operations = new ArrayList<>();
        CSVReader csv = CSVReader.fromFile(new File(mExportDir, OtherCostColumns.TABLE_NAME + ".csv"), true);
        for (int i = 0; i < csv.getRowCount(); i++) {
            Long id = csv.getLong(i, OtherCostColumns._ID);

            OtherCostContentValues values = new OtherCostContentValues();
            values.putTitle(csv.getString(i, OtherCostColumns.TITLE));
            values.putDate(csv.getDate(i, OtherCostColumns.DATE));
            values.putMileage(csv.getInteger(i, OtherCostColumns.MILEAGE));
            values.putPrice(csv.getFloat(i, OtherCostColumns.PRICE));
            values.putRecurrenceInterval(csv.getRecurrenceInterval(i, OtherCostColumns.RECURRENCE_INTERVAL));
            values.putRecurrenceMultiplier(csv.getInteger(i, OtherCostColumns.RECURRENCE_MULTIPLIER));
            values.putEndDate(csv.getDate(i, OtherCostColumns.END_DATE));
            values.putNote(csv.getString(i, OtherCostColumns.NOTE));
            values.putCarId(csv.getLong(i, OtherCostColumns.CAR_ID));

            boolean updated = false;
            if (id != null) {
                OtherCostSelection selection = new OtherCostSelection().id(id);
                OtherCostCursor otherCost = selection.query(mContext.getContentResolver());
                if (otherCost.getCount() > 0) {
                    operations.add(ContentProviderOperation.newUpdate(values.uri())
                            .withSelection(selection.sel(), selection.args())
                            .withValues(values.values())
                            .build());
                    updated = true;
                }
            }

            if (!updated) {
                if (id != null) {
                    values.values().put(OtherCostColumns._ID, id);
                }

                operations.add(ContentProviderOperation.newInsert(values.uri())
                        .withValues(values.values())
                        .build());
            }
        }

        return operations;
    }

    private ArrayList<ContentProviderOperation> importRefuelings() {
        ArrayList<ContentProviderOperation> operations = new ArrayList<>();
        CSVReader csv = CSVReader.fromFile(new File(mExportDir, RefuelingColumns.TABLE_NAME + ".csv"), true);
        for (int i = 0; i < csv.getRowCount(); i++) {
            Long id = csv.getLong(i, RefuelingColumns._ID);

            RefuelingContentValues values = new RefuelingContentValues();
            values.putDate(csv.getDate(i, RefuelingColumns.DATE));
            values.putMileage(csv.getInteger(i, RefuelingColumns.MILEAGE));
            values.putVolume(csv.getFloat(i, RefuelingColumns.VOLUME));
            values.putPrice(csv.getFloat(i, RefuelingColumns.PRICE));
            values.putPartial(csv.getBoolean(i, RefuelingColumns.PARTIAL));
            values.putNote(csv.getString(i, RefuelingColumns.NOTE));
            values.putFuelTypeId(csv.getLong(i, RefuelingColumns.FUEL_TYPE_ID));
            values.putCarId(csv.getLong(i, RefuelingColumns.CAR_ID));

            boolean updated = false;
            if (id != null) {
                RefuelingSelection selection = new RefuelingSelection().id(id);
                RefuelingCursor refueling = selection.query(mContext.getContentResolver());
                if (refueling.getCount() > 0) {
                    operations.add(ContentProviderOperation.newUpdate(values.uri())
                            .withSelection(selection.sel(), selection.args())
                            .withValues(values.values())
                            .build());
                    updated = true;
                }
            }

            if (!updated) {
                if (id != null) {
                    values.values().put(RefuelingColumns._ID, id);
                }

                operations.add(ContentProviderOperation.newInsert(values.uri())
                        .withValues(values.values())
                        .build());
            }
        }

        return operations;
    }

    private ArrayList<ContentProviderOperation> importReminders() {
        ArrayList<ContentProviderOperation> operations = new ArrayList<>();
        CSVReader csv = CSVReader.fromFile(new File(mExportDir, ReminderColumns.TABLE_NAME + ".csv"), true);
        for (int i = 0; i < csv.getRowCount(); i++) {
            Long id = csv.getLong(i, ReminderColumns._ID);

            ReminderContentValues values = new ReminderContentValues();
            values.putTitle(csv.getString(i, ReminderColumns.TITLE));
            values.putAfterTimeSpanUnit(csv.getTimeSpanUnit(i, ReminderColumns.AFTER_TIME_SPAN_UNIT));
            values.putAfterTimeSpanCount(csv.getInteger(i, ReminderColumns.AFTER_TIME_SPAN_COUNT));
            values.putAfterDistance(csv.getInteger(i, ReminderColumns.AFTER_DISTANCE));
            values.putStartDate(csv.getDate(i, ReminderColumns.START_DATE));
            values.putStartMileage(csv.getInteger(i, ReminderColumns.START_MILEAGE));
            values.putNotificationDismissed(csv.getBoolean(i, ReminderColumns.NOTIFICATION_DISMISSED));
            values.putSnoozedUntil(csv.getDate(i, ReminderColumns.SNOOZED_UNTIL));
            values.putCarId(csv.getLong(i, ReminderColumns.CAR_ID));

            boolean updated = false;
            if (id != null) {
                ReminderSelection selection = new ReminderSelection().id(id);
                ReminderCursor reminder = selection.query(mContext.getContentResolver());
                if (reminder.getCount() > 0) {
                    operations.add(ContentProviderOperation.newUpdate(values.uri())
                            .withSelection(selection.sel(), selection.args())
                            .withValues(values.values())
                            .build());
                    updated = true;
                }
            }

            if (!updated) {
                if (id != null) {
                    values.values().put(ReminderColumns._ID, id);
                }

                operations.add(ContentProviderOperation.newInsert(values.uri())
                        .withValues(values.values())
                        .build());
            }
        }

        return operations;
    }
}
