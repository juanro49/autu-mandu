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
package org.juanro.autumandu.presentation;

import android.content.Context;

import java.util.HashSet;
import java.util.Set;

import org.juanro.autumandu.model.AutuManduDatabase;
import org.juanro.autumandu.model.entity.FuelType;
import org.juanro.autumandu.provider.fueltype.FuelTypeColumns;
import org.juanro.autumandu.provider.fueltype.FuelTypeContentValues;
import org.juanro.autumandu.provider.fueltype.FuelTypeCursor;
import org.juanro.autumandu.provider.fueltype.FuelTypeSelection;

public class FuelTypePresenter {

    private Context mContext;
    private AutuManduDatabase mDB;

    private FuelTypePresenter(Context context) {
        mContext = context;
        mDB = AutuManduDatabase.getInstance(mContext);
    }

    public static FuelTypePresenter getInstance(Context context) {
        return new FuelTypePresenter(context);
    }

    public String[] getAllCategories() {
        Set<String> categories = new HashSet<>();

        for (FuelType ft: mDB.getFuelTypeDao().getAll()) {
            categories.add(ft.getCategory());
        }

        return categories.toArray(new String[0]);
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
