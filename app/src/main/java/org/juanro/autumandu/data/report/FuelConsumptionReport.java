/*
 * Copyright 2012 Jan Kühle
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

package org.juanro.autumandu.data.report;

import android.content.Context;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.juanro.autumandu.FuelConsumption;
import org.juanro.autumandu.FuelConsumption.Type;
import org.juanro.autumandu.Preferences;
import org.juanro.autumandu.R;
import org.juanro.autumandu.model.AutuManduDatabase;
import org.juanro.autumandu.model.dto.BalancedRefueling;
import org.juanro.autumandu.model.dto.RefuelingWithDetails;
import org.juanro.autumandu.model.entity.Car;
import org.juanro.autumandu.model.entity.FuelCategory;
import org.juanro.autumandu.util.Calculator;

public class FuelConsumptionReport extends AbstractReport {
    private static final String CONSUMPTION_FORMAT = "%.2f %s";

    private class ReportChartData extends AbstractReportChartLineData {
        private final double mAvgConsumption;
        private final String mUnit;

        public ReportChartData(Context context, Car car, String categoryName, FuelCategory category, List<BalancedRefueling> refuelings) {
            super(context, String.format("%s (%s)", car.getName(), categoryName), car.getColor());

            FuelConsumption fuelConsumption = new FuelConsumption(context);
            mUnit = fuelConsumption.getUnitLabel(Type.fromId(new Preferences(context).getUnitFuelConsumption()), category.getVolumeUnit(context));
            mDateFormat = android.text.format.DateFormat.getDateFormat(mContext);

            int lastMileage = 0;
            int totalDistance = 0;
            int partialDistance = 0;
            float totalVolume = 0;
            float partialVolume = 0;
            boolean foundFullRefueling = false;

            for (BalancedRefueling refueling : refuelings) {
                if (!foundFullRefueling) {
                    if (!refueling.isPartial()) {
                        foundFullRefueling = true;
                    }
                } else {
                    partialDistance += refueling.getMileage() - lastMileage;
                    partialVolume += refueling.getVolume();

                    if (!refueling.isPartial() && partialDistance > 0) {
                        totalDistance += partialDistance;
                        totalVolume += partialVolume;

                        if (partialVolume > 0) {
                            float consumption = fuelConsumption.computeFuelConsumption(partialVolume,
                                    partialDistance);
                            String tooltip = makeTooltip(car.getName(), consumption, refueling.getFuelTypeName(), refueling.getDate(), refueling.isGuessed());

                            add(ReportDateHelper.toFloat(refueling.getDate()),
                                    consumption,
                                    tooltip,
                                    refueling.isGuessed());
                        }

                        partialDistance = 0;
                        partialVolume = 0;
                    }
                }

                lastMileage = refueling.getMileage();
            }

            mAvgConsumption = fuelConsumption.computeFuelConsumption(totalVolume, totalDistance);
        }

        private String makeTooltip(String carName, float consumption, String fuelTypeName, Date date, boolean guessed) {
            String tooltip = mContext.getString(R.string.report_toast_fuel_consumption,
                    carName,
                    consumption,
                    mUnit,
                    fuelTypeName,
                    mDateFormat.format(date));
            if (guessed) {
                tooltip += "\n" + mContext.getString(R.string.report_toast_guessed);
            }
            return tooltip;
        }

        public double getAverageConsumption() {
            return mAvgConsumption;
        }

        public String getUnit() {
            return mUnit;
        }
    }

    private final List<AbstractReportChartData> reportData = new ArrayList<>();
    private DateFormat mDateFormat;

    public FuelConsumptionReport(Context context) {
        super(context);
    }

    @Override
    public String formatXValue(float value, int chartOption) {
        return mDateFormat.format(ReportDateHelper.toDate(value));
    }

    @Override
    public String formatYValue(float value, int chartOption) {
        return String.format(Locale.getDefault(), "%.2f", value);
    }

    @Override
    public int[] getAvailableChartOptions() {
        return new int[1];
    }

    @Override
    public String getTitle() {
        return mContext.getString(R.string.report_title_fuel_consumption);
    }

    @Override
    public List<AbstractReportChartData> getRawChartData(int chartOption) {
        synchronized (mCachedChartData) {
            if (mCachedChartData.containsKey(chartOption)) {
                return mCachedChartData.get(chartOption);
            }
            List<AbstractReportChartData> data = new ArrayList<>(reportData);
            mCachedChartData.put(chartOption, data);
            return data;
        }
    }

    @Override
    protected void onUpdate() {
        reportData.clear();
        mDateFormat = android.text.format.DateFormat.getDateFormat(mContext);

        AutuManduDatabase db = AutuManduDatabase.getInstance(mContext);

        // Bulk load all data to optimize performance (N+1 avoidance)
        List<Car> cars = db.getCarDao().getAll();
        Map<Long, List<RefuelingWithDetails>> refuelingsByCar = db.getRefuelingDao().getAllWithDetails()
                .stream().collect(Collectors.groupingBy(RefuelingWithDetails::carId));

        for (Car car : cars) {
            processCar(car, refuelingsByCar);
        }
    }

    private void processCar(Car car, Map<Long, List<RefuelingWithDetails>> refuelingsByCar) {
        Long carIdObj = car.getId();
        if (carIdObj == null) return;
        long carId = carIdObj;
        boolean sectionAdded = false;

        // Get balanced refuelings for this car once.
        List<RefuelingWithDetails> carRefuelings = refuelingsByCar.get(carId);
        if (carRefuelings == null) carRefuelings = Collections.emptyList();

        Preferences prefsForGuess = new Preferences(mContext);
        FuelConsumption.Type consumptionType = Type.fromId(prefsForGuess.getUnitFuelConsumption());
        List<BalancedRefueling> balancedRefuelings = BalancedRefueling.balance(carRefuelings, consumptionType, prefsForGuess.isAutoGuessMissingDataEnabled(), false);
        if (balancedRefuelings == null) balancedRefuelings = Collections.emptyList();

        Map<String, List<BalancedRefueling>> refuelingsByCategoryKey = balancedRefuelings.stream()
                .collect(Collectors.groupingBy(r -> {
                    String key = r.getFuelTypeCategory();
                    if (key == null) return "general";
                    return key;
                }));

        for (Map.Entry<String, List<BalancedRefueling>> entry : refuelingsByCategoryKey.entrySet()) {
            String categoryKey = entry.getKey();
            FuelCategory category = FuelCategory.fromKey(categoryKey);
            String categoryName = category.getName(mContext);
            List<BalancedRefueling> categoryRefuelings = entry.getValue();

            ReportChartData carData = new ReportChartData(mContext, car, categoryName, category, categoryRefuelings);
            if (carData.isEmpty()) {
                continue;
            }

            reportData.add(carData);

            Section section = addDataSection(car, categoryName);
            Float[] yValues = carData.getYValues().toArray(new Float[0]);
            section.addItem(new Item(mContext.getString(R.string.report_highest), String.format(Locale.getDefault(),
                    CONSUMPTION_FORMAT, Calculator.max(yValues), carData.getUnit())));
            section.addItem(new Item(mContext.getString(R.string.report_lowest), String.format(Locale.getDefault(),
                    CONSUMPTION_FORMAT, Calculator.min(yValues), carData.getUnit())));
            section.addItem(new Item(mContext.getString(R.string.report_average), String.format(Locale.getDefault(),
                    CONSUMPTION_FORMAT, carData.getAverageConsumption(), carData.getUnit())));

            // Total volume metric
            float totalVolume = 0;
            for (BalancedRefueling refueling : categoryRefuelings) {
                totalVolume += refueling.getVolume();
            }
            section.addItem(new Item(mContext.getString(R.string.report_total_volume),
                    String.format(Locale.getDefault(), CONSUMPTION_FORMAT, totalVolume, category.getVolumeUnit(mContext))));

            sectionAdded = true;
        }

        if (!sectionAdded) {
            Section section = addDataSection(car);
            if (!carRefuelings.isEmpty()) {
                float totalVolume = 0;
                for (RefuelingWithDetails refueling : carRefuelings) {
                    totalVolume += refueling.volume();
                }
                section.addItem(new Item(mContext.getString(R.string.report_total_volume),
                        String.format(Locale.getDefault(), CONSUMPTION_FORMAT, totalVolume, prefsForGuess.getUnitVolume())));
            }
            section.addItem(new Item(mContext.getString(R.string.report_not_enough_data), ""));
        }
    }

    private Section addDataSection(Car car) {
        String name = car.getName();
        if (car.getSuspendedSince() != null) {
            return addDataSection(String.format("%s [%s]", name,
                    mContext.getString(R.string.suspended)), car.getColor(), 1);
        } else {
            return addDataSection(name, car.getColor());
        }
    }

    private Section addDataSection(Car car, String category) {
        String name = String.format("%s (%s)", car.getName(), category);
        if (car.getSuspendedSince() != null) {
            return addDataSection(String.format("%s [%s]", name,
                    mContext.getString(R.string.suspended)), car.getColor(), 1);
        } else {
            return addDataSection(name, car.getColor());
        }
    }
}
