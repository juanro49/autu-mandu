/*
 * Copyright 2026 Juanro49
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.juanro.autumandu.Preferences;
import org.juanro.autumandu.R;
import org.juanro.autumandu.model.AutuManduDatabase;
import org.juanro.autumandu.model.dto.BalancedRefueling;
import org.juanro.autumandu.model.dto.RefuelingWithDetails;
import org.juanro.autumandu.model.entity.Car;
import org.juanro.autumandu.model.entity.FuelType;

public class DistanceVolumeCalculation extends AbstractCalculation {

    public enum Direction {
        DISTANCE_TO_VOLUME,
        VOLUME_TO_DISTANCE
    }

    private final Direction mDirection;
    private List<String> mNames;
    private List<Double> mAvgConsumptions;
    private List<Integer> mColors;

    public DistanceVolumeCalculation(Context context, Direction direction) {
        super(context);
        mDirection = direction;
    }

    @Override
    public String getName() {
        int resId = mDirection == Direction.DISTANCE_TO_VOLUME
                ? R.string.calc_option_distance_to_volume
                : R.string.calc_option_volume_to_distance;
        return mContext.getString(resId, getInputUnit(), getOutputUnit());
    }

    @Override
    public String getInputUnit() {
        Preferences prefs = new Preferences(mContext);
        return mDirection == Direction.DISTANCE_TO_VOLUME ? prefs.getUnitDistance() : prefs.getUnitVolume();
    }

    @Override
    public String getOutputUnit() {
        Preferences prefs = new Preferences(mContext);
        return mDirection == Direction.DISTANCE_TO_VOLUME ? prefs.getUnitVolume() : prefs.getUnitDistance();
    }

    @Override
    public boolean hasColors() {
        return true;
    }

    @Override
    protected void onLoadData() {
        mNames = new ArrayList<>();
        mAvgConsumptions = new ArrayList<>();
        mColors = new ArrayList<>();

        AutuManduDatabase db = AutuManduDatabase.getInstance(mContext);
        List<Car> cars = db.getCarDao().getAll();
        List<RefuelingWithDetails> allRefuelings = db.getRefuelingDao().getAllWithDetails();
        Map<Long, List<RefuelingWithDetails>> refuelingsByCar = allRefuelings.stream()
                .collect(Collectors.groupingBy(RefuelingWithDetails::carId));
        Map<Long, String> fuelTypeToCategory = db.getFuelTypeDao().getAll().stream()
                .collect(Collectors.toMap(FuelType::getId, ft -> Objects.toString(ft.getCategory(), ""), (existing, replacement) -> existing));

        for (Car car : cars) {
            List<RefuelingWithDetails> carRefuelings = refuelingsByCar.get(car.getId());
            if (carRefuelings == null || carRefuelings.isEmpty()) continue;

            Map<String, List<RefuelingWithDetails>> byCategory = carRefuelings.stream()
                    .collect(Collectors.groupingBy(r -> {
                        String category = fuelTypeToCategory.get(r.fuelTypeId());
                        return category != null ? category : "";
                    }));

            for (Map.Entry<String, List<RefuelingWithDetails>> entry : byCategory.entrySet()) {
                String category = entry.getKey();
                if (category.isEmpty()) continue;

                Preferences prefsForGuess = new Preferences(mContext);
                List<BalancedRefueling> balanced = BalancedRefueling.balance(entry.getValue(), prefsForGuess.isAutoGuessMissingDataEnabled(), false);

                int lastMileage = 0;
                int totalDistance = 0, distance = 0;
                float totalVolume = 0, volume = 0;
                boolean foundFullRefueling = false;

                for (BalancedRefueling refueling : balanced) {
                    if (!foundFullRefueling) {
                        if (!refueling.isPartial()) foundFullRefueling = true;
                    } else {
                        distance += refueling.getMileage() - lastMileage;
                        volume += refueling.getVolume();
                        if (!refueling.isPartial()) {
                            totalDistance += distance;
                            totalVolume += volume;
                            distance = 0;
                            volume = 0;
                        }
                    }
                    lastMileage = refueling.getMileage();
                }

                if (totalDistance > 0 && totalVolume > 0) {
                    mNames.add(String.format("%s (%s)", car.getName(), category));
                    mAvgConsumptions.add((double) totalVolume / totalDistance);
                    mColors.add(car.getColor());
                }
            }
        }
    }

    @Override
    protected CalculationItem[] onCalculate(double input) {
        return IntStream.range(0, mNames.size())
                .mapToObj(i -> {
                    double result = mDirection == Direction.DISTANCE_TO_VOLUME
                            ? input * mAvgConsumptions.get(i)
                            : input / mAvgConsumptions.get(i);
                    return new CalculationItem(mNames.get(i), result, mColors.get(i));
                })
                .toArray(CalculationItem[]::new);
    }
}
