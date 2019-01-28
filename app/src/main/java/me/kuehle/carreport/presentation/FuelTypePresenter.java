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
package me.kuehle.carreport.presentation;

import android.content.Context;

import java.util.HashSet;
import java.util.Set;

import me.kuehle.carreport.model.CarReportDatabase;
import me.kuehle.carreport.model.entity.FuelType;
import me.kuehle.carreport.provider.fueltype.FuelTypeColumns;
import me.kuehle.carreport.provider.fueltype.FuelTypeContentValues;
import me.kuehle.carreport.provider.fueltype.FuelTypeCursor;
import me.kuehle.carreport.provider.fueltype.FuelTypeSelection;
import me.kuehle.carreport.provider.refueling.RefuelingColumns;
import me.kuehle.carreport.provider.refueling.RefuelingCursor;
import me.kuehle.carreport.provider.refueling.RefuelingSelection;

public class FuelTypePresenter {

    private Context mContext;
    private CarReportDatabase mDB;

    private FuelTypePresenter(Context context) {
        mContext = context;
        mDB = CarReportDatabase.getInstance(mContext);
    }

    public static FuelTypePresenter getInstance(Context context) {
        return new FuelTypePresenter(context);
    }

    public String[] getAllCategories() {
        Set<String> categories = new HashSet<>();

        for (FuelType ft: mDB.getFuelTypeDao().getAll()) {
            categories.add(ft.getCategory());
        }

        return categories.toArray(new String[categories.size()]);
    }

    public void ensureAtLeastOne() {
        FuelTypeCursor cursor = new FuelTypeSelection().query(mContext.getContentResolver(), new String[]{FuelTypeColumns._ID});
        if (cursor.getCount() == 0) {
            FuelTypeContentValues values = new FuelTypeContentValues();
            values.putName("Default");
            values.putCategory("Default");
            values.insert(mContext.getContentResolver());
        }
    }

    public long getMostUsedId(long carId) {
        FuelType mostUsed = mDB.getFuelTypeDao().getMostUsedForCar(carId);
        if (mostUsed != null) {
            return mostUsed.getId();
        } else {
            return 0;
        }
    }

    public boolean isUsed(long fuelTypeId) {
        return mDB.getFuelTypeDao().getUsageCount(fuelTypeId) > 0;
    }
}
