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
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.juanro.autumandu.Preferences;
import org.juanro.autumandu.R;
import org.juanro.autumandu.model.AutuManduDatabase;
import org.juanro.autumandu.model.entity.FuelType;
import org.juanro.autumandu.model.entity.Refueling;

public class PriceVolumeCalculation extends AbstractCalculation {

    public enum Direction {
        PRICE_TO_VOLUME,
        VOLUME_TO_PRICE
    }

    private final Direction mDirection;
    protected List<String> mNames;
    protected List<Double> mAvgFuelPrices;

    public PriceVolumeCalculation(Context context, Direction direction) {
        super(context);
        mDirection = direction;
    }

    @Override
    public String getName() {
        int resId = mDirection == Direction.PRICE_TO_VOLUME
                ? R.string.calc_option_price_to_volume
                : R.string.calc_option_volume_to_price;
        return mContext.getString(resId, getInputUnit(), getOutputUnit());
    }

    @Override
    public String getInputUnit() {
        Preferences prefs = new Preferences(mContext);
        return mDirection == Direction.PRICE_TO_VOLUME ? prefs.getUnitCurrency() : prefs.getUnitVolume();
    }

    @Override
    public String getOutputUnit() {
        Preferences prefs = new Preferences(mContext);
        return mDirection == Direction.PRICE_TO_VOLUME ? prefs.getUnitVolume() : prefs.getUnitCurrency();
    }

    @Override
    public boolean hasColors() {
        return false;
    }

    @Override
    protected void onLoadData() {
        mNames = new ArrayList<>();
        mAvgFuelPrices = new ArrayList<>();

        AutuManduDatabase db = AutuManduDatabase.getInstance(mContext);
        List<FuelType> fuelTypes = db.getFuelTypeDao().getAll();
        Map<Long, List<Refueling>> refuelingsByFuelType = db.getRefuelingDao().getAll()
                .stream()
                .collect(Collectors.groupingBy(Refueling::getFuelTypeId));

        for (FuelType fuelType : fuelTypes) {
            List<Refueling> refuelings = refuelingsByFuelType.get(fuelType.getId());
            if (refuelings != null && !refuelings.isEmpty()) {
                double avgFuelPrice = refuelings.stream()
                        .filter(r -> r.getVolume() > 0)
                        .mapToDouble(r -> r.getPrice() / r.getVolume())
                        .average()
                        .orElse(0.0);

                if (avgFuelPrice > 0) {
                    mNames.add(fuelType.getName());
                    mAvgFuelPrices.add(avgFuelPrice);
                }
            }
        }
    }

    @Override
    protected CalculationItem[] onCalculate(double input) {
        return IntStream.range(0, mNames.size())
                .mapToObj(i -> {
                    double result = mDirection == Direction.PRICE_TO_VOLUME
                            ? input / mAvgFuelPrices.get(i)
                            : input * mAvgFuelPrices.get(i);
                    return new CalculationItem(mNames.get(i), result);
                })
                .toArray(CalculationItem[]::new);
    }
}
