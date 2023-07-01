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

import org.juanro.autumandu.data.balancing.BalancedRefuelingCursor;
import org.juanro.autumandu.data.balancing.RefuelingBalancer;
import org.juanro.autumandu.provider.car.CarColumns;
import org.juanro.autumandu.provider.car.CarCursor;
import org.juanro.autumandu.provider.car.CarSelection;
import org.juanro.autumandu.provider.othercost.OtherCostColumns;
import org.juanro.autumandu.provider.othercost.OtherCostCursor;
import org.juanro.autumandu.provider.othercost.OtherCostSelection;
import org.juanro.autumandu.util.Recurrences;

public abstract class AbstractDistancePriceCalculation extends AbstractCalculation {
    @SuppressWarnings({"MismatchedQueryAndUpdateOfCollection", "FieldCanBeLocal"})
    private List<Cursor> mCursorStore;
    private boolean mIncludeOtherCosts;

    protected List<String> mNames;
    protected List<Double> mAvgDistancePrices;
    protected List<Integer> mColors;

    public AbstractDistancePriceCalculation(Context context, boolean includeOtherCosts) {
        super(context);

        mIncludeOtherCosts = includeOtherCosts;
    }

    @Override
    public boolean hasColors() {
        return true;
    }

    @Override
    protected void onLoadData(ContentObserver observer) {
        mCursorStore = new ArrayList<>();

        RefuelingBalancer balancer = new RefuelingBalancer(mContext);

        CarCursor car = new CarSelection().query(mContext.getContentResolver(), null, CarColumns.NAME + " COLLATE UNICODE");
        car.registerContentObserver(observer);
        mCursorStore.add(car);

        mNames = new ArrayList<>(car.getCount());
        mAvgDistancePrices = new ArrayList<>(car.getCount());
        mColors = new ArrayList<>(car.getCount());

        while (car.moveToNext()) {
            double totalCosts = 0;
            final int startMileage = car.getInitialMileage();
            int endMileage = Integer.MIN_VALUE;

            BalancedRefuelingCursor refueling = balancer.getBalancedRefuelings(car.getId());
            refueling.registerContentObserver(observer);
            mCursorStore.add(refueling);
            while (refueling.moveToNext()) {
                totalCosts += refueling.getPrice();
                endMileage = Math.max(endMileage, refueling.getMileage());
            }

            if (mIncludeOtherCosts) {
                OtherCostCursor otherCost = new OtherCostSelection().carId(car.getId())
                        .query(mContext.getContentResolver(), OtherCostColumns.ALL_COLUMNS);
                otherCost.registerContentObserver(observer);
                mCursorStore.add(otherCost);
                while (otherCost.moveToNext()) {
                    int recurrences;
                    if (otherCost.getEndDate() == null) {
                        recurrences = Recurrences.getRecurrencesSince(otherCost.getRecurrenceInterval(),
                                otherCost.getRecurrenceMultiplier(), otherCost.getDate());
                    } else {
                        recurrences = Recurrences.getRecurrencesBetween(otherCost.getRecurrenceInterval(),
                                otherCost.getRecurrenceMultiplier(), otherCost.getDate(), otherCost.getEndDate());
                    }

                    totalCosts += otherCost.getPrice() * recurrences;

                    if (otherCost.getMileage() != null && otherCost.getMileage() > -1) {
                        endMileage = Math.max(endMileage, otherCost.getMileage());
                    }
                }
            }

            if (totalCosts > 0 && startMileage > -1 && endMileage > startMileage) {
                double avgDistancePrice = totalCosts / (endMileage - startMileage);

                mNames.add(car.getName());
                mAvgDistancePrices.add(avgDistancePrice);
                mColors.add(car.getColor());
            }
        }
    }
}
