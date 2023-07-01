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
import android.database.Cursor;
import androidx.collection.LongSparseArray;
import android.text.format.DateFormat;

import org.joda.time.DateTime;
import org.joda.time.Seconds;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.juanro.autumandu.Preferences;
import org.juanro.autumandu.R;
import org.juanro.autumandu.data.balancing.BalancedRefuelingCursor;
import org.juanro.autumandu.data.balancing.RefuelingBalancer;
import org.juanro.autumandu.provider.car.CarColumns;
import org.juanro.autumandu.provider.car.CarCursor;
import org.juanro.autumandu.provider.car.CarSelection;
import org.juanro.autumandu.provider.othercost.OtherCostCursor;
import org.juanro.autumandu.provider.othercost.OtherCostSelection;
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
        private int mOption;

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
                costs += getYValues().get(index);
                set(index, dateValue, costs, makeTooltip(costs, dateValue));
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

    private LongSparseArray<ReportChartData> mCostsPerMonth = new LongSparseArray<>();
    private LongSparseArray<ReportChartData> mCostsPerYear = new LongSparseArray<>();
    private String mUnit;
    private String[] mXLabelFormat = new String[2];

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
        for (int i = 0; i < mCostsPerMonth.size(); i++) {
            ReportChartData carData = chartOption == GRAPH_OPTION_MONTH
                    ? mCostsPerMonth.get(mCostsPerMonth.keyAt(i))
                    : mCostsPerYear.get(mCostsPerYear.keyAt(i));
            if (!carData.isEmpty()) {
                data.add(carData);
            }
        }

        return data;
    }

    @Override
    protected Cursor[] onUpdate() {
        Preferences prefs = new Preferences(mContext);
        mUnit = prefs.getUnitCurrency();

        // Settings, which are based on the screen size.
        if (mContext.getResources().getConfiguration().smallestScreenWidthDp > 480) {
            mXLabelFormat[GRAPH_OPTION_MONTH] = "MMMM yyyy";
            mXLabelFormat[GRAPH_OPTION_YEAR] = "yyyy";
        } else {
            mXLabelFormat[GRAPH_OPTION_MONTH] = "MMM yyyy";
            mXLabelFormat[GRAPH_OPTION_YEAR] = "yyyy";
        }

        ArrayList<Cursor> cursors = new ArrayList<>();

        RefuelingBalancer balancer = new RefuelingBalancer(mContext);
        CarCursor car = new CarSelection().query(mContext.getContentResolver(), null, CarColumns.NAME + " COLLATE UNICODE");
        cursors.add(car);
        while (car.moveToNext()) {
            Section section;
            if (car.getSuspendedSince() != null) {
                section = addDataSection(String.format("%s [%s]", car.getName(),
                        mContext.getString(R.string.suspended)), car.getColor(), 1);
            } else {
                section = addDataSection(car.getName(), car.getColor());
            }

            mCostsPerMonth.put(car.getId(), new ReportChartData(mContext, car.getName(),
                    car.getColor(), GRAPH_OPTION_MONTH));
            mCostsPerYear.put(car.getId(), new ReportChartData(mContext, car.getName(),
                    car.getColor(), GRAPH_OPTION_YEAR));

            final int startMileage = car.getInitialMileage();
            int endMileage = Integer.MIN_VALUE;
            DateTime startDate = new DateTime();
            DateTime endDate;
            if (car.getSuspendedSince() != null) {
                endDate = new DateTime(car.getSuspendedSince());
            } else {
                endDate = new DateTime();
            }

            BalancedRefuelingCursor refueling = balancer.getBalancedRefuelings(car.getId());
            cursors.add(refueling);
            OtherCostCursor otherCost = new OtherCostSelection().carId(car.getId()).query(mContext.getContentResolver());
            cursors.add(otherCost);

            if ((refueling.getCount() + otherCost.getCount()) < 2) {
                section.addItem(new Item(mContext.getString(R.string.report_not_enough_data), ""));
                continue;
            }

            double totalCosts = 0;
            double costsWithinYear = 0;

            while (refueling.moveToNext()) {
                if (refueling.getPrice() == 0.0f) {
                    continue;
                }
                totalCosts += refueling.getPrice();

                DateTime date = new DateTime(refueling.getDate());
                mCostsPerMonth.get(car.getId()).add(date, refueling.getPrice());
                mCostsPerYear.get(car.getId()).add(date, refueling.getPrice());

                if (Seconds.secondsBetween(endDate, date).getSeconds() < YEAR_SECONDS) {
                    costsWithinYear += refueling.getPrice();
                }

                endMileage = Math.max(endMileage, refueling.getMileage());
                if (startDate.isAfter(date)) {
                    startDate = date;
                }
            }

            Date now = new Date();

            while (otherCost.moveToNext()) {
                int recurrences;
                int recurrencesInLastYear;
                if (otherCost.getEndDate() == null ||
                        new DateTime(otherCost.getEndDate()).isAfterNow()) {
                    recurrences = Recurrences.getRecurrencesSince(
                            otherCost.getRecurrenceInterval(),
                            otherCost.getRecurrenceMultiplier(),
                            otherCost.getDate());
                    recurrencesInLastYear = Recurrences.getRecurrencesSince(
                            otherCost.getRecurrenceInterval(),
                            otherCost.getRecurrenceMultiplier(),
                            otherCost.getDate(),
                            new Date(now.getTime() - YEAR_SECONDS * 1000));
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
                            new Date(now.getTime() - YEAR_SECONDS * 1000),
                            otherCost.getEndDate());
                }

                totalCosts += otherCost.getPrice() * recurrences;
                costsWithinYear += otherCost.getPrice() * recurrencesInLastYear;

                DateTime date = new DateTime(otherCost.getDate());
                DateTime recurrenceEndDate;
                if (otherCost.getEndDate() != null && endDate.isAfter(otherCost.getEndDate().getTime())) {
                    recurrenceEndDate = new DateTime(otherCost.getEndDate());
                } else {
                    recurrenceEndDate = endDate;
                }

                while (date.isBefore(recurrenceEndDate)) {
                    mCostsPerMonth.get(car.getId()).add(date, otherCost.getPrice());
                    mCostsPerYear.get(car.getId()).add(date, otherCost.getPrice());
                    switch (otherCost.getRecurrenceInterval()) {
                        case ONCE:
                            // Set date after now, so the loop ends.
                            date = DateTime.now().plusYears(1);
                            break;
                        case DAY:
                            date = date.plusDays(otherCost.getRecurrenceMultiplier());
                            break;
                        case MONTH:
                            date = date.plusMonths(otherCost.getRecurrenceMultiplier());
                            break;
                        case QUARTER:
                            date = date.plusMonths(otherCost.getRecurrenceMultiplier() * 3);
                            break;
                        case YEAR:
                            date = date.plusYears(otherCost.getRecurrenceMultiplier());
                            break;
                    }
                }

                if (otherCost.getMileage() != null && otherCost.getMileage() > -1) {
                    endMileage = Math.max(endMileage, otherCost.getMileage());
                }

                if (startDate.isAfter(otherCost.getDate().getTime())) {
                    startDate = new DateTime(otherCost.getDate());
                }
            }

            // Calculate averages
            Seconds elapsedSeconds = Seconds.secondsBetween(startDate, endDate);
            double costsPerSecond = totalCosts / elapsedSeconds.getSeconds();

            // Average costs per day
            section.addItem(new Item("\u00D8 " + mContext.getString(R.string.report_day),
                    mContext.getString((elapsedSeconds.getSeconds() > DAY_SECONDS ?
                                    R.string.report_price : R.string.report_price_estimated),
                            costsPerSecond * DAY_SECONDS, mUnit)));

            // Average costs per month
            section.addItem(new Item("\u00D8 " + mContext.getString(R.string.report_month),
                    mContext.getString((elapsedSeconds.getSeconds() > MONTH_SECONDS ?
                                    R.string.report_price : R.string.report_price_estimated),
                            costsPerSecond * MONTH_SECONDS, mUnit)));

            // Average costs per year
            section.addItem(new Item("\u00D8 " + mContext.getString(R.string.report_year),
                    mContext.getString((elapsedSeconds.getSeconds() > YEAR_SECONDS ?
                                    R.string.report_price : R.string.report_price_estimated),
                            costsPerSecond * YEAR_SECONDS, mUnit)));

            // Average costs per [distance unit]
            int mileageDiff = Math.max(1, endMileage - startMileage);
            section.addItem(new Item("\u00D8 " + prefs.getUnitDistance(),
                    mContext.getString(R.string.report_price, totalCosts / mileageDiff, mUnit)));

            // Total costs in last year
            section.addItem(new Item(mContext.getString(R.string.report_last_year),
                    mContext.getString(R.string.report_price, costsWithinYear, mUnit)));

            // Total costs
            section.addItem(new Item(mContext.getString(R.string.report_since,
                    DateFormat.getDateFormat(mContext).format(startDate.toDate())),
                    mContext.getString(R.string.report_price, totalCosts, mUnit)));
        }

        return cursors.toArray(new Cursor[cursors.size()]);
    }
}
