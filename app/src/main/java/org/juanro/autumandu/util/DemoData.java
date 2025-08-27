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
import android.net.Uri;
import android.view.Menu;
import android.view.MenuItem;

import org.joda.time.DateTime;

import java.util.Date;

import org.juanro.autumandu.Application;
import org.juanro.autumandu.provider.car.CarColumns;
import org.juanro.autumandu.provider.car.CarContentValues;
import org.juanro.autumandu.provider.fueltype.FuelTypeContentValues;
import org.juanro.autumandu.provider.othercost.OtherCostContentValues;
import org.juanro.autumandu.provider.othercost.RecurrenceInterval;
import org.juanro.autumandu.provider.refueling.RefuelingContentValues;
import org.juanro.autumandu.provider.station.StationContentValues;
import org.juanro.autumandu.provider.tirelist.TireListContentValues;

public class DemoData {
    private static final CharSequence MENU_TITLE_CREATE = "Create demo data";
    private static final CharSequence MENU_TITLE_REMOVE = "Remove demo data";

    public static void createMenuItem(Menu menu) {
        menu.add(MENU_TITLE_CREATE);
        menu.add(MENU_TITLE_REMOVE);
    }

    public static boolean onOptionsItemSelected(MenuItem item) {
        if (item.getTitle().equals(MENU_TITLE_CREATE)) {
            addDemoData();
            return true;
        } else if (item.getTitle().equals(MENU_TITLE_REMOVE)) {
            removeDemoData();
            return true;
        }

        return false;
    }

    public static void addDemoData() {
        Context context = Application.getContext();

        long super95 = createFuelType(context, "Super 95", "Benzin");
        long superE10 = createFuelType(context, "Super E10", "Benzin");
        long lpg = createFuelType(context, "LPG", "Gas");

        long stationId =  createStation(context, "Iberdoex");

        // Fiat Punto

        long punto = createCar(context, "Fiat Punto", Color.BLUE, "Fiat", "Punto", 2023, "0000ABC", new Date());

        int puntoCount = 50;
        DateTime puntoDate = DateTime.now().minusMonths(puntoCount / 2).withSecondOfMinute(0).withMillisOfSecond(0);
        int puntoMileage = 15000;

        createOtherCost(context, "Rechtes Abblendlicht", puntoDate.plusDays(randInt(50, 100)), null,
                121009, 10, RecurrenceInterval.ONCE, 1, "", punto);
        createOtherCost(context, "Steuern", puntoDate, null, -1, 210, RecurrenceInterval.YEAR, 1,
                "", punto);

        createTire(context, puntoDate, null, 50, 4, "Insa Turbo", "Eco Evolution", "", punto);

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
            long fuelType = super95;
            if (randBooleanTrueInOneOutOf(4)) {
                fuelType = superE10;
            }

            if (!randBooleanTrueInOneOutOf(15)) {
                createRefueling(context, puntoDate, puntoMileage, volume, price, partial, "",
                        fuelType, stationId, punto);
            }
        }

        // Opel Astra

        long astra = createCar(context, "Opel Astra", Color.RED, "Opel", "Astra", 2010, "9999XYZ", new Date());

        int astraCountSuper95 = 30;
        int astraCountLpg = 30;
        DateTime astraDateSuper95 = DateTime.now().minusMonths(astraCountSuper95 / 3).withSecondOfMinute(0).withMillisOfSecond(0);
        DateTime astraDateLpg = DateTime.now().minusMonths(astraCountSuper95 / 3).withSecondOfMinute(0).withMillisOfSecond(0);
        int astraMileageSuper95 = 120000;
        int astraMileageLpg = 120000;

        createOtherCost(context, "Steuern", astraDateSuper95, null, -1, 250, RecurrenceInterval.YEAR, 1,
                "", astra);
        createOtherCost(context, "Versicherung", astraDateSuper95, null, -1, 40, RecurrenceInterval.MONTH,
                1, "", astra);

        createTire(context, astraDateSuper95, null, 50, 4, "Insa Turbo", "All Season 4", "", astra);

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
                createRefueling(context, astraDateSuper95, astraMileageSuper95, volume, price, partial, "",
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
                createRefueling(context, astraDateLpg, astraMileageLpg, volume, price, partial, "",
                        lpg, stationId, astra);
            }
        }
    }

    public static void removeDemoData() {
        Context context = Application.getContext();

        context.getContentResolver().delete(CarColumns.CONTENT_URI, null, null);
    }

    private static long createCar(Context context, String name, int color, String make, String model, int year, String licensePlate, Date buyingDate) {
        return getIdFromUri(new CarContentValues()
                .putName(name)
                .putColor(color)
                .putInitialMileage(0)
                .putBuyingPrice(0)
                .putNumTires(4)
                /*.putMake(make)
                .putModel(model)
                .putYear(year)
                .putLicensePlate(licensePlate)
                .putBuyingDate(buyingDate)*/
                .insert(context.getContentResolver()));
    }

    private static long createFuelType(Context context, String name, String category) {
        return getIdFromUri(new FuelTypeContentValues()
                .putName(name)
                .putCategory(category)
                .insert(context.getContentResolver()));
    }

    private static long createStation(Context context, String name) {
        return getIdFromUri(new StationContentValues()
            .putName(name)
            .insert(context.getContentResolver()));
    }

    private static long createRefueling(Context context, DateTime date, int mileage, float volume,
                                        float price, boolean partial, String note, long fuelTypeId,
                                        long stationId, long carId) {
        // Has a chance of 10% to fail. This emulates, that the user forgot to enter the refueling.
        if (Math.random() > 0.95) {
            return -1;
        }

        return getIdFromUri(new RefuelingContentValues()
                .putDate(date.toDate())
                .putMileage(mileage)
                .putVolume(volume)
                .putPrice(price)
                .putPartial(partial)
                .putNote(note)
                .putFuelTypeId(fuelTypeId)
                .putStationId(stationId)
                .putCarId(carId)
                .insert(context.getContentResolver()));
    }

    private static long createOtherCost(Context context, String title, DateTime date,
                                        DateTime endDate, int mileage, float price,
                                        RecurrenceInterval recurrenceInterval,
                                        int recurrenceMultiplier, String note, long carId) {
        return getIdFromUri(new OtherCostContentValues()
                .putTitle(title)
                .putDate(date.toDate())
                .putEndDate(endDate == null ? null : endDate.toDate())
                .putMileage(mileage)
                .putPrice(price)
                .putRecurrenceInterval(recurrenceInterval)
                .putRecurrenceMultiplier(recurrenceMultiplier)
                .putNote(note)
                .putCarId(carId)
                .insert(context.getContentResolver()));
    }

    private static long createTire(Context context, DateTime buyDate, DateTime trashDate, float price, int quantity, String manufacturer, String model, String note, long carId) {
        return getIdFromUri(new TireListContentValues()
            .putBuyDate(buyDate.toDate())
            .putTrashDate(trashDate == null ? null : trashDate.toDate())
            .putPrice(price)
            .putQuantity(quantity)
            .putManufacturer(manufacturer)
            .putModel(model)
            .putNote(note)
            .putCarId(carId)
            .insert(context.getContentResolver()));
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

    private static long getIdFromUri(Uri uri) {
        String lastPart = uri.getLastPathSegment();
        return Long.parseLong(lastPart);
    }
}
