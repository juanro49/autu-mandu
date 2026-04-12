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

import org.joda.time.DateTime;
import org.joda.time.Seconds;

import java.util.ArrayList;
import java.util.Collections;
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

        public void add(DateTime date, float costs) {
            float dateValue;
            if (mOption == GRAPH_OPTION_MONTH) {
                dateValue = date.getYear() * 100 + date.getMonthOfYear();
            } else {
                dateValue = date.getYear() * 100 + 1;
            }

            int index = indexOf(dateValue);
            if (index == -1) {
                add(dateValue, costs, makeTooltip(costs, dateValue));
            } else {
                DataPoint dp = mDataPoints.get(index);
                dp.y += costs;
                dp.tooltip = makeTooltip(dp.y, dateValue);
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
    protected String formatXValue(float value, int chartOption) {
        int dateValue = (int) value;
        int year = dateValue / 100;
        int month = dateValue % 100;
        DateTime date = new DateTime(year, month, 1, 0, 0);
        return date.toString(mXLabelFormat[chartOption]);
    }

    @Override
    protected String formatYValue(float value, int chartOption) {
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
    protected List<AbstractReportChartData> getRawChartData(int chartOption) {
        List<AbstractReportChartData> data = new ArrayList<>(mCostsPerMonth.size());
        for (ReportChartData carData : (chartOption == GRAPH_OPTION_MONTH ? mCostsPerMonth : mCostsPerYear).values()) {
            if (!carData.isEmpty()) {
                data.add(carData);
            }
        }
        return data;
    }

    @Override
    protected void onUpdate() {
        Preferences prefs = new Preferences(mContext);
        mUnit = prefs.getUnitCurrency();
        mCostsPerMonth.clear();
        mCostsPerYear.clear();

        // Settings, which are based on the screen size.
        String monthFormat = "MMM yyyy";
        String yearFormat = "yyyy";
        if (mContext.getResources().getConfiguration().smallestScreenWidthDp > 480) {
            monthFormat = "MMMM yyyy";
        }
        mXLabelFormat[GRAPH_OPTION_MONTH] = monthFormat;
        mXLabelFormat[GRAPH_OPTION_YEAR] = yearFormat;

        AutuManduDatabase db = AutuManduDatabase.getInstance(mContext);

        // Bulk load all data to optimize performance (N+1 avoidance)
        List<Car> cars = db.getCarDao().getAll();
        Map<Long, List<RefuelingWithDetails>> refuelingsByCar = db.getRefuelingDao().getAllWithDetails()
                .stream().collect(Collectors.groupingBy(RefuelingWithDetails::carId));
        Map<Long, List<OtherCost>> otherCostsByCar = db.getOtherCostDao().getAll()
                .stream().collect(Collectors.groupingBy(OtherCost::getCarId));
        Map<Long, List<TireList>> tiresByCar = db.getTireDao().getAllTireLists()
                .stream().collect(Collectors.groupingBy(TireList::getCarId));

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

            ReportChartData monthReportData = new ReportChartData(mContext, car.getName(),
                    car.getColor(), GRAPH_OPTION_MONTH);
            ReportChartData yearReportData = new ReportChartData(mContext, car.getName(),
                    car.getColor(), GRAPH_OPTION_YEAR);
            mCostsPerMonth.put(carId, monthReportData);
            mCostsPerYear.put(carId, yearReportData);

            final int startMileage = car.getInitialMileage();
            int endMileage = Integer.MIN_VALUE;
            DateTime startDate = new DateTime();
            DateTime endDate = (car.getSuspendedSince() != null) ? new DateTime(car.getSuspendedSince()) : new DateTime();

            // Use balancer on pre-loaded refuelings
            List<RefuelingWithDetails> carRefuelings = refuelingsByCar.get(carId);
            if (carRefuelings == null) carRefuelings = Collections.emptyList();

            Preferences prefsForGuess = new Preferences(mContext);
            List<BalancedRefueling> refuelings = BalancedRefueling.balance(carRefuelings, prefsForGuess.isAutoGuessMissingDataEnabled(), false);

            List<OtherCost> otherCosts = otherCostsByCar.get(carId);
            if (otherCosts == null) otherCosts = Collections.emptyList();

            List<TireList> tireLists = tiresByCar.get(carId);
            if (tireLists == null) tireLists = Collections.emptyList();

            if ((refuelings.size() + otherCosts.size() + tireLists.size()) < 2) {
                section.addItem(new Item(mContext.getString(R.string.report_not_enough_data), ""));
                continue;
            }

            double totalCosts = 0;
            double costsWithinYear = 0;

            DateTime now = DateTime.now();
            DateTime lastYearDate = now.minusYears(1);

            for (BalancedRefueling refueling : refuelings) {
                if (refueling.getPrice() == 0.0f) {
                    continue;
                }
                totalCosts += refueling.getPrice();

                DateTime date = new DateTime(refueling.getDate());
                monthReportData.add(date, refueling.getPrice());
                yearReportData.add(date, refueling.getPrice());

                if (date.isAfter(lastYearDate) && date.isBefore(now.plusSeconds(1))) {
                    costsWithinYear += refueling.getPrice();
                }

                endMileage = Math.max(endMileage, refueling.getMileage());
                if (startDate.isAfter(date)) {
                    startDate = date;
                }
            }

            for (OtherCost otherCost : otherCosts) {
                int recurrences;
                int recurrencesInLastYear;

                if (otherCost.getEndDate() == null ||
                        new DateTime(otherCost.getEndDate()).isAfterNow()) {
                    recurrences = Recurrences.getRecurrencesSince(
                            otherCost.getRecurrenceInterval(),
                            otherCost.getRecurrenceMultiplier(),
                            otherCost.getDate());

                    // Calculamos recurrencias desde el inicio del gasto hasta ahora,
                    // y le restamos las que hubo antes de que empezara el "último año".
                    int totalRecurrences = Recurrences.getRecurrencesSince(
                            otherCost.getRecurrenceInterval(),
                            otherCost.getRecurrenceMultiplier(),
                            otherCost.getDate());
                    int recurrencesBeforeLastYear = Recurrences.getRecurrencesSince(
                            otherCost.getRecurrenceInterval(),
                            otherCost.getRecurrenceMultiplier(),
                            otherCost.getDate(),
                            lastYearDate.toDate());

                    // Si el gasto empezó hace menos de un año, recurrencesBeforeLastYear será 0 o similar
                    // Depende de la implementación de Recurrences, pero lo más fiable es:
                    // Vamos a simplificarlo a lo que Recurrences espera:
                    recurrencesInLastYear = Recurrences.getRecurrencesBetween(
                            otherCost.getRecurrenceInterval(),
                            otherCost.getRecurrenceMultiplier(),
                            otherCost.getDate(),
                            now.toDate(),
                            lastYearDate.toDate(),
                            now.toDate());
                } else {
                    recurrences = Recurrences.getRecurrencesBetween(
                            otherCost.getRecurrenceInterval(),
                            otherCost.getRecurrenceMultiplier(),
                            otherCost.getDate(),
                            otherCost.getEndDate());
                    recurrencesInLastYear = Recurrences.getRecurrencesBetween(
                            otherCost.getRecurrenceInterval(),
                            otherCost.getRecurrenceMultiplier(),
                            otherCost.getDate(),
                            otherCost.getEndDate(),
                            lastYearDate.toDate(),
                            otherCost.getEndDate());
                }

                totalCosts += otherCost.getPrice() * recurrences;
                costsWithinYear += otherCost.getPrice() * recurrencesInLastYear;

                DateTime date = new DateTime(otherCost.getDate());
                DateTime recurrenceEndDate = (otherCost.getEndDate() != null && endDate.isAfter(otherCost.getEndDate().getTime()))
                        ? new DateTime(otherCost.getEndDate()) : endDate;

                while (date.isBefore(recurrenceEndDate)) {
                    monthReportData.add(date, otherCost.getPrice());
                    yearReportData.add(date, otherCost.getPrice());
                    switch (otherCost.getRecurrenceInterval()) {
                        case ONCE -> date = DateTime.now().plusYears(100); // Terminate loop
                        case DAY -> date = date.plusDays(otherCost.getRecurrenceMultiplier());
                        case MONTH -> date = date.plusMonths(otherCost.getRecurrenceMultiplier());
                        case QUARTER -> date = date.plusMonths(otherCost.getRecurrenceMultiplier() * 3);
                        case YEAR -> date = date.plusYears(otherCost.getRecurrenceMultiplier());
                    }
                }

                if (otherCost.getMileage() != null && otherCost.getMileage() > -1) {
                    endMileage = Math.max(endMileage, otherCost.getMileage());
                }

                if (startDate.isAfter(otherCost.getDate().getTime())) {
                    startDate = new DateTime(otherCost.getDate());
                }
            }

            for (TireList tireList : tireLists) {
                if (tireList.getPrice() == 0.0f) {
                    continue;
                }
                totalCosts += tireList.getPrice();

                DateTime date = new DateTime(tireList.getBuyDate());
                monthReportData.add(date, tireList.getPrice());
                yearReportData.add(date, tireList.getPrice());

                if (date.isAfter(lastYearDate) && date.isBefore(now.plusSeconds(1))) {
                    costsWithinYear += tireList.getPrice();
                }

                if (startDate.isAfter(date)) {
                    startDate = date;
                }
            }

            // Calculate averages
            Seconds elapsedSeconds = Seconds.secondsBetween(startDate, endDate);
            double costsPerSecond = totalCosts / Math.max(1, elapsedSeconds.getSeconds());

            section.addItem(new Item("Ø " + mContext.getString(R.string.report_day),
                    mContext.getString((elapsedSeconds.getSeconds() > DAY_SECONDS ?
                                    R.string.report_price : R.string.report_price_estimated),
                            costsPerSecond * DAY_SECONDS, mUnit)));

            section.addItem(new Item("Ø " + mContext.getString(R.string.report_month),
                    mContext.getString((elapsedSeconds.getSeconds() > MONTH_SECONDS ?
                                    R.string.report_price : R.string.report_price_estimated),
                            costsPerSecond * MONTH_SECONDS, mUnit)));

            section.addItem(new Item("Ø " + mContext.getString(R.string.report_year),
                    mContext.getString((elapsedSeconds.getSeconds() > YEAR_SECONDS ?
                                    R.string.report_price : R.string.report_price_estimated),
                            costsPerSecond * YEAR_SECONDS, mUnit)));

            int mileageDiff = Math.max(1, endMileage - startMileage);
            section.addItem(new Item("Ø " + prefs.getUnitDistance(),
                    mContext.getString(R.string.report_price, totalCosts / mileageDiff, mUnit)));

            section.addItem(new Item(mContext.getString(R.string.report_last_year),
                    mContext.getString(R.string.report_price, costsWithinYear, mUnit)));

            section.addItem(new Item(mContext.getString(R.string.report_since,
                    DateFormat.getDateFormat(mContext).format(startDate.toDate())),
                    mContext.getString(R.string.report_price, totalCosts, mUnit)));
        }
    }
}
