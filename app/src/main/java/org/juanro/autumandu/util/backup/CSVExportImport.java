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

package org.juanro.autumandu.util.backup;

import android.content.ContentProviderOperation;
import android.content.Context;
import android.os.Environment;
import android.util.Log;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.csv.QuoteMode;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.Semaphore;

import org.juanro.autumandu.provider.DataProvider;
import org.juanro.autumandu.provider.car.CarColumns;
import org.juanro.autumandu.provider.car.CarContentValues;
import org.juanro.autumandu.provider.car.CarCursor;
import org.juanro.autumandu.provider.car.CarSelection;
import org.juanro.autumandu.provider.fueltype.FuelTypeColumns;
import org.juanro.autumandu.provider.fueltype.FuelTypeContentValues;
import org.juanro.autumandu.provider.fueltype.FuelTypeCursor;
import org.juanro.autumandu.provider.fueltype.FuelTypeSelection;
import org.juanro.autumandu.provider.othercost.OtherCostColumns;
import org.juanro.autumandu.provider.othercost.OtherCostContentValues;
import org.juanro.autumandu.provider.othercost.OtherCostCursor;
import org.juanro.autumandu.provider.othercost.OtherCostSelection;
import org.juanro.autumandu.provider.refueling.RefuelingColumns;
import org.juanro.autumandu.provider.refueling.RefuelingContentValues;
import org.juanro.autumandu.provider.refueling.RefuelingCursor;
import org.juanro.autumandu.provider.refueling.RefuelingSelection;
import org.juanro.autumandu.provider.reminder.ReminderColumns;
import org.juanro.autumandu.provider.reminder.ReminderContentValues;
import org.juanro.autumandu.provider.reminder.ReminderCursor;
import org.juanro.autumandu.provider.reminder.ReminderSelection;
import org.juanro.autumandu.provider.station.StationColumns;
import org.juanro.autumandu.provider.station.StationContentValues;
import org.juanro.autumandu.provider.station.StationCursor;
import org.juanro.autumandu.provider.station.StationSelection;

public class CSVExportImport {
    private static final String LOG_TAG = "CSVExportImport";
    public static final String DIRECTORY = "Autu Mandu CSV";
    private static final String DATE_INVALID_FORMAT_EXC = "A date has an invalid format. Please " +
        "change either data to match the defined ISO-based date or your phone localization.";

    public Context mContext;

    private File mExportDir;

    private CSVFormat mCSVFormat;
    private Semaphore mImExportSemaphore;

    private static String[] allTables = {
            CarColumns.TABLE_NAME,
            FuelTypeColumns.TABLE_NAME,
            StationColumns.TABLE_NAME,
            OtherCostColumns.TABLE_NAME,
            RefuelingColumns.TABLE_NAME,
            ReminderColumns.TABLE_NAME
    };

    public CSVExportImport(Context context) {
        mContext = context;
        File mExternalStorageDir = Environment.getExternalStorageDirectory();
        mExportDir = new File(mExternalStorageDir, DIRECTORY);

        mCSVFormat = CSVFormat.DEFAULT
                .withQuoteMode(QuoteMode.MINIMAL)
                .withDelimiter(',')
                .withIgnoreEmptyLines()
                .withIgnoreSurroundingSpaces()
                .withIgnoreHeaderCase()
                .withHeader()
                .withTrim();

        mImExportSemaphore = new Semaphore(1);
    }

    public void export() throws CSVImportException {
        try {
            mImExportSemaphore.acquire();
        } catch (InterruptedException e) {
            throw new CSVImportException("Another import or export is already running.");
        }

        try {
            if (!mExportDir.isDirectory()) {
                if (!mExportDir.mkdir()) {
                    throw new CSVImportException("Could not create export directory.");
                }
            }

            exportCars();
            exportFuelTypes();
            exportStations();
            exportOtherCosts();
            exportRefuelings();
            exportReminders();
        } catch (CSVImportException e) {
            throw e;
        } catch (Exception e) {
            throw new CSVImportException(e);
        } finally {
            mImExportSemaphore.release();
        }
    }

    private void exportCars() throws IOException {
        CSVPrinter csv = new CSVPrinter(
                new PrintWriter(new File(mExportDir, CarColumns.TABLE_NAME + ".csv")),
                mCSVFormat);
        csv.printRecord((Object[]) CarColumns.ALL_COLUMNS);

        CarCursor car = new CarSelection().query(mContext.getContentResolver());
        while (car.moveToNext()) {
            csv.printRecord(
                    car.getId(),
                    car.getName(),
                    car.getColor(),
                    car.getInitialMileage(),
                    CSVConvert.toString(car.getSuspendedSince()),
                    car.getBuyingPrice()/*,
                    car.getMake(),
                    car.getModel(),
                    car.getYear(),
                    car.getLicensePlate(),
                    CSVConvert.toString(car.getBuyingDate())*/);
        }

        csv.close();
    }

    private void exportFuelTypes() throws IOException {
        CSVPrinter csv = new CSVPrinter(
                new PrintWriter(new File(mExportDir, FuelTypeColumns.TABLE_NAME + ".csv")),
                mCSVFormat);
        csv.printRecord((Object[]) FuelTypeColumns.ALL_COLUMNS);

        FuelTypeCursor fuelType = new FuelTypeSelection().query(mContext.getContentResolver());
        while (fuelType.moveToNext()) {
            csv.printRecord(
                    fuelType.getId(),
                    fuelType.getName(),
                    fuelType.getCategory());
        }

        csv.close();
    }

    private void exportStations() throws IOException {
        CSVPrinter csv = new CSVPrinter(
            new PrintWriter(new File(mExportDir, StationColumns.TABLE_NAME + ".csv")),
            mCSVFormat);
        csv.printRecord((Object[]) StationColumns.ALL_COLUMNS);

        StationCursor station = new StationSelection().query(mContext.getContentResolver());
        while (station.moveToNext()) {
            csv.printRecord(
                station.getId(),
                station.getName());
        }

        csv.close();
    }

    private void exportOtherCosts() throws IOException {
        CSVPrinter csv = new CSVPrinter(
                new PrintWriter(new File(mExportDir, OtherCostColumns.TABLE_NAME + ".csv")),
                mCSVFormat);
        csv.printRecord((Object[]) OtherCostColumns.ALL_COLUMNS);

        OtherCostCursor otherCost = new OtherCostSelection().query(mContext.getContentResolver());
        while (otherCost.moveToNext()) {
            csv.printRecord(
                    otherCost.getId(),
                    otherCost.getTitle(),
                    CSVConvert.toString(otherCost.getDate()),
                    otherCost.getMileage(),
                    CSVConvert.toString(otherCost.getPrice()),
                    CSVConvert.toString(otherCost.getRecurrenceInterval()),
                    otherCost.getRecurrenceMultiplier(),
                    CSVConvert.toString(otherCost.getEndDate()),
                    otherCost.getNote(),
                    otherCost.getCarId());
        }

        csv.close();
    }

    private void exportRefuelings() throws IOException {
        CSVPrinter csv = new CSVPrinter(
                new PrintWriter(new File(mExportDir, RefuelingColumns.TABLE_NAME + ".csv")),
                mCSVFormat);
        csv.printRecord((Object[]) RefuelingColumns.ALL_COLUMNS);

        RefuelingCursor refueling = new RefuelingSelection().query(mContext.getContentResolver());
        while (refueling.moveToNext()) {
            csv.printRecord(
                    refueling.getId(),
                    CSVConvert.toString(refueling.getDate()),
                    refueling.getMileage(),
                    CSVConvert.toString(refueling.getVolume()),
                    CSVConvert.toString(refueling.getPrice()),
                    refueling.getPartial(),
                    refueling.getNote(),
                    refueling.getFuelTypeId(),
                    refueling.getStationId(),
                    refueling.getCarId());
        }

        csv.close();
    }

    private void exportReminders() throws IOException {
        CSVPrinter csv = new CSVPrinter(
                new PrintWriter(new File(mExportDir, ReminderColumns.TABLE_NAME + ".csv")),
                mCSVFormat);
        csv.printRecord((Object[]) ReminderColumns.ALL_COLUMNS);

        ReminderCursor reminder = new ReminderSelection().query(mContext.getContentResolver());
        while (reminder.moveToNext()) {
            csv.printRecord(
                    reminder.getId(),
                    reminder.getTitle(),
                    CSVConvert.toString(reminder.getAfterTimeSpanUnit()),
                    reminder.getAfterTimeSpanCount(),
                    reminder.getAfterDistance(),
                    CSVConvert.toString(reminder.getStartDate()),
                    reminder.getStartMileage(),
                    reminder.getNotificationDismissed(),
                    CSVConvert.toString(reminder.getSnoozedUntil()),
                    reminder.getCarId());
        }

        csv.close();
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

    private boolean filesUseSemicolon() {
        File carFile = new File(mExportDir, "car.csv");
        try {
            BufferedReader bufCarFile = new BufferedReader(new FileReader(carFile));
            String line = "";
            while (line.isEmpty()) {
                line = bufCarFile.readLine();
            }
            bufCarFile.close();
            String[] splitBySemicolon = line.split(";");
            return (splitBySemicolon.length >= 4);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Could not determine whether files are using semicolon as " +
                "delimiter. Continuing to import.");
            e.printStackTrace();
            return false;
        }
    }

    public void import_() throws CSVImportException {
        try {
            mImExportSemaphore.acquire();
        } catch (InterruptedException e) {
            throw new CSVImportException("Another import or export is already running.");
        }

        boolean semicolonDelimiter = false;
        try {
            if (!allExportFilesExist()) {
                throw new CSVImportException("Some import files are missing.");
            }

            semicolonDelimiter = filesUseSemicolon();
            if (semicolonDelimiter) {
                this.mCSVFormat = this.mCSVFormat.withDelimiter(';');
            }

            ArrayList<ContentProviderOperation> operations = new ArrayList<>();
            operations.addAll(importCars());
            operations.addAll(importFuelTypes());
            operations.addAll(importStations());
            operations.addAll(importOtherCosts());
            operations.addAll(importRefuelings());
            operations.addAll(importReminders());

            mContext.getContentResolver().applyBatch(DataProvider.AUTHORITY, operations);
        } catch (CSVImportException e) {
            throw e;
        } catch (Exception e) {
            throw new CSVImportException(e);
        } finally {
            if (semicolonDelimiter) {
                this.mCSVFormat = this.mCSVFormat.withDelimiter(',');
            }
            mImExportSemaphore.release();
        }
    }

    private ArrayList<ContentProviderOperation> importCars() throws IOException, CSVImportException {
        ArrayList<ContentProviderOperation> operations = new ArrayList<>();

        CSVParser csv = CSVParser.parse(
                new File(mExportDir, CarColumns.TABLE_NAME + ".csv"),
                Charset.defaultCharset(),
                mCSVFormat);

        for (CSVRecord record : csv) {
            Long id = CSVConvert.toLong(record.get(CarColumns._ID));

            CarContentValues values = new CarContentValues();
            values.putName(record.get(CarColumns.NAME));
            //noinspection ConstantConditions
            values.putColor(CSVConvert.toInteger(record.get(CarColumns.COLOR)));
            //noinspection ConstantConditions
            values.putInitialMileage(CSVConvert.toInteger(record.get(CarColumns.INITIAL_MILEAGE)));
            Date suspensionDate = CSVConvert.toDate(record.get(CarColumns.SUSPENDED_SINCE));
            if (!record.get(CarColumns.SUSPENDED_SINCE).isEmpty() && suspensionDate == null) {
                throw new CSVImportException(DATE_INVALID_FORMAT_EXC);
            }
            values.putSuspendedSince(suspensionDate);
            //noinspection ConstantConditions
            values.putBuyingPrice(CSVConvert.toFloat(record.get(CarColumns.BUYING_PRICE)));
            /*values.putMake(record.get(CarColumns.MAKE));
            values.putModel(record.get(CarColumns.MODEL));
            //noinspection ConstantConditions
            values.putYear(CSVConvert.toInteger(record.get(CarColumns.YEAR)));
            values.putLicensePlate(record.get(CarColumns.LICENSE_PLATE));
            Date buyingDate = CSVConvert.toDate(record.get(CarColumns.BUYING_DATE));
            if (!record.get(CarColumns.BUYING_DATE).isEmpty() && buyingDate == null) {
                throw new CSVImportException(DATE_INVALID_FORMAT_EXC);
            }
            values.putBuyingDate(buyingDate);*/

            boolean updated = false;
            if (id != null) {
                CarSelection selection = new CarSelection().id(id);
                CarCursor car = selection.query(mContext.getContentResolver());
                if (car.getCount() > 0) {
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

    private ArrayList<ContentProviderOperation> importFuelTypes() throws IOException {
        ArrayList<ContentProviderOperation> operations = new ArrayList<>();

        CSVParser csv = CSVParser.parse(
                new File(mExportDir, FuelTypeColumns.TABLE_NAME + ".csv"),
                Charset.defaultCharset(),
                mCSVFormat);

        for (CSVRecord record : csv) {
            Long id = CSVConvert.toLong(record.get(FuelTypeColumns._ID));

            FuelTypeContentValues values = new FuelTypeContentValues();
            values.putName(record.get(FuelTypeColumns.NAME));
            values.putCategory(record.get(FuelTypeColumns.CATEGORY));

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

    private ArrayList<ContentProviderOperation> importStations() throws IOException {
        ArrayList<ContentProviderOperation> operations = new ArrayList<>();

        CSVParser csv = CSVParser.parse(
            new File(mExportDir, StationColumns.TABLE_NAME + ".csv"),
            Charset.defaultCharset(),
            mCSVFormat);

        for (CSVRecord record : csv) {
            Long id = CSVConvert.toLong(record.get(StationColumns._ID));

            StationContentValues values = new StationContentValues();
            values.putName(record.get(StationColumns.NAME));

            boolean updated = false;
            if (id != null) {
                StationSelection selection = new StationSelection().id(id);
                StationCursor station = selection.query(mContext.getContentResolver());
                if (station.getCount() > 0) {
                    operations.add(ContentProviderOperation.newUpdate(values.uri())
                        .withSelection(selection.sel(), selection.args())
                        .withValues(values.values())
                        .build());
                    updated = true;
                }
            }

            if (!updated) {
                if (id != null) {
                    values.values().put(StationColumns._ID, id);
                }

                operations.add(ContentProviderOperation.newInsert(values.uri())
                    .withValues(values.values())
                    .build());
            }
        }

        return operations;
    }

    private ArrayList<ContentProviderOperation> importOtherCosts() throws IOException, CSVImportException {
        ArrayList<ContentProviderOperation> operations = new ArrayList<>();

        CSVParser csv = CSVParser.parse(
                new File(mExportDir, OtherCostColumns.TABLE_NAME + ".csv"),
                Charset.defaultCharset(),
                mCSVFormat);

        for (CSVRecord record : csv) {
            Long id = CSVConvert.toLong(record.get(OtherCostColumns._ID));

            OtherCostContentValues values = new OtherCostContentValues();
            values.putTitle(record.get(OtherCostColumns.TITLE));
            //noinspection ConstantConditions
            Date date = CSVConvert.toDate(record.get(OtherCostColumns.DATE));
            if (date == null) {
                throw new CSVImportException(DATE_INVALID_FORMAT_EXC);
            }
            values.putDate(date);
            values.putMileage(CSVConvert.toInteger(record.get(OtherCostColumns.MILEAGE)));
            //noinspection ConstantConditions
            values.putPrice(CSVConvert.toFloat(record.get(OtherCostColumns.PRICE)));
            //noinspection ConstantConditions
            values.putRecurrenceInterval(CSVConvert.toRecurrenceInterval(record.get(OtherCostColumns.RECURRENCE_INTERVAL)));
            //noinspection ConstantConditions
            values.putRecurrenceMultiplier(CSVConvert.toInteger(record.get(OtherCostColumns.RECURRENCE_MULTIPLIER)));
            Date endDate = CSVConvert.toDate(record.get(OtherCostColumns.END_DATE));
            if (!record.get(OtherCostColumns.END_DATE).isEmpty() && endDate == null) {
                throw new CSVImportException(DATE_INVALID_FORMAT_EXC);
            }
            values.putEndDate(endDate);
            values.putNote(record.get(OtherCostColumns.NOTE));
            //noinspection ConstantConditions
            values.putCarId(CSVConvert.toLong(record.get(OtherCostColumns.CAR_ID)));

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

    private ArrayList<ContentProviderOperation> importRefuelings() throws IOException, CSVImportException {
        ArrayList<ContentProviderOperation> operations = new ArrayList<>();

        CSVParser csv = CSVParser.parse(
                new File(mExportDir, RefuelingColumns.TABLE_NAME + ".csv"),
                Charset.defaultCharset(),
                mCSVFormat);

        for (CSVRecord record : csv) {
            Long id = CSVConvert.toLong(record.get(RefuelingColumns._ID));

            RefuelingContentValues values = new RefuelingContentValues();
            //noinspection ConstantConditions
            Date date = CSVConvert.toDate(record.get(RefuelingColumns.DATE));
            if (date == null) {
                throw new CSVImportException(DATE_INVALID_FORMAT_EXC);
            }
            values.putDate(date);
            //noinspection ConstantConditions
            values.putMileage(CSVConvert.toInteger(record.get(RefuelingColumns.MILEAGE)));
            //noinspection ConstantConditions
            values.putVolume(CSVConvert.toFloat(record.get(RefuelingColumns.VOLUME)));
            //noinspection ConstantConditions
            values.putPrice(CSVConvert.toFloat(record.get(RefuelingColumns.PRICE)));
            //noinspection ConstantConditions
            values.putPartial(CSVConvert.toBoolean(record.get(RefuelingColumns.PARTIAL)));
            values.putNote(record.get(RefuelingColumns.NOTE));
            //noinspection ConstantConditions
            values.putFuelTypeId(CSVConvert.toLong(record.get(RefuelingColumns.FUEL_TYPE_ID)));
            //noinspection ConstantConditions
            values.putStationId(CSVConvert.toLong(record.get(RefuelingColumns.STATION_ID)));
            //noinspection ConstantConditions
            values.putCarId(CSVConvert.toLong(record.get(RefuelingColumns.CAR_ID)));

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

    private ArrayList<ContentProviderOperation> importReminders() throws IOException {
        ArrayList<ContentProviderOperation> operations = new ArrayList<>();
        CSVParser csv = CSVParser.parse(
                new File(mExportDir, ReminderColumns.TABLE_NAME + ".csv"),
                Charset.defaultCharset(),
                mCSVFormat);

        for (CSVRecord record : csv) {
            Long id = CSVConvert.toLong(record.get(ReminderColumns._ID));

            ReminderContentValues values = new ReminderContentValues();
            values.putTitle(record.get(ReminderColumns.TITLE));
            values.putAfterTimeSpanUnit(CSVConvert.toTimeSpanUnit(record.get(ReminderColumns.AFTER_TIME_SPAN_UNIT)));
            values.putAfterTimeSpanCount(CSVConvert.toInteger(record.get(ReminderColumns.AFTER_TIME_SPAN_COUNT)));
            values.putAfterDistance(CSVConvert.toInteger(record.get(ReminderColumns.AFTER_DISTANCE)));
            //noinspection ConstantConditions
            values.putStartDate(CSVConvert.toDate(record.get(ReminderColumns.START_DATE)));
            //noinspection ConstantConditions
            values.putStartMileage(CSVConvert.toInteger(record.get(ReminderColumns.START_MILEAGE)));
            //noinspection ConstantConditions
            values.putNotificationDismissed(CSVConvert.toBoolean(record.get(ReminderColumns.NOTIFICATION_DISMISSED)));
            values.putSnoozedUntil(CSVConvert.toDate(record.get(ReminderColumns.SNOOZED_UNTIL)));
            //noinspection ConstantConditions
            values.putCarId(CSVConvert.toLong(record.get(ReminderColumns.CAR_ID)));

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
