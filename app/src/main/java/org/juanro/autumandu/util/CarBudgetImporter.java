/*
 * Copyright 2026 Juanro49
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

package org.juanro.autumandu.util;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import org.juanro.autumandu.model.AutuManduDatabase;
import org.juanro.autumandu.model.entity.Car;
import org.juanro.autumandu.model.entity.FuelCategory;
import org.juanro.autumandu.model.entity.FuelType;
import org.juanro.autumandu.model.entity.OtherCost;
import org.juanro.autumandu.model.entity.Refueling;
import org.juanro.autumandu.model.entity.Station;
import org.juanro.autumandu.model.entity.TireList;
import org.juanro.autumandu.model.entity.TireUsage;

import java.io.File;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Utility class for importing CarBudget databases (.cbg).
 */
public class CarBudgetImporter {

    private static final String TAG = "CarBudgetImporter";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SS");

    private final Context context;
    private final AutuManduDatabase localDb;

    public CarBudgetImporter(Context context) {
        this.context = context;
        this.localDb = AutuManduDatabase.getInstance(context);
    }

    /**
     * Converts a CarBudget date string to a Date object.
     *
     * @param dateString the date string from CarBudget database.
     * @return the parsed Date, or current date if parsing fails.
     */
    public static Date parseDate(String dateString) {
        if (dateString == null || dateString.isEmpty()) {
            return new Date();
        }
        try {
            LocalDateTime localDateTime = LocalDateTime.parse(dateString, DATE_FORMATTER);
            return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse date: " + dateString, e);
            return new Date();
        }
    }

    /**
     * Reads metadata from the CarBudget table.
     *
     * @param db the CarBudget database.
     * @return a map of key-value pairs.
     */
    public Map<String, String> getMetadata(SQLiteDatabase db) {
        Map<String, String> metadata = new HashMap<>();
        try (Cursor cursor = db.query("CarBudget", new String[]{"id", "value"}, null, null, null, null, null)) {
            while (cursor.moveToNext()) {
                String key = cursor.getString(0);
                String value = cursor.getString(1);
                if (key != null) {
                    metadata.put(key, Objects.requireNonNullElse(value, ""));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to read CarBudget metadata", e);
        }
        return metadata;
    }

    public String getCarDescription(Map<String, String> metadata) {
        String make = Objects.requireNonNullElse(metadata.get("make"), "");
        String model = Objects.requireNonNullElse(metadata.get("model"), "");
        String plate = metadata.get("licensePlate");
        StringBuilder desc = new StringBuilder(make).append(" ").append(model);
        if (plate != null && !plate.isEmpty()) {
            desc.append(" (").append(plate).append(")");
        }
        return desc.toString().trim();
    }

    private int getInitialMileage(SQLiteDatabase db) {
        try (Cursor cursor = db.rawQuery("SELECT MIN(distance) FROM Event WHERE distance > 0", null)) {
            if (cursor.moveToFirst()) {
                return cursor.getInt(0);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to determine initial mileage", e);
        }
        return 0;
    }

    private long createCarFromMetadata(Map<String, String> metadata, SQLiteDatabase externalDb) {
        String name = getCarDescription(metadata);
        String buyingPriceStr = Objects.requireNonNullElse(metadata.get("buyingprice"), "0");
        String nbTireStr = Objects.requireNonNullElse(metadata.get("nbtire"), "4");

        double buyingPrice = Double.parseDouble(buyingPriceStr);
        int numTires = Integer.parseInt(nbTireStr);

        Car car = new Car();
        car.setName(name);
        car.setBuyingPrice(buyingPrice);
        car.setNumTires(numTires);
        car.setInitialMileage(getInitialMileage(externalDb));
        car.setColor(-13092808); // Default color

        long[] ids = localDb.getCarDao().insert(car);
        return ids[0];
    }

    private long getRowCount(SQLiteDatabase db, String tableName) {
        try (Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + tableName, null)) {
            if (cursor.moveToFirst()) {
                return cursor.getLong(0);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to get row count for " + tableName, e);
        }
        return 0;
    }

    /**
     * Imports a .cbg file into the application.
     *
     * @param cbgFile  the CarBudget database file.
     * @param targetId the target car ID, or -1 to create a new one.
     * @return an ImportResult object with counts and status.
     */
    public CarBudgetImportResult importDatabase(File cbgFile, long targetId) {
        CarBudgetImportResult result = new CarBudgetImportResult();
        try (SQLiteDatabase externalDb = SQLiteDatabase.openDatabase(cbgFile.getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY)) {
            Log.i(TAG, "Starting import from: " + cbgFile.getName());

            long sourceTankCount = getRowCount(externalDb, "TankList");
            long sourceCostCount = getRowCount(externalDb, "CostList");
            long sourceTireCount = getRowCount(externalDb, "TireList");

            Map<String, String> metadata = getMetadata(externalDb);
            final long carId = (targetId == -1) ? createCarFromMetadata(metadata, externalDb) : targetId;

            // 1. Import Stations
            Map<Long, Long> stationIdMap = importStations(externalDb);

            // 2. Import Fuel Types
            Map<Long, Long> fuelTypeIdMap = importFuelTypes(externalDb);

            // 3. Import Refuelings
            result.setRefuelingCount(importRefuelings(externalDb, carId, stationIdMap, fuelTypeIdMap));

            // 4. Import Other Costs
            result.setOtherCostCount(importOtherCosts(externalDb, carId));

            // 5. Import Tires
            result.setTireCount(importTires(externalDb, carId));

            Log.i(TAG, String.format(Locale.US, "Import finished. Tank: %d/%d, Cost: %d/%d, Tire: %d/%d",
                    result.getRefuelingCount(), sourceTankCount,
                    result.getOtherCostCount(), sourceCostCount,
                    result.getTireCount(), sourceTireCount));

            result.setSuccess(true);
        } catch (Exception e) {
            Log.e(TAG, "Import failed", e);
            result.setErrorMessage(e.getMessage());
            result.setSuccess(false);
        }
        return result;
    }

    private Map<Long, Long> importStations(SQLiteDatabase externalDb) {
        Map<Long, Long> map = new HashMap<>();
        try (Cursor cursor = externalDb.query("StationList", new String[]{"id", "name"}, null, null, null, null, null)) {
            while (cursor.moveToNext()) {
                long oldId = cursor.getLong(0);
                String name = cursor.getString(1);

                Station existing = localDb.getStationDao().getByName(name);
                if (existing != null) {
                    map.put(oldId, existing.getId());
                } else {
                    Station station = new Station(name);
                    long[] newIds = localDb.getStationDao().insert(station);
                    map.put(oldId, newIds[0]);
                }
            }
        }
        return map;
    }

    private Map<Long, Long> importFuelTypes(SQLiteDatabase externalDb) {
        Map<Long, Long> map = new HashMap<>();
        try (Cursor cursor = externalDb.query("FueltypeList", new String[]{"id", "name"}, null, null, null, null, null)) {
            while (cursor.moveToNext()) {
                long oldId = cursor.getLong(0);
                String name = cursor.getString(1);

                FuelType existing = localDb.getFuelTypeDao().getByName(name);
                if (existing != null) {
                    map.put(oldId, existing.getId());
                } else {
                    FuelType fuelType = new FuelType(name, FuelCategory.GASOLINE.getKey());
                    long[] newIds = localDb.getFuelTypeDao().insert(fuelType);
                    map.put(oldId, newIds[0]);
                }
            }
        }
        return map;
    }

    private int importRefuelings(SQLiteDatabase externalDb, long carId, Map<Long, Long> stationIdMap, Map<Long, Long> fuelTypeIdMap) {
        int refuelingCount = 0;
        String query = "SELECT E.date, E.distance, T.quantity, T.price, T.\"full\", T.note, T.station, T.fueltype " +
                "FROM TankList T JOIN Event E ON T.event = E.id";
        try (Cursor cursor = externalDb.rawQuery(query, null)) {
            while (cursor.moveToNext()) {
                Refueling refueling = new Refueling();
                refueling.setCarId(carId);
                refueling.setDate(parseDate(cursor.getString(0)));
                refueling.setMileage(cursor.getInt(1));
                refueling.setVolume(cursor.getFloat(2));
                refueling.setPrice(cursor.getFloat(3));
                refueling.setPartial(cursor.getInt(4) == 0);
                refueling.setNote(cursor.getString(5) != null ? cursor.getString(5) : "");

                long oldStationId = cursor.getLong(6);
                refueling.setStationId(Objects.requireNonNullElse(stationIdMap.get(oldStationId), 1L));

                long oldFuelTypeId = cursor.getLong(7);
                refueling.setFuelTypeId(Objects.requireNonNullElse(fuelTypeIdMap.get(oldFuelTypeId), 1L));

                localDb.getRefuelingDao().insert(refueling);
                refuelingCount++;
            }
        }
        return refuelingCount;
    }

    private int importOtherCosts(SQLiteDatabase externalDb, long carId) {
        int otherCostCount = 0;
        String query = "SELECT E.date, E.distance, C.cost, C.\"desc\", CL.name " +
                "FROM CostList C " +
                "JOIN Event E ON C.event = E.id " +
                "JOIN CosttypeList CL ON C.costtype = CL.id";
        try (Cursor cursor = externalDb.rawQuery(query, null)) {
            while (cursor.moveToNext()) {
                OtherCost cost = new OtherCost();
                cost.setCarId(carId);
                cost.setDate(parseDate(cursor.getString(0)));
                cost.setMileage(cursor.getInt(1));
                cost.setPrice(cursor.getFloat(2));
                cost.setNote(cursor.getString(3) != null ? cursor.getString(3) : "");
                cost.setTitle(cursor.getString(4));

                localDb.getOtherCostDao().insert(cost);
                otherCostCount++;
            }
        }
        return otherCostCount;
    }

    private int importTires(SQLiteDatabase externalDb, long carId) {
        int importedTireCount = 0;
        Map<Long, Long> tireIdMap = new HashMap<>();
        try (Cursor cursor = externalDb.query("TireList", new String[]{"id", "buydate", "trashdate", "price", "quantity", "name", "manufacturer", "model"}, null, null, null, null, null)) {
            while (cursor.moveToNext()) {
                long oldId = cursor.getLong(0);
                TireList tire = new TireList();
                tire.setCarId(carId);
                tire.setBuyDate(parseDate(cursor.getString(1)));
                String trashDateStr = cursor.getString(2);
                if (trashDateStr != null && !trashDateStr.isEmpty()) {
                    tire.setTrashDate(parseDate(trashDateStr));
                }
                tire.setPrice(cursor.getFloat(3));
                tire.setQuantity(cursor.getInt(4));
                tire.setManufacturer(cursor.getString(6));
                tire.setModel(cursor.getString(7));
                tire.setNote(cursor.getString(5) != null ? cursor.getString(5) : "");

                long[] newIds = localDb.getTireDao().insert(tire);
                tireIdMap.put(oldId, newIds[0]);
                importedTireCount++;
            }
        }

        // Import Tire Usage
        String usageQuery = "SELECT tire, event_mount, event_umount FROM TireUsage";
        try (Cursor cursor = externalDb.rawQuery(usageQuery, null)) {
            while (cursor.moveToNext()) {
                long oldTireId = cursor.getLong(0);
                long mountEventId = cursor.getLong(1);
                long umountEventId = cursor.getLong(2);

                if (tireIdMap.containsKey(oldTireId)) {
                    TireUsage usage = new TireUsage();
                    usage.setTireId(Objects.requireNonNull(tireIdMap.get(oldTireId)));

                    // Lookup mount event
                    fillUsageEvent(externalDb, mountEventId, usage, true);
                    // Lookup umount event
                    if (umountEventId != 0) {
                        fillUsageEvent(externalDb, umountEventId, usage, false);
                    }

                    localDb.getTireDao().insert(usage);
                }
            }
        }
        return importedTireCount;
    }

    private void fillUsageEvent(SQLiteDatabase externalDb, long eventId, TireUsage usage, boolean mount) {
        try (Cursor cursor = externalDb.query("Event", new String[]{"date", "distance"}, "id = ?", new String[]{eventId + ""}, null, null, null)) {
            if (cursor.moveToFirst()) {
                Date date = parseDate(cursor.getString(0));
                int distance = cursor.getInt(1);
                if (mount) {
                    usage.setDateMount(date);
                    usage.setDistanceMount(distance);
                } else {
                    usage.setDateUmount(date);
                    usage.setDistanceUmount(distance);
                }
            }
        }
    }

    public Context getContext() {
        return context;
    }
}
