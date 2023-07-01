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

package org.juanro.autumandu.data.calculation;

import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;

import java.util.ArrayList;
import java.util.List;

import org.juanro.autumandu.provider.fueltype.FuelTypeColumns;
import org.juanro.autumandu.provider.fueltype.FuelTypeCursor;
import org.juanro.autumandu.provider.fueltype.FuelTypeSelection;
import org.juanro.autumandu.provider.refueling.RefuelingCursor;
import org.juanro.autumandu.provider.refueling.RefuelingSelection;
import org.juanro.autumandu.util.Calculator;

public abstract class AbstractPriceVolumeCalculation extends AbstractCalculation {
    @SuppressWarnings({"MismatchedQueryAndUpdateOfCollection", "FieldCanBeLocal"})
    private List<Cursor> mCursorStore;

    protected List<String> mNames;
    protected List<Double> mAvgFuelPrices;

    public AbstractPriceVolumeCalculation(Context context) {
        super(context);
    }

    @Override
    public boolean hasColors() {
        return false;
    }

    @Override
    protected void onLoadData(ContentObserver observer) {
        mCursorStore = new ArrayList<>();
        mNames = new ArrayList<>();
        mAvgFuelPrices = new ArrayList<>();

        FuelTypeCursor fuelType = new FuelTypeSelection().query(mContext.getContentResolver(), null,
                FuelTypeColumns.NAME + " COLLATE UNICODE");
        fuelType.registerContentObserver(observer);
        mCursorStore.add(fuelType);
        while (fuelType.moveToNext()) {
            RefuelingCursor refueling = new RefuelingSelection().fuelTypeId(fuelType.getId()).query(mContext.getContentResolver());
            refueling.registerContentObserver(observer);
            mCursorStore.add(refueling);
            if (refueling.getCount() > 0) {
                List<Float> fuelPrices = new ArrayList<>(refueling.getCount());
                while (refueling.moveToNext()) {
                    if (refueling.getPrice() != 0.0f) {
                        fuelPrices.add(refueling.getPrice() / refueling.getVolume());
                    }
                }

                double avgFuelPrice = Calculator.avg(fuelPrices.toArray(new Float[refueling.getCount()]));

                mNames.add(fuelType.getName());
                mAvgFuelPrices.add(avgFuelPrice);
            }
        }

    }
}
