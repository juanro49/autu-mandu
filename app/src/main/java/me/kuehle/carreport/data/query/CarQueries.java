/*
 * Copyright 2015 Jan Kühle
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package me.kuehle.carreport.data.query;

import android.content.Context;

import java.util.HashSet;
import java.util.Set;

import me.kuehle.carreport.provider.car.CarColumns;
import me.kuehle.carreport.provider.car.CarCursor;
import me.kuehle.carreport.provider.car.CarSelection;
import me.kuehle.carreport.provider.fueltype.FuelTypeColumns;
import me.kuehle.carreport.provider.othercost.OtherCostColumns;
import me.kuehle.carreport.provider.othercost.OtherCostCursor;
import me.kuehle.carreport.provider.othercost.OtherCostSelection;
import me.kuehle.carreport.provider.refueling.RefuelingColumns;
import me.kuehle.carreport.provider.refueling.RefuelingCursor;
import me.kuehle.carreport.provider.refueling.RefuelingSelection;

public class CarQueries {
    public static int getCount(Context context) {
        CarCursor cursor = new CarSelection().query(context.getContentResolver(), new String[]{CarColumns._ID});
        return cursor.getCount();
    }

    public static String[] getUsedFuelTypeCategories(Context context, long carId) {
        Set<String> categories = new HashSet<>();

        RefuelingCursor refueling = new RefuelingSelection().carId(carId).query(context.getContentResolver(),
                new String[]{FuelTypeColumns.CATEGORY},
                FuelTypeColumns.CATEGORY + " COLLATE UNICODE ASC");
        while (refueling.moveToNext()) {
            categories.add(refueling.getFuelTypeCategory());
        }

        return categories.toArray(new String[categories.size()]);
    }

    public static int getLatestMileage(Context context, long carId) {
        int latestRefuelingMileage = 0;
        int latestOtherCostMileage = 0;

        CarCursor car = new CarSelection().id(carId).query(context.getContentResolver());
        car.moveToNext();

        RefuelingCursor refueling = new RefuelingSelection().carId(carId).limit(1).query(context.getContentResolver(), new String[]{RefuelingColumns.MILEAGE}, RefuelingColumns.MILEAGE + " DESC");
        if (refueling.moveToNext()) {
            latestRefuelingMileage = refueling.getMileage();
        }

        OtherCostCursor otherCost = new OtherCostSelection().carId(carId).limit(1).query(context.getContentResolver(), new String[]{OtherCostColumns.MILEAGE}, OtherCostColumns.MILEAGE + " DESC");
        if (otherCost.moveToNext() && otherCost.getMileage() != null) {
            latestOtherCostMileage = otherCost.getMileage();
        }

        return Math.max(car.getInitialMileage(), Math.max(latestOtherCostMileage, latestRefuelingMileage));
    }
}
