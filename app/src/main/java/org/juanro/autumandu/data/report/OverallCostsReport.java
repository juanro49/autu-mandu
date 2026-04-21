/*
 * Copyright 2026 Juanro49
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

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.juanro.autumandu.Preferences;
import org.juanro.autumandu.R;
import org.juanro.autumandu.model.AutuManduDatabase;
import org.juanro.autumandu.model.dto.BalancedRefueling;
import org.juanro.autumandu.model.dto.RefuelingWithDetails;
import org.juanro.autumandu.model.entity.Car;
import org.juanro.autumandu.model.entity.OtherCost;
import org.juanro.autumandu.model.entity.TireList;
import org.juanro.autumandu.util.Recurrences;

public class OverallCostsReport extends AbstractReport {

    private final List<AbstractReportChartData> mChartData = new ArrayList<>();

    public OverallCostsReport(Context context) {
        super(context);
    }

    @Override
    public int[] getAvailableChartOptions() {
        return new int[]{
                R.string.report_chart_option_pie,
                R.string.report_chart_option_donut
        };
    }

    @Override
    public String getTitle() {
        return mContext.getString(R.string.report_title_overall_costs);
    }

    @Override
    public String formatXValue(float value, int chartOption) {
        return "";
    }

    @Override
    public String formatYValue(float value, int chartOption) {
        return String.format(Locale.getDefault(), "%.1f%%", value);
    }

    @Override
    public List<AbstractReportChartData> getRawChartData(int chartOption) {
        synchronized (mCachedChartData) {
            if (mCachedChartData.containsKey(chartOption)) {
                return mCachedChartData.get(chartOption);
            }
            List<AbstractReportChartData> data = new ArrayList<>(mChartData);
            mCachedChartData.put(chartOption, data);
            return data;
        }
    }

    @Override
    protected void onUpdate() {
        Preferences prefs = new Preferences(mContext);
        String mUnit = prefs.getUnitCurrency();
        String mDistanceUnit = prefs.getUnitDistance();
        mChartData.clear();

        AutuManduDatabase db = AutuManduDatabase.getInstance(mContext);

        List<Car> cars = db.getCarDao().getAll();
        Map<Long, List<RefuelingWithDetails>> refuelingsByCar = db.getRefuelingDao().getAllWithDetails()
                .stream().collect(Collectors.groupingBy(RefuelingWithDetails::carId));
        Map<Long, List<OtherCost>> otherCostsByCar = db.getOtherCostDao().getAll()
                .stream().collect(Collectors.groupingBy(OtherCost::getCarId));
        Map<Long, List<TireList>> tiresByCar = db.getTireDao().getAllTireLists()
                .stream().collect(Collectors.groupingBy(TireList::getCarId));

        double globalFuelCosts = 0;
        double globalBillsCosts = 0;
        double globalTiresCosts = 0;
        double globalInvestmentCosts = 0;
        double globalIncomeTotal = 0;

        for (Car car : cars) {
            Long carIdObj = car.getId();
            if (carIdObj == null) continue;
            long carId = carIdObj;

            Section section;
            if (car.getSuspendedSince() != null) {
                section = addDataSection(String.format("%s [%s]", car.getName(),
                        mContext.getString(R.string.suspended)), car.getColor(), 1);
            } else {
                section = addDataSection(car.getName(), car.getColor());
            }

            double fuelCosts = 0;
            double billsCosts = 0;
            double tiresCosts = 0;
            double investmentCosts = car.getBuyingPrice();
            double incomeTotal = 0;

            List<RefuelingWithDetails> carRefuelings = refuelingsByCar.get(carId);
            if (carRefuelings == null) carRefuelings = Collections.emptyList();
            List<BalancedRefueling> balancedRefuelings = BalancedRefueling.balance(carRefuelings, prefs.isAutoGuessMissingDataEnabled(), false);

            for (BalancedRefueling refueling : balancedRefuelings) {
                fuelCosts += refueling.getPrice();
            }

            List<OtherCost> otherCosts = otherCostsByCar.get(carId);
            if (otherCosts != null) {
                for (OtherCost otherCost : otherCosts) {
                    int recurrences;
                    if (otherCost.getEndDate() == null || otherCost.getEndDate().after(new Date())) {
                        recurrences = Recurrences.getRecurrencesSince(
                                otherCost.getRecurrenceInterval(),
                                otherCost.getRecurrenceMultiplier(),
                                otherCost.getDate());
                    } else {
                        recurrences = Recurrences.getRecurrencesBetween(
                                otherCost.getRecurrenceInterval(),
                                otherCost.getRecurrenceMultiplier(),
                                otherCost.getDate(),
                                otherCost.getEndDate());
                    }

                    if (otherCost.getPrice() > 0) {
                        billsCosts += otherCost.getPrice() * recurrences;
                    } else {
                        incomeTotal += Math.abs(otherCost.getPrice()) * recurrences;
                    }
                }
            }

            List<TireList> tireLists = tiresByCar.get(carId);
            if (tireLists != null) {
                for (TireList tireList : tireLists) {
                    tiresCosts += tireList.getPrice();
                }
            }

            globalFuelCosts += fuelCosts;
            globalBillsCosts += billsCosts;
            globalTiresCosts += tiresCosts;
            globalInvestmentCosts += investmentCosts;
            globalIncomeTotal += incomeTotal;

            double totalCosts = fuelCosts + billsCosts + tiresCosts + investmentCosts;
            if (totalCosts == 0 && balancedRefuelings.isEmpty()) {
                section.addItem(new Item(mContext.getString(R.string.report_not_enough_data), ""));
                continue;
            }

            section.addItem(new Item(mContext.getString(R.string.report_overall_costs_fuel),
                    mContext.getString(R.string.report_price, fuelCosts, mUnit)));
            section.addItem(new Item(mContext.getString(R.string.report_overall_costs_bills),
                    mContext.getString(R.string.report_price, billsCosts, mUnit)));
            section.addItem(new Item(mContext.getString(R.string.report_overall_costs_tires),
                    mContext.getString(R.string.report_price, tiresCosts, mUnit)));
            section.addItem(new Item(mContext.getString(R.string.report_overall_costs_investment),
                    mContext.getString(R.string.report_price, investmentCosts, mUnit)));
            section.addItem(new Item(mContext.getString(R.string.report_overall_costs_income),
                    mContext.getString(R.string.report_price, incomeTotal, mUnit)));

            // Efficiency metrics - Cost per Distance
            int endMileage = car.getInitialMileage();
            ZonedDateTime startDate = (car.getSuspendedSince() != null) ?
                    ZonedDateTime.ofInstant(car.getSuspendedSince().toInstant(), ZoneId.systemDefault()) :
                    ZonedDateTime.now();

            if (!balancedRefuelings.isEmpty()) {
                endMileage = Math.max(endMileage, balancedRefuelings.get(balancedRefuelings.size() - 1).getMileage());
                if (balancedRefuelings.get(0).getDate().getTime() < startDate.toInstant().toEpochMilli()) {
                    startDate = ZonedDateTime.ofInstant(balancedRefuelings.get(0).getDate().toInstant(), ZoneId.systemDefault());
                }
            }

            if (otherCosts != null) {
                for (OtherCost otherCost : otherCosts) {
                    if (otherCost.getMileage() != null && otherCost.getMileage() > -1) {
                        endMileage = Math.max(endMileage, otherCost.getMileage());
                    }
                    if (otherCost.getDate().getTime() < startDate.toInstant().toEpochMilli()) {
                        startDate = ZonedDateTime.ofInstant(otherCost.getDate().toInstant(), ZoneId.systemDefault());
                    }
                }
            }

            int totalDistance = endMileage - car.getInitialMileage();
            if (totalDistance > 0) {
                section.addItem(new Item(mContext.getString(R.string.report_average) + " " + mDistanceUnit,
                        mContext.getString(R.string.report_price, totalCosts / totalDistance * 100.0, mUnit) + "/100 " + mDistanceUnit));
                section.addItem(new Item("  - " + mContext.getString(R.string.report_overall_costs_fuel),
                        mContext.getString(R.string.report_price, fuelCosts / totalDistance * 100.0, mUnit) + "/100 " + mDistanceUnit));
                section.addItem(new Item("  - " + mContext.getString(R.string.report_overall_costs_bills),
                        mContext.getString(R.string.report_price, billsCosts / totalDistance * 100.0, mUnit) + "/100 " + mDistanceUnit));
                section.addItem(new Item("  - " + mContext.getString(R.string.report_overall_costs_tires),
                        mContext.getString(R.string.report_price, tiresCosts / totalDistance * 100.0, mUnit) + "/100 " + mDistanceUnit));
                section.addItem(new Item("  - " + mContext.getString(R.string.report_overall_costs_investment),
                        mContext.getString(R.string.report_price, investmentCosts / totalDistance * 100.0, mUnit) + "/100 " + mDistanceUnit));
            }

            // Cost per Time
            long days = Math.max(1, ChronoUnit.DAYS.between(startDate, ZonedDateTime.now()));
            section.addItem(new Item(mContext.getString(R.string.report_average) + " " + mContext.getString(R.string.report_month),
                    mContext.getString(R.string.report_price, totalCosts / days * 30.4375, mUnit)));
            section.addItem(new Item("  - " + mContext.getString(R.string.report_overall_costs_fuel),
                    mContext.getString(R.string.report_price, fuelCosts / days * 30.4375, mUnit)));
            section.addItem(new Item("  - " + mContext.getString(R.string.report_overall_costs_bills),
                    mContext.getString(R.string.report_price, billsCosts / days * 30.4375, mUnit)));
            section.addItem(new Item("  - " + mContext.getString(R.string.report_overall_costs_tires),
                    mContext.getString(R.string.report_price, tiresCosts / days * 30.4375, mUnit)));
            section.addItem(new Item("  - " + mContext.getString(R.string.report_overall_costs_investment),
                    mContext.getString(R.string.report_price, investmentCosts / days * 30.4375, mUnit)));

            section.addItem(new Item(mContext.getString(R.string.report_average) + " " + mContext.getString(R.string.report_day),
                    mContext.getString(R.string.report_price, totalCosts / days, mUnit)));
            section.addItem(new Item("  - " + mContext.getString(R.string.report_overall_costs_fuel),
                    mContext.getString(R.string.report_price, fuelCosts / days, mUnit)));
            section.addItem(new Item("  - " + mContext.getString(R.string.report_overall_costs_bills),
                    mContext.getString(R.string.report_price, billsCosts / days, mUnit)));
            section.addItem(new Item("  - " + mContext.getString(R.string.report_overall_costs_tires),
                    mContext.getString(R.string.report_price, tiresCosts / days, mUnit)));
            section.addItem(new Item("  - " + mContext.getString(R.string.report_overall_costs_investment),
                    mContext.getString(R.string.report_price, investmentCosts / days, mUnit)));
        }

        // Add categorical data to chart based on global totals
        ReportChartData globalChartData = new ReportChartData(mContext, mContext.getString(R.string.report_title_overall_costs), -1);
        double globalTotal = globalFuelCosts + globalBillsCosts + globalTiresCosts + globalInvestmentCosts + globalIncomeTotal;
        if (globalTotal > 0) {
            globalChartData.add(0f, (float) (globalFuelCosts / globalTotal * 100), mContext.getString(R.string.report_overall_costs_fuel));
            globalChartData.add(1f, (float) (globalBillsCosts / globalTotal * 100), mContext.getString(R.string.report_overall_costs_bills));
            globalChartData.add(2f, (float) (globalTiresCosts / globalTotal * 100), mContext.getString(R.string.report_overall_costs_tires));
            globalChartData.add(3f, (float) (globalInvestmentCosts / globalTotal * 100), mContext.getString(R.string.report_overall_costs_investment));
            globalChartData.add(4f, (float) (globalIncomeTotal / globalTotal * 100), mContext.getString(R.string.report_overall_costs_income));
            mChartData.add(globalChartData);
        }
    }

    private static class ReportChartData extends AbstractReportChartColumnData {
        public ReportChartData(Context context, String name, int color) {
            super(context, name, color);
        }

        @Override
        protected void add(Float x, Float y, String tooltip) {
            super.add(x, y, tooltip);
        }
    }
}
