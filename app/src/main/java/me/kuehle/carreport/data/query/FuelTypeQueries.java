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

import me.kuehle.carreport.provider.fueltype.FuelTypeColumns;
import me.kuehle.carreport.provider.fueltype.FuelTypeContentValues;
import me.kuehle.carreport.provider.fueltype.FuelTypeCursor;
import me.kuehle.carreport.provider.fueltype.FuelTypeSelection;
import me.kuehle.carreport.provider.refueling.RefuelingColumns;
import me.kuehle.carreport.provider.refueling.RefuelingCursor;
import me.kuehle.carreport.provider.refueling.RefuelingSelection;

public class FuelTypeQueries {
    public static String[] getAllCategories(Context context) {
        Set<String> categories = new HashSet<>();

        FuelTypeCursor fuelType = new FuelTypeSelection().query(context.getContentResolver(),
                new String[]{FuelTypeColumns.CATEGORY},
                FuelTypeColumns.CATEGORY + " COLLATE UNICODE ASC");
        while (fuelType.moveToNext()) {
            categories.add(fuelType.getCategory());
        }

        return categories.toArray(new String[categories.size()]);
    }

    public static void ensureAtLeastOne(Context context) {
        FuelTypeCursor cursor = new FuelTypeSelection().query(context.getContentResolver(), new String[]{FuelTypeColumns._ID});
        if (cursor.getCount() == 0) {
            FuelTypeContentValues values = new FuelTypeContentValues();
            values.putName("Default");
            values.putCategory("Default");
            values.insert(context.getContentResolver());
        }
    }

    public static long getMostUsedId(Context context, long carId) {
        RefuelingCursor cursor = new RefuelingSelection()
                .carId(carId)
                .groupBy(RefuelingColumns.FUEL_TYPE_ID)
                .limit(1)
                .query(context.getContentResolver(),
                        new String[]{RefuelingColumns.FUEL_TYPE_ID},
                        String.format("COUNT(%s) DESC", RefuelingColumns._ID));
        if (cursor.moveToNext()) {
            return cursor.getFuelTypeId();
        } else {
            return 0;
        }
    }

    public static boolean isUsed(Context context, long fuelTypeId) {
        RefuelingCursor refueling = new RefuelingSelection().fuelTypeId(fuelTypeId).query(context.getContentResolver(), new String[]{RefuelingColumns._ID});
        return refueling.getCount() > 0;
    }
}
