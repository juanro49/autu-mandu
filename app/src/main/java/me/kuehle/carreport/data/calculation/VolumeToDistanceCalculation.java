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
import me.kuehle.carreport.data.balancing.RefuelingBalancer;
import me.kuehle.carreport.db.Car;
import me.kuehle.carreport.db.Refueling;

public class VolumeToDistanceCalculation extends AbstractCalculation {
    public VolumeToDistanceCalculation(Context context) {
        super(context);
    }

    @Override
    public String getName() {
        return context.getString(R.string.calc_option_volume_to_distance,
                getInputUnit(), getOutputUnit());
    }

    @Override
    public String getInputUnit() {
        Preferences prefs = new Preferences(context);
        return prefs.getUnitVolume();
    }

    @Override
    public String getOutputUnit() {
        Preferences prefs = new Preferences(context);
        return prefs.getUnitDistance();
    }

    @Override
    public CalculationItem[] calculate(double input) {
        List<CalculationItem> items = new ArrayList<>();
        for (Car car : Car.getAll()) {
            List<String> categories = car.getUsedFuelTypeCategories();
            for (String category : categories) {
                RefuelingBalancer balancer = new RefuelingBalancer(context);
                List<Refueling> refuelings = balancer.getBalancedRefuelings(car, category);

                int totalDistance = 0, distance = 0;
                float totalVolume = 0, volume = 0;
                int lastFullRefueling = -1;
                for (int i = 0; i < refuelings.size(); i++) {
                    Refueling refueling = refuelings.get(i);
                    if (lastFullRefueling < 0) {
                        if (!refueling.partial) {
                            lastFullRefueling = i;
                        }

                        continue;
                    }

                    distance += refueling.mileage
                            - refuelings.get(i - 1).mileage;
                    volume += refueling.volume;

                    if (!refueling.partial) {
                        totalDistance += distance;
                        totalVolume += volume;

                        distance = 0;
                        volume = 0;

                        lastFullRefueling = i;
                    }
                }

                if (totalDistance > 0 && totalVolume > 0) {
                    double avgConsumption = totalVolume / totalDistance;
                    items.add(new CalculationItem(String.format("%s (%s)",
                            car.name, category), input / avgConsumption));
                }
            }
        }

        return items.toArray(new CalculationItem[items.size()]);
    }
}
