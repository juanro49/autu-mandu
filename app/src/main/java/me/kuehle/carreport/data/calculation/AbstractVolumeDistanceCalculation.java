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

package me.kuehle.carreport.data.calculation;

import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;

import java.util.ArrayList;
import java.util.List;

import me.kuehle.carreport.data.balancing.BalancedRefuelingCursor;
import me.kuehle.carreport.data.balancing.RefuelingBalancer;
import me.kuehle.carreport.data.query.CarQueries;
import me.kuehle.carreport.provider.car.CarColumns;
import me.kuehle.carreport.provider.car.CarCursor;
import me.kuehle.carreport.provider.car.CarSelection;

public abstract class AbstractVolumeDistanceCalculation extends AbstractCalculation {
    @SuppressWarnings({"MismatchedQueryAndUpdateOfCollection", "FieldCanBeLocal"})
    private List<Cursor> mCursorStore;

    protected List<String> mNames;
    protected List<Double> mAvgConsumptions;
    protected List<Integer> mColors;

    public AbstractVolumeDistanceCalculation(Context context) {
        super(context);
    }

    @Override
    public boolean hasColors() {
        return true;
    }

    @Override
    protected void onLoadData(ContentObserver observer) {
        mCursorStore = new ArrayList<>();

        mNames = new ArrayList<>();
        mAvgConsumptions = new ArrayList<>();
        mColors = new ArrayList<>();

        RefuelingBalancer balancer = new RefuelingBalancer(mContext);

        CarCursor car = new CarSelection().query(mContext.getContentResolver(), null, CarColumns.NAME + " COLLATE UNICODE");
        car.registerContentObserver(observer);
        mCursorStore.add(car);
        while (car.moveToNext()) {
            String[] categories = CarQueries.getUsedFuelTypeCategories(mContext, car.getId());
            for (String category : categories) {
                BalancedRefuelingCursor refueling = balancer.getBalancedRefuelings(car.getId(), category);
                refueling.registerContentObserver(observer);
                mCursorStore.add(refueling);

                int lastMileage = 0;
                int totalDistance = 0, distance = 0;
                float totalVolume = 0, volume = 0;
                boolean foundFullRefueling = false;

                while (refueling.moveToNext()) {
                    if (!foundFullRefueling) {
                        if (!refueling.getPartial()) {
                            foundFullRefueling = true;
                        }
                    } else {
                        distance += refueling.getMileage() - lastMileage;
                        volume += refueling.getVolume();

                        if (!refueling.getPartial()) {
                            totalDistance += distance;
                            totalVolume += volume;

                            distance = 0;
                            volume = 0;
                        }
                    }

                    lastMileage = refueling.getMileage();
                }

                if (totalDistance > 0 && totalVolume > 0) {
                    double avgConsumption = totalVolume / totalDistance;

                    mNames.add(String.format("%s (%s)", car.getName(), category));
                    mAvgConsumptions.add(avgConsumption);
                    mColors.add(car.getColor());
                }
            }
        }
    }
}
