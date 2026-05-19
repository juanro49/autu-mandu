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
import android.util.Log;

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
import java.util.Objects;
import java.util.concurrent.Semaphore;

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
    private static final String TAG = "CSVExportImport";
    public static final String DIRECTORY = "CSV";
    private static final String FILE_EXTENSION = ".csv";

    // Table names
    private static final String TABLE_CAR = "car";
    private static final String TABLE_FUEL_TYPE = "fuel_type";
    private static final String TABLE_STATION = "station";
    private static final String TABLE_OTHER_COST = "other_cost";
    private static final String TABLE_REFUELING = "refueling";
    private static final String TABLE_REMINDER = "reminder";
    private static final String TABLE_TIRE_LIST = "tire_list";
    private static final String TABLE_TIRE_USAGE = "tire_usage";
    private static final String TABLE_TRIP = "trip";
    private static final String TABLE_TRIP_PREFAB = "trip_prefab";

    // Common columns
    private static final String COLUMN_ID = "_id";
    private static final String DEFAULT_NAME = "Unknown";

    // Car columns
    private static final String CAR_NAME = "car__name";
    private static final String CAR_COLOR = "color";
    private static final String CAR_INITIAL_MILEAGE = "initial_mileage";
    private static final String CAR_SUSPENDED_SINCE = "suspended_since";
    private static final String CAR_BUYING_PRICE = "buying_price";
    private static final String CAR_NUM_TIRES = "num_tires";
    private static final String[] CAR_ALL_COLUMNS = {COLUMN_ID, CAR_NAME, CAR_COLOR, CAR_INITIAL_MILEAGE, CAR_SUSPENDED_SINCE, CAR_BUYING_PRICE, CAR_NUM_TIRES};

    // FuelType columns
    private static final String FUEL_TYPE_NAME = "fuel_type__name";
    private static final String FUEL_TYPE_CATEGORY = "category";
    private static final String[] FUEL_TYPE_ALL_COLUMNS = {COLUMN_ID, FUEL_TYPE_NAME, FUEL_TYPE_CATEGORY};

    // Station columns
    private static final String STATION_NAME = "station__name";
    private static final String[] STATION_ALL_COLUMNS = {COLUMN_ID, STATION_NAME};

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
    private static final String[] OTHER_COST_ALL_COLUMNS = {COLUMN_ID, OTHER_COST_TITLE, OTHER_COST_DATE, OTHER_COST_MILEAGE, OTHER_COST_PRICE, OTHER_COST_RECURRENCE_INTERVAL, OTHER_COST_RECURRENCE_MULTIPLIER, OTHER_COST_END_DATE, OTHER_COST_NOTE, OTHER_COST_CAR_ID};

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
    private static final String[] REFUELING_ALL_COLUMNS = {COLUMN_ID, REFUELING_DATE, REFUELING_MILEAGE, REFUELING_VOLUME, REFUELING_PRICE, REFUELING_PARTIAL, REFUELING_NOTE, REFUELING_FUEL_TYPE_ID, REFUELING_STATION_ID, REFUELING_CAR_ID};

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
    private static final String[] REMINDER_ALL_COLUMNS = {COLUMN_ID, REMINDER_TITLE, REMINDER_AFTER_TIME_SPAN_UNIT, REMINDER_AFTER_TIME_SPAN_COUNT, REMINDER_AFTER_DISTANCE, REMINDER_START_DATE, REMINDER_START_MILEAGE, REMINDER_NOTIFICATION_DISMISSED, REMINDER_SNOOZED_UNTIL, REMINDER_CAR_ID};

    // TireList columns
    private static final String TIRE_LIST_BUY_DATE = "buy_date";
    private static final String TIRE_LIST_TRASH_DATE = "trash_date";
    private static final String TIRE_LIST_PRICE = "price";
    private static final String TIRE_LIST_QUANTITY = "quantity";
    private static final String TIRE_LIST_MANUFACTURER = "manufacturer";
    private static final String TIRE_LIST_MODEL = "model";
    private static final String TIRE_LIST_NOTE = "note";
    private static final String TIRE_LIST_CAR_ID = "car_id";
    private static final String[] TIRE_LIST_ALL_COLUMNS = {COLUMN_ID, TIRE_LIST_BUY_DATE, TIRE_LIST_TRASH_DATE, TIRE_LIST_PRICE, TIRE_LIST_QUANTITY, TIRE_LIST_MANUFACTURER, TIRE_LIST_MODEL, TIRE_LIST_NOTE, TIRE_LIST_CAR_ID};

    // TireUsage columns
    private static final String TIRE_USAGE_DISTANCE_MOUNT = "distance_mount";
    private static final String TIRE_USAGE_DATE_MOUNT = "date_mount";
    private static final String TIRE_USAGE_DISTANCE_UMOUNT = "distance_umount";
    private static final String TIRE_USAGE_DATE_UMOUNT = "date_umount";
    private static final String TIRE_USAGE_TIRE_ID = "tire_id";
    private static final String[] TIRE_USAGE_ALL_COLUMNS = {COLUMN_ID, TIRE_USAGE_DISTANCE_MOUNT, TIRE_USAGE_DATE_MOUNT, TIRE_USAGE_DISTANCE_UMOUNT, TIRE_USAGE_DATE_UMOUNT, TIRE_USAGE_TIRE_ID};

    // Trip columns
    private static final String TRIP_CAR_ID = "car_id";
    private static final String TRIP_REFUELING_ID = "refueling_id";
    private static final String TRIP_DATE = "date";
    private static final String TRIP_DATE_END = "date_end";
    private static final String TRIP_TIME_START = "time_start";
    private static final String TRIP_TIME_END = "time_end";
    private static final String TRIP_ROUTE_TARGET = "route_target";
    private static final String TRIP_PURPOSE = "purpose";
    private static final String TRIP_COMPANIES_VISITED = "companies_visited";
    private static final String TRIP_DRIVER = "driver";
    private static final String TRIP_OCCUPANTS = "occupants";
    private static final String TRIP_CARGO = "cargo";
    private static final String TRIP_KM_START = "km_start";
    private static final String TRIP_KM_END = "km_end";
    private static final String TRIP_KM_BUSINESS = "km_business";
    private static final String TRIP_KM_PRIVATE = "km_private";
    private static final String TRIP_KM_HOME_WORK = "km_home_work";
    private static final String TRIP_FUEL_LITERS = "fuel_liters";
    private static final String TRIP_FUEL_COST = "fuel_cost";
    private static final String TRIP_OTHER_COSTS_DESCRIPTION = "other_costs_description";
    private static final String TRIP_OTHER_COSTS_AMOUNT = "other_costs_amount";
    private static final String TRIP_CREATED_AT = "created_at";
    private static final String TRIP_UPDATED_AT = "updated_at";
    private static final String[] TRIP_ALL_COLUMNS = {COLUMN_ID, TRIP_CAR_ID, TRIP_REFUELING_ID, TRIP_DATE, TRIP_DATE_END, TRIP_TIME_START, TRIP_TIME_END, TRIP_ROUTE_TARGET, TRIP_PURPOSE, TRIP_COMPANIES_VISITED, TRIP_DRIVER, TRIP_OCCUPANTS, TRIP_CARGO, TRIP_KM_START, TRIP_KM_END, TRIP_KM_BUSINESS, TRIP_KM_PRIVATE, TRIP_KM_HOME_WORK, TRIP_FUEL_LITERS, TRIP_FUEL_COST, TRIP_OTHER_COSTS_DESCRIPTION, TRIP_OTHER_COSTS_AMOUNT, TRIP_CREATED_AT, TRIP_UPDATED_AT};

    // TripPrefab columns
    private static final String TRIP_PREFAB_CAR_ID = "car_id";
    private static final String TRIP_PREFAB_TYPE = "type";
    private static final String TRIP_PREFAB_VALUE = "value";
    private static final String TRIP_PREFAB_USAGE_COUNT = "usage_count";
    private static final String[] TRIP_PREFAB_ALL_COLUMNS = {COLUMN_ID, TRIP_PREFAB_CAR_ID, TRIP_PREFAB_TYPE, TRIP_PREFAB_VALUE, TRIP_PREFAB_USAGE_COUNT};

    private final Context context;
    private DocumentFile exportDir;
    private final CSVFormat csvFormat;
    private final Semaphore imExportSemaphore = new Semaphore(1);

    public void init() {
        DocumentFile baseDir = new Backup(context).getBackupDir();
        final DocumentFile targetDir;
        if (baseDir != null && baseDir.isDirectory()) {
            var csvDir = baseDir.findFile(DIRECTORY);
            targetDir = (csvDir == null) ? baseDir.createDirectory(DIRECTORY) : csvDir;
        } else {
            targetDir = null;
        }
        this.exportDir = targetDir;
    }

    private DocumentFile getExportDir() {
        init();
        return exportDir;
    }

    public CSVExportImport(Context context) {
        this.context = context;

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
        DocumentFile dir = getExportDir();
        if (dir == null) return false;
        return dir.findFile(TABLE_CAR + FILE_EXTENSION) != null ||
                dir.findFile(TABLE_FUEL_TYPE + FILE_EXTENSION) != null ||
                dir.findFile(TABLE_STATION + FILE_EXTENSION) != null ||
                dir.findFile(TABLE_OTHER_COST + FILE_EXTENSION) != null ||
                dir.findFile(TABLE_REFUELING + FILE_EXTENSION) != null ||
                dir.findFile(TABLE_REMINDER + FILE_EXTENSION) != null ||
                dir.findFile(TABLE_TIRE_LIST + FILE_EXTENSION) != null ||
                dir.findFile(TABLE_TIRE_USAGE + FILE_EXTENSION) != null ||
                dir.findFile(TABLE_TRIP + FILE_EXTENSION) != null ||
                dir.findFile(TABLE_TRIP_PREFAB + FILE_EXTENSION) != null;
    }

    public void export() throws CSVImportException {
        try {
            imExportSemaphore.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CSVImportException("Another import or export is already running.");
        }

        try {
            DocumentFile dir = getExportDir();
            if (dir == null || !dir.isDirectory()) {
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
            exportTrips(db);
            exportTripPrefabs(db);

        } catch (CSVImportException e) {
            throw e;
        } catch (Exception e) {
            throw new CSVImportException(e);
        } finally {
            imExportSemaphore.release();
        }
    }

    private void exportCars(AutuManduDatabase db) throws IOException {
        doExport(TABLE_CAR + FILE_EXTENSION, CAR_ALL_COLUMNS, csv -> {
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
        doExport(TABLE_FUEL_TYPE + FILE_EXTENSION, FUEL_TYPE_ALL_COLUMNS, csv -> {
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
        doExport(TABLE_STATION + FILE_EXTENSION, STATION_ALL_COLUMNS, csv -> {
            var stations = db.getStationDao().getAll();
            for (var station : stations) {
                csv.printRecord(
                        station.getId(),
                        station.getName());
            }
        });
    }

    private void exportOtherCosts(AutuManduDatabase db) throws IOException {
        doExport(TABLE_OTHER_COST + FILE_EXTENSION, OTHER_COST_ALL_COLUMNS, csv -> {
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
        doExport(TABLE_REFUELING + FILE_EXTENSION, REFUELING_ALL_COLUMNS, csv -> {
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
        doExport(TABLE_REMINDER + FILE_EXTENSION, REMINDER_ALL_COLUMNS, csv -> {
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
        doExport(TABLE_TIRE_LIST + FILE_EXTENSION, TIRE_LIST_ALL_COLUMNS, csv -> {
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
        doExport(TABLE_TIRE_USAGE + FILE_EXTENSION, TIRE_USAGE_ALL_COLUMNS, csv -> {
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

    private void exportTrips(AutuManduDatabase db) throws IOException {
        doExport(TABLE_TRIP + FILE_EXTENSION, TRIP_ALL_COLUMNS, csv -> {
            for (var trip : db.getTripDao().getAll()) {
                csv.printRecord(
                        trip.getId(),
                        trip.getCarId(),
                        trip.getRefuelingId(),
                        CSVConvert.toString(trip.getDate()),
                        CSVConvert.toString(trip.getDateEnd()),
                        CSVConvert.toString(trip.getTimeStart()),
                        CSVConvert.toString(trip.getTimeEnd()),
                        trip.getRouteTarget(),
                        trip.getPurpose(),
                        trip.getCompaniesVisited(),
                        trip.getDriver(),
                        trip.getOccupants(),
                        trip.getCargo(),
                        trip.getKmStart(),
                        trip.getKmEnd(),
                        trip.getKmBusiness(),
                        trip.getKmPrivate(),
                        trip.getKmHomeWork(),
                        CSVConvert.toString(trip.getFuelLiters()),
                        CSVConvert.toString(trip.getFuelCost()),
                        trip.getOtherCostsDescription(),
                        CSVConvert.toString(trip.getOtherCostsAmount()),
                        CSVConvert.toString(trip.getCreatedAt()),
                        CSVConvert.toString(trip.getUpdatedAt())
                );
            }
        });
    }

    private void exportTripPrefabs(AutuManduDatabase db) throws IOException {
        doExport(TABLE_TRIP_PREFAB + FILE_EXTENSION, TRIP_PREFAB_ALL_COLUMNS, csv -> {
            for (var prefab : db.getTripPrefabDao().getAll()) {
                csv.printRecord(
                        prefab.getId(),
                        prefab.getCarId(),
                        prefab.getType(),
                        prefab.getValue(),
                        prefab.getUsageCount()
                );
            }
        });
    }

    private DocumentFile getOrCreateFile(String name) throws IOException {
        DocumentFile dir = getExportDir();
        if (dir == null) throw new IOException("Export directory not accessible.");
        DocumentFile existingFile = dir.findFile(name);

        String nameWithoutExtension = name;
        if (name.endsWith(FILE_EXTENSION)) {
            nameWithoutExtension = name.substring(0, name.length() - FILE_EXTENSION.length());
        }

        DocumentFile file = (existingFile != null) ? existingFile : dir.createFile("text/csv", nameWithoutExtension);
        if (file == null) {
            throw new IOException("Could not create file: " + name);
        }
        return file;
    }

    public void importData() throws CSVImportException {
        try {
            imExportSemaphore.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CSVImportException("Another import or export is already running.");
        }

        Log.d(TAG, "Starting CSV import...");
        try {
            final var importFormat = getImportFormat();
            var db = AutuManduDatabase.getInstance(context);
            db.runInTransaction(() -> performImport(db, importFormat));
        } catch (CSVImportException e) {
            Log.e(TAG, "CSVImportException during import", e);
            throw e;
        } catch (Exception e) {
            Log.e(TAG, "Unexpected exception during import", e);
            throw new CSVImportException(e);
        } finally {
            imExportSemaphore.release();
        }
    }

    private CSVFormat getImportFormat() throws CSVImportException {
        try {
            var format = findDelimiter(csvFormat);
            Log.d(TAG, "Using delimiter: " + format.getDelimiterString());
            return format;
        } catch (IOException e) {
            Log.e(TAG, "Error determining CSV delimiter", e);
            throw new CSVImportException("Error determining CSV delimiter.");
        }
    }

    private void performImport(AutuManduDatabase db, CSVFormat format) {
        try {
            // La importación CSV actualiza entradas existentes y agrega nuevas, no elimina.
            Log.d(TAG, "Importing data...");
            importCars(db, format);
            importFuelTypes(db, format);
            importStations(db, format);
            importOtherCosts(db, format);
            importRefuelings(db, format);
            importReminders(db, format);
            importTireList(db, format);
            importTireUsages(db, format);
            importTrips(db, format);
            importTripPrefabs(db, format);
            Log.d(TAG, "Import finished successfully.");
        } catch (Exception e) {
            Log.e(TAG, "Error during import perform", e);
            throw new CSVImportProcessingRuntimeException("Error during import perform", e);
        }
    }

    private CSVFormat findDelimiter(CSVFormat format) throws IOException {
        DocumentFile dir = getExportDir();
        if (dir == null) return format;
        var carFile = dir.findFile(TABLE_CAR + FILE_EXTENSION);
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

    private String getSafe(CSVRecord csvRecord, String column) {
        return (csvRecord.isMapped(column) && csvRecord.isSet(column)) ? csvRecord.get(column) : null;
    }

    private void importCars(AutuManduDatabase db, CSVFormat format) throws IOException {
        doImport(TABLE_CAR + FILE_EXTENSION, format, csvRecord -> {
            Long id = CSVConvert.toLong(getSafe(csvRecord, COLUMN_ID));
            if (id != null) {
                db.getCarDao().insert(mapCar(csvRecord, id));
            }
        });
    }

    private Car mapCar(CSVRecord csvRecord, long id) {
        var car = new Car();
        car.setId(id);
        car.setName(Objects.requireNonNullElse(getSafe(csvRecord, CAR_NAME), DEFAULT_NAME));
        var color = CSVConvert.toInteger(getSafe(csvRecord, CAR_COLOR));
        car.setColor(color != null ? color : 0);
        var initMileage = CSVConvert.toInteger(getSafe(csvRecord, CAR_INITIAL_MILEAGE));
        car.setInitialMileage(initMileage != null ? initMileage : 0);
        car.setSuspendedSince(CSVConvert.toDate(getSafe(csvRecord, CAR_SUSPENDED_SINCE)));
        var price = CSVConvert.toFloat(getSafe(csvRecord, CAR_BUYING_PRICE));
        car.setBuyingPrice(price != null ? price.doubleValue() : 0.0);
        var tires = CSVConvert.toInteger(getSafe(csvRecord, CAR_NUM_TIRES));
        car.setNumTires(tires != null ? tires : 4);
        return car;
    }

    private void importFuelTypes(AutuManduDatabase db, CSVFormat format) throws IOException {
        doImport(TABLE_FUEL_TYPE + FILE_EXTENSION, format, csvRecord -> {
            Long id = CSVConvert.toLong(getSafe(csvRecord, COLUMN_ID));
            if (id != null) {
                var fuelType = new FuelType();
                fuelType.setId(id);
                fuelType.setName(Objects.requireNonNullElse(getSafe(csvRecord, FUEL_TYPE_NAME), DEFAULT_NAME));
                fuelType.setCategory(getSafe(csvRecord, FUEL_TYPE_CATEGORY));
                db.getFuelTypeDao().insert(fuelType);
            }
        });
    }

    private void importStations(AutuManduDatabase db, CSVFormat format) throws IOException {
        doImport(TABLE_STATION + FILE_EXTENSION, format, csvRecord -> {
            Long id = CSVConvert.toLong(getSafe(csvRecord, COLUMN_ID));
            if (id != null) {
                var station = new Station();
                station.setId(id);
                station.setName(Objects.requireNonNullElse(getSafe(csvRecord, STATION_NAME), DEFAULT_NAME));
                db.getStationDao().insert(station);
            }
        });
    }

    private void importOtherCosts(AutuManduDatabase db, CSVFormat format) throws IOException {
        doImport(TABLE_OTHER_COST + FILE_EXTENSION, format, csvRecord -> {
            Long id = CSVConvert.toLong(getSafe(csvRecord, COLUMN_ID));
            if (id != null) {
                db.getOtherCostDao().insert(mapOtherCost(csvRecord, id));
            }
        });
    }

    private OtherCost mapOtherCost(CSVRecord csvRecord, long id) {
        var otherCost = new OtherCost();
        otherCost.setId(id);
        otherCost.setTitle(Objects.requireNonNullElse(getSafe(csvRecord, OTHER_COST_TITLE), DEFAULT_NAME));
        var date = CSVConvert.toDate(getSafe(csvRecord, OTHER_COST_DATE));
        otherCost.setDate(date != null ? date : new Date());
        otherCost.setMileage(CSVConvert.toInteger(getSafe(csvRecord, OTHER_COST_MILEAGE)));
        var price = CSVConvert.toFloat(getSafe(csvRecord, OTHER_COST_PRICE));
        otherCost.setPrice(price != null ? price : 0f);
        var interval = CSVConvert.toRecurrenceInterval(getSafe(csvRecord, OTHER_COST_RECURRENCE_INTERVAL));
        otherCost.setRecurrenceInterval(interval != null ? interval : org.juanro.autumandu.model.entity.helper.RecurrenceInterval.ONCE);
        var mult = CSVConvert.toInteger(getSafe(csvRecord, OTHER_COST_RECURRENCE_MULTIPLIER));
        otherCost.setRecurrenceMultiplier(mult != null ? mult : 0);
        otherCost.setEndDate(CSVConvert.toDate(getSafe(csvRecord, OTHER_COST_END_DATE)));
        otherCost.setNote(Objects.requireNonNullElse(getSafe(csvRecord, OTHER_COST_NOTE), ""));
        var carId = CSVConvert.toLong(getSafe(csvRecord, OTHER_COST_CAR_ID));
        otherCost.setCarId(carId != null ? carId : 0L);
        return otherCost;
    }

    private void importRefuelings(AutuManduDatabase db, CSVFormat format) throws IOException {
        doImport(TABLE_REFUELING + FILE_EXTENSION, format, csvRecord -> {
            Long id = CSVConvert.toLong(getSafe(csvRecord, COLUMN_ID));
            if (id != null) {
                db.getRefuelingDao().insert(mapRefueling(csvRecord, id));
            }
        });
    }

    private Refueling mapRefueling(CSVRecord csvRecord, long id) {
        var refueling = new Refueling();
        refueling.setId(id);
        var date = CSVConvert.toDate(getSafe(csvRecord, REFUELING_DATE));
        refueling.setDate(date != null ? date : new Date());
        var mileage = CSVConvert.toInteger(getSafe(csvRecord, REFUELING_MILEAGE));
        refueling.setMileage(mileage != null ? mileage : 0);
        var volume = CSVConvert.toFloat(getSafe(csvRecord, REFUELING_VOLUME));
        refueling.setVolume(volume != null ? volume : 0f);
        var price = CSVConvert.toFloat(getSafe(csvRecord, REFUELING_PRICE));
        refueling.setPrice(price != null ? price : 0f);
        var partial = CSVConvert.toBoolean(getSafe(csvRecord, REFUELING_PARTIAL));
        refueling.setPartial(Boolean.TRUE.equals(partial));
        refueling.setNote(Objects.requireNonNullElse(getSafe(csvRecord, REFUELING_NOTE), ""));
        var fuelId = CSVConvert.toLong(getSafe(csvRecord, REFUELING_FUEL_TYPE_ID));
        refueling.setFuelTypeId(fuelId != null ? fuelId : 0L);
        var stationId = CSVConvert.toLong(getSafe(csvRecord, REFUELING_STATION_ID));
        refueling.setStationId(stationId != null ? stationId : 0L);
        var carId = CSVConvert.toLong(getSafe(csvRecord, REFUELING_CAR_ID));
        refueling.setCarId(carId != null ? carId : 0L);
        return refueling;
    }

    private void importReminders(AutuManduDatabase db, CSVFormat format) throws IOException {
        doImport(TABLE_REMINDER + FILE_EXTENSION, format, csvRecord -> {
            Long id = CSVConvert.toLong(getSafe(csvRecord, COLUMN_ID));
            if (id != null) {
                db.getReminderDao().insert(mapReminder(csvRecord, id));
            }
        });
    }

    private Reminder mapReminder(CSVRecord csvRecord, long id) {
        var reminder = new Reminder();
        reminder.setId(id);
        reminder.setTitle(Objects.requireNonNullElse(getSafe(csvRecord, REMINDER_TITLE), DEFAULT_NAME));
        reminder.setAfterTimeSpanUnit(CSVConvert.toTimeSpanUnit(getSafe(csvRecord, REMINDER_AFTER_TIME_SPAN_UNIT)));
        reminder.setAfterTimeSpanCount(CSVConvert.toInteger(getSafe(csvRecord, REMINDER_AFTER_TIME_SPAN_COUNT)));
        reminder.setAfterDistance(CSVConvert.toInteger(getSafe(csvRecord, REMINDER_AFTER_DISTANCE)));
        var startDate = CSVConvert.toDate(getSafe(csvRecord, REMINDER_START_DATE));
        reminder.setStartDate(startDate != null ? startDate : new Date());
        var startMileage = CSVConvert.toInteger(getSafe(csvRecord, REMINDER_START_MILEAGE));
        reminder.setStartMileage(startMileage != null ? startMileage : 0);
        var dismissed = CSVConvert.toBoolean(getSafe(csvRecord, REMINDER_NOTIFICATION_DISMISSED));
        reminder.setNotificationDismissed(Boolean.TRUE.equals(dismissed));
        reminder.setSnoozedUntil(CSVConvert.toDate(getSafe(csvRecord, REMINDER_SNOOZED_UNTIL)));
        var carId = CSVConvert.toLong(getSafe(csvRecord, REMINDER_CAR_ID));
        reminder.setCarId(carId != null ? carId : 0L);
        return reminder;
    }

    private void importTireList(AutuManduDatabase db, CSVFormat format) throws IOException {
        doImport(TABLE_TIRE_LIST + FILE_EXTENSION, format, csvRecord -> {
            Long id = CSVConvert.toLong(getSafe(csvRecord, COLUMN_ID));
            if (id != null) {
                db.getTireDao().insert(mapTireList(csvRecord, id));
            }
        });
    }

    private TireList mapTireList(CSVRecord csvRecord, long id) {
        var tireList = new TireList();
        tireList.setId(id);
        var buyDate = CSVConvert.toDate(getSafe(csvRecord, TIRE_LIST_BUY_DATE));
        tireList.setBuyDate(buyDate != null ? buyDate : new Date());
        tireList.setTrashDate(CSVConvert.toDate(getSafe(csvRecord, TIRE_LIST_TRASH_DATE)));
        var price = CSVConvert.toFloat(getSafe(csvRecord, TIRE_LIST_PRICE));
        tireList.setPrice(price != null ? price : 0f);
        var quantity = CSVConvert.toInteger(getSafe(csvRecord, TIRE_LIST_QUANTITY));
        tireList.setQuantity(quantity != null ? quantity : 0);
        tireList.setManufacturer(Objects.requireNonNullElse(getSafe(csvRecord, TIRE_LIST_MANUFACTURER), DEFAULT_NAME));
        tireList.setModel(Objects.requireNonNullElse(getSafe(csvRecord, TIRE_LIST_MODEL), DEFAULT_NAME));
        tireList.setNote(Objects.requireNonNullElse(getSafe(csvRecord, TIRE_LIST_NOTE), ""));
        var carId = CSVConvert.toLong(getSafe(csvRecord, TIRE_LIST_CAR_ID));
        tireList.setCarId(carId != null ? carId : 0L);
        return tireList;
    }

    private void importTireUsages(AutuManduDatabase db, CSVFormat format) throws IOException {
        doImport(TABLE_TIRE_USAGE + FILE_EXTENSION, format, csvRecord -> {
            Long id = CSVConvert.toLong(getSafe(csvRecord, COLUMN_ID));
            if (id != null) {
                var tireUsage = new TireUsage();
                tireUsage.setId(id);
                var mountDist = CSVConvert.toInteger(getSafe(csvRecord, TIRE_USAGE_DISTANCE_MOUNT));
                tireUsage.setDistanceMount(mountDist != null ? mountDist : 0);
                var mountDate = CSVConvert.toDate(getSafe(csvRecord, TIRE_USAGE_DATE_MOUNT));
                tireUsage.setDateMount(mountDate != null ? mountDate : new Date());
                var umountDist = CSVConvert.toInteger(getSafe(csvRecord, TIRE_USAGE_DISTANCE_UMOUNT));
                tireUsage.setDistanceUmount(umountDist != null ? umountDist : 0);
                tireUsage.setDateUmount(CSVConvert.toDate(getSafe(csvRecord, TIRE_USAGE_DATE_UMOUNT)));
                var tireId = CSVConvert.toLong(getSafe(csvRecord, TIRE_USAGE_TIRE_ID));
                tireUsage.setTireId(tireId != null ? tireId : 0L);
                db.getTireDao().insert(tireUsage);
            }
        });
    }

    private void importTrips(AutuManduDatabase db, CSVFormat format) throws IOException {
        doImport(TABLE_TRIP + FILE_EXTENSION, format, csvRecord -> {
            Long id = CSVConvert.toLong(getSafe(csvRecord, COLUMN_ID));
            if (id != null) {
                db.getTripDao().insert(mapTrip(csvRecord, id));
            }
        });
    }

    private org.juanro.autumandu.model.entity.Trip mapTrip(CSVRecord csvRecord, long id) {
        var trip = new org.juanro.autumandu.model.entity.Trip();
        trip.setId(id);
        trip.setCarId(Objects.requireNonNullElse(CSVConvert.toLong(getSafe(csvRecord, TRIP_CAR_ID)), 0L));
        trip.setRefuelingId(CSVConvert.toLong(getSafe(csvRecord, TRIP_REFUELING_ID)));

        var date = CSVConvert.toLocalDate(getSafe(csvRecord, TRIP_DATE));
        trip.setDate(date != null ? date : java.time.LocalDate.now());

        var dateEnd = CSVConvert.toLocalDate(getSafe(csvRecord, TRIP_DATE_END));
        trip.setDateEnd(dateEnd != null ? dateEnd : trip.getDate());

        var start = CSVConvert.toLocalTime(getSafe(csvRecord, TRIP_TIME_START));
        trip.setTimeStart(start != null ? start : java.time.LocalTime.now());

        var end = CSVConvert.toLocalTime(getSafe(csvRecord, TRIP_TIME_END));
        trip.setTimeEnd(end != null ? end : java.time.LocalTime.now());

        trip.setRouteTarget(Objects.requireNonNullElse(getSafe(csvRecord, TRIP_ROUTE_TARGET), ""));
        trip.setPurpose(Objects.requireNonNullElse(getSafe(csvRecord, TRIP_PURPOSE), ""));
        trip.setCompaniesVisited(getSafe(csvRecord, TRIP_COMPANIES_VISITED));
        trip.setDriver(getSafe(csvRecord, TRIP_DRIVER));
        trip.setOccupants(CSVConvert.toInteger(getSafe(csvRecord, TRIP_OCCUPANTS)));
        trip.setCargo(getSafe(csvRecord, TRIP_CARGO));
        trip.setKmStart(Objects.requireNonNullElse(CSVConvert.toInteger(getSafe(csvRecord, TRIP_KM_START)), 0));
        trip.setKmEnd(Objects.requireNonNullElse(CSVConvert.toInteger(getSafe(csvRecord, TRIP_KM_END)), 0));
        trip.setKmBusiness(Objects.requireNonNullElse(CSVConvert.toInteger(getSafe(csvRecord, TRIP_KM_BUSINESS)), 0));
        trip.setKmPrivate(Objects.requireNonNullElse(CSVConvert.toInteger(getSafe(csvRecord, TRIP_KM_PRIVATE)), 0));
        trip.setKmHomeWork(Objects.requireNonNullElse(CSVConvert.toInteger(getSafe(csvRecord, TRIP_KM_HOME_WORK)), 0));
        trip.setFuelLiters(CSVConvert.toDouble(getSafe(csvRecord, TRIP_FUEL_LITERS)));
        trip.setFuelCost(CSVConvert.toDouble(getSafe(csvRecord, TRIP_FUEL_COST)));
        trip.setOtherCostsDescription(getSafe(csvRecord, TRIP_OTHER_COSTS_DESCRIPTION));
        trip.setOtherCostsAmount(CSVConvert.toDouble(getSafe(csvRecord, TRIP_OTHER_COSTS_AMOUNT)));

        var created = CSVConvert.toLocalDateTime(getSafe(csvRecord, TRIP_CREATED_AT));
        trip.setCreatedAt(created != null ? created : java.time.LocalDateTime.now());

        var updated = CSVConvert.toLocalDateTime(getSafe(csvRecord, TRIP_UPDATED_AT));
        trip.setUpdatedAt(updated != null ? updated : java.time.LocalDateTime.now());

        return trip;
    }

    private void importTripPrefabs(AutuManduDatabase db, CSVFormat format) throws IOException {
        doImport(TABLE_TRIP_PREFAB + FILE_EXTENSION, format, csvRecord -> {
            Long id = CSVConvert.toLong(getSafe(csvRecord, COLUMN_ID));
            if (id != null) {
                var prefab = new org.juanro.autumandu.model.entity.TripPrefab();
                prefab.setId(id);
                prefab.setCarId(Objects.requireNonNullElse(CSVConvert.toLong(getSafe(csvRecord, TRIP_PREFAB_CAR_ID)), 0L));
                prefab.setType(Objects.requireNonNullElse(getSafe(csvRecord, TRIP_PREFAB_TYPE), "route"));
                prefab.setValue(Objects.requireNonNullElse(getSafe(csvRecord, TRIP_PREFAB_VALUE), ""));
                prefab.setUsageCount(Objects.requireNonNullElse(CSVConvert.toInteger(getSafe(csvRecord, TRIP_PREFAB_USAGE_COUNT)), 1));
                db.getTripPrefabDao().insert(prefab);
            }
        });
    }

    @FunctionalInterface
    private interface ExportAction {
        void export(CSVPrinter csv) throws IOException;
    }

    @FunctionalInterface
    private interface ImportAction {
        void importRecord(CSVRecord csvRecord) throws IOException;
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
        DocumentFile dir = getExportDir();
        if (dir == null) return;
        var file = dir.findFile(fileName);
        if (file == null) return;

        try (var in = context.getContentResolver().openInputStream(file.getUri())) {
            if (in == null) throw new IOException("Could not open input stream for " + fileName);
            try (var reader = new InputStreamReader(in);
                 var parser = CSVParser.parse(reader, format)) {
                for (var csvRecord : parser) {
                    action.importRecord(csvRecord);
                }
            }
        }
    }
}
