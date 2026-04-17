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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.juanro.autumandu.Preferences;
import org.juanro.autumandu.R;
import org.juanro.autumandu.model.AutuManduDatabase;
import org.juanro.autumandu.model.dto.BalancedRefueling;
import org.juanro.autumandu.model.dto.RefuelingWithDetails;
import org.juanro.autumandu.model.entity.Car;
import org.juanro.autumandu.model.entity.OtherCost;
import org.juanro.autumandu.util.Recurrences;

public class DistancePriceCalculation extends AbstractCalculation {

    public enum Direction {
        DISTANCE_TO_PRICE,
        PRICE_TO_DISTANCE,
        DISTANCE_TO_FUEL_PRICE,
        FUEL_PRICE_TO_DISTANCE
    }

    private final Direction mDirection;
    private final boolean mIncludeOtherCosts;
    protected List<String> mNames;
    protected List<Double> mAvgDistancePrices;
    protected List<Integer> mColors;

    public DistancePriceCalculation(Context context, Direction direction) {
        super(context);
        mDirection = direction;
        mIncludeOtherCosts = (direction == Direction.DISTANCE_TO_PRICE || direction == Direction.PRICE_TO_DISTANCE);
    }

    @Override
    public String getName() {
        int resId = switch (mDirection) {
            case DISTANCE_TO_PRICE -> R.string.calc_option_distance_to_price;
            case PRICE_TO_DISTANCE -> R.string.calc_option_price_to_distance;
            case DISTANCE_TO_FUEL_PRICE -> R.string.calc_option_distance_to_fuel_price;
            case FUEL_PRICE_TO_DISTANCE -> R.string.calc_option_fuel_price_to_distance;
        };
        return mContext.getString(resId, getInputUnit(), getOutputUnit());
    }

    @Override
    public String getInputUnit() {
        Preferences prefs = new Preferences(mContext);
        return (mDirection == Direction.DISTANCE_TO_PRICE || mDirection == Direction.DISTANCE_TO_FUEL_PRICE)
                ? prefs.getUnitDistance() : prefs.getUnitCurrency();
    }

    @Override
    public String getOutputUnit() {
        Preferences prefs = new Preferences(mContext);
        return (mDirection == Direction.DISTANCE_TO_PRICE || mDirection == Direction.DISTANCE_TO_FUEL_PRICE)
                ? prefs.getUnitCurrency() : prefs.getUnitDistance();
    }

    @Override
    protected void onLoadData() {
        AutuManduDatabase db = AutuManduDatabase.getInstance(mContext);
        List<Car> cars = db.getCarDao().getAll();
        Map<Long, List<RefuelingWithDetails>> refuelingsByCar = db.getRefuelingDao().getAllWithDetails()
                .stream().collect(Collectors.groupingBy(RefuelingWithDetails::carId));
        Map<Long, List<OtherCost>> otherCostsByCar = mIncludeOtherCosts ?
                db.getOtherCostDao().getAll().stream().collect(Collectors.groupingBy(OtherCost::getCarId)) :
                Collections.emptyMap();

        mNames = new ArrayList<>(cars.size());
        mAvgDistancePrices = new ArrayList<>(cars.size());
        mColors = new ArrayList<>(cars.size());

        for (Car car : cars) {
            List<RefuelingWithDetails> carRefuelings = refuelingsByCar.getOrDefault(car.getId(), Collections.emptyList());
            Preferences prefsForGuess = new Preferences(mContext);
            List<BalancedRefueling> balanced = BalancedRefueling.balance(carRefuelings, prefsForGuess.isAutoGuessMissingDataEnabled(), false);

            double refuelingCosts = balanced.stream().mapToDouble(BalancedRefueling::getPrice).sum();
            int refuelingMaxMileage = balanced.stream().mapToInt(BalancedRefueling::getMileage).max().orElse(Integer.MIN_VALUE);

            double otherCostsSum = 0;
            int otherCostsMaxMileage = Integer.MIN_VALUE;
            if (mIncludeOtherCosts) {
                List<OtherCost> otherCosts = otherCostsByCar.get(car.getId());
                if (otherCosts != null) {
                    for (OtherCost otherCost : otherCosts) {
                        int recurrences = (otherCost.getEndDate() == null)
                                ? Recurrences.getRecurrencesSince(otherCost.getRecurrenceInterval(), otherCost.getRecurrenceMultiplier(), otherCost.getDate())
                                : Recurrences.getRecurrencesBetween(otherCost.getRecurrenceInterval(), otherCost.getRecurrenceMultiplier(), otherCost.getDate(), otherCost.getEndDate());
                        otherCostsSum += otherCost.getPrice() * recurrences;
                        if (otherCost.getMileage() != null && otherCost.getMileage() > -1) {
                            otherCostsMaxMileage = Math.max(otherCostsMaxMileage, otherCost.getMileage());
                        }
                    }
                }
            }

            double totalCosts = refuelingCosts + otherCostsSum;
            int endMileage = Math.max(refuelingMaxMileage, otherCostsMaxMileage);
            int startMileage = car.getInitialMileage();

            if (totalCosts > 0 && startMileage > -1 && endMileage > startMileage) {
                mNames.add(car.getName());
                mAvgDistancePrices.add(totalCosts / (endMileage - startMileage));
                mColors.add(car.getColor());
            }
        }
    }

    @Override
    protected CalculationItem[] onCalculate(double input) {
        return IntStream.range(0, mNames.size())
                .mapToObj(i -> {
                    double result = switch (mDirection) {
                        case DISTANCE_TO_PRICE, DISTANCE_TO_FUEL_PRICE -> input * mAvgDistancePrices.get(i);
                        case PRICE_TO_DISTANCE, FUEL_PRICE_TO_DISTANCE -> input / mAvgDistancePrices.get(i);
                    };
                    return new CalculationItem(mNames.get(i), result, mColors.get(i));
                })
                .toArray(CalculationItem[]::new);
    }
}
