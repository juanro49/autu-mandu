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

import android.content.Context;

import androidx.documentfile.provider.DocumentFile;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.csv.QuoteMode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Date;
import java.util.concurrent.Semaphore;
import org.juanro.autumandu.Application;
import org.juanro.autumandu.model.AutuManduDatabase;
import org.juanro.autumandu.model.entity.Car;
import org.juanro.autumandu.model.entity.FuelType;
import org.juanro.autumandu.model.entity.OtherCost;
import org.juanro.autumandu.model.entity.Refueling;
import org.juanro.autumandu.model.entity.Reminder;
import org.juanro.autumandu.model.entity.Station;
import org.juanro.autumandu.model.entity.TireList;
import org.juanro.autumandu.model.entity.TireUsage;

/**
 * Class for exporting and importing data in CSV format.
 */
public class CSVExportImport {
    public static final String DIRECTORY = "CSV";

    // Table names
    private static final String TABLE_CAR = "car";
    private static final String TABLE_FUEL_TYPE = "fuel_type";
    private static final String TABLE_STATION = "station";
    private static final String TABLE_OTHER_COST = "other_cost";
    private static final String TABLE_REFUELING = "refueling";
    private static final String TABLE_REMINDER = "reminder";
    private static final String TABLE_TIRE_LIST = "tire_list";
    private static final String TABLE_TIRE_USAGE = "tire_usage";

    // Common columns
    private static final String _ID = "_id";

    // Car columns
    private static final String CAR_NAME = "car__name";
    private static final String CAR_COLOR = "color";
    private static final String CAR_INITIAL_MILEAGE = "initial_mileage";
    private static final String CAR_SUSPENDED_SINCE = "suspended_since";
    private static final String CAR_BUYING_PRICE = "buying_price";
    private static final String CAR_NUM_TIRES = "num_tires";
    private static final String[] CAR_ALL_COLUMNS = {_ID, CAR_NAME, CAR_COLOR, CAR_INITIAL_MILEAGE, CAR_SUSPENDED_SINCE, CAR_BUYING_PRICE, CAR_NUM_TIRES};

    // FuelType columns
    private static final String FUEL_TYPE_NAME = "fuel_type__name";
    private static final String FUEL_TYPE_CATEGORY = "category";
    private static final String[] FUEL_TYPE_ALL_COLUMNS = {_ID, FUEL_TYPE_NAME, FUEL_TYPE_CATEGORY};

    // Station columns
    private static final String STATION_NAME = "station__name";
    private static final String[] STATION_ALL_COLUMNS = {_ID, STATION_NAME};

    // OtherCost columns
    private static final String OTHER_COST_TITLE = "title";
    private static final String OTHER_COST_DATE = "date";
    private static final String OTHER_COST_MILEAGE = "mileage";
    private static final String OTHER_COST_PRICE = "price";
    private static final String OTHER_COST_RECURRENCE_INTERVAL = "recurrence_interval";
    private static final String OTHER_COST_RECURRENCE_MULTIPLIER = "recurrence_multiplier";
    private static final String OTHER_COST_END_DATE = "end_date";
    private static final String OTHER_COST_NOTE = "note";
    private static final String OTHER_COST_CAR_ID = "car_id";
    private static final String[] OTHER_COST_ALL_COLUMNS = {_ID, OTHER_COST_TITLE, OTHER_COST_DATE, OTHER_COST_MILEAGE, OTHER_COST_PRICE, OTHER_COST_RECURRENCE_INTERVAL, OTHER_COST_RECURRENCE_MULTIPLIER, OTHER_COST_END_DATE, OTHER_COST_NOTE, OTHER_COST_CAR_ID};

    // Refueling columns
    private static final String REFUELING_DATE = "date";
    private static final String REFUELING_MILEAGE = "mileage";
    private static final String REFUELING_VOLUME = "volume";
    private static final String REFUELING_PRICE = "price";
    private static final String REFUELING_PARTIAL = "partial";
    private static final String REFUELING_NOTE = "note";
    private static final String REFUELING_FUEL_TYPE_ID = "fuel_type_id";
    private static final String REFUELING_STATION_ID = "station_id";
    private static final String REFUELING_CAR_ID = "car_id";
    private static final String[] REFUELING_ALL_COLUMNS = {_ID, REFUELING_DATE, REFUELING_MILEAGE, REFUELING_VOLUME, REFUELING_PRICE, REFUELING_PARTIAL, REFUELING_NOTE, REFUELING_FUEL_TYPE_ID, REFUELING_STATION_ID, REFUELING_CAR_ID};

    // Reminder columns
    private static final String REMINDER_TITLE = "title";
    private static final String REMINDER_AFTER_TIME_SPAN_UNIT = "after_time_span_unit";
    private static final String REMINDER_AFTER_TIME_SPAN_COUNT = "after_time_span_count";
    private static final String REMINDER_AFTER_DISTANCE = "after_distance";
    private static final String REMINDER_START_DATE = "start_date";
    private static final String REMINDER_START_MILEAGE = "start_mileage";
    private static final String REMINDER_NOTIFICATION_DISMISSED = "notification_dismissed";
    private static final String REMINDER_SNOOZED_UNTIL = "snoozed_until";
    private static final String REMINDER_CAR_ID = "car_id";
    private static final String[] REMINDER_ALL_COLUMNS = {_ID, REMINDER_TITLE, REMINDER_AFTER_TIME_SPAN_UNIT, REMINDER_AFTER_TIME_SPAN_COUNT, REMINDER_AFTER_DISTANCE, REMINDER_START_DATE, REMINDER_START_MILEAGE, REMINDER_NOTIFICATION_DISMISSED, REMINDER_SNOOZED_UNTIL, REMINDER_CAR_ID};

    // TireList columns
    private static final String TIRE_LIST_BUY_DATE = "buy_date";
    private static final String TIRE_LIST_TRASH_DATE = "trash_date";
    private static final String TIRE_LIST_PRICE = "price";
    private static final String TIRE_LIST_QUANTITY = "quantity";
    private static final String TIRE_LIST_MANUFACTURER = "manufacturer";
    private static final String TIRE_LIST_MODEL = "model";
    private static final String TIRE_LIST_NOTE = "note";
    private static final String TIRE_LIST_CAR_ID = "car_id";
    private static final String[] TIRE_LIST_ALL_COLUMNS = {_ID, TIRE_LIST_BUY_DATE, TIRE_LIST_TRASH_DATE, TIRE_LIST_PRICE, TIRE_LIST_QUANTITY, TIRE_LIST_MANUFACTURER, TIRE_LIST_MODEL, TIRE_LIST_NOTE, TIRE_LIST_CAR_ID};

    // TireUsage columns
    private static final String TIRE_USAGE_DISTANCE_MOUNT = "distance_mount";
    private static final String TIRE_USAGE_DATE_MOUNT = "date_mount";
    private static final String TIRE_USAGE_DISTANCE_UMOUNT = "distance_umount";
    private static final String TIRE_USAGE_DATE_UMOUNT = "date_umount";
    private static final String TIRE_USAGE_TIRE_ID = "tire_id";
    private static final String[] TIRE_USAGE_ALL_COLUMNS = {_ID, TIRE_USAGE_DISTANCE_MOUNT, TIRE_USAGE_DATE_MOUNT, TIRE_USAGE_DISTANCE_UMOUNT, TIRE_USAGE_DATE_UMOUNT, TIRE_USAGE_TIRE_ID};

    private final Context context;
    private DocumentFile exportDir;
    private final CSVFormat csvFormat;
    private final Semaphore imExportSemaphore = new Semaphore(1);

    public void init() {
        DocumentFile dir = new Backup(context).getBackupDir();
        if (dir != null && dir.isDirectory()) {
            var csvDir = dir.findFile(DIRECTORY);
            if (csvDir == null) {
                dir = dir.createDirectory(DIRECTORY);
            } else {
                dir = csvDir;
            }
        } else {
            dir = null;
        }
        this.exportDir = dir;
    }

    public CSVExportImport(Context context) {
        this.context = context;
        init();

        this.csvFormat = CSVFormat.Builder.create()
                .setQuoteMode(QuoteMode.MINIMAL)
                .setDelimiter(',')
                .setIgnoreEmptyLines(true)
                .setIgnoreSurroundingSpaces(true)
                .setIgnoreHeaderCase(true)
                .setHeader()
                .setTrim(true)
                .get();
    }

    public boolean anyExportFileExist() {
        if (exportDir == null) return false;
        return exportDir.findFile(TABLE_CAR + ".csv") != null ||
                exportDir.findFile(TABLE_FUEL_TYPE + ".csv") != null ||
                exportDir.findFile(TABLE_STATION + ".csv") != null ||
                exportDir.findFile(TABLE_OTHER_COST + ".csv") != null ||
                exportDir.findFile(TABLE_REFUELING + ".csv") != null ||
                exportDir.findFile(TABLE_REMINDER + ".csv") != null ||
                exportDir.findFile(TABLE_TIRE_LIST + ".csv") != null ||
                exportDir.findFile(TABLE_TIRE_USAGE + ".csv") != null;
    }

    public boolean allExportFilesExist() {
        if (exportDir == null) return false;
        return exportDir.findFile(TABLE_CAR + ".csv") != null &&
                exportDir.findFile(TABLE_FUEL_TYPE + ".csv") != null &&
                exportDir.findFile(TABLE_STATION + ".csv") != null &&
                exportDir.findFile(TABLE_OTHER_COST + ".csv") != null &&
                exportDir.findFile(TABLE_REFUELING + ".csv") != null &&
                exportDir.findFile(TABLE_REMINDER + ".csv") != null &&
                exportDir.findFile(TABLE_TIRE_LIST + ".csv") != null &&
                exportDir.findFile(TABLE_TIRE_USAGE + ".csv") != null;
    }

    public void export() throws CSVImportException {
        try {
            imExportSemaphore.acquire();
        } catch (InterruptedException e) {
            throw new CSVImportException("Another import or export is already running.");
        }

        try {
            if (exportDir == null || !exportDir.isDirectory()) {
                throw new CSVImportException("Could not access export directory.");
            }

            var db = AutuManduDatabase.getInstance(context);
            exportCars(db);
            exportFuelTypes(db);
            exportStations(db);
            exportOtherCosts(db);
            exportRefuelings(db);
            exportReminders(db);
            exportTireList(db);
            exportTireUsages(db);

        } catch (CSVImportException e) {
            throw e;
        } catch (Exception e) {
            throw new CSVImportException(e);
        } finally {
            imExportSemaphore.release();
        }
    }

    private void exportCars(AutuManduDatabase db) throws IOException {
        doExport(TABLE_CAR + ".csv", CAR_ALL_COLUMNS, csv -> {
            var cars = db.getCarDao().getAll();
            for (var car : cars) {
                csv.printRecord(
                        car.getId(),
                        car.getName(),
                        car.getColor(),
                        car.getInitialMileage(),
                        CSVConvert.toString(car.getSuspendedSince()),
                        CSVConvert.toString(car.getBuyingPrice()),
                        car.getNumTires());
            }
        });
    }

    private void exportFuelTypes(AutuManduDatabase db) throws IOException {
        doExport(TABLE_FUEL_TYPE + ".csv", FUEL_TYPE_ALL_COLUMNS, csv -> {
            var fuelTypes = db.getFuelTypeDao().getAll();
            for (var fuelType : fuelTypes) {
                csv.printRecord(
                        fuelType.getId(),
                        fuelType.getName(),
                        fuelType.getCategory());
            }
        });
    }

    private void exportStations(AutuManduDatabase db) throws IOException {
        doExport(TABLE_STATION + ".csv", STATION_ALL_COLUMNS, csv -> {
            var stations = db.getStationDao().getAll();
            for (var station : stations) {
                csv.printRecord(
                        station.getId(),
                        station.getName());
            }
        });
    }

    private void exportOtherCosts(AutuManduDatabase db) throws IOException {
        doExport(TABLE_OTHER_COST + ".csv", OTHER_COST_ALL_COLUMNS, csv -> {
            var otherCosts = db.getOtherCostDao().getAll();
            for (var otherCost : otherCosts) {
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
        });
    }

    private void exportRefuelings(AutuManduDatabase db) throws IOException {
        doExport(TABLE_REFUELING + ".csv", REFUELING_ALL_COLUMNS, csv -> {
            var refuelings = db.getRefuelingDao().getAll();
            for (var refueling : refuelings) {
                csv.printRecord(
                        refueling.getId(),
                        CSVConvert.toString(refueling.getDate()),
                        refueling.getMileage(),
                        CSVConvert.toString(refueling.getVolume()),
                        CSVConvert.toString(refueling.getPrice()),
                        refueling.isPartial(),
                        refueling.getNote(),
                        refueling.getFuelTypeId(),
                        refueling.getStationId(),
                        refueling.getCarId());
            }
        });
    }

    private void exportReminders(AutuManduDatabase db) throws IOException {
        doExport(TABLE_REMINDER + ".csv", REMINDER_ALL_COLUMNS, csv -> {
            var reminders = db.getReminderDao().getAll();
            for (var reminder : reminders) {
                csv.printRecord(
                        reminder.getId(),
                        reminder.getTitle(),
                        CSVConvert.toString(reminder.getAfterTimeSpanUnit()),
                        reminder.getAfterTimeSpanCount(),
                        reminder.getAfterDistance(),
                        CSVConvert.toString(reminder.getStartDate()),
                        reminder.getStartMileage(),
                        reminder.isNotificationDismissed(),
                        CSVConvert.toString(reminder.getSnoozedUntil()),
                        reminder.getCarId());
            }
        });
    }

    private void exportTireList(AutuManduDatabase db) throws IOException {
        doExport(TABLE_TIRE_LIST + ".csv", TIRE_LIST_ALL_COLUMNS, csv -> {
            var tires = db.getTireDao().getAllTireLists();
            for (var tire : tires) {
                csv.printRecord(
                        tire.getId(),
                        CSVConvert.toString(tire.getBuyDate()),
                        CSVConvert.toString(tire.getTrashDate()),
                        CSVConvert.toString(tire.getPrice()),
                        tire.getQuantity(),
                        tire.getManufacturer(),
                        tire.getModel(),
                        tire.getNote(),
                        tire.getCarId());
            }
        });
    }

    private void exportTireUsages(AutuManduDatabase db) throws IOException {
        doExport(TABLE_TIRE_USAGE + ".csv", TIRE_USAGE_ALL_COLUMNS, csv -> {
            var usages = db.getTireDao().getAllTireUsages();
            for (var usage : usages) {
                csv.printRecord(
                        usage.getId(),
                        usage.getDistanceMount(),
                        CSVConvert.toString(usage.getDateMount()),
                        usage.getDistanceUmount(),
                        CSVConvert.toString(usage.getDateUmount()),
                        usage.getTireId());
            }
        });
    }

    private DocumentFile getOrCreateFile(String name) throws IOException {
        var file = exportDir.findFile(name);
        if (file == null) {
            file = exportDir.createFile("text/csv", name);
        }
        if (file == null) {
            throw new IOException("Could not create file: " + name);
        }
        return file;
    }

    public void importData() throws CSVImportException {
        try {
            imExportSemaphore.acquire();
        } catch (InterruptedException e) {
            throw new CSVImportException("Another import or export is already running.");
        }

        try {
            var tempFormat = csvFormat;
            try {
                tempFormat = findDelimiter(tempFormat);
            } catch (IOException e) {
                throw new CSVImportException("Error determining CSV delimiter.");
            }
            final var importFormat = tempFormat;

            var db = AutuManduDatabase.getInstance(context);
            db.runInTransaction(() -> {
                try {
                    db.clearAllTables();

                    importCars(db, importFormat);
                    importFuelTypes(db, importFormat);
                    importStations(db, importFormat);
                    importOtherCosts(db, importFormat);
                    importRefuelings(db, importFormat);
                    importReminders(db, importFormat);
                    importTireList(db, importFormat);
                    importTireUsages(db, importFormat);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

        } catch (CSVImportException e) {
            throw e;
        } catch (Exception e) {
            var cause = e.getCause();
            if (cause instanceof IOException) {
                throw new CSVImportException(cause);
            }
            throw new CSVImportException(e);
        } finally {
            imExportSemaphore.release();
        }
    }

    private CSVFormat findDelimiter(CSVFormat format) throws IOException {
        if (exportDir == null) return format;
        var carFile = exportDir.findFile(TABLE_CAR + ".csv");
        if (carFile == null) {
            return format;
        }

        try (var in = context.getContentResolver().openInputStream(carFile.getUri())) {
            if (in == null) return format;
            try (var reader = new BufferedReader(new InputStreamReader(in))) {
                var header = reader.readLine();
                if (header != null && header.contains(";")) {
                    return CSVFormat.Builder.create(format).setDelimiter(';').get();
                }
            }
        }
        return CSVFormat.Builder.create(format).setDelimiter(',').get();
    }

    private void importCars(AutuManduDatabase db, CSVFormat format) throws IOException {
        doImport(TABLE_CAR + ".csv", format, record -> {
            var car = new Car();
            car.setId(CSVConvert.toLong(record.get(_ID)));
            car.setName(record.get(CAR_NAME));
            var color = CSVConvert.toInteger(record.get(CAR_COLOR));
            car.setColor(color != null ? color : 0);
            var initMileage = CSVConvert.toInteger(record.get(CAR_INITIAL_MILEAGE));
            car.setInitialMileage(initMileage != null ? initMileage : 0);
            car.setSuspendedSince(CSVConvert.toDate(record.get(CAR_SUSPENDED_SINCE)));
            var price = CSVConvert.toFloat(record.get(CAR_BUYING_PRICE));
            car.setBuyingPrice(price != null ? price.doubleValue() : 0.0);
            var tires = CSVConvert.toInteger(record.get(CAR_NUM_TIRES));
            car.setNumTires(tires != null ? tires : 4);
            db.getCarDao().insert(car);
        });
    }

    private void importFuelTypes(AutuManduDatabase db, CSVFormat format) throws IOException {
        doImport(TABLE_FUEL_TYPE + ".csv", format, record -> {
            var fuelType = new FuelType();
            fuelType.setId(CSVConvert.toLong(record.get(_ID)));
            fuelType.setName(record.get(FUEL_TYPE_NAME));
            fuelType.setCategory(record.get(FUEL_TYPE_CATEGORY));
            db.getFuelTypeDao().insert(fuelType);
        });
    }

    private void importStations(AutuManduDatabase db, CSVFormat format) throws IOException {
        doImport(TABLE_STATION + ".csv", format, record -> {
            var station = new Station();
            station.setId(CSVConvert.toLong(record.get(_ID)));
            station.setName(record.get(STATION_NAME));
            db.getStationDao().insert(station);
        });
    }

    private void importOtherCosts(AutuManduDatabase db, CSVFormat format) throws IOException {
        doImport(TABLE_OTHER_COST + ".csv", format, record -> {
            var otherCost = new OtherCost();
            otherCost.setId(CSVConvert.toLong(record.get(_ID)));
            otherCost.setTitle(record.get(OTHER_COST_TITLE));
            var date = CSVConvert.toDate(record.get(OTHER_COST_DATE));
            otherCost.setDate(date != null ? date : new Date());
            otherCost.setMileage(CSVConvert.toInteger(record.get(OTHER_COST_MILEAGE)));
            var price = CSVConvert.toFloat(record.get(OTHER_COST_PRICE));
            otherCost.setPrice(price != null ? price : 0f);
            var interval = CSVConvert.toRecurrenceInterval(record.get(OTHER_COST_RECURRENCE_INTERVAL));
            otherCost.setRecurrenceInterval(interval != null ? interval : org.juanro.autumandu.model.entity.helper.RecurrenceInterval.ONCE);
            var mult = CSVConvert.toInteger(record.get(OTHER_COST_RECURRENCE_MULTIPLIER));
            otherCost.setRecurrenceMultiplier(mult != null ? mult : 0);
            otherCost.setEndDate(CSVConvert.toDate(record.get(OTHER_COST_END_DATE)));
            otherCost.setNote(record.get(OTHER_COST_NOTE));
            var carId = CSVConvert.toLong(record.get(OTHER_COST_CAR_ID));
            otherCost.setCarId(carId != null ? carId : 0L);
            db.getOtherCostDao().insert(otherCost);
        });
    }

    private void importRefuelings(AutuManduDatabase db, CSVFormat format) throws IOException {
        doImport(TABLE_REFUELING + ".csv", format, record -> {
            var refueling = new Refueling();
            refueling.setId(CSVConvert.toLong(record.get(_ID)));
            var date = CSVConvert.toDate(record.get(REFUELING_DATE));
            refueling.setDate(date != null ? date : new Date());
            var mileage = CSVConvert.toInteger(record.get(REFUELING_MILEAGE));
            refueling.setMileage(mileage != null ? mileage : 0);
            var volume = CSVConvert.toFloat(record.get(REFUELING_VOLUME));
            refueling.setVolume(volume != null ? volume : 0f);
            var price = CSVConvert.toFloat(record.get(REFUELING_PRICE));
            refueling.setPrice(price != null ? price : 0f);
            var partial = CSVConvert.toBoolean(record.get(REFUELING_PARTIAL));
            refueling.setPartial(partial != null ? partial : false);
            refueling.setNote(record.get(REFUELING_NOTE));
            var fuelId = CSVConvert.toLong(record.get(REFUELING_FUEL_TYPE_ID));
            refueling.setFuelTypeId(fuelId != null ? fuelId : 0L);
            var stationId = CSVConvert.toLong(record.get(REFUELING_STATION_ID));
            refueling.setStationId(stationId != null ? stationId : 0L);
            var carId = CSVConvert.toLong(record.get(REFUELING_CAR_ID));
            refueling.setCarId(carId != null ? carId : 0L);
            db.getRefuelingDao().insert(refueling);
        });
    }

    private void importReminders(AutuManduDatabase db, CSVFormat format) throws IOException {
        doImport(TABLE_REMINDER + ".csv", format, record -> {
            var reminder = new Reminder();
            reminder.setId(CSVConvert.toLong(record.get(_ID)));
            reminder.setTitle(record.get(REMINDER_TITLE));
            reminder.setAfterTimeSpanUnit(CSVConvert.toTimeSpanUnit(record.get(REMINDER_AFTER_TIME_SPAN_UNIT)));
            reminder.setAfterTimeSpanCount(CSVConvert.toInteger(record.get(REMINDER_AFTER_TIME_SPAN_COUNT)));
            reminder.setAfterDistance(CSVConvert.toInteger(record.get(REMINDER_AFTER_DISTANCE)));
            var startDate = CSVConvert.toDate(record.get(REMINDER_START_DATE));
            reminder.setStartDate(startDate != null ? startDate : new Date());
            var startMileage = CSVConvert.toInteger(record.get(REMINDER_START_MILEAGE));
            reminder.setStartMileage(startMileage != null ? startMileage : 0);
            var dismissed = CSVConvert.toBoolean(record.get(REMINDER_NOTIFICATION_DISMISSED));
            reminder.setNotificationDismissed(dismissed != null ? dismissed : false);
            reminder.setSnoozedUntil(CSVConvert.toDate(record.get(REMINDER_SNOOZED_UNTIL)));
            var carId = CSVConvert.toLong(record.get(REMINDER_CAR_ID));
            reminder.setCarId(carId != null ? carId : 0L);
            db.getReminderDao().insert(reminder);
        });
    }

    private void importTireList(AutuManduDatabase db, CSVFormat format) throws IOException {
        doImport(TABLE_TIRE_LIST + ".csv", format, record -> {
            var tireList = new TireList();
            tireList.setId(CSVConvert.toLong(record.get(_ID)));
            var buyDate = CSVConvert.toDate(record.get(TIRE_LIST_BUY_DATE));
            tireList.setBuyDate(buyDate != null ? buyDate : new Date());
            tireList.setTrashDate(CSVConvert.toDate(record.get(TIRE_LIST_TRASH_DATE)));
            var price = CSVConvert.toFloat(record.get(TIRE_LIST_PRICE));
            tireList.setPrice(price != null ? price : 0f);
            var quantity = CSVConvert.toInteger(record.get(TIRE_LIST_QUANTITY));
            tireList.setQuantity(quantity != null ? quantity : 0);
            tireList.setManufacturer(record.get(TIRE_LIST_MANUFACTURER));
            tireList.setModel(record.get(TIRE_LIST_MODEL));
            tireList.setNote(record.get(TIRE_LIST_NOTE));
            var carId = CSVConvert.toLong(record.get(TIRE_LIST_CAR_ID));
            tireList.setCarId(carId != null ? carId : 0L);
            db.getTireDao().insert(tireList);
        });
    }

    private void importTireUsages(AutuManduDatabase db, CSVFormat format) throws IOException {
        doImport(TABLE_TIRE_USAGE + ".csv", format, record -> {
            var tireUsage = new TireUsage();
            tireUsage.setId(CSVConvert.toLong(record.get(_ID)));
            var mountDist = CSVConvert.toInteger(record.get(TIRE_USAGE_DISTANCE_MOUNT));
            tireUsage.setDistanceMount(mountDist != null ? mountDist : 0);
            var mountDate = CSVConvert.toDate(record.get(TIRE_USAGE_DATE_MOUNT));
            tireUsage.setDateMount(mountDate != null ? mountDate : new Date());
            var umountDist = CSVConvert.toInteger(record.get(TIRE_USAGE_DISTANCE_UMOUNT));
            tireUsage.setDistanceUmount(umountDist != null ? umountDist : 0);
            tireUsage.setDateUmount(CSVConvert.toDate(record.get(TIRE_USAGE_DATE_UMOUNT)));
            var tireId = CSVConvert.toLong(record.get(TIRE_USAGE_TIRE_ID));
            tireUsage.setTireId(tireId != null ? tireId : 0L);
            db.getTireDao().insert(tireUsage);
        });
    }

    @FunctionalInterface
    private interface ExportAction {
        void export(CSVPrinter csv) throws IOException;
    }

    @FunctionalInterface
    private interface ImportAction {
        void importRecord(CSVRecord record) throws IOException;
    }

    private void doExport(String fileName, String[] columns, ExportAction action) throws IOException {
        var file = getOrCreateFile(fileName);
        try (var out = context.getContentResolver().openOutputStream(file.getUri())) {
            if (out == null) throw new IOException("Could not open output stream for " + fileName);
            try (var writer = new PrintWriter(out);
                 var csv = new CSVPrinter(writer, csvFormat)) {
                csv.printRecord((Object[]) columns);
                action.export(csv);
            }
        }
    }

    private void doImport(String fileName, CSVFormat format, ImportAction action) throws IOException {
        if (exportDir == null) return;
        var file = exportDir.findFile(fileName);
        if (file == null) return;

        try (var in = context.getContentResolver().openInputStream(file.getUri())) {
            if (in == null) throw new IOException("Could not open input stream for " + fileName);
            try (var reader = new InputStreamReader(in);
                 var parser = CSVParser.parse(reader, format)) {
                for (var record : parser) {
                    action.importRecord(record);
                }
            }
        }
    }
}
