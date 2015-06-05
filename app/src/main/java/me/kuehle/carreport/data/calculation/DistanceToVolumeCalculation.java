/*
 * Copyright 2014 Jan Kühle
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

package me.kuehle.carreport.data.calculation;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

import me.kuehle.carreport.Preferences;
import me.kuehle.carreport.R;
import me.kuehle.carreport.data.balancing.BalancedRefuelingCursor;
import me.kuehle.carreport.data.balancing.RefuelingBalancer;
import me.kuehle.carreport.data.query.CarQueries;
import me.kuehle.carreport.provider.car.CarCursor;
import me.kuehle.carreport.provider.car.CarSelection;

public class DistanceToVolumeCalculation extends AbstractCalculation {
    public DistanceToVolumeCalculation(Context context) {
        super(context);
    }

    @Override
    public String getName() {
        return mContext.getString(R.string.calc_option_distance_to_volume,
                getInputUnit(), getOutputUnit());
    }

    @Override
    public String getInputUnit() {
        Preferences prefs = new Preferences(mContext);
        return prefs.getUnitDistance();
    }

    @Override
    public String getOutputUnit() {
        Preferences prefs = new Preferences(mContext);
        return prefs.getUnitVolume();
    }

    @Override
    public CalculationItem[] calculate(double input) {
        List<CalculationItem> items = new ArrayList<>();

        CarCursor car = new CarSelection().query(mContext.getContentResolver());
        while (car.moveToNext()) {
            String[] categories = CarQueries.getUsedFuelTypeCategories(mContext, car.getId());
            for (String category : categories) {
                RefuelingBalancer balancer = new RefuelingBalancer(mContext);
                BalancedRefuelingCursor refueling = balancer.getBalancedRefuelings(car.getId(), category);

                int lastMileage = 0;
                int totalDistance = 0, distance = 0;
                float totalVolume = 0, volume = 0;
                boolean foundFullRefueling = false;

                while(refueling.moveToNext()) {
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
                    items.add(new CalculationItem(String.format("%s (%s)", car.getName(), category),
                            input * avgConsumption));
                }
            }
        }

        return items.toArray(new CalculationItem[items.size()]);
    }
}
