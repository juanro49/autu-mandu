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

package org.juanro.autumandu.util;

import android.content.Context;
import android.graphics.Color;
import android.view.Menu;
import android.view.MenuItem;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.juanro.autumandu.AutuManduApplication;
import org.juanro.autumandu.model.AutuManduDatabase;
import org.juanro.autumandu.model.entity.Car;
import org.juanro.autumandu.model.entity.FuelType;
import org.juanro.autumandu.model.entity.OtherCost;
import org.juanro.autumandu.model.entity.Refueling;
import org.juanro.autumandu.model.entity.Station;
import org.juanro.autumandu.model.entity.TireList;
import org.juanro.autumandu.model.entity.Trip;
import org.juanro.autumandu.model.entity.helper.RecurrenceInterval;

/**
 * Utility class to populate the database with demo data.
 */
@SuppressWarnings("SameParameterValue")
public final class DemoData {
    private static final String MENU_TITLE_CREATE = "Create demo data";
    private static final String MENU_TITLE_REMOVE = "Remove demo data";
    private static final Executor DB_EXECUTOR = Executors.newSingleThreadExecutor();
    private static final SecureRandom RANDOM = new SecureRandom();

    private DemoData() {
        // Utility class
    }

    public static void createMenuItem(Menu menu) {
        menu.add(MENU_TITLE_CREATE);
        menu.add(MENU_TITLE_REMOVE);
    }

    public static boolean onOptionsItemSelected(MenuItem item) {
        CharSequence title = item.getTitle();
        if (title == null) return false;

        if (MENU_TITLE_CREATE.contentEquals(title)) {
            addDemoData();
            return true;
        } else if (MENU_TITLE_REMOVE.contentEquals(title)) {
            removeDemoData();
            return true;
        }

        return false;
    }

    public static void addDemoData() {
        DB_EXECUTOR.execute(() -> {
            Context context = AutuManduApplication.getContext();
            addDemoDataSync(context);
        });
    }

    public static void addDemoDataSync(Context context) {
        AutuManduDatabase db = AutuManduDatabase.getInstance(context);

        long super95 = createFuelType(db, "Super 95", "Benzin");
        long superE10 = createFuelType(db, "Super E10", "Benzin");
        long lpg = createFuelType(db, "LPG", "Gas");
        long stationId = createStation(db, "Iberdoex");

        addPuntoDemoData(db, super95, superE10, stationId);
        addAstraDemoData(db, super95, lpg, stationId);
    }

    private static void addPuntoDemoData(AutuManduDatabase db, long super95, long superE10, long stationId) {
        long punto = createCar(db, "Fiat Punto", Color.BLUE);
        int puntoCount = 50;
        ZonedDateTime puntoDate = ZonedDateTime.now().minusMonths(puntoCount / 2).withSecond(0).withNano(0);
        int puntoMileage = 15000;

        createOtherCost(db, new OtherCostConfig("Rechtes Abblendlicht", puntoDate.plusDays(randInt(50, 100)), null,
                121009, 10, RecurrenceInterval.ONCE, 1, "", punto));
        createOtherCost(db, new OtherCostConfig("Steuern", puntoDate, null, -1, 210, RecurrenceInterval.YEAR, 1,
                "", punto));

        createTire(db, new TireConfig(puntoDate, null, 50, 4, "Insa Turbo", "Eco Evolution", "", punto));

        for (int i = 0; i < puntoCount; i++) {
            puntoDate = puntoDate.plusDays(randInt(12, 18));
            puntoMileage += randInt(570, 620);
            float volume = randFloat(43, 48);
            boolean partial = false;
            if (randBooleanTrueInOneOutOf(6)) {
                volume -= randFloat(20, 40);
                partial = true;
            }

            float price = volume * randFloat(140, 160) / 100;
            long fuelType = randBooleanTrueInOneOutOf(4) ? superE10 : super95;

            if (!randBooleanTrueInOneOutOf(15)) {
                createRefueling(db, new RefuelingConfig(puntoDate, puntoMileage, volume, price, partial, "",
                        fuelType, stationId, punto));
            }

            // Add some trips around refuelings
            if (i % 5 == 0) {
                int tripDist = randInt(10, 100);
                createTrip(db, new TripConfig(
                        puntoDate.toLocalDate(),
                        puntoDate.toLocalDate(),
                        puntoDate.toLocalTime(),
                        puntoDate.toLocalTime().plusMinutes(randInt(15, 120)),
                        "Office",
                        "Work",
                        puntoMileage - tripDist,
                        puntoMileage,
                        tripDist, 0, 0,
                        punto
                ));
            }
        }
    }

    private static void addAstraDemoData(AutuManduDatabase db, long super95, long lpg, long stationId) {
        long astra = createCar(db, "Opel Astra", Color.RED);
        int astraCount = 30;
        ZonedDateTime astraDate = ZonedDateTime.now().minusMonths(astraCount / 3).withSecond(0).withNano(0);
        int astraMileage = 120000;

        createOtherCost(db, new OtherCostConfig("Steuern", astraDate, null, -1, 250, RecurrenceInterval.YEAR, 1,
                "", astra));
        createOtherCost(db, new OtherCostConfig("Versicherung", astraDate, null, -1, 40, RecurrenceInterval.MONTH,
                1, "", astra));

        createTire(db, new TireConfig(astraDate, null, 50, 4, "Insa Turbo", "All Season 4", "", astra));

        AstraRefuelingConfig config = new AstraRefuelingConfig(astra, astraDate, astraMileage, astraCount, super95, lpg, stationId);
        addRefuelingsForAstra(db, config);

        // Add some trips for Astra
        ZonedDateTime tripDate = astraDate;
        int tripMileage = astraMileage;
        for (int i = 0; i < 20; i++) {
            tripDate = tripDate.plusDays(randInt(1, 3));
            int dist = randInt(20, 150);
            createTrip(db, new TripConfig(
                    tripDate.toLocalDate(),
                    tripDate.toLocalDate(),
                    tripDate.toLocalTime(),
                    tripDate.toLocalTime().plusMinutes(randInt(30, 180)),
                    "Trip " + (i + 1),
                    "Purpose " + (i % 3),
                    tripMileage,
                    tripMileage + dist,
                    dist, 0, 0,
                    astra
            ));
            tripMileage += dist + randInt(0, 10);
        }
    }

    private static void addRefuelingsForAstra(AutuManduDatabase db, AstraRefuelingConfig config) {
        ZonedDateTime date95 = config.startDate();
        int mileage95 = config.startMileage();
        for (int i = 0; i < config.count(); i++) {
            date95 = date95.plusDays(randInt(8, 13));
            mileage95 += randInt(570, 620);
            float volume = randFloat(55, 60);
            boolean partial = false;
            if (randBooleanTrueInOneOutOf(6)) {
                volume -= randFloat(20, 40);
                partial = true;
            }

            float price = volume * randFloat(140, 160) / 100;
            if (!randBooleanTrueInOneOutOf(15)) {
                createRefueling(db, new RefuelingConfig(date95, mileage95, volume, price, partial, "",
                        config.super95(), config.stationId(), config.carId()));
            }
        }

        ZonedDateTime dateLpg = config.startDate();
        int mileageLpg = config.startMileage();
        for (int i = 0; i < config.count(); i++) {
            dateLpg = dateLpg.plusDays(randInt(8, 13));
            mileageLpg += randInt(570, 620);
            float volume = randFloat(25, 30);
            boolean partial = false;
            if (randBooleanTrueInOneOutOf(6)) {
                volume -= randFloat(10, 20);
                partial = true;
            }

            float price = volume * randFloat(85, 105) / 100;
            if (!randBooleanTrueInOneOutOf(15)) {
                createRefueling(db, new RefuelingConfig(dateLpg, mileageLpg, volume, price, partial, "",
                        config.lpg(), config.stationId(), config.carId()));
            }
        }
    }

    private record AstraRefuelingConfig(long carId, ZonedDateTime startDate, int startMileage, int count, long super95, long lpg, long stationId) {}

    public static void removeDemoData() {
        DB_EXECUTOR.execute(() -> {
            AutuManduDatabase db = AutuManduDatabase.getInstance(AutuManduApplication.getContext());
            db.runInTransaction(() -> {
                for (Car car : db.getCarDao().getAll()) {
                    db.getCarDao().delete(car);
                }
            });
        });
    }

    private static long createCar(AutuManduDatabase db, String name, int color) {
        Car car = new Car();
        car.setName(name);
        car.setColor(color);
        car.setInitialMileage(0);
        car.setBuyingPrice(0);
        car.setNumTires(4);
        return db.getCarDao().insert(car)[0];
    }

    private static long createFuelType(AutuManduDatabase db, String name, String category) {
        FuelType fuelType = new FuelType();
        fuelType.setName(name);
        fuelType.setCategory(category);
        return db.getFuelTypeDao().insert(fuelType)[0];
    }

    private static long createStation(AutuManduDatabase db, String name) {
        Station station = new Station();
        station.setName(name);
        return db.getStationDao().insert(station)[0];
    }

    private static void createRefueling(AutuManduDatabase db, RefuelingConfig config) {
        if (RANDOM.nextDouble() > 0.95) return;

        Refueling refueling = new Refueling();
        refueling.setDate(Date.from(config.date().toInstant()));
        refueling.setMileage(config.mileage());
        refueling.setVolume(config.volume());
        refueling.setPrice(config.price());
        refueling.setPartial(config.partial());
        refueling.setNote(config.note());
        refueling.setFuelTypeId(config.fuelTypeId());
        refueling.setStationId(config.stationId());
        refueling.setCarId(config.carId());

        db.getRefuelingDao().insert(refueling);
    }

    private record RefuelingConfig(ZonedDateTime date, int mileage, float volume, float price, boolean partial, String note, long fuelTypeId, long stationId, long carId) {}

    private static void createOtherCost(AutuManduDatabase db, OtherCostConfig config) {
        OtherCost otherCost = new OtherCost();
        otherCost.setTitle(config.title());
        otherCost.setDate(Date.from(config.date().toInstant()));
        otherCost.setEndDate(config.endDate() == null ? null : Date.from(config.endDate().toInstant()));
        otherCost.setMileage(config.mileage());
        otherCost.setPrice(config.price());
        otherCost.setRecurrenceInterval(config.recurrenceInterval());
        otherCost.setRecurrenceMultiplier(config.recurrenceMultiplier());
        otherCost.setNote(config.note());
        otherCost.setCarId(config.carId());

        db.getOtherCostDao().insert(otherCost);
    }

    private record OtherCostConfig(String title, ZonedDateTime date, ZonedDateTime endDate, int mileage, float price, RecurrenceInterval recurrenceInterval, int recurrenceMultiplier, String note, long carId) {}

    private static void createTire(AutuManduDatabase db, TireConfig config) {
        TireList tireList = new TireList();
        tireList.setBuyDate(Date.from(config.buyDate().toInstant()));
        tireList.setTrashDate(config.trashDate() == null ? null : Date.from(config.trashDate().toInstant()));
        tireList.setPrice(config.price());
        tireList.setQuantity(config.quantity());
        tireList.setManufacturer(config.manufacturer());
        tireList.setModel(config.model());
        tireList.setNote(config.note());
        tireList.setCarId(config.carId());

        db.getTireDao().insert(tireList);
    }

    private record TireConfig(ZonedDateTime buyDate, ZonedDateTime trashDate, float price, int quantity, String manufacturer, String model, String note, long carId) {}

    private static void createTrip(AutuManduDatabase db, TripConfig config) {
        Trip trip = new Trip();
        trip.setCarId(config.carId());
        trip.setDate(config.date());
        trip.setDateEnd(config.dateEnd());
        trip.setTimeStart(config.timeStart());
        trip.setTimeEnd(config.timeEnd());
        trip.setRouteTarget(config.route());
        trip.setPurpose(config.purpose());
        trip.setKmStart(config.kmStart());
        trip.setKmEnd(config.kmEnd());
        trip.setKmBusiness(config.kmBusiness());
        trip.setKmPrivate(config.kmPrivate());
        trip.setKmHomeWork(config.kmHomeWork());
        trip.setCreatedAt(LocalDateTime.now());
        trip.setUpdatedAt(LocalDateTime.now());

        db.getTripDao().insert(trip);
    }

    private record TripConfig(LocalDate date, LocalDate dateEnd, LocalTime timeStart, LocalTime timeEnd, String route, String purpose, int kmStart, int kmEnd, int kmBusiness, int kmPrivate, int kmHomeWork, long carId) {}

    private static int randInt(int min, int max) {
        return min + RANDOM.nextInt((max - min) + 1);
    }

    private static float randFloat(int min, int max) {
        return (float) randInt(min * 10, max * 10) / 10;
    }

    private static boolean randBooleanTrueInOneOutOf(int x) {
        return randInt(0, x) == 0;
    }
}
