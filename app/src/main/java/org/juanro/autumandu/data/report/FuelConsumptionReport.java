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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.juanro.autumandu.FuelConsumption;
import org.juanro.autumandu.Preferences;
import org.juanro.autumandu.R;
import org.juanro.autumandu.model.AutuManduDatabase;
import org.juanro.autumandu.model.dto.BalancedRefueling;
import org.juanro.autumandu.model.dto.RefuelingWithDetails;
import org.juanro.autumandu.model.entity.Car;
import org.juanro.autumandu.util.Calculator;

public class FuelConsumptionReport extends AbstractReport {

    private class ReportChartData extends AbstractReportChartLineData {
        private final double mAvgConsumption;

        public ReportChartData(Context context, Car car, String category, List<BalancedRefueling> refuelings) {
            super(context, String.format("%s (%s)", car.getName(), category), car.getColor());

            FuelConsumption fuelConsumption = new FuelConsumption(context);

            int lastMileage = 0;
            int totalDistance = 0, partialDistance = 0;
            float totalVolume = 0, partialVolume = 0;
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

                        float consumption = fuelConsumption.computeFuelConsumption(partialVolume,
                                partialDistance);
                        String tooltip = mContext.getString(R.string.report_toast_fuel_consumption,
                                car.getName(),
                                consumption,
                                mUnit,
                                refueling.getFuelTypeName(),
                                mDateFormat.format(refueling.getDate()));
                        if (refueling.isGuessed()) {
                            tooltip += "\n" + mContext.getString(R.string.report_toast_guessed);
                        }

                        add(ReportDateHelper.toFloat(refueling.getDate()),
                                consumption,
                                tooltip,
                                refueling.isGuessed());

                        partialDistance = 0;
                        partialVolume = 0;
                    }
                }

                lastMileage = refueling.getMileage();
            }

            mAvgConsumption = fuelConsumption.computeFuelConsumption(totalVolume, totalDistance);
        }

        public double getAverageConsumption() {
            return mAvgConsumption;
        }
    }

    private final List<AbstractReportChartData> reportData = new ArrayList<>();
    private String mUnit;
    private DateFormat mDateFormat;

    public FuelConsumptionReport(Context context) {
        super(context);
    }

    @Override
    protected String formatXValue(float value, int chartOption) {
        return mDateFormat.format(ReportDateHelper.toDate(value));
    }

    @Override
    protected String formatYValue(float value, int chartOption) {
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
    protected List<AbstractReportChartData> getRawChartData(int chartOption) {
        return reportData;
    }

    @Override
    protected void onUpdate() {
        reportData.clear();
        FuelConsumption fuelConsumption = new FuelConsumption(mContext);
        mUnit = fuelConsumption.getUnitLabel();
        mDateFormat = android.text.format.DateFormat.getDateFormat(mContext);

        AutuManduDatabase db = AutuManduDatabase.getInstance(mContext);

        // Bulk load all data to optimize performance (N+1 avoidance)
        List<Car> cars = db.getCarDao().getAll();
        Map<Long, List<RefuelingWithDetails>> refuelingsByCar = db.getRefuelingDao().getAllWithDetails()
                .stream().collect(Collectors.groupingBy(RefuelingWithDetails::carId));

        for (Car car : cars) {
            Long carIdObj = car.getId();
            if (carIdObj == null) continue;
            long carId = carIdObj;
            boolean sectionAdded = false;

            // Get balanced refuelings for this car once.
            List<RefuelingWithDetails> carRefuelings = refuelingsByCar.get(carId);
            if (carRefuelings == null) carRefuelings = Collections.emptyList();

            Preferences prefsForGuess = new Preferences(mContext);
            List<BalancedRefueling> balancedRefuelings = BalancedRefueling.balance(carRefuelings, prefsForGuess.isAutoGuessMissingDataEnabled(), false);
            if (balancedRefuelings == null) balancedRefuelings = Collections.emptyList();

            // Group balanced refuelings by category in memory.
            Map<String, List<BalancedRefueling>> refuelingsByCategory = balancedRefuelings.stream()
                    .filter(r -> r.getFuelTypeCategory() != null)
                    .collect(Collectors.groupingBy(BalancedRefueling::getFuelTypeCategory));

            for (Map.Entry<String, List<BalancedRefueling>> entry : refuelingsByCategory.entrySet()) {
                String category = entry.getKey();
                List<BalancedRefueling> categoryRefuelings = entry.getValue();

                ReportChartData carData = new ReportChartData(mContext, car, category, categoryRefuelings);
                if (carData.isEmpty()) {
                    continue;
                }

                reportData.add(carData);

                Section section = addDataSection(car, category);
                Float[] yValues = carData.getYValues().toArray(new Float[0]);
                section.addItem(new Item(mContext.getString(R.string.report_highest), String.format(Locale.getDefault(),
                        "%.2f %s", Calculator.max(yValues), mUnit)));
                section.addItem(new Item(mContext.getString(R.string.report_lowest), String.format(Locale.getDefault(),
                        "%.2f %s", Calculator.min(yValues), mUnit)));
                section.addItem(new Item(mContext.getString(R.string.report_average), String.format(Locale.getDefault(),
                        "%.2f %s", carData.getAverageConsumption(), mUnit)));

                // Total volume metric
                float totalVolume = 0;
                for (BalancedRefueling refueling : balancedRefuelings) {
                    totalVolume += refueling.getVolume();
                }
                section.addItem(new Item(mContext.getString(R.string.report_total_volume),
                        String.format(Locale.getDefault(), "%.2f %s", totalVolume, prefsForGuess.getUnitVolume())));

                sectionAdded = true;
            }

            if (!sectionAdded) {
                Section section = addDataSection(car);
                section.addItem(new Item(mContext.getString(R.string.report_not_enough_data), ""));
            }
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
