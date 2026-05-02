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
import android.text.format.DateFormat;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
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

public class CostsReport extends AbstractReport {

    /**
     * 86400 seconds per day * 365,25 days per year = 31557600 seconds per year
     */
    private static final long YEAR_SECONDS = 31557600;

    /**
     * 86400 seconds per day * 30,4375 days per month = 2629800 seconds per month
     * (365,25 days per year means 365,25 / 12 = 30,4375 days per month)
     */
    private static final long MONTH_SECONDS = 2629800;

    /**
     * 60 seconds per minute * 60 minutes per hour * 24 hours per day = 86400 seconds per day
     */
    private static final long DAY_SECONDS = 86400;

    private class ReportChartData extends AbstractReportChartColumnData {
        private final int mOption;

        public ReportChartData(Context context, String carName, int carColor, int option) {
            super(context, carName, carColor);
            mOption = option;
        }

        public void add(ZonedDateTime date, float costs) {
            float x;
            if (mOption == GRAPH_OPTION_MONTH) {
                x = date.getYear() * 12 + date.getMonthValue() - 1;
            } else {
                x = date.getYear();
            }

            int index = indexOf(x);
            if (index == -1) {
                add(x, costs, makeTooltip(costs, x));
            } else {
                DataPoint dp = mDataPoints.get(index);
                dp.y += costs;
                dp.tooltip = makeTooltip(dp.y, x);
            }
        }

        private String makeTooltip(double costs, float dateValue) {
            return mContext.getString(R.string.report_toast_costs,
                    getName(),
                    costs,
                    mUnit,
                    formatXValue(dateValue, mOption));
        }
    }

    private static final int GRAPH_OPTION_MONTH = 0;
    private static final int GRAPH_OPTION_YEAR = 1;

    private final Map<Long, ReportChartData> mCostsPerMonth = new HashMap<>();
    private final Map<Long, ReportChartData> mCostsPerYear = new HashMap<>();
    private String mUnit;
    private final String[] mXLabelFormat = new String[2];

    public CostsReport(Context context) {
        super(context);
    }

    @Override
    public String formatXValue(float value, int chartOption) {
        try {
            if (chartOption == GRAPH_OPTION_MONTH) {
                int dateValue = (int) value;
                int year = dateValue / 12;
                int month = (dateValue % 12) + 1;
                LocalDate date = LocalDate.of(year, month, 1);
                return date.format(DateTimeFormatter.ofPattern(mXLabelFormat[chartOption]));
            } else {
                return String.valueOf((int) value);
            }
        } catch (Exception e) {
            return String.valueOf((int) value);
        }
    }

    @Override
    public String formatYValue(float value, int chartOption) {
        return String.format(Locale.getDefault(), "%.0f", value);
    }

    @Override
    public int[] getAvailableChartOptions() {
        int[] options = new int[2];
        options[GRAPH_OPTION_MONTH] = R.string.report_graph_month_history;
        options[GRAPH_OPTION_YEAR] = R.string.report_graph_year_history;
        return options;
    }

    @Override
    public String getTitle() {
        return mContext.getString(R.string.report_title_costs);
    }

    @Override
    public List<AbstractReportChartData> getRawChartData(int chartOption) {
        synchronized (mCachedChartData) {
            if (mCachedChartData.containsKey(chartOption)) {
                return mCachedChartData.get(chartOption);
            }

            List<AbstractReportChartData> data = new ArrayList<>();
            for (ReportChartData carData : (chartOption == GRAPH_OPTION_MONTH ? mCostsPerMonth : mCostsPerYear).values()) {
                if (!carData.isEmpty()) {
                    data.add(carData);
                }
            }
            mCachedChartData.put(chartOption, data);
            return data;
        }
    }

    @Override
    protected void onUpdate() {
        Preferences prefs = new Preferences(mContext);
        mUnit = prefs.getUnitCurrency();
        mCostsPerMonth.clear();
        mCostsPerYear.clear();

        setupXLabelFormats();

        AutuManduDatabase db = AutuManduDatabase.getInstance(mContext);

        // Carga masiva de todos los datos para optimizar el rendimiento (evitar N+1)
        List<Car> cars = db.getCarDao().getAll();
        Map<Long, List<RefuelingWithDetails>> refuelingsByCar = db.getRefuelingDao().getAllWithDetails()
                .stream().collect(Collectors.groupingBy(RefuelingWithDetails::carId));
        Map<Long, List<OtherCost>> otherCostsByCar = db.getOtherCostDao().getAll()
                .stream().collect(Collectors.groupingBy(OtherCost::getCarId));
        Map<Long, List<TireList>> tiresByCar = db.getTireDao().getAllTireLists()
                .stream().collect(Collectors.groupingBy(TireList::getCarId));

        for (Car car : cars) {
            processCar(car, refuelingsByCar, otherCostsByCar, tiresByCar, prefs);
        }
    }

    private void setupXLabelFormats() {
        String monthFormat = "MMM yyyy";
        String yearFormat = "yyyy";
        if (mContext.getResources().getConfiguration().smallestScreenWidthDp > 480) {
            monthFormat = "MMMM yyyy";
        }
        mXLabelFormat[GRAPH_OPTION_MONTH] = monthFormat;
        mXLabelFormat[GRAPH_OPTION_YEAR] = yearFormat;
    }

    private void processCar(Car car, Map<Long, List<RefuelingWithDetails>> refuelingsByCar,
                            Map<Long, List<OtherCost>> otherCostsByCar,
                            Map<Long, List<TireList>> tiresByCar, Preferences prefs) {
        Long carIdObj = car.getId();
        if (carIdObj == null) return;
        long carId = carIdObj;

        Section section = createSectionForCar(car);

        ReportChartData monthReportData = new ReportChartData(mContext, car.getName(), car.getColor(), GRAPH_OPTION_MONTH);
        ReportChartData yearReportData = new ReportChartData(mContext, car.getName(), car.getColor(), GRAPH_OPTION_YEAR);
        mCostsPerMonth.put(carId, monthReportData);
        mCostsPerYear.put(carId, yearReportData);

        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime lastYearDate = now.minusYears(1);
        CalculationContext context = new CalculationContext(car, monthReportData, yearReportData, now, lastYearDate);

        // Calculate all costs
        calculateFuelCosts(refuelingsByCar.get(carId), context, prefs);
        calculateOtherCosts(otherCostsByCar.get(carId), context);
        calculateTireCosts(tiresByCar.get(carId), context);

        if ((context.refuelingsCount + context.otherCostsCount + context.tiresCount) < 2) {
            section.addItem(new Item(mContext.getString(R.string.report_not_enough_data), ""));
            return;
        }

        addSummaryItems(section, context, prefs);
    }

    private Section createSectionForCar(Car car) {
        if (car.getSuspendedSince() != null) {
            return addDataSection(String.format("%s [%s]", car.getName(),
                    mContext.getString(R.string.suspended)), car.getColor(), 1);
        } else {
            return addDataSection(car.getName(), car.getColor());
        }
    }

    private void calculateFuelCosts(List<RefuelingWithDetails> carRefuelings, CalculationContext ctx, Preferences prefs) {
        if (carRefuelings == null) return;
        List<BalancedRefueling> refuelings = BalancedRefueling.balance(carRefuelings, prefs.isAutoGuessMissingDataEnabled(), false);
        ctx.refuelingsCount = refuelings.size();

        for (BalancedRefueling refueling : refuelings) {
            if (refueling.getPrice() == 0.0f) continue;
            ctx.totalCosts += refueling.getPrice();

            ZonedDateTime date = ZonedDateTime.ofInstant(refueling.getDate().toInstant(), ZoneId.systemDefault());
            ctx.monthData.add(date, refueling.getPrice());
            ctx.yearData.add(date, refueling.getPrice());

            if (date.isAfter(ctx.lastYearDate) && date.isBefore(ctx.now.plusSeconds(1))) {
                ctx.costsWithinYear += refueling.getPrice();
            }

            ctx.endMileage = Math.max(ctx.endMileage, refueling.getMileage());
            if (ctx.startDate.isAfter(date)) ctx.startDate = date;
        }
    }

    private void calculateOtherCosts(List<OtherCost> otherCosts, CalculationContext ctx) {
        if (otherCosts == null) return;
        ctx.otherCostsCount = otherCosts.size();

        for (OtherCost otherCost : otherCosts) {
            int recurrences = calculateRecurrences(otherCost);
            int recurrencesInLastYear = calculateRecurrencesInLastYear(otherCost, ctx);

            ctx.totalCosts += otherCost.getPrice() * recurrences;
            ctx.costsWithinYear += otherCost.getPrice() * recurrencesInLastYear;

            addOtherCostToCharts(otherCost, ctx);

            if (otherCost.getMileage() != null && otherCost.getMileage() > -1) {
                ctx.endMileage = Math.max(ctx.endMileage, otherCost.getMileage());
            }

            ZonedDateTime date = ZonedDateTime.ofInstant(otherCost.getDate().toInstant(), ZoneId.systemDefault());
            if (ctx.startDate.isAfter(date)) ctx.startDate = date;
        }
    }

    private int calculateRecurrences(OtherCost otherCost) {
        if (otherCost.getEndDate() == null || otherCost.getEndDate().after(new java.util.Date())) {
            return Recurrences.getRecurrencesSince(otherCost.getRecurrenceInterval(), otherCost.getRecurrenceMultiplier(), otherCost.getDate());
        } else {
            return Recurrences.getRecurrencesBetween(otherCost.getRecurrenceInterval(), otherCost.getRecurrenceMultiplier(), otherCost.getDate(), otherCost.getEndDate());
        }
    }

    private int calculateRecurrencesInLastYear(OtherCost otherCost, CalculationContext ctx) {
        if (otherCost.getEndDate() == null || otherCost.getEndDate().after(new java.util.Date())) {
            return Recurrences.getRecurrencesBetween(otherCost.getRecurrenceInterval(), otherCost.getRecurrenceMultiplier(), otherCost.getDate(), java.util.Date.from(ctx.now.toInstant()), java.util.Date.from(ctx.lastYearDate.toInstant()), java.util.Date.from(ctx.now.toInstant()));
        } else {
            return Recurrences.getRecurrencesBetween(otherCost.getRecurrenceInterval(), otherCost.getRecurrenceMultiplier(), otherCost.getDate(), otherCost.getEndDate(), java.util.Date.from(ctx.lastYearDate.toInstant()), otherCost.getEndDate());
        }
    }

    private void addOtherCostToCharts(OtherCost otherCost, CalculationContext ctx) {
        ZonedDateTime date = ZonedDateTime.ofInstant(otherCost.getDate().toInstant(), ZoneId.systemDefault());
        ZonedDateTime endDate = (otherCost.getEndDate() != null && ctx.endDate.isAfter(ZonedDateTime.ofInstant(otherCost.getEndDate().toInstant(), ZoneId.systemDefault())))
                ? ZonedDateTime.ofInstant(otherCost.getEndDate().toInstant(), ZoneId.systemDefault()) : ctx.endDate;

        while (date.isBefore(endDate)) {
            ctx.monthData.add(date, otherCost.getPrice());
            ctx.yearData.add(date, otherCost.getPrice());
            switch (otherCost.getRecurrenceInterval()) {
                case ONCE -> date = ZonedDateTime.now().plusYears(100);
                case DAY -> date = date.plusDays(otherCost.getRecurrenceMultiplier());
                case MONTH -> date = date.plusMonths(otherCost.getRecurrenceMultiplier());
                case QUARTER -> date = date.plusMonths(otherCost.getRecurrenceMultiplier() * 3L);
                case YEAR -> date = date.plusYears(otherCost.getRecurrenceMultiplier());
            }
        }
    }

    private void calculateTireCosts(List<TireList> tireLists, CalculationContext ctx) {
        if (tireLists == null) return;
        ctx.tiresCount = tireLists.size();

        for (TireList tireList : tireLists) {
            if (tireList.getPrice() == 0.0f) continue;
            ctx.totalCosts += tireList.getPrice();

            ZonedDateTime date = ZonedDateTime.ofInstant(tireList.getBuyDate().toInstant(), ZoneId.systemDefault());
            ctx.monthData.add(date, tireList.getPrice());
            ctx.yearData.add(date, tireList.getPrice());

            if (date.isAfter(ctx.lastYearDate) && date.isBefore(ctx.now.plusSeconds(1))) {
                ctx.costsWithinYear += tireList.getPrice();
            }

            if (ctx.startDate.isAfter(date)) ctx.startDate = date;
        }
    }

    private void addSummaryItems(Section section, CalculationContext ctx, Preferences prefs) {
        long elapsedSeconds = ChronoUnit.SECONDS.between(ctx.startDate, ctx.endDate);
        double costsPerSecond = ctx.totalCosts / Math.max(1L, elapsedSeconds);

        section.addItem(new Item("Ø " + mContext.getString(R.string.report_day),
                mContext.getString((elapsedSeconds > DAY_SECONDS ? R.string.report_price : R.string.report_price_estimated),
                        costsPerSecond * DAY_SECONDS, mUnit)));

        section.addItem(new Item("Ø " + mContext.getString(R.string.report_month),
                mContext.getString((elapsedSeconds > MONTH_SECONDS ? R.string.report_price : R.string.report_price_estimated),
                        costsPerSecond * MONTH_SECONDS, mUnit)));

        section.addItem(new Item("Ø " + mContext.getString(R.string.report_year),
                mContext.getString((elapsedSeconds > YEAR_SECONDS ? R.string.report_price : R.string.report_price_estimated),
                        costsPerSecond * YEAR_SECONDS, mUnit)));

        int mileageDiff = Math.max(1, ctx.endMileage - ctx.startMileage);
        section.addItem(new Item("Ø " + prefs.getUnitDistance(),
                mContext.getString(R.string.report_price, ctx.totalCosts / mileageDiff, mUnit)));

        section.addItem(new Item(mContext.getString(R.string.report_last_year),
                mContext.getString(R.string.report_price, ctx.costsWithinYear, mUnit)));

        section.addItem(new Item(mContext.getString(R.string.report_since,
                DateFormat.getDateFormat(mContext).format(java.util.Date.from(ctx.startDate.toInstant()))),
                mContext.getString(R.string.report_price, ctx.totalCosts, mUnit)));
    }

    private static class CalculationContext {
        final int startMileage;
        int endMileage = Integer.MIN_VALUE;
        ZonedDateTime startDate = ZonedDateTime.now();
        final ZonedDateTime endDate;
        final ZonedDateTime now;
        final ZonedDateTime lastYearDate;
        final ReportChartData monthData;
        final ReportChartData yearData;

        double totalCosts = 0;
        double costsWithinYear = 0;
        int refuelingsCount = 0;
        int otherCostsCount = 0;
        int tiresCount = 0;

        CalculationContext(Car car, ReportChartData monthData, ReportChartData yearData, ZonedDateTime now, ZonedDateTime lastYearDate) {
            this.startMileage = car.getInitialMileage();
            this.now = now;
            this.lastYearDate = lastYearDate;
            this.monthData = monthData;
            this.yearData = yearData;
            this.endDate = (car.getSuspendedSince() != null) ?
                    ZonedDateTime.ofInstant(car.getSuspendedSince().toInstant(), ZoneId.systemDefault()) : now;
        }
    }
}
