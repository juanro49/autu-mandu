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

    private static final String COST_PER_100_UNIT_FORMAT = "/100 ";
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
        mChartData.clear();

        AutuManduDatabase db = AutuManduDatabase.getInstance(mContext);

        List<Car> cars = db.getCarDao().getAll();
        Map<Long, List<RefuelingWithDetails>> refuelingsByCar = db.getRefuelingDao().getAllWithDetails()
                .stream().collect(Collectors.groupingBy(RefuelingWithDetails::carId));
        Map<Long, List<OtherCost>> otherCostsByCar = db.getOtherCostDao().getAll()
                .stream().collect(Collectors.groupingBy(OtherCost::getCarId));
        Map<Long, List<TireList>> tiresByCar = db.getTireDao().getAllTireLists()
                .stream().collect(Collectors.groupingBy(TireList::getCarId));

        CarDataMaps dataMaps = new CarDataMaps(refuelingsByCar, otherCostsByCar, tiresByCar);
        GlobalCosts globalCosts = new GlobalCosts();

        for (Car car : cars) {
            processCar(car, dataMaps, globalCosts, prefs);
        }

        // Add categorical data to chart based on global totals
        ReportChartData globalChartData = new ReportChartData(mContext, mContext.getString(R.string.report_title_overall_costs), -1);
        double globalTotal = globalCosts.fuel + globalCosts.bills + globalCosts.tires + globalCosts.investment + globalCosts.income;
        if (globalTotal > 0) {
            globalChartData.add(0f, (float) (globalCosts.fuel / globalTotal * 100), mContext.getString(R.string.report_overall_costs_fuel));
            globalChartData.add(1f, (float) (globalCosts.bills / globalTotal * 100), mContext.getString(R.string.report_overall_costs_bills));
            globalChartData.add(2f, (float) (globalCosts.tires / globalTotal * 100), mContext.getString(R.string.report_overall_costs_tires));
            globalChartData.add(3f, (float) (globalCosts.investment / globalTotal * 100), mContext.getString(R.string.report_overall_costs_investment));
            globalChartData.add(4f, (float) (globalCosts.income / globalTotal * 100), mContext.getString(R.string.report_overall_costs_income));
            mChartData.add(globalChartData);
        }
    }

    private void processCar(Car car, CarDataMaps dataMaps, GlobalCosts globalCosts, Preferences prefs) {
        Long carIdObj = car.getId();
        if (carIdObj == null) return;
        long carId = carIdObj;

        Section section = createSectionForCar(car);
        CarCosts costs = calculateCarCosts(car, carId, dataMaps, prefs);

        globalCosts.fuel += costs.fuel;
        globalCosts.bills += costs.bills;
        globalCosts.tires += costs.tires;
        globalCosts.investment += costs.investment;
        globalCosts.income += costs.income;

        if (costs.total == 0 && costs.refuelingsCount == 0) {
            section.addItem(new Item(mContext.getString(R.string.report_not_enough_data), ""));
            return;
        }

        addSummaryItems(section, costs.fuel, costs.bills, costs.tires, costs.investment, costs.income, prefs.getUnitCurrency());

        var timeBounds = calculateTimeBounds(car, costs.refuelings, dataMaps.otherCosts.get(carId));
        EfficiencyContext context = new EfficiencyContext(costs.total, costs.fuel, costs.bills,
                costs.tires, costs.investment, car.getInitialMileage(), timeBounds.endMileage, timeBounds.startDate);
        addEfficiencyItems(section, context, prefs);
    }

    private CarCosts calculateCarCosts(Car car, long carId, CarDataMaps dataMaps, Preferences prefs) {
        CarCosts costs = new CarCosts();
        costs.investment = car.getBuyingPrice();

        costs.refuelings = BalancedRefueling.balance(dataMaps.refuelings.getOrDefault(carId, Collections.emptyList()),
                prefs.isAutoGuessMissingDataEnabled(), false);
        costs.refuelingsCount = costs.refuelings.size();
        for (BalancedRefueling refueling : costs.refuelings) {
            costs.fuel += refueling.getPrice();
        }

        List<OtherCost> otherCosts = dataMaps.otherCosts.get(carId);
        if (otherCosts != null) {
            for (OtherCost otherCost : otherCosts) {
                int recurrences = calculateRecurrences(otherCost);
                if (otherCost.getPrice() > 0) {
                    costs.bills += otherCost.getPrice() * recurrences;
                } else {
                    costs.income += Math.abs(otherCost.getPrice()) * recurrences;
                }
            }
        }

        List<TireList> tireLists = dataMaps.tires.get(carId);
        if (tireLists != null) {
            for (TireList tireList : tireLists) {
                costs.tires += tireList.getPrice();
            }
        }

        costs.total = costs.fuel + costs.bills + costs.tires + costs.investment;
        return costs;
    }

    private TimeBounds calculateTimeBounds(Car car, List<BalancedRefueling> balancedRefuelings, List<OtherCost> otherCosts) {
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
        return new TimeBounds(endMileage, startDate);
    }

    private static class CarCosts {
        double fuel = 0;
        double bills = 0;
        double tires = 0;
        double investment = 0;
        double income = 0;
        double total = 0;
        int refuelingsCount = 0;
        List<BalancedRefueling> refuelings;
    }

    private record TimeBounds(int endMileage, ZonedDateTime startDate) {}

    private int calculateRecurrences(OtherCost otherCost) {
        if (otherCost.getEndDate() == null || otherCost.getEndDate().after(new java.util.Date())) {
            return Recurrences.getRecurrencesSince(
                    otherCost.getRecurrenceInterval(),
                    otherCost.getRecurrenceMultiplier(),
                    otherCost.getDate());
        } else {
            return Recurrences.getRecurrencesBetween(
                    otherCost.getRecurrenceInterval(),
                    otherCost.getRecurrenceMultiplier(),
                    otherCost.getDate(),
                    otherCost.getEndDate());
        }
    }

    private void addSummaryItems(Section section, double fuel, double bills, double tires, double investment, double income, String unit) {
        section.addItem(new Item(mContext.getString(R.string.report_overall_costs_fuel),
                mContext.getString(R.string.report_price, fuel, unit)));
        section.addItem(new Item(mContext.getString(R.string.report_overall_costs_bills),
                mContext.getString(R.string.report_price, bills, unit)));
        section.addItem(new Item(mContext.getString(R.string.report_overall_costs_tires),
                mContext.getString(R.string.report_price, tires, unit)));
        section.addItem(new Item(mContext.getString(R.string.report_overall_costs_investment),
                mContext.getString(R.string.report_price, investment, unit)));
        section.addItem(new Item(mContext.getString(R.string.report_overall_costs_income),
                mContext.getString(R.string.report_price, income, unit)));
    }

    private void addEfficiencyItems(Section section, EfficiencyContext ctx, Preferences prefs) {
        addDistanceEfficiencyItems(section, ctx, prefs);
        addTimeEfficiencyItems(section, ctx, prefs);
    }

    private void addDistanceEfficiencyItems(Section section, EfficiencyContext ctx, Preferences prefs) {
        String unit = prefs.getUnitCurrency();
        String distanceUnit = prefs.getUnitDistance();
        int totalDistance = ctx.endMileage() - ctx.startMileage();
        if (totalDistance > 0) {
            section.addItem(new Item(mContext.getString(R.string.report_average) + " " + distanceUnit,
                    mContext.getString(R.string.report_price, ctx.totalCosts() / totalDistance * 100.0, unit) + COST_PER_100_UNIT_FORMAT + distanceUnit));
            section.addItem(new Item("  - " + mContext.getString(R.string.report_overall_costs_fuel),
                    mContext.getString(R.string.report_price, ctx.fuelCosts() / totalDistance * 100.0, unit) + COST_PER_100_UNIT_FORMAT + distanceUnit));
            section.addItem(new Item("  - " + mContext.getString(R.string.report_overall_costs_bills),
                    mContext.getString(R.string.report_price, ctx.billsCosts() / totalDistance * 100.0, unit) + COST_PER_100_UNIT_FORMAT + distanceUnit));
            section.addItem(new Item("  - " + mContext.getString(R.string.report_overall_costs_tires),
                    mContext.getString(R.string.report_price, ctx.tiresCosts() / totalDistance * 100.0, unit) + COST_PER_100_UNIT_FORMAT + distanceUnit));
            section.addItem(new Item("  - " + mContext.getString(R.string.report_overall_costs_investment),
                    mContext.getString(R.string.report_price, ctx.investmentCosts() / totalDistance * 100.0, unit) + COST_PER_100_UNIT_FORMAT + distanceUnit));
        }
    }

    private void addTimeEfficiencyItems(Section section, EfficiencyContext ctx, Preferences prefs) {
        String unit = prefs.getUnitCurrency();
        long days = Math.max(1, ChronoUnit.DAYS.between(ctx.startDate(), ZonedDateTime.now()));

        section.addItem(new Item(mContext.getString(R.string.report_average) + " " + mContext.getString(R.string.report_month),
                mContext.getString(R.string.report_price, ctx.totalCosts() / days * 30.4375, unit)));
        section.addItem(new Item("  - " + mContext.getString(R.string.report_overall_costs_fuel),
                mContext.getString(R.string.report_price, ctx.fuelCosts() / days * 30.4375, unit)));
        section.addItem(new Item("  - " + mContext.getString(R.string.report_overall_costs_bills),
                mContext.getString(R.string.report_price, ctx.billsCosts() / days * 30.4375, unit)));
        section.addItem(new Item("  - " + mContext.getString(R.string.report_overall_costs_tires),
                mContext.getString(R.string.report_price, ctx.tiresCosts() / days * 30.4375, unit)));
        section.addItem(new Item("  - " + mContext.getString(R.string.report_overall_costs_investment),
                mContext.getString(R.string.report_price, ctx.investmentCosts() / days * 30.4375, unit)));

        section.addItem(new Item(mContext.getString(R.string.report_average) + " " + mContext.getString(R.string.report_day),
                mContext.getString(R.string.report_price, ctx.totalCosts() / days, unit)));
        section.addItem(new Item("  - " + mContext.getString(R.string.report_overall_costs_fuel),
                mContext.getString(R.string.report_price, ctx.fuelCosts() / days, unit)));
        section.addItem(new Item("  - " + mContext.getString(R.string.report_overall_costs_bills),
                mContext.getString(R.string.report_price, ctx.billsCosts() / days, unit)));
        section.addItem(new Item("  - " + mContext.getString(R.string.report_overall_costs_tires),
                mContext.getString(R.string.report_price, ctx.tiresCosts() / days, unit)));
        section.addItem(new Item("  - " + mContext.getString(R.string.report_overall_costs_investment),
                mContext.getString(R.string.report_price, ctx.investmentCosts() / days, unit)));
    }

    private static class GlobalCosts {
        double fuel = 0;
        double bills = 0;
        double tires = 0;
        double investment = 0;
        double income = 0;
    }

    private record CarDataMaps(
            Map<Long, List<RefuelingWithDetails>> refuelings,
            Map<Long, List<OtherCost>> otherCosts,
            Map<Long, List<TireList>> tires
    ) {}

    private record EfficiencyContext(
            double totalCosts,
            double fuelCosts,
            double billsCosts,
            double tiresCosts,
            double investmentCosts,
            int startMileage,
            int endMileage,
            ZonedDateTime startDate
    ) {}

    private Section createSectionForCar(Car car) {
        if (car.getSuspendedSince() != null) {
            return addDataSection(String.format("%s [%s]", car.getName(),
                    mContext.getString(R.string.suspended)), car.getColor(), 1);
        } else {
            return addDataSection(car.getName(), car.getColor());
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
