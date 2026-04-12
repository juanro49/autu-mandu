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

import org.joda.time.DateTime;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.juanro.autumandu.Application;
import org.juanro.autumandu.model.AutuManduDatabase;
import org.juanro.autumandu.model.entity.Car;
import org.juanro.autumandu.model.entity.FuelType;
import org.juanro.autumandu.model.entity.OtherCost;
import org.juanro.autumandu.model.entity.Refueling;
import org.juanro.autumandu.model.entity.Station;
import org.juanro.autumandu.model.entity.TireList;
import org.juanro.autumandu.model.entity.helper.RecurrenceInterval;

/**
 * Utility class to populate the database with demo data.
 */
@SuppressWarnings("SameParameterValue")
public final class DemoData {
    private static final String MENU_TITLE_CREATE = "Create demo data";
    private static final String MENU_TITLE_REMOVE = "Remove demo data";
    private static final Executor DB_EXECUTOR = Executors.newSingleThreadExecutor();

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
            Context context = Application.getContext();
            AutuManduDatabase db = AutuManduDatabase.getInstance(context);

            long super95 = createFuelType(db, "Super 95", "Benzin");
            long superE10 = createFuelType(db, "Super E10", "Benzin");
            long lpg = createFuelType(db, "LPG", "Gas");

            long stationId = createStation(db, "Iberdoex");

            // Fiat Punto
            long punto = createCar(db, "Fiat Punto", Color.BLUE);

            int puntoCount = 50;
            DateTime puntoDate = DateTime.now().minusMonths(puntoCount / 2).withSecondOfMinute(0).withMillisOfSecond(0);
            int puntoMileage = 15000;

            createOtherCost(db, "Rechtes Abblendlicht", puntoDate.plusDays(randInt(50, 100)), null,
                    121009, 10, RecurrenceInterval.ONCE, 1, "", punto);
            createOtherCost(db, "Steuern", puntoDate, null, -1, 210, RecurrenceInterval.YEAR, 1,
                    "", punto);

            createTire(db, puntoDate, null, 50, 4, "Insa Turbo", "Eco Evolution", "", punto);

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
                    createRefueling(db, puntoDate, puntoMileage, volume, price, partial, "",
                            fuelType, stationId, punto);
                }
            }

            // Opel Astra
            long astra = createCar(db, "Opel Astra", Color.RED);

            int astraCountSuper95 = 30;
            int astraCountLpg = 30;
            DateTime astraDateSuper95 = DateTime.now().minusMonths(astraCountSuper95 / 3).withSecondOfMinute(0).withMillisOfSecond(0);
            DateTime astraDateLpg = DateTime.now().minusMonths(astraCountSuper95 / 3).withSecondOfMinute(0).withMillisOfSecond(0);
            int astraMileageSuper95 = 120000;
            int astraMileageLpg = 120000;

            createOtherCost(db, "Steuern", astraDateSuper95, null, -1, 250, RecurrenceInterval.YEAR, 1,
                    "", astra);
            createOtherCost(db, "Versicherung", astraDateSuper95, null, -1, 40, RecurrenceInterval.MONTH,
                    1, "", astra);

            createTire(db, astraDateSuper95, null, 50, 4, "Insa Turbo", "All Season 4", "", astra);

            for (int i = 0; i < astraCountSuper95; i++) {
                astraDateSuper95 = astraDateSuper95.plusDays(randInt(8, 13));
                astraMileageSuper95 += randInt(570, 620);
                float volume = randFloat(55, 60);
                boolean partial = false;
                if (randBooleanTrueInOneOutOf(6)) {
                    volume -= randFloat(20, 40);
                    partial = true;
                }

                float price = volume * randFloat(140, 160) / 100;

                if (!randBooleanTrueInOneOutOf(15)) {
                    createRefueling(db, astraDateSuper95, astraMileageSuper95, volume, price, partial, "",
                            super95, stationId, astra);
                }
            }

            for (int i = 0; i < astraCountLpg; i++) {
                astraDateLpg = astraDateLpg.plusDays(randInt(8, 13));
                astraMileageLpg += randInt(570, 620);
                float volume = randFloat(25, 30);
                boolean partial = false;
                if (randBooleanTrueInOneOutOf(6)) {
                    volume -= randFloat(10, 20);
                    partial = true;
                }

                float price = volume * randFloat(85, 105) / 100;

                if (!randBooleanTrueInOneOutOf(15)) {
                    createRefueling(db, astraDateLpg, astraMileageLpg, volume, price, partial, "",
                            lpg, stationId, astra);
                }
            }
        });
    }

    public static void removeDemoData() {
        DB_EXECUTOR.execute(() -> {
            AutuManduDatabase db = AutuManduDatabase.getInstance(Application.getContext());
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

    private static void createRefueling(AutuManduDatabase db, DateTime date, int mileage, float volume,
                                        float price, boolean partial, String note, long fuelTypeId,
                                        long stationId, long carId) {
        if (Math.random() > 0.95) return;

        Refueling refueling = new Refueling();
        refueling.setDate(date.toDate());
        refueling.setMileage(mileage);
        refueling.setVolume(volume);
        refueling.setPrice(price);
        refueling.setPartial(partial);
        refueling.setNote(note);
        refueling.setFuelTypeId(fuelTypeId);
        refueling.setStationId(stationId);
        refueling.setCarId(carId);

        db.getRefuelingDao().insert(refueling);
    }

    private static void createOtherCost(AutuManduDatabase db, String title, DateTime date,
                                        DateTime endDate, int mileage, float price,
                                        RecurrenceInterval recurrenceInterval,
                                        int recurrenceMultiplier, String note, long carId) {
        OtherCost otherCost = new OtherCost();
        otherCost.setTitle(title);
        otherCost.setDate(date.toDate());
        otherCost.setEndDate(endDate == null ? null : endDate.toDate());
        otherCost.setMileage(mileage);
        otherCost.setPrice(price);
        otherCost.setRecurrenceInterval(recurrenceInterval);
        otherCost.setRecurrenceMultiplier(recurrenceMultiplier);
        otherCost.setNote(note);
        otherCost.setCarId(carId);

        db.getOtherCostDao().insert(otherCost);
    }

    private static void createTire(AutuManduDatabase db, DateTime buyDate, DateTime trashDate, float price, int quantity, String manufacturer, String model, String note, long carId) {
        TireList tireList = new TireList();
        tireList.setBuyDate(buyDate.toDate());
        tireList.setTrashDate(trashDate == null ? null : trashDate.toDate());
        tireList.setPrice(price);
        tireList.setQuantity(quantity);
        tireList.setManufacturer(manufacturer);
        tireList.setModel(model);
        tireList.setNote(note);
        tireList.setCarId(carId);

        db.getTireDao().insert(tireList);
    }

    private static int randInt(int min, int max) {
        return min + (int) (Math.random() * ((max - min) + 1));
    }

    private static float randFloat(int min, int max) {
        return (float) randInt(min * 10, max * 10) / 10;
    }

    private static boolean randBooleanTrueInOneOutOf(int x) {
        return randInt(0, x) == 0;
    }
}
